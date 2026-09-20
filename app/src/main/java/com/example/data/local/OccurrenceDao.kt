package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OccurrenceDao {
  @Query("SELECT * FROM occurrences ORDER BY timestamp DESC")
  fun getAllOccurrences(): Flow<List<OccurrenceEntity>>

  @Query("SELECT * FROM occurrences WHERE id = :id")
  suspend fun getOccurrenceById(id: String): OccurrenceEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(occurrence: OccurrenceEntity)

  @Update
  suspend fun update(occurrence: OccurrenceEntity)

  @Query("DELETE FROM occurrences WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM occurrences")
  suspend fun deleteAll()
}
