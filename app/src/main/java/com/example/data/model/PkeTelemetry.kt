package com.example.data.model

enum class SessionState {
  PARADO,
  CALIBRANDO,
  MONITORANDO,
  PAUSADO
}

data class EnvironmentalBaseline(
  val magneticFieldUtd: Float = 45f,
  val audioDbfs: Float = -65f,
  val accelerationMps2: Float = 9.81f,
  val lightLux: Float = 250f,
  val samplesCount: Int = 0
)

data class EnvironmentalThresholds(
  val magneticDeltaUtd: Float = 12.0f,
  val audioDeltaDbfs: Float = 16.0f,
  val movementDeltaMps2: Float = 3.5f,
  val lightDeltaLux: Float = 100.0f,
  val calibrationSeconds: Int = 20,
  val alertDebounceSeconds: Int = 10
)

data class EnvironmentalEvent(
  val id: String,
  val timestamp: Long,
  val formattedTime: String,
  val reason: String,
  val physicalExplanations: String,
  val observedValue: String,
  val baselineValue: String
)

data class ChartPoint(
  val timestamp: Long,
  val value: Float
)
