package com.shopmate.ui.screen.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shopmate.R
import com.shopmate.domain.model.Category
import com.shopmate.ui.components.ConstrainedContent
import com.shopmate.ui.theme.ShopMateTheme

val categoryPresetColours = listOf(
    "#005B52", "#99FFAA", "#E57373", "#FFB74D",
    "#FFF176", "#64B5F6", "#BA68C8", "#4DB6AC",
)

fun String.toComposeColor(): Color =
    Color(android.graphics.Color.parseColor(this))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showSheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var deleteTarget by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is CategoryUiState.Success) {
            showSheet = false
            editingCategory = null
            viewModel.resetState()
        }
    }

    CategoriesContent(
        categories = categories,
        uiState = uiState,
        onAddClick = { editingCategory = null; showSheet = true },
        onEditClick = { editingCategory = it; showSheet = true },
        onDeleteClick = { deleteTarget = it },
    )

    if (showSheet) {
        CategoryBottomSheet(
            initial = editingCategory,
            isSaving = uiState is CategoryUiState.Loading,
            onDismiss = { showSheet = false; editingCategory = null; viewModel.resetState() },
            onSave = { name, colour ->
                if (editingCategory == null) viewModel.addCategory(name, colour)
                else viewModel.updateCategory(editingCategory!!.copy(name = name, colour = colour))
            },
        )
    }

    deleteTarget?.let { cat ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_category_title)) },
            text = { Text(stringResource(R.string.delete_category_message, cat.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCategory(cat.id); deleteTarget = null }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun CategoriesContent(
    categories: List<Category>,
    uiState: CategoryUiState,
    onAddClick: () -> Unit,
    onEditClick: (Category) -> Unit,
    onDeleteClick: (Category) -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_category))
            }
        }
    ) { padding ->
        if (categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text(
                        stringResource(R.string.no_categories),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.add_first_category),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            ConstrainedContent(modifier = Modifier.padding(padding)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories, key = { it.id }) { category ->
                        CategoryCard(
                            category = category,
                            onEdit = { onEditClick(category) },
                            onDelete = { onDeleteClick(category) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(runCatching { category.colour.toComposeColor() }.getOrElse { Color.Gray }),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                category.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryBottomSheet(
    initial: Category?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, colour: String) -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var selectedColour by remember(initial) { mutableStateOf(initial?.colour ?: categoryPresetColours.first()) }
    var nameError by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                if (initial == null) stringResource(R.string.add_category)
                else stringResource(R.string.edit_category),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text(stringResource(R.string.category_name)) },
                isError = nameError,
                supportingText = if (nameError) ({ Text(stringResource(R.string.field_required)) }) else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.colour), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categoryPresetColours.forEach { hex ->
                    val isSelected = hex == selectedColour
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(runCatching { hex.toComposeColor() }.getOrElse { Color.Gray })
                            .then(
                                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable { selectedColour = hex },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.isBlank()) { nameError = true; return@Button }
                        onSave(name.trim(), selectedColour)
                    },
                    enabled = !isSaving,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CategoriesEmptyPreview() {
    ShopMateTheme {
        CategoriesContent(
            categories = emptyList(),
            uiState = CategoryUiState.Idle,
            onAddClick = {},
            onEditClick = {},
            onDeleteClick = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CategoriesListPreview() {
    ShopMateTheme {
        CategoriesContent(
            categories = listOf(
                Category("1", "Beverages", "#005B52"),
                Category("2", "Snacks", "#E57373"),
                Category("3", "Dairy", "#64B5F6"),
            ),
            uiState = CategoryUiState.Idle,
            onAddClick = {},
            onEditClick = {},
            onDeleteClick = {},
        )
    }
}
