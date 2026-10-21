package com.shopmate.data.mapper

import com.shopmate.data.local.entity.CategoryEntity
import com.shopmate.domain.model.Category

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    colour = colour,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    colour = colour,
)
