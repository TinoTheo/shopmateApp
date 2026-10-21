package com.shopmate.ui.screen.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopmate.R
import com.shopmate.domain.model.Product
import com.shopmate.domain.model.ProductInsight
import com.shopmate.ui.components.InsightRow
import com.shopmate.ui.components.InsightRowValue
import com.shopmate.ui.components.MarginGauge
import com.shopmate.ui.components.SectionHeader
import com.shopmate.ui.components.Severity
import com.shopmate.ui.components.StoppedChip
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackMid
import com.shopmate.ui.theme.StockOrangeSubtle
import com.shopmate.ui.util.CurrencyFormatter
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * The Products list: margin health in sections, each row the shared [InsightRow] carrying a
 * [MarginGauge] in the same slot Stock puts a runway in. One row component, two units.
 */
@Composable
fun ProductInsightList(
    sections: Map<ProductSection, List<ProductInsight>>,
    targetMargin: Double,
    unknownBarcodeCount: Int,
    onProductClick: (Product) -> Unit,
    onOpenBarcodeInbox: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        if (unknownBarcodeCount > 0) {
            item(key = "barcode-inbox") {
                BarcodeInboxCard(count = unknownBarcodeCount, onClick = onOpenBarcodeInbox)
            }
        }
        sections.forEach { (section, rows) ->
            item(key = "header-${section.name}") {
                SectionHeader(label = section.label(), count = rows.size)
            }
            items(rows, key = { it.product.id }) { insight ->
                ProductMarginRow(
                    insight = insight,
                    section = section,
                    targetMargin = targetMargin,
                    onClick = { onProductClick(insight.product) },
                )
            }
        }
    }
}

/**
 * "3 codes scanned but not named" — an invitation, not an error. They sold anyway, which is why
 * this sits on the Warning tint rather than in error styling (§ Copy and voice, § Severity).
 */
@Composable
private fun BarcodeInboxCard(count: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(StockOrangeSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(
            text = stringResource(R.string.products_unknown_codes_title, count),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ShopBlack,
        )
        Text(
            text = stringResource(R.string.products_unknown_codes_body),
            fontSize = 12.sp,
            color = ShopBlackMid,
        )
    }
}

@Composable
private fun ProductMarginRow(
    insight: ProductInsight,
    section: ProductSection,
    targetMargin: Double,
    onClick: () -> Unit,
) {
    InsightRow(
        name = insight.product.name,
        meta = insight.marginMeta(),
        severity = when (section) {
            ProductSection.SellingAtALoss -> Severity.Stopped
            ProductSection.CostWentUp, ProductSection.BelowTarget -> Severity.Warning
            ProductSection.HoldingTarget -> Severity.Healthy
        },
        dormant = insight.dormant,
        gauge = { MarginGauge(margin = insight.margin, targetMargin = targetMargin) },
        trailing = {
            if (insight.sellingAtALoss) {
                StoppedChip(stringResource(R.string.products_at_a_loss))
            } else {
                InsightRowValue(
                    value = "${(insight.margin * 100).roundToInt()}%",
                    sub = insight.marginBefore
                        ?.let { stringResource(R.string.products_margin_was, (it * 100).roundToInt()) },
                )
            }
        },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

/**
 * The meta line carries the **actual movement** — "cost $1.95 → $2.12 on 28 Aug" — which is
 * exactly what `previous_cost_price` and `cost_price_changed_at` exist for. Without the
 * movement the row can only state today's margin and never why it changed.
 */
@Composable
private fun ProductInsight.marginMeta(): String {
    val previous = product.previousCostPrice
    val changedAt = product.costPriceChangedAt
    return when {
        previous != null && changedAt != null && costRose -> stringResource(
            R.string.products_cost_moved,
            CurrencyFormatter.format(previous),
            CurrencyFormatter.format(product.costPrice),
            DATE.format(changedAt.atZone(ZoneId.systemDefault())),
        )
        sellingAtALoss -> stringResource(
            R.string.products_meta_below_cost,
            CurrencyFormatter.format(product.costPrice),
        )
        dormant -> stringResource(R.string.stock_meta_dormant, daysSinceLastSale ?: 0)
        else -> stringResource(
            R.string.products_meta_cost,
            CurrencyFormatter.format(product.costPrice),
        )
    }
}

private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

@Composable
private fun ProductSection.label(): String = stringResource(
    when (this) {
        ProductSection.CostWentUp -> R.string.products_section_cost_up
        ProductSection.SellingAtALoss -> R.string.products_section_at_a_loss
        ProductSection.HoldingTarget -> R.string.products_section_holding_target
        ProductSection.BelowTarget -> R.string.products_section_below_target
    }
)
