package com.shopmate.ui.screen.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shopmate.R
import com.shopmate.domain.model.UnknownBarcode
import com.shopmate.ui.components.EmptyState
import com.shopmate.ui.components.ShopMateCard
import com.shopmate.ui.components.ShopMateSecondaryButton
import com.shopmate.ui.navigation.Screen
import com.shopmate.ui.theme.NumberRowValue
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackMid
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Codes rung up at the till that aren't in the catalogue yet.
 *
 * **An unknown barcode never blocks a sale** (architecture rule 11) — it landed here so it could
 * be named later, which is why this screen reads as a to-do list and not as an error log.
 */
@Composable
fun BarcodeInboxScreen(
    navController: NavController,
    viewModel: BarcodeInboxViewModel = hiltViewModel(),
) {
    val codes by viewModel.unknownBarcodes.collectAsState()

    if (codes.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.QrCode2,
            title = stringResource(R.string.barcode_inbox_title),
            subtitle = stringResource(R.string.barcode_inbox_empty),
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(codes, key = { it.code }) { unknown ->
            UnknownBarcodeCard(
                unknown = unknown,
                // Naming it means creating the product it belongs to, with the code prefilled.
                onNameIt = { navController.navigate(Screen.productAddWithCode(unknown.code)) },
            )
        }
    }
}

@Composable
private fun UnknownBarcodeCard(unknown: UnknownBarcode, onNameIt: () -> Unit) {
    ShopMateCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = unknown.code, style = NumberRowValue, fontSize = 15.sp, color = ShopBlack)
                Text(
                    text = unknown.metaLine(),
                    fontSize = 11.5.sp,
                    color = ShopBlackMid,
                )
            }
            ShopMateSecondaryButton(
                text = stringResource(R.string.barcode_inbox_name_it),
                onClick = onNameIt,
            )
        }
    }
}

/** Hits first: three sightings of the same code is a different problem from one. */
@Composable
private fun UnknownBarcode.metaLine(): String {
    val firstSeen = DATE.format(firstSeenAt.atZone(ZoneId.systemDefault()))
    val scanned = pluralStringResource(R.plurals.barcode_scanned, hits, hits, firstSeen)
    return if (lastTill != null) stringResource(R.string.header_status_join, scanned, lastTill) else scanned
}

private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
