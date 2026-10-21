package com.shopmate.data.repository

import com.shopmate.data.local.dao.CategoryDao
import com.shopmate.data.mapper.toDomain
import com.shopmate.data.mapper.toEntity
import com.shopmate.domain.model.Category
import com.shopmate.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoryRepository {

    override fun getAll(): Flow<List<Category>> =
        categoryDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun add(category: Category) =
        categoryDao.insert(category.toEntity())

    override suspend fun update(category: Category) =
        categoryDao.update(category.toEntity())

    override suspend fun delete(id: String) =
        categoryDao.delete(id)
}
