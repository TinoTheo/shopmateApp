package com.shopmate.ui.screen.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.R
import com.shopmate.domain.model.Licence
import com.shopmate.domain.model.LicencePlan
import com.shopmate.domain.model.LicenceState
import com.shopmate.domain.repository.StaffMember
import com.shopmate.domain.repository.StaffRole
import com.shopmate.domain.repository.TenancyRefusal
import com.shopmate.ui.components.ShopMateFilterChip
import com.shopmate.ui.components.ShopMateTextButton
import com.shopmate.ui.components.ShopMateTextField
import com.shopmate.ui.theme.ErrorRed
import com.shopmate.ui.theme.NumberStyle
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackSecondary
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.StockOrange
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.DateFormatter
import java.time.ZoneId

/**
 * Settings › Plan and Staff, plus "Add a store" (CLAUDE.md § Tenancy, staff and licensing).
 * A cashier sees the plan line and nothing to manage.
 */
@Composable
fun TeamSection(
    state: TeamUiState,
    onInvite: (StaffRole, Boolean) -> Unit,
    onDismissInvite: () -> Unit,
    onChangeRole: (StaffMember, StaffRole) -> Unit,
    onSetStockRights: (StaffMember, Boolean) -> Unit,
    onRemove: (StaffMember) -> Unit,
    onAddStore: (String) -> Unit,
) {
    var showInvite by remember { mutableStateOf(false) }
    var showAddStore by remember { mutableStateOf(false) }
    var openMember by remember { mutableStateOf<StaffMember?>(null) }

    Column {
        state.licence?.let { PlanBlock(it) }

        if (state.canManageTeam) {
            ShopMateTextButton(
                text = stringResource(R.string.team_add_store),
                onClick = { showAddStore = true },
            )
        }

        SectionHeader(stringResource(R.string.settings_section_staff))
        if (!state.staffLoaded) {
            Text(
                text = stringResource(R.string.team_refused_offline),
                style = MaterialTheme.typography.bodyMedium,
                color = ShopBlackTertiary,
            )
        } else if (!state.canManageTeam) {
            Text(
                text = stringResource(R.string.team_cashier_note),
                style = MaterialTheme.typography.bodyMedium,
                color = ShopBlackTertiary,
            )
        } else {
            state.staff.forEach { member ->
                StaffRow(
                    member = member,
                    isMe = member.userId == state.myUserId,
                    // Owners manage everyone but themselves; nobody manages the owner.
                    onClick = if (state.isOwner && member.role != StaffRole.OWNER) {
                        { openMember = member }
                    } else {
                        null
                    },
                )
            }
            ShopMateTextButton(
                text = stringResource(R.string.team_invite),
                onClick = { showInvite = true },
                enabled = !state.busy,
            )
        }

        state.notice?.let { NoticeText(it, state.licence) }
    }

    if (showInvite) {
        InviteDialog(
            storeName = state.activeStoreName,
            ownerInviting = state.isOwner,
            onDismiss = { showInvite = false },
            onCreate = { role, stock ->
                showInvite = false
                onInvite(role, stock)
            },
        )
    }
    state.newInvite?.let { invite ->
        InviteCodeDialog(code = invite.code, role = invite.role, storeName = invite.storeName, onDismiss = onDismissInvite)
    }
    if (showAddStore) {
        AddStoreDialog(
            onDismiss = { showAddStore = false },
            onAdd = {
                showAddStore = false
                onAddStore(it)
            },
        )
    }
    openMember?.let { member ->
        MemberDialog(
            member = member,
            activeStoreId = state.activeStoreId,
            onDismiss = { openMember = null },
            onChangeRole = { onChangeRole(member, it); openMember = null },
            onSetStockRights = { onSetStockRights(member, it); openMember = null },
            onRemove = { onRemove(member); openMember = null },
        )
    }
}

