package mx.buap.fcc.microplan.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.buap.fcc.microplan.domain.model.DayStreakIndicator
import mx.buap.fcc.microplan.domain.model.GamificationSummary
import mx.buap.fcc.microplan.domain.model.ProtectedBlockType
import mx.buap.fcc.microplan.domain.model.Subtask
import mx.buap.fcc.microplan.domain.repository.AuthRepository
import mx.buap.fcc.microplan.domain.repository.PlannerRepository
import mx.buap.fcc.microplan.domain.repository.WellnessRepository
import mx.buap.fcc.microplan.domain.usecase.StreakEvaluatorUseCase
import java.time.LocalDate

data class DashboardUiState(
  val streakDays: Int = 5,
  val weeklyIndicators: List<DayStreakIndicator> = emptyList(),
  val gamification: GamificationSummary? = null,
  val todayTasks: List<Subtask> = emptyList(),
  val completedTodayCount: Int = 0,
  val pendingTodayCount: Int = 0,
  val pendingPomodorosCount: Int = 0,
  val completionProgress: Float = 0f,
  val emergencyActiveToday: Boolean = false
)

class DashboardViewModel(
  authRepository: AuthRepository,
  private val plannerRepository: PlannerRepository,
  wellnessRepository: WellnessRepository,
  private val streakEvaluator: StreakEvaluatorUseCase
) : ViewModel() {

  private val todayEpochDay: Long
    get() = LocalDate.now().toEpochDay()

  val uiState: StateFlow<DashboardUiState> = combine(
    authRepository.preferencesFlow,
    plannerRepository.observeProjects(),
    plannerRepository.observeSubtasksForDay(todayEpochDay),
    wellnessRepository.observeProtectedBlocks(),
    wellnessRepository.observeStreakHistory()
  ) { prefs, projects, todayTasks, blocks, history ->
    val historyMap = history.associateBy { it.epochDay }
    val weekly = streakEvaluator.buildWeeklyIndicators(
      todayEpochDay = todayEpochDay,
      historyByDay = historyMap,
      currentStreakDays = prefs.currentStreakDays
    )
    val gamification = streakEvaluator.evaluateGamification(
      xpPoints = prefs.xpPoints,
      currentStreakDays = prefs.currentStreakDays,
      hasAiProject = projects.any { it.isAiGenerated },
      hasProtectedLeisure = blocks.any { it.type == ProtectedBlockType.DAYTIME_LEISURE },
      respectedCurfewCount = history.count { it.respectedCurfew }
    )
    val completed = todayTasks.count { it.isCompleted }
    val pendingList = todayTasks.filter { !it.isCompleted }
    val total = todayTasks.size
    val progress = if (total > 0) completed.toFloat() / total.toFloat() else 1f

    DashboardUiState(
      streakDays = prefs.currentStreakDays,
      weeklyIndicators = weekly,
      gamification = gamification,
      todayTasks = todayTasks,
      completedTodayCount = completed,
      pendingTodayCount = pendingList.size,
      pendingPomodorosCount = pendingList.sumOf { it.estimatedPomodoros },
      completionProgress = progress,
      emergencyActiveToday = prefs.emergencyModeActiveToday
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = DashboardUiState()
  )

  fun toggleSubtask(subtaskId: String) {
    viewModelScope.launch {
      plannerRepository.toggleSubtaskCompletion(subtaskId)
    }
  }
}
