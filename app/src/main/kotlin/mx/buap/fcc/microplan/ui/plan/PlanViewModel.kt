package mx.buap.fcc.microplan.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.buap.fcc.microplan.domain.model.AcademicProject
import mx.buap.fcc.microplan.domain.repository.AuthRepository
import mx.buap.fcc.microplan.domain.repository.PlannerRepository
import java.time.LocalDate

data class PlanUiState(
  val projects: List<AcademicProject> = emptyList(),
  val streakDays: Int = 5,
  val activityPrompt: String = "",
  val expandedProjectIds: Set<String> = setOf("proj-ensayo-metodologias"),
  val isProcessingAi: Boolean = false,
  val statusMessage: String? = null,
  val showManualProjectDialog: Boolean = false,
  val addSubtaskForProjectId: String? = null
) {
  val nearestDeadlineDays: Int
    get() {
      val today = LocalDate.now().toEpochDay()
      return projects.minOfOrNull { it.daysRemaining(today) } ?: 0
    }
}

class PlanViewModel(
  authRepository: AuthRepository,
  private val plannerRepository: PlannerRepository
) : ViewModel() {

  private val activityPromptFlow = MutableStateFlow("")
  private val expandedIdsFlow = MutableStateFlow(setOf("proj-ensayo-metodologias"))
  private val isProcessingAiFlow = MutableStateFlow(false)
  private val statusMessageFlow = MutableStateFlow<String?>(null)
  private val showManualProjectDialogFlow = MutableStateFlow(false)
  private val addSubtaskProjectFlow = MutableStateFlow<String?>(null)

  val uiState: StateFlow<PlanUiState> = combine(
    plannerRepository.observeProjects(),
    authRepository.preferencesFlow,
    activityPromptFlow,
    expandedIdsFlow,
    isProcessingAiFlow
  ) { projects, prefs, prompt, expanded, processing ->
    PlanUiState(
      projects = projects,
      streakDays = prefs.currentStreakDays,
      activityPrompt = prompt,
      expandedProjectIds = expanded,
      isProcessingAi = processing
    )
  }.combine(
    combine(statusMessageFlow, showManualProjectDialogFlow, addSubtaskProjectFlow) { msg, manualDlg, subProj ->
      Triple(msg, manualDlg, subProj)
    }
  ) { base, extras ->
    base.copy(
      statusMessage = extras.first,
      showManualProjectDialog = extras.second,
      addSubtaskForProjectId = extras.third
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = PlanUiState()
  )

  fun onPromptChange(value: String) {
    activityPromptFlow.value = value
  }

  fun toggleProjectExpanded(projectId: String) {
    expandedIdsFlow.update { current ->
      if (projectId in current) current - projectId else current + projectId
    }
  }

  fun openManualProjectDialog(show: Boolean) {
    showManualProjectDialogFlow.value = show
  }

  fun openAddSubtaskDialog(projectId: String?) {
    addSubtaskProjectFlow.value = projectId
  }

  fun clearStatusMessage() {
    statusMessageFlow.value = null
  }

  fun breakdownWithAi() {
    val prompt = activityPromptFlow.value.trim()
    if (prompt.isBlank()) {
      statusMessageFlow.value = "Escribe una actividad académica arriba para desglosarla automáticamente."
      return
    }
    viewModelScope.launch {
      isProcessingAiFlow.value = true
      val result = plannerRepository.createAiBreakdownProject(prompt)
      isProcessingAiFlow.value = false
      result.onSuccess { created ->
        activityPromptFlow.value = ""
        expandedIdsFlow.update { it + created.id }
        statusMessageFlow.value = "Desglose generado en modo local heurístico (${created.subtasks.size} subtareas)."
      }.onFailure { err ->
        statusMessageFlow.value = err.message ?: "No se pudo desglosar la entrega."
      }
    }
  }

  fun createManualProject(title: String, courseName: String, dueInDays: Int, estimatedPomodoros: Int) {
    viewModelScope.launch {
      val result = plannerRepository.createManualProject(title, courseName, dueInDays, estimatedPomodoros)
      result.onSuccess { created ->
        activityPromptFlow.value = ""
        showManualProjectDialogFlow.value = false
        expandedIdsFlow.update { it + created.id }
        statusMessageFlow.value = "Entrega agregada y balanceada antes de tu hora de desconexión."
      }.onFailure { err ->
        statusMessageFlow.value = err.message
      }
    }
  }

  fun addManualSubtask(projectId: String, title: String, dueInDays: Int, pomodoros: Int) {
    viewModelScope.launch {
      plannerRepository.addManualSubtask(projectId, title, dueInDays, pomodoros)
      addSubtaskProjectFlow.value = null
      statusMessageFlow.value = "Subtarea añadida y carga diaria rebalanceada."
    }
  }

  fun toggleSubtaskCompletion(subtaskId: String) {
    viewModelScope.launch {
      plannerRepository.toggleSubtaskCompletion(subtaskId)
    }
  }

  fun rebalanceAll() {
    viewModelScope.launch {
      plannerRepository.rebalanceAll()
      statusMessageFlow.value = "Agenda rebalanceada respetando tu bloque protegido y capacidad diaria."
    }
  }

  fun deleteProject(projectId: String) {
    viewModelScope.launch {
      plannerRepository.deleteProject(projectId)
      statusMessageFlow.value = "Entrega eliminada."
    }
  }
}
