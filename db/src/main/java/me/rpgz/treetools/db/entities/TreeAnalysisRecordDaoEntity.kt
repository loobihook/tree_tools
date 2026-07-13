package me.rpgz.treetools.db.entities

import androidx.room.*

@Entity(tableName = "tree-analysis-records")
data class TreeAnalysisRecordEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "note")
    val note: String?,
    @ColumnInfo(name = "imageDir")
    val imageDir: String?,
    @ColumnInfo(name = "extra")
    val extra: String?,
    @ColumnInfo(name = "report")
    val report: String?,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long,
    @ColumnInfo(name = "deletedAt")
    val deletedAt: Long?
)