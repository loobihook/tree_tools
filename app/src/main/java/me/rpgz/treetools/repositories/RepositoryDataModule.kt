package me.rpgz.treetools.repositories


import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryDataModule {
    @Singleton
    @Binds
    abstract fun bindTreeAnalysisReportRepository(repository: TreeAnalysisReportRepositoryImpl): TreeAnalysisReportRepository
}
