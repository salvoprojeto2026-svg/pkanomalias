package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OccurrenceEntity
import com.example.ui.components.TacticalTopHeader
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.PrimaryNeonGreen
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerHighest
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TertiaryBright
import com.example.ui.viewmodel.PkeScannerViewModel
import com.example.ui.viewmodel.ScannerUiState
import java.io.File
import java.util.Locale

@Composable
fun VaultScreen(
  viewModel: PkeScannerViewModel,
  uiState: ScannerUiState,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("TODAS") }
  var occurrenceToDelete by remember { mutableStateOf<OccurrenceEntity?>(null) }
  var occurrenceToEditNotes by remember { mutableStateOf<OccurrenceEntity?>(null) }
  var editedNotesText by remember { mutableStateOf("") }

  // Filtra itens
  val filteredOccurrences = uiState.occurrences.filter { occ ->
    val matchesCategory = when (selectedCategory) {
      "MANUAIS" -> occ.origin.contains("MANUAL", ignoreCase = true)
      "AUTOMÁTICAS" -> occ.origin.contains("AUTOMÁTICA", ignoreCase = true) || occ.origin.contains("AUTOMATICA", ignoreCase = true)
      else -> true
    }
    val matchesSearch = searchQuery.isBlank() ||
      occ.triggerReason.contains(searchQuery, ignoreCase = true) ||
      occ.userNotes.contains(searchQuery, ignoreCase = true) ||
      occ.formattedDateTime.contains(searchQuery, ignoreCase = true)

    matchesCategory && matchesSearch
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(SurfaceDark)
  ) {
    TacticalTopHeader(
      sessionState = uiState.sessionState,
      sessionDurationFormatted = uiState.formattedDuration,
      locationStatus = uiState.locationReading.statusDescription
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(10.dp))

      // Barra de Pesquisa
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Buscar nos registros por motivo, data ou anotação...", fontSize = 12.sp) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = PrimaryNeonGreen,
            modifier = Modifier.size(18.dp)
          )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = PrimaryNeonGreen,
          unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
          focusedContainerColor = SurfaceContainerHigh,
          unfocusedContainerColor = SurfaceContainerHigh,
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White
        )
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Categorias de Filtro e Botões de Exportação
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Chips de categoria
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f)
        ) {
          val categories = listOf("TODAS", "MANUAIS", "AUTOMÁTICAS")
          items(categories) { cat ->
            val isSelected = selectedCategory == cat
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) PrimaryNeonGreen else SurfaceContainerHigh)
                .clickable { selectedCategory = cat }
                .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
              Text(
                text = cat,
                color = if (isSelected) Color.Black else Color.LightGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }

        // Ações de Exportação
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(
            onClick = { shareExport(context, viewModel.exportOccurrencesJson(), "application/json", "ocorrencias.json") },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.FileDownload,
              contentDescription = "Exportar JSON",
              tint = SecondaryCyan,
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = { shareExport(context, viewModel.exportOccurrencesCsv(), "text/csv", "ocorrencias.csv") },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Description,
              contentDescription = "Exportar CSV",
              tint = PrimaryNeonGreen,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Lista de Ocorrências
      if (filteredOccurrences.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 70.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Description,
              contentDescription = null,
              tint = Color.Gray,
              modifier = Modifier.size(40.dp)
            )
            Text(
              text = if (uiState.occurrences.isEmpty()) "NENHUMA OCORRÊNCIA GRAVADA" else "NENHUM RESULTADO PARA O FILTRO",
              color = Color.LightGray,
              fontSize = 13.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Inicie o monitoramento na aba Monitorar ou marque uma ocorrência com fotos.",
              color = Color.Gray,
              fontSize = 11.sp
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredOccurrences, key = { it.id }) { occ ->
            OccurrenceItemCard(
              occurrence = occ,
              isPlayingAudio = uiState.audioReading.isPlayingAudio,
              onPlayAudio = { path -> viewModel.playOccurrenceAudio(path) },
              onStopAudio = { viewModel.stopAudioPlayback() },
              onEditNotes = {
                occurrenceToEditNotes = occ
                editedNotesText = occ.userNotes
              },
              onAnalyzeAi = { viewModel.analyzeOccurrenceWithAi(occ) },
              onShare = { shareOccurrenceText(context, occ) },
              onDelete = { occurrenceToDelete = occ }
            )
          }
          item {
            Spacer(modifier = Modifier.height(80.dp))
          }
        }
      }
    }
  }

  // Diálogo de Confirmação de Exclusão
  occurrenceToDelete?.let { occ ->
    AlertDialog(
      onDismissRequest = { occurrenceToDelete = null },
      title = {
        Text("EXCLUIR OCORRÊNCIA", color = Color(0xFFFF5555), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      },
      text = {
        Text("Deseja realmente excluir permanentemente o registro #${occ.id} capturado em ${occ.formattedDateTime}?", color = Color.LightGray)
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteOccurrence(occ.id)
            occurrenceToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444))
        ) {
          Text("EXCLUIR", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { occurrenceToDelete = null }) {
          Text("CANCELAR", color = Color.Gray)
        }
      },
      containerColor = SurfaceContainerHigh
    )
  }

  // Diálogo de Edição de Notas
  occurrenceToEditNotes?.let { occ ->
    AlertDialog(
      onDismissRequest = { occurrenceToEditNotes = null },
      title = {
        Text("NOTAS DO OBSERVADOR", color = PrimaryNeonGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      },
      text = {
        Column {
          Text("Edite as observações deste registro (#${occ.id}):", color = Color.LightGray, fontSize = 12.sp)
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = editedNotesText,
            onValueChange = { editedNotesText = it },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = PrimaryNeonGreen,
              unfocusedBorderColor = Color.Gray,
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.updateOccurrenceNotes(occ.id, editedNotesText)
            occurrenceToEditNotes = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeonGreen)
        ) {
          Text("SALVAR", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { occurrenceToEditNotes = null }) {
          Text("CANCELAR", color = Color.Gray)
        }
      },
      containerColor = SurfaceContainerHigh
    )
  }
}

