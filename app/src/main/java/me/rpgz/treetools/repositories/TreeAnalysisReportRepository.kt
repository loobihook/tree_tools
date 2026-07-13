package me.rpgz.treetools.repositories

import kotlinx.coroutines.flow.Flow
import me.rpgz.treetools.db.entities.TreeAnalysisRecordEntity

interface TreeAnalysisReportRepository {
    
    fun getAllRecords(): Flow<List<TreeAnalysisRecordEntity>>
    
    fun getRecordsPaginated(limit: Int, offset: Int): Flow<List<TreeAnalysisRecordEntity>>
    
    suspend fun getRecordById(id: Long): TreeAnalysisRecordEntity?
    
    fun searchRecordsByName(searchQuery: String): Flow<List<TreeAnalysisRecordEntity>>
    
    fun searchRecordsByNamePaginated(searchQuery: String, limit: Int, offset: Int): Flow<List<TreeAnalysisRecordEntity>>
    
    suspend fun insertRecord(record: TreeAnalysisRecordEntity): Long
    
    suspend fun updateRecord(record: TreeAnalysisRecordEntity)
    
    suspend fun deleteRecord(id: Long)
    
    suspend fun hardDeleteRecord(record: TreeAnalysisRecordEntity)
    
    suspend fun cleanupDeletedRecords()
    
    suspend fun getRecordCount(): Int
}