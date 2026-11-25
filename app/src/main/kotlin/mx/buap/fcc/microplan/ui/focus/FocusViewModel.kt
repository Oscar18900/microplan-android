package mx.buap.fcc.microplan.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.buap.fcc.microplan.domain.model.PomodoroOutcome
import mx.buap.fcc.microplan.domain.model.PomodoroSession
import mx.buap.fcc.microplan.domain.model.Subtask
import mx.buap.fcc.microplan.domain.repository.AuthRepository
import mx.buap.fcc.microplan.domain.repository.FocusRepository
import mx.buap.fcc.microplan.domain.repository.PlannerRepository
import mx.buap.fcc.microplan.domain.repository.TelemetryRepository
import mx.buap.fcc.microplan.domain.model.TelemetryEventType
import java.util.Locale

data class FocusUiState(
  val selectedDurationMinutes: Int = 25,
  val remainingSeconds: Int = 25 * 60,
  val isRunning: Boolean = false,
  val antiSocialShieldEnabled: Boolean = true,
  val currentBlockNumber: Int = 1,
  val totalBlocksForTask: Int = 2,
  val availableTasks: List<Subtask> = emptyList(),
  val selectedSubtask: Subtask? = null,
  val courseSubtitle: String = "Enfoque universitario profundo",
  val showCompletionSheet: Boolean = false,
  val statusMessage: String? = null,
  val streakDays: Int = 5,
  val recentSessions: List<PomodoroSession> = emptyList()
) {
  val progressFraction: Float
    get() {
      val total = (selectedDurationMinutes * 60).coerceAtLeast(1)
      return (1f - (remainingSeconds.toFloat() / total.toFloat())).coerceIn(0f, 1f)
    }

  val formattedCountdown: String
    get() {
      val mins = remainingSeconds / 60
      val secs = remainingSeconds % 60
      return String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

class FocusViewModel(
  authRepository: AuthRepository,
  plannerRepository: PlannerRepository,
  private val focusRepository: FocusRepository,
  private val telemetryRepository: TelemetryRepository
) : ViewModel() {

  private val selectedDurationFlow = MutableStateFlow(25)
  private val remainingSecondsFlow = MutableStateFlow(25 * 60)
  private val isRunningFlow = MutableStateFlow(false)
  private val shieldEnabledFlow = MutableStateFlow(true)
  private val selectedSubtaskIdFlow = MutableStateFlow<String?>(null)
  private val showSheetFlow = MutableStateFlow(false)
  private val statusMessageFlow = MutableStateFlow<String?>(null)

  private var timerJob: Job? = null
  private var sessionStartMillis: Long = System.currentTimeMillis()

  val uiState: StateFlow<FocusUiState> = combine(
    plannerRepository.observeProjects(),
    focusRepository.observeRecentSessions(),
    authRepository.preferencesFlow,
    combine(selectedDurationFlow, remainingSecondsFlow, isRunningFlow, shieldEnabledFlow) { dur, rem, run, shield ->
      Quadruple(dur, rem, run, shield)
    },
    combine(selectedSubtaskIdFlow, showSheetFlow, statusMessageFlow) { subId, sheet, msg ->
      Triple(subId, sheet, msg)
    }
  ) { projects, sessions, prefs, timerState, uiExtras ->
    val allSubtasks = projects.flatMap { it.subtasks }
    val pendingTasks = allSubtasks.filter { !it.isCompleted }
    val pool = if (pendingTasks.isNotEmpty()) pendingTasks else allSubtasks
    val activeTask = pool.firstOrNull { it.id == uiExtras.first } ?: pool.firstOrNull()
    val parentProject = activeTask?.let { t -> projects.firstOrNull { it.id == t.projectId } }

    FocusUiState(
      selectedDurationMinutes = timerState.first,
      remainingSeconds = timerState.second,
      isRunning = timerState.third,
      antiSocialShieldEnabled = timerState.fourth,
      currentBlockNumber = ((activeTask?.completedPomodoros ?: 0) + 1).coerceAtLeast(1),
      totalBlocksForTask = (activeTask?.estimatedPomodoros ?: 1).coerceAtLeast(1),
      availableTasks = pool,
      selectedSubtask = activeTask,
      courseSubtitle = parentProject?.let { "${it.title} · ${it.courseName}" } ?: "Sesión libre de enfoque",
      showCompletionSheet = uiExtras.second,
      statusMessage = uiExtras.third,
      streakDays = prefs.currentStreakDays,
      recentSessions = sessions
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = FocusUiState()
  )

  fun selectSubtask(subtaskId: String?) {
    if (!subtaskId.isNullOrBlank()) {
      selectedSubtaskIdFlow.value = subtaskId
    }
  }

  fun selectDuration(minutes: Int) {
    pauseTimer()
    selectedDurationFlow.value = minutes
    remainingSecondsFlow.value = minutes * 60
  }

  fun toggleShield(enabled: Boolean) {
    shieldEnabledFlow.value = enabled
  }

  fun startOrPauseTimer() {
    if (isRunningFlow.value) {
      pauseTimer()
    } else {
      startTimer()
    }
  }

  private fun startTimer() {
    if (timerJob?.isActive == true) return
    if (remainingSecondsFlow.value <= 0) {
      remainingSecondsFlow.value = selectedDurationFlow.value * 60
    }
    sessionStartMillis = System.currentTimeMillis()
    isRunningFlow.value = true
    viewModelScope.launch {
      telemetryRepository.logEvent(
        TelemetryEventType.POMODORO_STARTED,
        """{"durationMinutes":${selectedDurationFlow.value}}"""
      )
    }
    timerJob = viewModelScope.launch {
      while (remainingSecondsFlow.value > 0 && isRunningFlow.value) {
        delay(1000L)
        if (isRunningFlow.value) {
          remainingSecondsFlow.update { (it - 1).coerceAtLeast(0) }
        }
      }
      if (remainingSecondsFlow.value == 0 && isRunningFlow.value) {
        isRunningFlow.value = false
        showSheetFlow.value = true
      }
    }
  }

  private fun pauseTimer() {
    isRunningFlow.value = false
    timerJob?.cancel()
    timerJob = null
  }

  fun resetTimer() {
    pauseTimer()
    remainingSecondsFlow.value = selectedDurationFlow.value * 60
  }

  fun simulatePomodoroEndDebug() {
    pauseTimer()
    remainingSecondsFlow.value = 0
    showSheetFlow.value = true
  }

  fun dismissCompletionSheet() {
    showSheetFlow.value = false
    resetTimer()
  }

  fun completeTaskFromSheet() {
    val snapshot = uiState.value
    viewModelScope.launch {
      focusRepository.recordPomodoroOutcome(
        subtaskId = snapshot.selectedSubtask?.id,
        subtaskTitle = snapshot.selectedSubtask?.title ?: "Sesión de enfoque",
        durationMinutes = snapshot.selectedDurationMinutes,
        startedAtMillis = sessionStartMillis,
        outcome = PomodoroOutcome.COMPLETED
      )
      showSheetFlow.value = false
      resetTimer()
      statusMessageFlow.value = "¡Micro-tarea completada! +25 XP ganados."
    }
  }

  fun extendAnotherBlock() {
    val snapshot = uiState.value
    viewModelScope.launch {
      focusRepository.recordPomodoroOutcome(
        subtaskId = snapshot.selectedSubtask?.id,
        subtaskTitle = snapshot.selectedSubtask?.title ?: "Sesión de enfoque",
        durationMinutes = snapshot.selectedDurationMinutes,
        startedAtMillis = sessionStartMillis,
        outcome = PomodoroOutcome.EXTENDED
      )
      showSheetFlow.value = false
      selectedDurationFlow.value = 25
      remainingSecondsFlow.value = 25 * 60
      startTimer()
      statusMessageFlow.value = "Nuevo bloque de +25 min iniciado (+25 XP)."
    }
  }

  fun pauseAndLoseStreak() {
    val snapshot = uiState.value
    viewModelScope.launch {
      focusRepository.recordPomodoroOutcome(
        subtaskId = snapshot.selectedSubtask?.id,
        subtaskTitle = snapshot.selectedSubtask?.title ?: "Sesión de enfoque",
        durationMinutes = snapshot.selectedDurationMinutes,
        startedAtMillis = sessionStartMillis,
        outcome = PomodoroOutcome.PAUSED_LOST_STREAK
      )
      showSheetFlow.value = false
      resetTimer()
      statusMessageFlow.value = "Sesión pausada. La racha diaria se reinició a 0."
    }
  }

  fun clearStatusMessage() {
    statusMessageFlow.value = null
  }

  override fun onCleared() {
    super.onCleared()
    timerJob?.cancel()
  }

  private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
