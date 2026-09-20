package com.example.data.hardware

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

data class AudioReadingState(
  val isRecordingAudio: Boolean = false,
  val currentDbfs: Float = -90f,
  val peakDbfs: Float = -90f,
  val waveformSamples: List<Float> = emptyList(),
  val hasPermission: Boolean = false,
  val isAudioCapturingActive: Boolean = false,
  val activeRecordingFilePath: String? = null,
  val isPlayingAudio: Boolean = false
)

class AudioMonitor(private val context: Context) {
  private val _audioState = MutableStateFlow(AudioReadingState())
  val audioState: StateFlow<AudioReadingState> = _audioState.asStateFlow()

  private var captureJob: Job? = null
  private var audioRecord: AudioRecord? = null
  private var mediaPlayer: MediaPlayer? = null

  // File recording state
  private var fileRecordingFile: File? = null
  private var fileOutputStream: FileOutputStream? = null
  private var isSavingToFile = false
  private var totalAudioBytesWritten = 0L

  private val sampleRate = 44100
  private val channelConfig = AudioFormat.CHANNEL_IN_MONO
  private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
  private val bufferSize = max(
    AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat),
    2048
  )

  fun checkPermission(): Boolean {
    val granted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    _audioState.value = _audioState.value.copy(hasPermission = granted)
    return granted
  }

  fun startMonitoring(scope: CoroutineScope) {
    if (!checkPermission()) return
    if (_audioState.value.isAudioCapturingActive) return

    captureJob?.cancel()
    captureJob = scope.launch(Dispatchers.IO) {
      try {
        val record = AudioRecord(
          MediaRecorder.AudioSource.MIC,
          sampleRate,
          channelConfig,
          audioFormat,
          bufferSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
          return@launch
        }

        audioRecord = record
        record.startRecording()
        _audioState.value = _audioState.value.copy(isAudioCapturingActive = true)

        val buffer = ShortArray(bufferSize / 2)
        val rollingSamples = ArrayList<Float>(64)

        while (isActive && _audioState.value.isAudioCapturingActive) {
          val readCount = record.read(buffer, 0, buffer.size)
          if (readCount > 0) {
            // Se estiver gravando arquivo WAV
            if (isSavingToFile && fileOutputStream != null) {
              val byteBuffer = ByteArray(readCount * 2)
              for (i in 0 until readCount) {
                val s = buffer[i]
                byteBuffer[i * 2] = (s.toInt() and 0xFF).toByte()
                byteBuffer[i * 2 + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
              }
              fileOutputStream?.write(byteBuffer)
              totalAudioBytesWritten += byteBuffer.size
            }

            // Calcula RMS real e dBFS real
            var sumSquares = 0.0
            var peakSample = 0
            for (i in 0 until readCount) {
              val sample = buffer[i].toInt()
              sumSquares += sample * sample
              val absSample = kotlin.math.abs(sample)
              if (absSample > peakSample) peakSample = absSample
            }

            val rms = sqrt(sumSquares / readCount)
            // 20 * log10(rms / 32767.0)
            val dbfs = if (rms > 0.0) {
              (20.0 * log10(rms / 32767.0)).toFloat().coerceIn(-90f, 0f)
            } else {
              -90f
            }

            val peakDbfs = if (peakSample > 0) {
              (20.0 * log10(peakSample.toDouble() / 32767.0)).toFloat().coerceIn(-90f, 0f)
            } else {
              -90f
            }

            // Amostras para visualização gráfica
            val step = max(1, readCount / 32)
            rollingSamples.clear()
            for (i in 0 until readCount step step) {
              rollingSamples.add((buffer[i] / 32768.0f).coerceIn(-1f, 1f))
            }

            _audioState.value = _audioState.value.copy(
              currentDbfs = dbfs,
              peakDbfs = peakDbfs,
              waveformSamples = ArrayList(rollingSamples)
            )
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        stopMonitoring()
      }
    }
  }

  fun stopMonitoring() {
    _audioState.value = _audioState.value.copy(isAudioCapturingActive = false)
    captureJob?.cancel()
    captureJob = null

    try {
      if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
        audioRecord?.stop()
      }
      audioRecord?.release()
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      audioRecord = null
    }

    if (isSavingToFile) {
      stopFileRecording()
    }
  }

  fun startFileRecording(): File? {
    if (!checkPermission()) return null

    return try {
      val audioDir = File(context.filesDir, "audio_records").apply { mkdirs() }
      val file = File(audioDir, "audio_${System.currentTimeMillis()}.wav")
      val fos = FileOutputStream(file)
      // Escreve cabeçalho WAV preliminar de 44 bytes
      writeWavHeader(fos, 0, 0, sampleRate, 1, 16)

      fileRecordingFile = file
      fileOutputStream = fos
      totalAudioBytesWritten = 0L
      isSavingToFile = true

      _audioState.value = _audioState.value.copy(
        isRecordingAudio = true,
        activeRecordingFilePath = file.absolutePath
      )
      file
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  fun stopFileRecording(): String? {
    if (!isSavingToFile) return null

    val file = fileRecordingFile
    val path = file?.absolutePath
    try {
      fileOutputStream?.flush()
      fileOutputStream?.close()
      fileOutputStream = null

      if (file != null && file.exists() && totalAudioBytesWritten > 0) {
        // Atualiza cabeçalho WAV com tamanho real de dados
        updateWavHeader(file, totalAudioBytesWritten, sampleRate, 1, 16)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      isSavingToFile = false
      fileRecordingFile = null
      _audioState.value = _audioState.value.copy(
        isRecordingAudio = false,
        activeRecordingFilePath = null
      )
    }
    return path
  }

  fun playAudio(filePath: String, onCompletion: () -> Unit = {}) {
    stopPlayback()
    val file = File(filePath)
    if (!file.exists()) return

    try {
      mediaPlayer = MediaPlayer().apply {
        setDataSource(filePath)
        prepare()
        setOnCompletionListener {
          _audioState.value = _audioState.value.copy(isPlayingAudio = false)
          onCompletion()
        }
        start()
      }
      _audioState.value = _audioState.value.copy(isPlayingAudio = true)
    } catch (e: Exception) {
      e.printStackTrace()
      _audioState.value = _audioState.value.copy(isPlayingAudio = false)
    }
  }

  fun stopPlayback() {
    mediaPlayer?.stop()
    mediaPlayer?.release()
    mediaPlayer = null
    _audioState.value = _audioState.value.copy(isPlayingAudio = false)
  }

  private fun writeWavHeader(
    out: FileOutputStream,
    totalAudioLen: Long,
    totalDataLen: Long,
    longSampleRate: Int,
    channels: Int,
    byteRate: Long
  ) {
    val header = ByteArray(44)
    header[0] = 'R'.code.toByte()
    header[1] = 'I'.code.toByte()
    header[2] = 'F'.code.toByte()
    header[3] = 'F'.code.toByte()
    header[4] = (totalDataLen and 0xff).toByte()
    header[5] = ((totalDataLen shr 8) and 0xff).toByte()
    header[6] = ((totalDataLen shr 16) and 0xff).toByte()
    header[7] = ((totalDataLen shr 24) and 0xff).toByte()
    header[8] = 'W'.code.toByte()
    header[9] = 'A'.code.toByte()
    header[10] = 'V'.code.toByte()
    header[11] = 'E'.code.toByte()
    header[12] = 'f'.code.toByte()
    header[13] = 'm'.code.toByte()
    header[14] = 't'.code.toByte()
    header[15] = ' '.code.toByte()
    header[16] = 16
    header[17] = 0
    header[18] = 0
    header[19] = 0
    header[20] = 1 // PCM
    header[21] = 0
    header[22] = channels.toByte()
    header[23] = 0
    header[24] = (longSampleRate and 0xff).toByte()
    header[25] = ((longSampleRate shr 8) and 0xff).toByte()
    header[26] = ((longSampleRate shr 16) and 0xff).toByte()
    header[27] = ((longSampleRate shr 24) and 0xff).toByte()
    val byteRateCalc = longSampleRate * channels * 16 / 8
    header[28] = (byteRateCalc and 0xff).toByte()
    header[29] = ((byteRateCalc shr 8) and 0xff).toByte()
    header[30] = ((byteRateCalc shr 16) and 0xff).toByte()
    header[31] = ((byteRateCalc shr 24) and 0xff).toByte()
    header[32] = (channels * 16 / 8).toByte()
    header[33] = 0
    header[34] = 16 // bits per sample
    header[35] = 0
    header[36] = 'd'.code.toByte()
    header[37] = 'a'.code.toByte()
    header[38] = 't'.code.toByte()
    header[39] = 'a'.code.toByte()
    header[40] = (totalAudioLen and 0xff).toByte()
    header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
    header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
    header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
    out.write(header, 0, 44)
  }

  private fun updateWavHeader(file: File, totalAudioBytes: Long, sampleRate: Int, channels: Int, bitsPerSample: Int) {
    try {
      val raf = RandomAccessFile(file, "rw")
      val totalDataLen = totalAudioBytes + 36

      raf.seek(4)
      raf.write((totalDataLen and 0xff).toInt())
      raf.write(((totalDataLen shr 8) and 0xff).toInt())
      raf.write(((totalDataLen shr 16) and 0xff).toInt())
      raf.write(((totalDataLen shr 24) and 0xff).toInt())

      raf.seek(40)
      raf.write((totalAudioBytes and 0xff).toInt())
      raf.write(((totalAudioBytes shr 8) and 0xff).toInt())
      raf.write(((totalAudioBytes shr 16) and 0xff).toInt())
      raf.write(((totalAudioBytes shr 24) and 0xff).toInt())

      raf.close()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