/** "Trial · ends 22 Nov" and "1 of 1 stores · 2 of 3 staff", with the licence state in words. */
@Composable
private fun PlanBlock(licence: Licence) {
    SectionHeader(stringResource(R.string.team_section_plan))
    val ends = DateFormatter.formatShortDate(licence.expiresAt.atZone(ZoneId.systemDefault()).toLocalDate())
    Text(
        text = stringResource(R.string.team_plan_line, stringResource(licence.plan.labelRes()), ends),
        style = MaterialTheme.typography.bodyLarge,
        color = ShopBlack,
    )
    Text(
        text = stringResource(
            R.string.team_plan_usage,
            pluralStringResource(R.plurals.team_stores_used, licence.maxStores, licence.storesUsed, licence.maxStores),
            pluralStringResource(R.plurals.team_staff_used, licence.maxStaff, licence.staffUsed, licence.maxStaff),
        ),
        style = NumberStyle(13.sp),
        color = ShopBlackSecondary,
        modifier = Modifier.padding(top = 2.dp),
    )
    licenceStateText(licence.state())?.let { (text, color) ->
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Ending and grace are warnings (StockOrange); expired is stopped (ShopBlack) — never ErrorRed. */
@Composable
fun licenceStateText(state: LicenceState): Pair<String, androidx.compose.ui.graphics.Color>? = when (state) {
    LicenceState.Active -> null
    is LicenceState.Ending ->
        pluralStringResource(R.plurals.licence_ending, state.daysLeft, state.daysLeft) to StockOrange
    is LicenceState.Grace ->
        pluralStringResource(R.plurals.licence_grace, state.daysLeft, state.daysLeft) to StockOrange
    LicenceState.Expired -> stringResource(R.string.licence_expired) to ShopBlack
}

@Composable
private fun StaffRow(member: StaffMember, isMe: Boolean, onClick: (() -> Unit)?) {
    val stores = member.stores.joinToString(", ") { it.name }
    val stock = member.role == StaffRole.CASHIER && member.stores.any { it.canManageStock }
    SettingsRow(
        title = if (isMe) stringResource(R.string.team_me, member.email) else member.email,
        description = listOfNotNull(
            stringResource(member.role.labelRes()),
            stores.takeIf { it.isNotBlank() },
            stringResource(R.string.team_can_move_stock).takeIf { stock },
        ).joinToString(" · "),
        onClick = onClick,
    )
}

@Composable
private fun NoticeText(notice: TeamNotice, licence: Licence?) {
    val text = when (notice) {
        is TeamNotice.StoreAdded -> stringResource(R.string.team_store_added, notice.name)
        is TeamNotice.Removed -> stringResource(R.string.team_removed, notice.email)
        is TeamNotice.Refused -> when (notice.reason) {
            TenancyRefusal.STORE_LIMIT -> pluralStringResource(
                R.plurals.team_refused_store_limit, licence?.maxStores ?: 1, licence?.maxStores ?: 1,
            )
            TenancyRefusal.STAFF_LIMIT -> pluralStringResource(
                R.plurals.team_refused_staff_limit, licence?.maxStaff ?: 0, licence?.maxStaff ?: 0,
            )
            TenancyRefusal.LICENCE_EXPIRED -> stringResource(R.string.team_refused_licence)
            TenancyRefusal.NOT_ALLOWED -> stringResource(R.string.team_refused_not_allowed)
            TenancyRefusal.OFFLINE -> stringResource(R.string.team_refused_offline)
            TenancyRefusal.INVITE_NOT_FOUND, TenancyRefusal.OTHER -> stringResource(R.string.team_refused_other)
        }
    }
    val failed = notice is TeamNotice.Refused
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (failed) ShopBlack else ShopBlackSecondary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun InviteDialog(
    storeName: String,
    ownerInviting: Boolean,
    onDismiss: () -> Unit,
    onCreate: (StaffRole, Boolean) -> Unit,
) {
    var role by remember { mutableStateOf(StaffRole.CASHIER) }
    var stock by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.team_invite_title, storeName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShopMateFilterChip(
                        label = stringResource(R.string.role_cashier),
                        selected = role == StaffRole.CASHIER,
                        onClick = { role = StaffRole.CASHIER },
                    )
                    // Managers invite cashiers only.
                    if (ownerInviting) {
                        ShopMateFilterChip(
                            label = stringResource(R.string.role_manager),
                            selected = role == StaffRole.MANAGER,
                            onClick = { role = StaffRole.MANAGER },
                        )
                    }
                }
                if (role == StaffRole.CASHIER) {
                    StockRightsSwitch(checked = stock, onCheckedChange = { stock = it })
                } else {
                    Text(
                        text = stringResource(R.string.team_manager_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = ShopBlackSecondary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(role, role == StaffRole.CASHIER && stock) }) {
                Text(stringResource(R.string.team_invite_create), color = TradeGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = ShopBlackTertiary) }
        },
    )
}

@Composable
private fun StockRightsSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.team_stock_rights), style = MaterialTheme.typography.bodyMedium, color = ShopBlack)
            Text(
                stringResource(R.string.team_stock_rights_detail),
                style = MaterialTheme.typography.bodySmall,
                color = ShopBlackSecondary,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = TradeGreen),
        )
    }
}

