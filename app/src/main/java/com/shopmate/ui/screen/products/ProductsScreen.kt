package com.shopmate.ui.screen.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shopmate.R
import com.shopmate.domain.model.Category
import com.shopmate.domain.model.Product
import com.shopmate.domain.model.ProductInsight
import com.shopmate.ui.components.ConstrainedContent
import com.shopmate.ui.components.EmptyState
import com.shopmate.ui.components.ShopMateFilterChip
import com.shopmate.ui.components.ShopMateSearchBar
import com.shopmate.ui.navigation.Screen
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopBlackTertiary
import com.shopmate.ui.theme.ShopMateTheme
import com.shopmate.ui.util.CurrencyFormatter
import java.math.BigDecimal
import java.time.Instant

@Composable
fun ProductsScreen(
    navController: NavController,
    viewModel: ProductsViewModel = hiltViewModel(),
) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val targetMargin by viewModel.targetMargin.collectAsState()
    val unknownBarcodeCount by viewModel.unknownBarcodeCount.collectAsState()

    ProductsContent(
        products = products,
        categories = categories,
        searchQuery = searchQuery,
        selectedCategoryId = selectedCategoryId,
        sections = sections,
        targetMargin = targetMargin,
        unknownBarcodeCount = unknownBarcodeCount,
        onSearchChange = viewModel::searchProducts,
        onCategorySelect = viewModel::selectCategory,
        onProductClick = { navController.navigate(Screen.productEdit(it.id)) },
        onAddClick = { navController.navigate(Screen.ProductAdd.route) },
        onOpenBarcodeInbox = { navController.navigate(Screen.BarcodeInbox.route) },
    )
}

@Composable
fun ProductsContent(
    products: List<Product>,
    categories: List<Category>,
    searchQuery: String,
    selectedCategoryId: String?,
    sections: Map<ProductSection, List<ProductInsight>>,
    targetMargin: Double,
    unknownBarcodeCount: Int,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onProductClick: (Product) -> Unit,
    onAddClick: () -> Unit,
    onOpenBarcodeInbox: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_product))
            }
        }
    ) { padding ->
        ConstrainedContent(modifier = Modifier.padding(padding)) {
            ProductsBody(
                products = products,
                categories = categories,
                searchQuery = searchQuery,
                selectedCategoryId = selectedCategoryId,
                sections = sections,
                targetMargin = targetMargin,
                unknownBarcodeCount = unknownBarcodeCount,
                onSearchChange = onSearchChange,
                onCategorySelect = onCategorySelect,
                onProductClick = onProductClick,
                onAddClick = onAddClick,
                onOpenBarcodeInbox = onOpenBarcodeInbox,
            )
        }
    }
}

@Composable
private fun ProductsBody(
    products: List<Product>,
    categories: List<Category>,
    searchQuery: String,
    selectedCategoryId: String?,
    sections: Map<ProductSection, List<ProductInsight>>,
    targetMargin: Double,
    unknownBarcodeCount: Int,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onProductClick: (Product) -> Unit,
    onAddClick: () -> Unit,
    onOpenBarcodeInbox: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ShopMateSearchBar(
            query = searchQuery,
            onQueryChange = onSearchChange,
            placeholder = stringResource(R.string.search_products),
            onClear = { onSearchChange("") },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                items(categories, key = { it.id }) { cat ->
                    ShopMateFilterChip(
                        label = cat.name,
                        selected = cat.id == selectedCategoryId,
                        onClick = {
                            onCategorySelect(if (cat.id == selectedCategoryId) null else cat.id)
                        },
                    )
                }
            }
        }

        when {
            products.isEmpty() && searchQuery.isBlank() && selectedCategoryId == null -> {
                EmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = stringResource(R.string.no_products),
                    subtitle = stringResource(R.string.add_first_product),
                    actionText = stringResource(R.string.add_product),
                    onAction = onAddClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            products.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.no_products_matching, searchQuery),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                // Margin health, not an alphabetical catalogue: Products answers "am I still
                // making money on this?", so the money at stake orders the screen.
                ProductInsightList(
                    sections = sections,
                    targetMargin = targetMargin,
                    unknownBarcodeCount = unknownBarcodeCount,
                    onProductClick = onProductClick,
                    onOpenBarcodeInbox = onOpenBarcodeInbox,
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProductsEmptyPreview() {
    ShopMateTheme {
        ProductsContent(
            products = emptyList(),
            categories = emptyList(),
            searchQuery = "",
            selectedCategoryId = null,
            sections = emptyMap(),
            targetMargin = 0.30,
            unknownBarcodeCount = 0,
            onSearchChange = {},
            onCategorySelect = {},
            onProductClick = {},
            onAddClick = {},
            onOpenBarcodeInbox = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProductsListPreview() {
    val cats = listOf(
        Category("c1", "Beverages", "#005B52"),
        Category("c2", "Dairy", "#64B5F6"),
    )
    val products = listOf(
        Product("1", "Coca Cola 500ml", "CC500", null, "c1", BigDecimal("2.50"), BigDecimal("1.50"), stockQuantity = 24, lowStockThreshold = 5, unit = "pcs", createdAt = Instant.now(), updatedAt = Instant.now()),
        Product("2", "Bread Loaf", "BRD", null, null, BigDecimal("3.99"), BigDecimal("2.00"), stockQuantity = 3, lowStockThreshold = 5, unit = "pcs", createdAt = Instant.now(), updatedAt = Instant.now()),
        Product("3", "Milk 1L", "MLK1", null, "c2", BigDecimal("1.20"), BigDecimal("0.80"), stockQuantity = 0, lowStockThreshold = 5, unit = "litre", createdAt = Instant.now(), updatedAt = Instant.now()),
    )
    ShopMateTheme {
        ProductsContent(
            products = products,
            categories = cats,
            searchQuery = "",
            selectedCategoryId = null,
            sections = emptyMap(),
            targetMargin = 0.30,
            unknownBarcodeCount = 0,
            onSearchChange = {},
            onCategorySelect = {},
            onProductClick = {},
            onAddClick = {},
            onOpenBarcodeInbox = {},
        )
    }
}
