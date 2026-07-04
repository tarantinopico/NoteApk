package com.example.data.repository

import com.example.data.local.dao.FolderDao
import com.example.data.local.entity.FolderEntity
import com.example.domain.model.Folder
import com.example.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao
) : FolderRepository {

    override fun getAllFolders(): Flow<List<Folder>> {
        return folderDao.getAllFolders().map { entities ->
            entities.map { entity ->
                Folder(
                    id = entity.id,
                    path = entity.path,
                    name = entity.name,
                    parentId = entity.parentId
                )
            }
        }
    }

    override suspend fun createFolder(name: String, parentId: String?): Folder {
        val id = UUID.randomUUID().toString()
        val path = if (parentId != null) "$parentId/$name" else name // Simplified path
        val entity = FolderEntity(id = id, path = path, name = name, parentId = parentId)
        folderDao.insertFolder(entity)
        return Folder(id, path, name, parentId)
    }

    override suspend fun deleteFolder(folder: Folder) {
        val entity = FolderEntity(folder.id, folder.path, folder.name, folder.parentId)
        folderDao.deleteFolder(entity)
    }
}
