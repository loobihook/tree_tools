package me.rpgz.treetools.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.rpgz.treetools.db.dao.TreeAnalysisRecordDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providesCEADatabase(
        @ApplicationContext context: Context
    ): DataBase = Room.databaseBuilder(
        context,
        DataBase::class.java,
        "tree-tools-db"
    ).build()

    @Provides
    fun provideTreeAnalysisRecordDao(database: DataBase): TreeAnalysisRecordDao =
        database.treeAnalysisRecordDao()
}
