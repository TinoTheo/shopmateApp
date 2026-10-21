package com.shopmate.ui.screen.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shopmate.domain.model.Product

@Composable
fun ProductGrid(
    products: List<Product>,
    categoryColours: Map<String, String>,   // categoryId -> hex colour
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        // Adaptive: 2 columns on a phone (content is ~276dp wide beside the sidebar),
        // more as width allows (tablet / landscape).
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(products, key = { it.id }) { product ->
            ProductGridCard(
                product = product,
                categoryColour = product.categoryId?.let { categoryColours[it] },
                onClick = { onProductClick(product) },
            )
        }
    }
}
