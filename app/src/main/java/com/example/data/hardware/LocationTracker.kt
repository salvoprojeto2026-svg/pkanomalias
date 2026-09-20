package com.example.data.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocationReadingState(
  val latitude: Double? = null,
  val longitude: Double? = null,
  val altitudeMeters: Double? = null,
  val accuracyMeters: Float? = null,
  val provider: String? = null,
  val timestampFormatted: String = "---",
  val hasPermission: Boolean = false,
  val isLocationEnabled: Boolean = false,
  val statusDescription: String = "Aguardando leitura"
)

class LocationTracker(private val context: Context) : LocationListener {
  private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

  private val _locationState = MutableStateFlow(LocationReadingState())
  val locationState: StateFlow<LocationReadingState> = _locationState.asStateFlow()

  private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
  private var isTracking = false

  fun checkPermission(): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasPerm = fineGranted || coarseGranted

    val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
      locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

    _locationState.value = _locationState.value.copy(
      hasPermission = hasPerm,
      isLocationEnabled = isGpsEnabled,
      statusDescription = if (!hasPerm) "Sem permissão de localização"
      else if (!isGpsEnabled) "GPS desativado no aparelho"
      else _locationState.value.statusDescription
    )
    return hasPerm
  }

  @SuppressLint("MissingPermission")
  fun startTracking() {
    if (isTracking || locationManager == null) return
    if (!checkPermission()) return

    isTracking = true
    _locationState.value = _locationState.value.copy(
      statusDescription = "Buscando sinal de satélites..."
    )

    try {
      // Tenta última localização conhecida
      val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
      val bestLast = when {
        lastGps != null && lastNet != null -> if (lastGps.time > lastNet.time) lastGps else lastNet
        lastGps != null -> lastGps
        else -> lastNet
      }
      bestLast?.let { onLocationChanged(it) }

      // Solicita atualizações contínuas
      if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        locationManager.requestLocationUpdates(
          LocationManager.GPS_PROVIDER,
          2000L,
          1f,
          this
        )
      }
      if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        locationManager.requestLocationUpdates(
          LocationManager.NETWORK_PROVIDER,
          3000L,
          2f,
          this
        )
      }
    } catch (e: Exception) {
      e.printStackTrace()
      _locationState.value = _locationState.value.copy(
        statusDescription = "Erro ao acessar localização: ${e.localizedMessage}"
      )
    }
  }

  fun stopTracking() {
    if (!isTracking || locationManager == null) return
    isTracking = false
    try {
      locationManager.removeUpdates(this)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onLocationChanged(location: Location) {
    val dateStr = dateFormat.format(Date(location.time))
    val providerName = when (location.provider) {
      LocationManager.GPS_PROVIDER -> "GPS Satélite"
      LocationManager.NETWORK_PROVIDER -> "Rede Celular / Wi-Fi"
      else -> location.provider ?: "Local"
    }

    _locationState.value = _locationState.value.copy(
      latitude = location.latitude,
      longitude = location.longitude,
      altitudeMeters = if (location.hasAltitude()) location.altitude else null,
      accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
      provider = providerName,
      timestampFormatted = dateStr,
      statusDescription = "Sinal fixado (± ${String.format(Locale("pt", "BR"), "%.1f", location.accuracy)} m)"
    )
  }

  override fun onProviderEnabled(provider: String) {
    checkPermission()
  }

  override fun onProviderDisabled(provider: String) {
    checkPermission()
  }

  @Deprecated("Deprecated in Java")
  override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    // Manter compatibilidade com APIs legadas
  }
}
