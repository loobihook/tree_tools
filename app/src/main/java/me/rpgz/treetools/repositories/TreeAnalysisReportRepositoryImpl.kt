package me.rpgz.treetools.repositories

import kotlinx.coroutines.flow.Flow
import me.rpgz.treetools.db.dao.TreeAnalysisRecordDao
import me.rpgz.treetools.db.entities.TreeAnalysisRecordEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TreeAnalysisReportRepositoryImpl @Inject constructor(
    private val dataSource: TreeAnalysisRecordDao,
): TreeAnalysisReportRepository {

    override fun getAllRecords(): Flow<List<TreeAnalysisRecordEntity>> {
        return dataSource.getAllRecords()
    }
    
    override fun getRecordsPaginated(limit: Int, offset: Int): Flow<List<TreeAnalysisRecordEntity>> {
        return dataSource.getRecordsPaginated(limit, offset)
    }
    
    override suspend fun getRecordById(id: Long): TreeAnalysisRecordEntity? {
        return dataSource.getRecordById(id)
    }
    
    override fun searchRecordsByName(searchQuery: String): Flow<List<TreeAnalysisRecordEntity>> {
        return dataSource.searchRecordsByName(searchQuery)
    }
    
    override fun searchRecordsByNamePaginated(searchQuery: String, limit: Int, offset: Int): Flow<List<TreeAnalysisRecordEntity>> {
        return dataSource.searchRecordsByNamePaginated(searchQuery, limit, offset)
    }
    
    override suspend fun insertRecord(record: TreeAnalysisRecordEntity): Long {
        return dataSource.insertRecord(record)
    }
    
    override suspend fun updateRecord(record: TreeAnalysisRecordEntity) {
        dataSource.updateRecord(record)
    }
    
    override suspend fun deleteRecord(id: Long) {
        val currentTime = System.currentTimeMillis()
        dataSource.softDeleteRecord(id, currentTime)
    }
    
    override suspend fun hardDeleteRecord(record: TreeAnalysisRecordEntity) {
        dataSource.hardDeleteRecord(record)
    }
    
    override suspend fun cleanupDeletedRecords() {
        dataSource.cleanupDeletedRecords()
    }
    
    override suspend fun getRecordCount(): Int {
        return dataSource.getRecordCount()
    }
}