package com.example

import com.example.data.local.OccurrenceEntity
import com.example.data.model.SessionState
import com.example.ui.viewmodel.PkeScannerViewModel
import com.example.ui.viewmodel.TacticalNavDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PkeScannerRealFunctionTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `test inicializacao e estado padrao em portugues brasileiro`() = runTest {
    val viewModel = PkeScannerViewModel()
    val state = viewModel.uiState.value

    assertEquals(TacticalNavDestination.MONITORAR, state.currentTab)
    assertEquals(SessionState.PARADO, state.sessionState)
    assertEquals("00:00:00", state.formattedDuration)
    assertEquals(20, state.calibrationRemainingSeconds)
    assertNotNull(state.baseline)
    assertNotNull(state.thresholds)
    assertEquals(12.0f, state.thresholds.magneticDeltaUtd, 0.01f)
  }

  @Test
  fun `test alternancia de abas taticas`() = runTest {
    val viewModel = PkeScannerViewModel()

    viewModel.setTab(TacticalNavDestination.CAMERA)
    assertEquals(TacticalNavDestination.CAMERA, viewModel.uiState.value.currentTab)

    viewModel.setTab(TacticalNavDestination.REGISTROS)
    assertEquals(TacticalNavDestination.REGISTROS, viewModel.uiState.value.currentTab)

    viewModel.setTab(TacticalNavDestination.EQUIPAMENTO)
    assertEquals(TacticalNavDestination.EQUIPAMENTO, viewModel.uiState.value.currentTab)

    viewModel.setTab(TacticalNavDestination.MONITORAR)
    assertEquals(TacticalNavDestination.MONITORAR, viewModel.uiState.value.currentTab)
  }

  @Test
  fun `test ciclo de vida da sessao de monitoramento e calibracao`() = runTest {
    val viewModel = PkeScannerViewModel()

    viewModel.startMonitoring()
    assertEquals(SessionState.CALIBRANDO, viewModel.uiState.value.sessionState)
    assertTrue(viewModel.uiState.value.calibrationRemainingSeconds in 1..20)

    viewModel.pauseMonitoring()
    // Pausar durante monitoramento
    viewModel.stopMonitoring()
    assertEquals(SessionState.PARADO, viewModel.uiState.value.sessionState)
  }

  @Test
  fun `test exportacao de dados em formato JSON e CSV`() = runTest {
    val viewModel = PkeScannerViewModel()

    val json = viewModel.exportOccurrencesJson()
    assertTrue(json.startsWith("["))
    assertTrue(json.endsWith("]"))

    val csv = viewModel.exportOccurrencesCsv()
    assertTrue(csv.contains("ID;DataHora;Origem;Motivo"))
  }

  @Test
  fun `test avaliacao estruturada de ocorrencia com IA`() = runTest {
    val viewModel = PkeScannerViewModel()

    val occ = OccurrenceEntity(
      id = "TEST-01",
      timestamp = System.currentTimeMillis(),
      formattedDateTime = "19/09/2026 22:30:00",
      origin = "MANUAL",
      triggerReason = "Pico acústico e variação magnética no local de teste",
      possibleExplanations = "Fiação elétrica exposta ou manuseio manual do aparelho.",
      magneticFieldMicroTesla = 48.5f,
      baselineMagneticMicroTesla = 42.0f,
      audioDbfs = -52f,
      baselineAudioDbfs = -70f
    )

    viewModel.analyzeOccurrenceWithAi(occ)
    advanceTimeBy(1000)

    // Verifica que o feedback de conclusão foi emitido
    assertTrue(viewModel.uiState.value.feedbackMessage.contains("Avaliação técnica") || viewModel.uiState.value.feedbackMessage.isNotEmpty())
  }
}
