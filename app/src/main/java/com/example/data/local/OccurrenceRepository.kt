package com.example.data.local

import kotlinx.coroutines.flow.Flow

class OccurrenceRepository(private val dao: OccurrenceDao) {
  val allOccurrences: Flow<List<OccurrenceEntity>> = dao.getAllOccurrences()

  suspend fun insert(occurrence: OccurrenceEntity) = dao.insert(occurrence)

  suspend fun update(occurrence: OccurrenceEntity) = dao.update(occurrence)

  suspend fun deleteById(id: String) = dao.deleteById(id)

  suspend fun deleteAll() = dao.deleteAll()
}
