package com.shopmate.domain.repository

import com.shopmate.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAll(): Flow<List<Category>>
    suspend fun add(category: Category)
    suspend fun update(category: Category)
    suspend fun delete(id: String)
}
