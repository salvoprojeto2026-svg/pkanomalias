package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "occurrences")
data class OccurrenceEntity(
  @PrimaryKey
  val id: String,
  val timestamp: Long,
  val formattedDateTime: String,
  val origin: String, // "MANUAL" ou "REGRA_AUTOMATICA"
  val triggerReason: String,
  val possibleExplanations: String,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val altitude: Double? = null,
  val locationAccuracyMeters: Float? = null,
  val magneticFieldMicroTesla: Float? = null,
  val baselineMagneticMicroTesla: Float? = null,
  val audioDbfs: Float? = null,
  val baselineAudioDbfs: Float? = null,
  val accelerationMps2: Float? = null,
  val lightLux: Float? = null,
  val pressureHpa: Float? = null,
  val azimuthDegrees: Float? = null,
  val cardinalDirection: String? = null,
  val photoPath: String? = null,
  val audioPath: String? = null,
  val userNotes: String = "",
  val aiAnalysis: String? = null
)