/** The code, large, and a way to send it. Sharing is the owner's choice of app. */
@Composable
private fun InviteCodeDialog(code: String, role: StaffRole, storeName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val roleLabel = stringResource(role.labelRes())
    val message = stringResource(R.string.team_invite_share_text, storeName, roleLabel.lowercase(), code)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.team_code_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = code.chunked(3).joinToString(" "),
                    style = NumberStyle(34.sp, FontWeight.SemiBold),
                    color = ShopBlack,
                )
                Text(
                    text = stringResource(R.string.team_code_detail, roleLabel.lowercase(), storeName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShopBlackSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                context.startActivity(Intent.createChooser(send, null))
            }) { Text(stringResource(R.string.team_code_share), color = TradeGreen) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.team_code_done), color = ShopBlackTertiary) }
        },
    )
}

@Composable
private fun AddStoreDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.team_add_store)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShopMateTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.onboard_shop_name))
                Text(
                    text = stringResource(R.string.team_add_store_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = ShopBlackSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.team_add_store_confirm), color = TradeGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = ShopBlackTertiary) }
        },
    )
}

@Composable
private fun MemberDialog(
    member: StaffMember,
    activeStoreId: String?,
    onDismiss: () -> Unit,
    onChangeRole: (StaffRole) -> Unit,
    onSetStockRights: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    var confirmRemove by remember { mutableStateOf(false) }
    val stockHere = member.stores.firstOrNull { it.storeId == activeStoreId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(member.email) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(StaffRole.CASHIER, StaffRole.MANAGER).forEach { role ->
                        ShopMateFilterChip(
                            label = stringResource(role.labelRes()),
                            selected = member.role == role,
                            onClick = { if (member.role != role) onChangeRole(role) },
                        )
                    }
                }
                if (member.role == StaffRole.CASHIER && stockHere != null) {
                    StockRightsSwitch(checked = stockHere.canManageStock, onCheckedChange = onSetStockRights)
                }
                if (confirmRemove) {
                    Text(
                        text = stringResource(R.string.team_remove_confirm, member.email),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShopBlack,
                    )
                }
            }
        },
        confirmButton = {
            if (confirmRemove) {
                TextButton(onClick = onRemove) { Text(stringResource(R.string.team_remove), color = ErrorRed) }
            } else {
                TextButton(onClick = { confirmRemove = true }) {
                    Text(stringResource(R.string.team_remove), color = ShopBlackSecondary)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.team_code_done), color = TradeGreen) }
        },
    )
}

private fun StaffRole.labelRes(): Int = when (this) {
    StaffRole.OWNER -> R.string.role_owner
    StaffRole.MANAGER -> R.string.role_manager
    StaffRole.CASHIER -> R.string.role_cashier
}

private fun LicencePlan.labelRes(): Int = when (this) {
    LicencePlan.TRIAL -> R.string.plan_trial
    LicencePlan.BASIC -> R.string.plan_basic
    LicencePlan.PRO -> R.string.plan_pro
}
