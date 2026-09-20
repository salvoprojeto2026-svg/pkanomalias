package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.components.TacticalBottomBar
import com.example.ui.screens.ArCameraScreen
import com.example.ui.screens.EquipmentScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceDark
import com.example.ui.viewmodel.PkeScannerViewModel
import com.example.ui.viewmodel.TacticalNavDestination

class MainActivity : ComponentActivity() {
  private val viewModel: PkeScannerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        PkeAppRoot(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun PkeAppRoot(viewModel: PkeScannerViewModel) {
  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = SurfaceDark,
    bottomBar = {
      TacticalBottomBar(
        currentTab = uiState.currentTab,
        onTabSelected = { viewModel.setTab(it) }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(SurfaceDark)
        .padding(bottom = innerPadding.calculateBottomPadding())
    ) {
      Crossfade(
        targetState = uiState.currentTab,
        label = "tabCrossfade"
      ) { tab ->
        when (tab) {
          TacticalNavDestination.MONITORAR -> {
            ScannerScreen(uiState = uiState, viewModel = viewModel)
          }
          TacticalNavDestination.CAMERA -> {
            ArCameraScreen(uiState = uiState, viewModel = viewModel)
          }
          TacticalNavDestination.REGISTROS -> {
            VaultScreen(uiState = uiState, viewModel = viewModel)
          }
          TacticalNavDestination.EQUIPAMENTO -> {
            EquipmentScreen(uiState = uiState, viewModel = viewModel)
          }
        }
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  androidx.compose.material3.Text(text = "PKE Scanner Brasil $name!", modifier = modifier)
}
