package com.lxh.randompicker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TablesDao {
    @Insert
    suspend fun insert(table: TableEntity): Long

    @Update
    suspend fun update(table: TableEntity)

    @Delete
    suspend fun delete(table: TableEntity)

    @Query("SELECT * FROM tables ORDER BY id ASC")
    fun observeAll(): Flow<List<TableEntity>>

    @Query("SELECT * FROM tables ORDER BY id ASC")
    suspend fun getAll(): List<TableEntity>

    @Query("SELECT * FROM tables WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TableEntity?
}

@Dao
interface ItemsDao {
    @Insert
    suspend fun insert(items: List<ItemEntity>)

    @Insert
    suspend fun insertOne(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Delete
    suspend fun delete(item: ItemEntity)

    @Query("SELECT * FROM items WHERE tableId = :tableId ORDER BY id ASC")
    fun observeByTable(tableId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE tableId = :tableId")
    suspend fun getByTable(tableId: Long): List<ItemEntity>

    @Query("SELECT * FROM items WHERE tableId = :tableId AND isDrawn = 0")
    suspend fun getUndrawn(tableId: Long): List<ItemEntity>

    @Query("UPDATE items SET isDrawn = 1, drawnAt = :ts WHERE id = :itemId")
    suspend fun markDrawn(itemId: Long, ts: Long)

    @Query("UPDATE items SET isDrawn = 0, drawnAt = NULL WHERE tableId = :tableId")
    suspend fun resetDrawn(tableId: Long)

    @Query("DELETE FROM items WHERE tableId = :tableId")
    suspend fun deleteByTable(tableId: Long)
}
