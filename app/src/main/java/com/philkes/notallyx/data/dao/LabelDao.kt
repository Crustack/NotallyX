package com.philkes.notallyx.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.philkes.notallyx.data.model.Label

@Dao
interface LabelDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(label: Label)

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(labels: List<Label>)

    @Update suspend fun update(label: Label)

    @Update suspend fun update(labels: List<Label>)

    @Query("DELETE FROM Label WHERE value = :value") suspend fun delete(value: String)

    @Query("DELETE FROM Label") suspend fun deleteAll()

    @Query("UPDATE Label SET value = :newValue WHERE value = :oldValue")
    suspend fun update(oldValue: String, newValue: String)

    @Query("SELECT * FROM Label ORDER BY `order` DESC, value ASC")
    fun getAll(): LiveData<List<Label>>

    @Query("SELECT value FROM Label ORDER BY `order` DESC, value ASC")
    suspend fun getArrayOfAll(): Array<String>

    @Query("SELECT EXISTS(SELECT 1 FROM Label WHERE value = :value)")
    suspend fun exists(value: String): Boolean

    @Query("SELECT MAX(`order`) FROM Label") suspend fun getMaxOrder(): Int?
}
