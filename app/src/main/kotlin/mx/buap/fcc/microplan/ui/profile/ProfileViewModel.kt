package mx.buap.fcc.microplan.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.buap.fcc.microplan.data.remote.SyncManager
import mx.buap.fcc.microplan.data.remote.SyncSummary
import mx.buap.fcc.microplan.domain.model.GamificationSummary
import mx.buap.fcc.microplan.domain.model.ProtectedBlock
import mx.buap.fcc.microplan.domain.model.ProtectedBlockType
import mx.buap.fcc.microplan.domain.model.SleepRigidity
import mx.buap.fcc.microplan.domain.model.TimeFormatters
import mx.buap.fcc.microplan.domain.repository.AuthRepository
import mx.buap.fcc.microplan.domain.repository.PlannerRepository
import mx.buap.fcc.microplan.domain.repository.TelemetryRepository
import mx.buap.fcc.microplan.domain.repository.WellnessRepository
import mx.buap.fcc.microplan.domain.usecase.StreakEvaluatorUseCase

data class ProfileUiState(
  val fullName: String = "Angel García",
  val initials: String = "AG",
  val careerGroup: String = "Ciencias de la Computación · Grupo Piloto",
  val email: String = "estudiante@correo.edu.mx",
  val sleepGoalHours: Float = 7.5f,
  val disconnectAlertMinutes: Int = 22 * 60 + 30,
  val curfewBedtimeMinutes: Int = 23 * 60,
  val rigidity: SleepRigidity = SleepRigidity.BALANCED,
  val darkModeEnabled: Boolean = false,
  val protectedBlocks: List<ProtectedBlock> = emptyList(),
  val gamification: GamificationSummary? = null,
  val pendingSyncCount: Int = 0,
  val syncStatusMessage: String? = null,
  val showEditProfileDialog: Boolean = false,
  val showSleepGoalDialog: Boolean = false,
  val showDisconnectTimeDialog: Boolean = false,
  val showCurfewTimeDialog: Boolean = false,
  val showRigidityDialog: Boolean = false,
  val editingProtectedBlock: ProtectedBlock? = null,
  val showNewBlockDialog: Boolean = false
)

