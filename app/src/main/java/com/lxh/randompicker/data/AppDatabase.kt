package com.lxh.randompicker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TableEntity::class, ItemEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tablesDao(): TablesDao
    abstract fun itemsDao(): ItemsDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "random_picker.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // 首次安装时，预置一个默认表，里面放几条示例
                            CoroutineScope(Dispatchers.IO).launch {
                                val daoT = get(context).tablesDao()
                                val daoI = get(context).itemsDao()
                                val tableId = daoT.insert(TableEntity(name = "默认表"))
                                val defaults = listOf("炒饭","拉面","水饺","烧烤","沙拉","米线","寿司","披萨")
                                    .map { ItemEntity(tableId = tableId, text = it) }
                                daoI.insert(defaults)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