@Composable
private fun OccurrenceItemCard(
  occurrence: OccurrenceEntity,
  isPlayingAudio: Boolean,
  onPlayAudio: (String) -> Unit,
  onStopAudio: () -> Unit,
  onEditNotes: () -> Unit,
  onAnalyzeAi: () -> Unit,
  onShare: () -> Unit,
  onDelete: () -> Unit
) {
  val isAuto = occurrence.origin.contains("AUTOMÁTICA", ignoreCase = true) || occurrence.origin.contains("AUTOMATICA", ignoreCase = true)
  val tagColor = if (isAuto) TertiaryBright else SecondaryCyan

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(GlassBackground)
      .border(1.dp, tagColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      // Cabeçalho da Ocorrência
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(tagColor.copy(alpha = 0.2f))
              .border(1.dp, tagColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = if (isAuto) "REGRA AUTOMÁTICA" else "MANUAL",
              color = tagColor,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "#${occurrence.id}",
            color = Color.Gray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        Text(
          text = occurrence.formattedDateTime,
          color = Color.LightGray,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace
        )
      }

      // Motivo do Registro
      Text(
        text = occurrence.triggerReason,
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
      )

      // Hipóteses Físicas Plausíveis
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xFF03160D))
          .padding(8.dp)
      ) {
        Column {
          Text(
            text = "HIPÓTESES FÍSICAS PLAUSÍVEIS:",
            color = PrimaryNeonGreen,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = occurrence.possibleExplanations,
            color = Color(0xFFB4D2C1),
            fontSize = 11.sp,
            lineHeight = 15.sp
          )
        }
      }

      // Valores Capturados no Instante (Grade)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(SurfaceContainerHigh.copy(alpha = 0.6f))
          .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          TelemetryMiniRow("MAGNÉTICO", occurrence.magneticFieldMicroTesla?.let { "${String.format(Locale.US, "%.1f", it)} µT" } ?: "N/D")
          TelemetryMiniRow("ÁUDIO", occurrence.audioDbfs?.let { "${String.format(Locale.US, "%.0f", it)} dBFS" } ?: "N/D")
          TelemetryMiniRow("ACELERAÇÃO", occurrence.accelerationMps2?.let { "${String.format(Locale.US, "%.1f", it)} m/s²" } ?: "N/D")
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          TelemetryMiniRow("LUZ", occurrence.lightLux?.let { "${it.toInt()} lx" } ?: "N/D")
          val gpsStr = if (occurrence.latitude != null && occurrence.longitude != null) {
            "${String.format(Locale.US, "%.3f", occurrence.latitude)}, ${String.format(Locale.US, "%.3f", occurrence.longitude)}"
          } else "N/D"
          TelemetryMiniRow("GPS", gpsStr)
          TelemetryMiniRow("ORIENTAÇÃO", occurrence.cardinalDirection ?: "N/D")
        }
      }

      // Foto Vinculada (se existir)
      occurrence.photoPath?.let { path ->
        val file = File(path)
        if (file.exists()) {
          val bitmap = remember(path) { BitmapFactory.decodeFile(path) }
          if (bitmap != null) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, PrimaryNeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            ) {
              Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto da ocorrência",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
              )
            }
          }
        }
      }

      // Áudio Gravado (se existir)
      occurrence.audioPath?.let { audioPath ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceContainerHighest)
            .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "ÁUDIO VINCULADO (.WAV)",
            color = SecondaryCyan,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )

          Button(
            onClick = {
              if (isPlayingAudio) onStopAudio() else onPlayAudio(audioPath)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryCyan),
            shape = RoundedCornerShape(6.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Icon(
              imageVector = if (isPlayingAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
              contentDescription = null,
              tint = Color.Black,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isPlayingAudio) "PARAR" else "OUVIR", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Anotações do Usuário
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Notas: ${occurrence.userNotes}",
          color = Color.LightGray,
          fontSize = 11.sp,
          modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEditNotes, modifier = Modifier.size(28.dp)) {
          Icon(Icons.Default.Edit, contentDescription = "Editar Notas", tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
      }

      // Análise Técnica com IA (se gerada)
      occurrence.aiAnalysis?.let { analysis ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF071924))
            .border(1.dp, SecondaryCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(10.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SecondaryCyan, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("AVALIAÇÃO TÉCNICA ESTRUTURADA", color = SecondaryCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = analysis, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, lineHeight = 14.sp)
          }
        }
      }

      // Barra de Ações: Análise IA, Compartilhar, Excluir
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = onAnalyzeAi,
          colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHighest),
          shape = RoundedCornerShape(6.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryNeonGreen, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (occurrence.aiAnalysis == null) "AVALIAR COM IA" else "REAVALIAR", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = Color.LightGray, modifier = Modifier.size(16.dp))
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFFF5555), modifier = Modifier.size(16.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun TelemetryMiniRow(label: String, value: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(text = "$label: ", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    Text(text = value, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
  }
}

private fun shareOccurrenceText(context: Context, occ: OccurrenceEntity) {
  val report = buildString {
    appendLine("=== PKE SCANNER BRASIL // PROJETO JAIRO BAHIA ===")
    appendLine("REGISTRO DE OBSERVAÇÃO AMBIENTAL #${occ.id}")
    appendLine("Data e Hora: ${occ.formattedDateTime}")
    appendLine("Origem: ${occ.origin}")
    appendLine("Motivo: ${occ.triggerReason}")
    appendLine("Hipóteses Físicas: ${occ.possibleExplanations}")
    appendLine("Telemetria Capturada:")
    appendLine("  • Campo Magnético: ${occ.magneticFieldMicroTesla ?: "N/D"} µT")
    appendLine("  • Áudio: ${occ.audioDbfs ?: "N/D"} dBFS")
    appendLine("  • Aceleração: ${occ.accelerationMps2 ?: "N/D"} m/s²")
    appendLine("  • Luminosidade: ${occ.lightLux ?: "N/D"} lx")
    appendLine("  • GPS: ${occ.latitude ?: "N/D"}, ${occ.longitude ?: "N/D"} (Precisão: ±${occ.locationAccuracyMeters ?: "N/D"}m)")
    appendLine("  • Orientação: ${occ.cardinalDirection ?: "N/D"}")
    appendLine("Notas: ${occ.userNotes}")
    occ.aiAnalysis?.let {
      appendLine()
      appendLine("Análise Técnica:")
      appendLine(it)
    }
  }

  val intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_SUBJECT, "Ocorrência Ambiental #${occ.id}")
    putExtra(Intent.EXTRA_TEXT, report)
  }
  context.startActivity(Intent.createChooser(intent, "Compartilhar ocorrência via"))
}

private fun shareExport(context: Context, content: String, mimeType: String, filename: String) {
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = mimeType
    putExtra(Intent.EXTRA_SUBJECT, "PKE Scanner Brasil - Exportação ($filename)")
    putExtra(Intent.EXTRA_TEXT, content)
  }
  context.startActivity(Intent.createChooser(intent, "Exportar dados via"))
}
