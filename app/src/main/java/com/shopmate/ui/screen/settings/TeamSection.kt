package com.shopmate.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.R
import com.shopmate.domain.model.Licence
import com.shopmate.domain.model.LicencePlan
import com.shopmate.domain.model.LicenceState
import com.shopmate.domain.repository.StaffRole
import com.shopmate.domain.repository.TenancyRefusal
import com.shopmate.domain.repository.TillStaff
import com.shopmate.domain.util.PinHasher
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
 * Settings › Plan, Add a store and Staff (CLAUDE.md § Tenancy, § Staff, § Licensing). Only the
 * owner at the till manages anything; everyone else sees the plan line.
 */
@Composable
fun TeamSection(
    state: TeamUiState,
    onSetOwnerPin: (String, String) -> Unit,
    onAddStaff: (String, StaffRole, String, Boolean) -> Unit,
    onUpdate: (TillStaff, StaffRole, Boolean) -> Unit,
    onChangePin: (TillStaff, String) -> Unit,
    onRemove: (TillStaff) -> Unit,
    onAddStore: (String) -> Unit,
) {
    var showOwnerPin by remember { mutableStateOf(false) }
    var showAddStaff by remember { mutableStateOf(false) }
    var showAddStore by remember { mutableStateOf(false) }
    var openMember by remember { mutableStateOf<TillStaff?>(null) }

    Column {
        state.licence?.let { PlanBlock(it) }
        if (!state.ownerAtTill) return@Column

        ShopMateTextButton(text = stringResource(R.string.team_add_store), onClick = { showAddStore = true })
        // Each notice sits under the action it answers.
        state.notice?.takeIf { it.isAboutStores }?.let { NoticeText(it, state.licence) }

        SectionHeader(stringResource(R.string.settings_section_staff))
        if (!state.ownerHasPin) {
            // Before anyone else can lock the till, the owner must be able to unlock it.
            Text(
                text = stringResource(R.string.staff_owner_pin_first),
                style = MaterialTheme.typography.bodyMedium,
                color = ShopBlackSecondary,
            )
            ShopMateTextButton(text = stringResource(R.string.staff_set_owner_pin), onClick = { showOwnerPin = true })
        } else {
            state.staff.forEach { member ->
                StaffRow(member = member, onClick = { openMember = member })
            }
            ShopMateTextButton(
                text = stringResource(R.string.staff_add),
                onClick = { showAddStaff = true },
                enabled = !state.busy,
            )
        }
        state.notice?.takeUnless { it.isAboutStores }?.let { NoticeText(it, state.licence) }
    }

    if (showOwnerPin) {
        PinEntryDialog(
            title = stringResource(R.string.staff_owner_pin_title),
            askName = true,
            onDismiss = { showOwnerPin = false },
            onSave = { name, pin ->
                showOwnerPin = false
                onSetOwnerPin(name, pin)
            },
        )
    }
    if (showAddStaff) {
        AddStaffDialog(
            storeName = state.activeStoreName,
            onDismiss = { showAddStaff = false },
            onAdd = { name, role, pin, stock ->
                showAddStaff = false
                onAddStaff(name, role, pin, stock)
            },
        )
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
            onDismiss = { openMember = null },
            onUpdate = { role, stock -> onUpdate(member, role, stock); openMember = null },
            onChangePin = { pin -> onChangePin(member, pin); openMember = null },
            onRemove = { onRemove(member); openMember = null },
        )
    }
}

/** "Free trial · ends 22 Nov" and "1 of 1 store · 2 of 3 staff", with the licence state in words. */
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
fun licenceStateText(state: LicenceState): Pair<String, Color>? = when (state) {
    LicenceState.Active -> null
    is LicenceState.Ending ->
        pluralStringResource(R.plurals.licence_ending, state.daysLeft, state.daysLeft) to StockOrange
    is LicenceState.Grace ->
        pluralStringResource(R.plurals.licence_grace, state.daysLeft, state.daysLeft) to StockOrange
    LicenceState.Expired -> stringResource(R.string.licence_expired) to ShopBlack
}

@Composable
private fun StaffRow(member: TillStaff, onClick: () -> Unit) {
    SettingsRow(
        title = member.name,
        description = listOfNotNull(
            stringResource(member.role.labelRes()),
            stringResource(R.string.staff_can_move_stock).takeIf { member.role == StaffRole.CASHIER && member.canManageStock },
        ).joinToString(" · "),
        onClick = onClick,
    )
}

private val TeamNotice.isAboutStores: Boolean
    get() = this is TeamNotice.StoreAdded ||
        (this is TeamNotice.Refused && reason == TenancyRefusal.STORE_LIMIT)