class ProfileViewModel(
  private val authRepository: AuthRepository,
  plannerRepository: PlannerRepository,
  private val wellnessRepository: WellnessRepository,
  telemetryRepository: TelemetryRepository,
  private val syncManager: SyncManager,
  private val streakEvaluator: StreakEvaluatorUseCase
) : ViewModel() {

  private val dialogFlagsFlow = MutableStateFlow(DialogFlags())
  private val syncMessageFlow = MutableStateFlow<String?>(null)

  val uiState: StateFlow<ProfileUiState> = combine(
    authRepository.preferencesFlow,
    wellnessRepository.observeProtectedBlocks(),
    wellnessRepository.observeStreakHistory(),
    plannerRepository.observeProjects(),
    telemetryRepository.observePendingEventsCount()
  ) { prefs, blocks, history, projects, pendingTelemetry ->
    val gamification = streakEvaluator.evaluateGamification(
      xpPoints = prefs.xpPoints,
      currentStreakDays = prefs.currentStreakDays,
      hasAiProject = projects.any { it.isAiGenerated },
      hasProtectedLeisure = blocks.any { it.type == ProtectedBlockType.DAYTIME_LEISURE },
      respectedCurfewCount = history.count { it.respectedCurfew }
    )
    ProfileUiState(
      fullName = prefs.currentUserName,
      initials = TimeFormatters.computeInitials(prefs.currentUserName),
      careerGroup = prefs.careerGroup,
      email = prefs.currentUserEmail,
      sleepGoalHours = prefs.sleepGoalHours,
      disconnectAlertMinutes = prefs.disconnectAlertMinutes,
      curfewBedtimeMinutes = prefs.curfewBedtimeMinutes,
      rigidity = prefs.rigidity,
      darkModeEnabled = prefs.darkModeEnabled,
      protectedBlocks = blocks,
      gamification = gamification,
      pendingSyncCount = pendingTelemetry
    )
  }.combine(combine(dialogFlagsFlow, syncMessageFlow) { flags, msg -> flags to msg }) { base, extras ->
    val flags = extras.first
    base.copy(
      syncStatusMessage = extras.second,
      showEditProfileDialog = flags.editProfile,
      showSleepGoalDialog = flags.sleepGoal,
      showDisconnectTimeDialog = flags.disconnectTime,
      showCurfewTimeDialog = flags.curfewTime,
      showRigidityDialog = flags.rigidity,
      editingProtectedBlock = flags.editingBlock,
      showNewBlockDialog = flags.newBlock
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = ProfileUiState()
  )

  fun setEditProfileDialog(show: Boolean) {
    dialogFlagsFlow.value = dialogFlagsFlow.value.copy(editProfile = show)
  }

  fun setSleepGoalDialog(show: Boolean) {
    dialogFlagsFlow.value = dialogFlagsFlow.value.copy(sleepGoal = show)
  }

  fun setDisconnectTimeDialog(show: Boolean) {
    dialogFlagsFlow.value = dialogFlagsFlow.value.copy(disconnectTime = show)
  }

  fun setCurfewTimeDialog(show: Boolean) {
    dialogFlagsFlow.value = dialogFlagsFlow.value.copy(curfewTime = show)
  }

  fun setRigidityDialog(show: Boolean) {
    dialogFlagsFlow.value = dialogFlagsFlow.value.copy(rigidity = show)
  }

  fun openBlockEditor(block: ProtectedBlock?) {
    dialogFlagsFlow.value = dialogFlagsFlow.value.copy(editingBlock = block, newBlock = block == null)
  }

  fun closeBlockEditor() {
    dialogFlagsFlow.value = dialogFlagsFlow.value.copy(editingBlock = null, newBlock = false)
  }

  fun updateProfile(fullName: String, careerGroup: String) {
    viewModelScope.launch {
      authRepository.updateProfile(fullName, careerGroup)
      setEditProfileDialog(false)
      syncMessageFlow.value = "Perfil actualizado localmente."
    }
  }

  fun updateSleepGoal(hours: Float) {
    val s = uiState.value
    viewModelScope.launch {
      wellnessRepository.updateSleepSettings(hours, s.disconnectAlertMinutes, s.curfewBedtimeMinutes, s.rigidity)
      setSleepGoalDialog(false)
    }
  }

  fun updateDisconnectAlert(minutes: Int) {
    val s = uiState.value
    viewModelScope.launch {
      wellnessRepository.updateSleepSettings(s.sleepGoalHours, minutes, s.curfewBedtimeMinutes, s.rigidity)
      setDisconnectTimeDialog(false)
      syncMessageFlow.value = "Alerta nocturna reprogramada."
    }
  }

  fun updateCurfewBedtime(minutes: Int) {
    val s = uiState.value
    viewModelScope.launch {
      wellnessRepository.updateSleepSettings(s.sleepGoalHours, s.disconnectAlertMinutes, minutes, s.rigidity)
      setCurfewTimeDialog(false)
      syncMessageFlow.value = "Toque de queda actualizado."
    }
  }

  fun updateRigidity(rigidity: SleepRigidity) {
    val s = uiState.value
    viewModelScope.launch {
      wellnessRepository.updateSleepSettings(s.sleepGoalHours, s.disconnectAlertMinutes, s.curfewBedtimeMinutes, rigidity)
      setRigidityDialog(false)
    }
  }

  fun setDarkMode(enabled: Boolean) {
    viewModelScope.launch {
      authRepository.setDarkMode(enabled)
    }
  }

  fun saveProtectedBlock(
    id: String?,
    title: String,
    type: ProtectedBlockType,
    daysLabel: String,
    startMinutes: Int,
    endMinutes: Int,
    description: String,
    tagsText: String,
    isImmune: Boolean
  ) {
    viewModelScope.launch {
      wellnessRepository.upsertProtectedBlock(
        id = id,
        title = title,
        type = type,
        daysLabel = daysLabel,
        startMinutes = startMinutes,
        endMinutes = endMinutes,
        description = description,
        tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        isImmune = isImmune
      )
      closeBlockEditor()
      syncMessageFlow.value = "Bloque protegido guardado y agenda rebalanceada."
    }
  }

  fun deleteProtectedBlock(blockId: String) {
    viewModelScope.launch {
      wellnessRepository.deleteProtectedBlock(blockId)
      closeBlockEditor()
      syncMessageFlow.value = "Horario protegido eliminado."
    }
  }

  fun checkSyncQueue() {
    viewModelScope.launch {
      val summary: SyncSummary = syncManager.getPendingSummary()
      val total = summary.pendingProjects + summary.pendingSubtasks + summary.pendingSessions + summary.pendingTelemetryEvents
      syncMessageFlow.value = "Cola Offline-First: $total registros locales listos para Fase 2 Cloud."
    }
  }

  fun logout() {
    viewModelScope.launch {
      authRepository.logout()
    }
  }

  fun clearMessage() {
    syncMessageFlow.value = null
  }

  private data class DialogFlags(
    val editProfile: Boolean = false,
    val sleepGoal: Boolean = false,
    val disconnectTime: Boolean = false,
    val curfewTime: Boolean = false,
    val rigidity: Boolean = false,
    val editingBlock: ProtectedBlock? = null,
    val newBlock: Boolean = false
  )
}
