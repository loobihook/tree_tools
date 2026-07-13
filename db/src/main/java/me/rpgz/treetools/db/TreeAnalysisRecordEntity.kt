package me.rpgz.treetools.db


import androidx.room.Database
import androidx.room.RoomDatabase
import me.rpgz.treetools.db.dao.TreeAnalysisRecordDao
import me.rpgz.treetools.db.entities.TreeAnalysisRecordEntity


@Database(
    entities = [
        TreeAnalysisRecordEntity::class
    ],
    version = 1,
    autoMigrations = [],
    exportSchema = false,
)
abstract class DataBase : RoomDatabase() {
    abstract fun treeAnalysisRecordDao(): TreeAnalysisRecordDao
}