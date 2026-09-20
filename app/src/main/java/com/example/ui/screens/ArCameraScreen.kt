package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.PrimaryNeonGreen
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceDark
import com.example.ui.viewmodel.PkeScannerViewModel
import com.example.ui.viewmodel.ScannerUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun ArCameraScreen(
  viewModel: PkeScannerViewModel,
  uiState: ScannerUiState,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
      ) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasCameraPermission = granted
  }

  var isBackCamera by remember { mutableStateOf(true) }
  var isTorchEnabled by remember { mutableStateOf(false) }
  var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
  var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
  var isCapturing by remember { mutableStateOf(false) }
  var captureFeedbackMessage by remember { mutableStateOf<String?>(null) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(SurfaceDark)
  ) {
    if (hasCameraPermission) {
      // Prévia Real da Câmera com CameraX
      AndroidView(
        factory = { ctx ->
          val previewView = PreviewView(ctx)
          val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

          cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
              it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val capture = ImageCapture.Builder()
              .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
              .build()
            imageCapture = capture

            val cameraSelector = if (isBackCamera) {
              CameraSelector.DEFAULT_BACK_CAMERA
            } else {
              CameraSelector.DEFAULT_FRONT_CAMERA
            }

            try {
              cameraProvider.unbindAll()
              val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                capture
              )
              cameraControl = camera.cameraControl
              cameraControl?.enableTorch(isTorchEnabled)
            } catch (e: Exception) {
              e.printStackTrace()
            }
          }, ContextCompat.getMainExecutor(ctx))

          previewView
        },
        update = {
          // Atualiza tocha se alterada
          cameraControl?.enableTorch(isTorchEnabled)
        },
        modifier = Modifier.fillMaxSize()
      )

      // HUD Tático com Leituras dos Sensores Físicos Reais (Sem entidades falsas)
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Cabeçalho HUD Superior
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GlassBackground)
            .border(1.dp, PrimaryNeonGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "HUD ÓPTICO AMBIENTAL",
              color = PrimaryNeonGreen,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
            val azim = uiState.sensorReading.azimuthDegrees?.let { "${it.toInt()}°" } ?: "---"
            Text(
              text = "BÚSSOLA: $azim ${uiState.sensorReading.cardinalDirection}",
              color = Color.White,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            )
          }

          // Controles de Flash e Troca de Lente
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
              onClick = {
                isTorchEnabled = !isTorchEnabled
                cameraControl?.enableTorch(isTorchEnabled)
              },
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceContainerHigh)
            ) {
              Icon(
                imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Lanterna",
                tint = if (isTorchEnabled) Color.Yellow else Color.White,
                modifier = Modifier.size(18.dp)
              )
            }

            IconButton(
              onClick = { isBackCamera = !isBackCamera },
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceContainerHigh)
            ) {
              Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Alternar Câmera",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        // Mira Tática Central
        Box(
          modifier = Modifier.fillMaxWidth(),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(90.dp)
              .border(1.dp, PrimaryNeonGreen.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .background(PrimaryNeonGreen, CircleShape)
            )
          }
        }

        // Barra Inferior do HUD: Telemetria e Botão de Disparo
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Barra de Telemetria do Instante
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(GlassBackground)
              .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
              .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            val mag = uiState.sensorReading.magneticFieldTotal?.let { String.format(Locale.US, "%.1f µT", it) } ?: "---"
            val aud = if (uiState.audioReading.currentDbfs > -90f) {
              String.format(Locale.US, "%.0f dBFS", uiState.audioReading.currentDbfs)
            } else "Silêncio"

            Text(
              text = "MAG: $mag",
              color = PrimaryNeonGreen,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "ÁUDIO: $aud",
              color = SecondaryCyan,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
            val gpsStr = uiState.locationReading.accuracyMeters?.let { "GPS ±${it.toInt()}m" } ?: "GPS Buscando"
            Text(
              text = gpsStr,
              color = Color.LightGray,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            )
          }

          // Botão Central de Foto & Registro
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 75.dp),
            contentAlignment = Alignment.Center
          ) {
            Button(
              onClick = {
                val capture = imageCapture
                if (capture != null && !isCapturing) {
                  isCapturing = true
                  val photoDir = File(context.filesDir, "photos").apply { mkdirs() }
                  val photoFile = File(photoDir, "foto_${System.currentTimeMillis()}.jpg")
                  val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                  capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                      override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        isCapturing = false
                        viewModel.markOccurrence(
                          userNotes = "Registro fotográfico ambiental capturado pela câmera",
                          photoPath = photoFile.absolutePath
                        )
                        captureFeedbackMessage = "Foto gravada e vinculada à ocorrência!"
                      }

                      override fun onError(exception: ImageCaptureException) {
                        isCapturing = false
                        captureFeedbackMessage = "Erro na captura: ${exception.message}"
                      }
                    }
                  )
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonGreen),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp)
            ) {
              if (isCapturing) {
                CircularProgressIndicator(
                  color = Color.Black,
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("GRAVANDO...", color = Color.Black, fontWeight = FontWeight.Bold)
              } else {
                Icon(
                  imageVector = Icons.Default.CameraAlt,
                  contentDescription = "Disparar Foto",
                  tint = Color(0xFF003919),
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "FOTOGRAFAR & REGISTRAR",
                  color = Color(0xFF003919),
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 12.sp
                )
              }
            }
          }
        }
      }
    } else {
      // Estado de Permissão Pendente / Negada
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerHigh)
            .border(1.dp, PrimaryNeonGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(20.dp)
        ) {
          Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = PrimaryNeonGreen,
            modifier = Modifier.size(48.dp)
          )

          Text(
            text = "PERMISSÃO DA CÂMERA NECESSÁRIA",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )

          Text(
            text = "Para registrar imagens reais e sobrepor a telemetria física dos sensores em tempo real, o aplicativo precisa de acesso à câmera do aparelho.",
            color = Color.LightGray,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )

          Button(
            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonGreen),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("CONCEDER PERMISSÃO", color = Color.Black, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
