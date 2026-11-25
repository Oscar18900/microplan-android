package mx.buap.fcc.microplan.ui.wellness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.buap.fcc.microplan.domain.model.ProtectedBlock
import mx.buap.fcc.microplan.domain.model.ProtectedBlockType
import mx.buap.fcc.microplan.domain.model.Subtask
import mx.buap.fcc.microplan.domain.model.TimeFormatters
import mx.buap.fcc.microplan.domain.repository.AuthRepository
import mx.buap.fcc.microplan.domain.repository.PlannerRepository
import mx.buap.fcc.microplan.domain.repository.WellnessRepository
import java.time.LocalDate

sealed interface TimelineEntry {
  val sortMinutes: Int

  data class TaskEntry(
    val subtask: Subtask,
    val projectName: String,
    val isNextPending: Boolean,
    override val sortMinutes: Int
  ) : TimelineEntry

  data class ProtectedLeisureEntry(
    val block: ProtectedBlock,
    override val sortMinutes: Int
  ) : TimelineEntry
}

data class WellnessUiState(
  val streakDays: Int = 5,
  val sleepGoalHours: Float = 7.5f,
  val disconnectAlertMinutes: Int = 22 * 60 + 30,
  val curfewBedtimeMinutes: Int = 23 * 60,
  val isCurfewResolvedToday: Boolean = false,
  val emergencyModeActiveToday: Boolean = false,
  val timelineEntries: List<TimelineEntry> = emptyList(),
  val gainedSleepLabel: String = "+45m ganados vs ayer",
  val statusMessage: String? = null
) {
  val disconnectLabel: String
    get() = TimeFormatters.formatMinutesAs12Hour(disconnectAlertMinutes)

  val curfewLabel: String
    get() = TimeFormatters.formatMinutesAs12Hour(curfewBedtimeMinutes)
}

class WellnessViewModel(
  authRepository: AuthRepository,
  plannerRepository: PlannerRepository,
  private val wellnessRepository: WellnessRepository
) : ViewModel() {

  private val statusMessageFlow = MutableStateFlow<String?>(null)
  private val todayEpochDay: Long
    get() = LocalDate.now().toEpochDay()

  val uiState: StateFlow<WellnessUiState> = combine(
    authRepository.preferencesFlow,
    plannerRepository.observeProjects(),
    wellnessRepository.observeProtectedBlocks(),
    statusMessageFlow
  ) { prefs, projects, blocks, message ->
    val today = todayEpochDay
    val projectNames = projects.associate { it.id to it.title }
    val todayTasks = projects.flatMap { it.subtasks }.filter { it.assignedEpochDay == today }
    val firstPendingId = todayTasks.firstOrNull { !it.isCompleted }?.id

    val taskEntries = todayTasks.map { subtask ->
      TimelineEntry.TaskEntry(
        subtask = subtask,
        projectName = projectNames[subtask.projectId] ?: "Entrega académica",
        isNextPending = subtask.id == firstPendingId,
        sortMinutes = subtask.scheduledSlotMinutes
      )
    }

    val leisureBlocks = blocks
      .filter { it.isImmune || it.type == ProtectedBlockType.DAYTIME_LEISURE }
      .map { block ->
        TimelineEntry.ProtectedLeisureEntry(block = block, sortMinutes = block.startMinutes)
      }

    val combinedTimeline = (taskEntries + leisureBlocks).sortedBy { it.sortMinutes }
    val pendingTodayCount = todayTasks.count { !it.isCompleted }
    val gainedLabel = if (prefs.todayCurfewResolvedEpochDay == today && !prefs.emergencyModeActiveToday) {
      "+45m ganados vs ayer · Sueño blindado"
    } else if (pendingTodayCount == 0) {
      "+45m ganados vs ayer"
    } else {
      "Reprogramar $pendingTodayCount pendiente(s) protege +45m hoy"
    }

    WellnessUiState(
      streakDays = prefs.currentStreakDays,
      sleepGoalHours = prefs.sleepGoalHours,
      disconnectAlertMinutes = prefs.disconnectAlertMinutes,
      curfewBedtimeMinutes = prefs.curfewBedtimeMinutes,
      isCurfewResolvedToday = prefs.todayCurfewResolvedEpochDay == today && !prefs.emergencyModeActiveToday,
      emergencyModeActiveToday = prefs.emergencyModeActiveToday,
      timelineEntries = combinedTimeline,
      gainedSleepLabel = gainedLabel,
      statusMessage = message
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = WellnessUiState()
  )

  fun rescheduleBalanceAndSleep() {
    viewModelScope.launch {
      val movedCount = wellnessRepository.rescheduleBalanceAndSleep()
      statusMessageFlow.value = if (movedCount > 0) {
        "$movedCount tarea(s) movidas a mañana respetando capacidad. ¡Racha protegida y +40 XP!"
      } else {
        "Agenda cerrada a tiempo. ¡Descanso protegido y racha asegurada!"
      }
    }
  }

  fun activateEmergencyMode() {
    viewModelScope.launch {
      wellnessRepository.activateEmergencyMode()
      statusMessageFlow.value = "Modo emergencia activado: puedes continuar trabajando, pero perdiste la racha."
    }
  }

  fun triggerTestDisconnectNotification() {
    viewModelScope.launch {
      wellnessRepository.sendTestDisconnectNotification()
      statusMessageFlow.value = "Recordatorio de desconexión nocturna enviado (notificación local)."
    }
  }

  fun clearStatusMessage() {
    statusMessageFlow.value = null
  }
}
