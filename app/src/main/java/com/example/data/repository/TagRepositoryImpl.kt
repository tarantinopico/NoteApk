package com.example.data.repository

import com.example.data.local.dao.TagDao
import com.example.data.local.entity.TagEntity
import com.example.domain.model.Tag
import com.example.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao
) : TagRepository {

    override fun getAllTags(): Flow<List<Tag>> {
        return tagDao.getAllTags().map { entities ->
            entities.map { entity ->
                Tag(
                    id = entity.id,
                    name = entity.name,
                    color = entity.color
                )
            }
        }
    }

    override suspend fun createTag(name: String, color: Long): Tag {
        val id = UUID.randomUUID().toString()
        val entity = TagEntity(id = id, name = name, color = color)
        tagDao.insertTag(entity)
        return Tag(id, name, color)
    }
}