@Composable
private fun NoticeText(notice: TeamNotice, licence: Licence?) {
    val text = when (notice) {
        is TeamNotice.StoreAdded -> stringResource(R.string.team_store_added, notice.name)
        is TeamNotice.StaffAdded -> stringResource(R.string.staff_added, notice.name)
        is TeamNotice.Removed -> stringResource(R.string.staff_removed, notice.name)
        TeamNotice.PinChanged -> stringResource(R.string.staff_pin_saved)
        is TeamNotice.Refused -> when (notice.reason) {
            TenancyRefusal.STORE_LIMIT -> pluralStringResource(
                R.plurals.team_refused_store_limit, licence?.maxStores ?: 1, licence?.maxStores ?: 1,
            )
            TenancyRefusal.STAFF_LIMIT -> pluralStringResource(
                R.plurals.staff_refused_limit, licence?.maxStaff ?: 0, licence?.maxStaff ?: 0,
            )
            TenancyRefusal.NAME_TAKEN -> stringResource(R.string.staff_refused_name_taken)
            TenancyRefusal.LICENCE_EXPIRED -> stringResource(R.string.team_refused_licence)
            TenancyRefusal.OFFLINE -> stringResource(R.string.team_refused_offline)
            TenancyRefusal.NOT_ALLOWED, TenancyRefusal.OTHER -> stringResource(R.string.team_refused_other)
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (notice is TeamNotice.Refused) ShopBlack else ShopBlackSecondary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** Four digits, masked, number keyboard. */
@Composable
private fun PinField(value: String, onValueChange: (String) -> Unit) {
    ShopMateTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(PinHasher.PIN_LENGTH)) },
        label = stringResource(R.string.staff_pin),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
    )
}

@Composable
private fun PinEntryDialog(
    title: String,
    askName: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, pin: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    val ready = PinHasher.isValidPin(pin) && (!askName || name.isNotBlank())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (askName) {
                    ShopMateTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.staff_your_name))
                }
                PinField(value = pin, onValueChange = { pin = it })
                Text(
                    text = stringResource(R.string.staff_pin_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = ShopBlackSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, pin) }, enabled = ready) {
                Text(stringResource(R.string.staff_save_pin), color = if (ready) TradeGreen else ShopBlackTertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = ShopBlackTertiary) }
        },
    )
}

@Composable
private fun RoleChips(role: StaffRole, onRole: (StaffRole) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(StaffRole.CASHIER, StaffRole.MANAGER).forEach { option ->
            ShopMateFilterChip(
                label = stringResource(option.labelRes()),
                selected = role == option,
                onClick = { onRole(option) },
            )
        }
    }
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

@Composable
private fun AddStaffDialog(
    storeName: String,
    onDismiss: () -> Unit,
    onAdd: (String, StaffRole, String, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(StaffRole.CASHIER) }
    var pin by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf(false) }
    val ready = name.isNotBlank() && PinHasher.isValidPin(pin)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.staff_add_title, storeName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ShopMateTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.staff_name))
                RoleChips(role = role, onRole = { role = it })
                PinField(value = pin, onValueChange = { pin = it })
                Text(
                    text = stringResource(R.string.staff_pin_hint_other),
                    style = MaterialTheme.typography.bodySmall,
                    color = ShopBlackSecondary,
                )
                if (role == StaffRole.CASHIER) StockRightsSwitch(checked = stock, onCheckedChange = { stock = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, role, pin, stock) }, enabled = ready) {
                Text(stringResource(R.string.staff_add_confirm), color = if (ready) TradeGreen else ShopBlackTertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = ShopBlackTertiary) }
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

/** The owner's own row only changes its PIN; a staff row changes role, stock rights and PIN. */
@Composable
private fun MemberDialog(
    member: TillStaff,
    onDismiss: () -> Unit,
    onUpdate: (StaffRole, Boolean) -> Unit,
    onChangePin: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val isOwner = member.role == StaffRole.OWNER
    var role by remember { mutableStateOf(member.role) }
    var stock by remember { mutableStateOf(member.canManageStock) }
    var pin by remember { mutableStateOf("") }
    var confirmRemove by remember { mutableStateOf(false) }
    val changed = !isOwner && (role != member.role || stock != member.canManageStock)
    val pinReady = PinHasher.isValidPin(pin)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(member.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isOwner) {
                    RoleChips(role = role, onRole = { role = it })
                    if (role == StaffRole.CASHIER) StockRightsSwitch(checked = stock, onCheckedChange = { stock = it })
                }
                Text(
                    text = stringResource(R.string.staff_new_pin),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ShopBlack,
                )
                PinField(value = pin, onValueChange = { pin = it })
                if (!isOwner) {
                    if (confirmRemove) {
                        Text(
                            text = stringResource(R.string.staff_remove_confirm, member.name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ShopBlack,
                        )
                        TextButton(onClick = onRemove) { Text(stringResource(R.string.team_remove), color = ErrorRed) }
                    } else {
                        TextButton(onClick = { confirmRemove = true }) {
                            Text(stringResource(R.string.staff_remove, member.name), color = ShopBlackSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pinReady) onChangePin(pin)
                    if (changed) onUpdate(role, stock)
                    if (!pinReady && !changed) onDismiss()
                },
            ) { Text(stringResource(R.string.staff_save), color = TradeGreen) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = ShopBlackTertiary) }
        },
    )
}

fun StaffRole.labelRes(): Int = when (this) {
    StaffRole.OWNER -> R.string.role_owner
    StaffRole.MANAGER -> R.string.role_manager
    StaffRole.CASHIER -> R.string.role_cashier
}

private fun LicencePlan.labelRes(): Int = when (this) {
    LicencePlan.TRIAL -> R.string.plan_trial
    LicencePlan.BASIC -> R.string.plan_basic
    LicencePlan.PRO -> R.string.plan_pro
}
