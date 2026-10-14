package com.emailmanager.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDao {
    @Query("SELECT * FROM emails")
    fun observeAll(): Flow<List<EmailEntity>>

    @Query("SELECT * FROM emails")
    suspend fun getAll(): List<EmailEntity>

    /** Returns the new row id for each item, or -1 when the email already existed (ignored). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<EmailEntity>): List<Long>

    @Update
    suspend fun update(item: EmailEntity)

    @Delete
    suspend fun delete(item: EmailEntity)

    @Query("DELETE FROM emails")
    suspend fun deleteAll()
}
