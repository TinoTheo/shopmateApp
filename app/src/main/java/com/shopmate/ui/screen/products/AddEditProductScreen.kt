package com.shopmate.ui.screen.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.shopmate.R
import com.shopmate.domain.model.Category
import com.shopmate.domain.model.Product
import com.shopmate.ui.theme.ShopMateTheme
import java.math.BigDecimal
import java.time.Instant

val UNIT_OPTIONS = listOf("pcs", "kg", "litre", "box", "pack", "dozen")

@Composable
fun AddEditProductScreen(
    navController: NavController,
    viewModel: AddEditProductViewModel = hiltViewModel(),
) {
    val product by viewModel.product.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is ProductUiState.Success) navController.navigateUp()
    }

    AddEditProductContent(
        isEditMode = viewModel.isEditMode,
        existingProduct = product,
        prefilledBarcode = viewModel.prefilledBarcode,
        categories = categories,
        isSaving = uiState is ProductUiState.Loading,
        onSave = { name, sku, barcode, catId, selling, cost, stock, threshold, unit ->
            viewModel.save(name, sku, barcode, catId, selling, cost, stock, threshold, unit)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductContent(
    isEditMode: Boolean,
    existingProduct: Product?,
    /** Carried in from the unknown-barcode inbox: the form opens with the code already filled. */
    prefilledBarcode: String? = null,
    categories: List<Category>,
    isSaving: Boolean,
    onSave: (String, String, String?, String?, BigDecimal, BigDecimal, Int, Int, String) -> Unit,
) {
    if (isEditMode && existingProduct == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Pre-capture validation strings (can't call stringResource inside onClick)
    val errRequired = stringResource(R.string.field_required)
    val errPricePositive = stringResource(R.string.price_must_be_positive)
    val errStockNonNegative = stringResource(R.string.stock_must_be_non_negative)
    val labelNone = stringResource(R.string.none)

    var name by remember(existingProduct) { mutableStateOf(existingProduct?.name ?: "") }
    var sku by remember(existingProduct) { mutableStateOf(existingProduct?.sku ?: "") }
    var barcode by remember(existingProduct) {
        mutableStateOf(existingProduct?.barcode ?: prefilledBarcode.orEmpty())
    }
    var selectedCategoryId by remember(existingProduct) { mutableStateOf(existingProduct?.categoryId) }
    var sellingPriceText by remember(existingProduct) { mutableStateOf(existingProduct?.sellingPrice?.toPlainString() ?: "") }
    var costPriceText by remember(existingProduct) { mutableStateOf(existingProduct?.costPrice?.toPlainString() ?: "") }
    var stockText by remember { mutableStateOf("0") }
    var thresholdText by remember(existingProduct) { mutableStateOf(existingProduct?.lowStockThreshold?.toString() ?: "5") }
    var selectedUnit by remember(existingProduct) { mutableStateOf(existingProduct?.unit ?: UNIT_OPTIONS.first()) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var sellingPriceError by remember { mutableStateOf<String?>(null) }
    var stockError by remember { mutableStateOf<String?>(null) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 100) { name = it; nameError = null } },
            label = { Text(stringResource(R.string.product_name)) },
            isError = nameError != null,
            supportingText = nameError?.let { err -> { Text(err) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        OutlinedTextField(
            value = sku,
            onValueChange = { if (it.length <= 50) sku = it },
            label = { Text(stringResource(R.string.sku)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        OutlinedTextField(
            value = barcode,
            onValueChange = { if (it.length <= 20 && it.all(Char::isDigit)) barcode = it },
            label = { Text(stringResource(R.string.barcode)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        )

        ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
            OutlinedTextField(
                value = categories.find { it.id == selectedCategoryId }?.name ?: labelNone,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.category)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(labelNone) },
                    onClick = { selectedCategoryId = null; categoryExpanded = false },
                )
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.name) },
                        onClick = { selectedCategoryId = cat.id; categoryExpanded = false },
                    )
                }
            }
        }

        OutlinedTextField(
            value = sellingPriceText,
            onValueChange = { sellingPriceText = it; sellingPriceError = null },
            label = { Text(stringResource(R.string.selling_price)) },
            isError = sellingPriceError != null,
            supportingText = sellingPriceError?.let { err -> { Text(err) } },
            prefix = { Text("$") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        )

        OutlinedTextField(
            value = costPriceText,
            onValueChange = { costPriceText = it },
            label = { Text(stringResource(R.string.cost_price)) },
            prefix = { Text("$") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        )

        if (!isEditMode) {
            OutlinedTextField(
                value = stockText,
                onValueChange = { if (it.all(Char::isDigit)) { stockText = it; stockError = null } },
                label = { Text(stringResource(R.string.initial_stock)) },
                isError = stockError != null,
                supportingText = stockError?.let { err -> { Text(err) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            )
        } else {
            OutlinedTextField(
                value = existingProduct?.stockQuantity?.toString() ?: "",
                onValueChange = {},
                enabled = false,
                label = { Text(stringResource(R.string.stock_quantity)) },
                supportingText = { Text(stringResource(R.string.stock_adjust_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        OutlinedTextField(
            value = thresholdText,
            onValueChange = { if (it.all(Char::isDigit)) thresholdText = it },
            label = { Text(stringResource(R.string.low_stock_threshold)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        )

        ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
            OutlinedTextField(
                value = selectedUnit,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.unit)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                UNIT_OPTIONS.forEach { unit ->
                    DropdownMenuItem(
                        text = { Text(unit) },
                        onClick = { selectedUnit = unit; unitExpanded = false },
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = {
                var valid = true
                if (name.isBlank()) { nameError = errRequired; valid = false }
                val selling = sellingPriceText.toBigDecimalOrNull()
                if (selling == null || selling <= BigDecimal.ZERO) {
                    sellingPriceError = errPricePositive; valid = false
                }
                val stock = stockText.toIntOrNull()
                if (!isEditMode && (stock == null || stock < 0)) {
                    stockError = errStockNonNegative; valid = false
                }
                if (!valid) return@Button

                onSave(
                    name.trim(),
                    sku.trim(),
                    barcode.takeIf { it.isNotBlank() },
                    selectedCategoryId,
                    selling ?: BigDecimal.ZERO,
                    costPriceText.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    stock ?: 0,
                    thresholdText.toIntOrNull() ?: 5,
                    selectedUnit,
                )
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(R.string.save))
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun AddProductPreview() {
    ShopMateTheme {
        AddEditProductContent(
            isEditMode = false,
            existingProduct = null,
            categories = listOf(
                Category("1", "Beverages", "#005B52"),
                Category("2", "Dairy", "#64B5F6"),
            ),
            isSaving = false,
            onSave = { _, _, _, _, _, _, _, _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditProductPreview() {
    val product = Product(
        id = "1", name = "Coca Cola", sku = "CC500", barcode = null,
        categoryId = "1", sellingPrice = BigDecimal("2.50"), costPrice = BigDecimal("1.50"),
        stockQuantity = 24, lowStockThreshold = 5, unit = "pcs",
        createdAt = Instant.now(), updatedAt = Instant.now(),
    )
    ShopMateTheme {
        AddEditProductContent(
            isEditMode = true,
            existingProduct = product,
            categories = listOf(Category("1", "Beverages", "#005B52")),
            isSaving = false,
            onSave = { _, _, _, _, _, _, _, _, _ -> },
        )
    }
}
