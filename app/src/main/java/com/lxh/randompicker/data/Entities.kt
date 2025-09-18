package com.lxh.randompicker.data

import androidx.room.*

@Entity(tableName = "tables")
data class TableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = TableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tableId")]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long,
    val text: String,
    val isDrawn: Boolean = false,   // 是否已抽到
    val drawnAt: Long? = null       // 抽到时间戳（用于“已抽到”排序/展示）
)
