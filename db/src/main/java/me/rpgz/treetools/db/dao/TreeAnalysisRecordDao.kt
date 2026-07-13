package me.rpgz.treetools.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import me.rpgz.treetools.db.entities.TreeAnalysisRecordEntity

@Dao
interface TreeAnalysisRecordDao {
    
    @Query("SELECT * FROM `tree-analysis-records` WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<TreeAnalysisRecordEntity>>
    
    @Query("SELECT * FROM `tree-analysis-records` WHERE deletedAt IS NULL ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    fun getRecordsPaginated(limit: Int, offset: Int): Flow<List<TreeAnalysisRecordEntity>>
    
    @Query("SELECT * FROM `tree-analysis-records` WHERE id = :id AND deletedAt IS NULL")
    suspend fun getRecordById(id: Long): TreeAnalysisRecordEntity?
    
    @Query("SELECT * FROM `tree-analysis-records` WHERE name LIKE '%' || :searchQuery || '%' AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun searchRecordsByName(searchQuery: String): Flow<List<TreeAnalysisRecordEntity>>
    
    @Query("SELECT * FROM `tree-analysis-records` WHERE name LIKE '%' || :searchQuery || '%' AND deletedAt IS NULL ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    fun searchRecordsByNamePaginated(searchQuery: String, limit: Int, offset: Int): Flow<List<TreeAnalysisRecordEntity>>
    
    @Insert
    suspend fun insertRecord(record: TreeAnalysisRecordEntity): Long
    
    @Update
    suspend fun updateRecord(record: TreeAnalysisRecordEntity)
    
    @Query("UPDATE `tree-analysis-records` SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteRecord(id: Long, deletedAt: Long)
    
    @Delete
    suspend fun hardDeleteRecord(record: TreeAnalysisRecordEntity)
    
    @Query("DELETE FROM `tree-analysis-records` WHERE deletedAt IS NOT NULL")
    suspend fun cleanupDeletedRecords()
    
    @Query("SELECT COUNT(*) FROM `tree-analysis-records` WHERE deletedAt IS NULL")
    suspend fun getRecordCount(): Int
}

