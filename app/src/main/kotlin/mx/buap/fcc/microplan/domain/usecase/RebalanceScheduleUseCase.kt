package mx.buap.fcc.microplan.domain.usecase

import mx.buap.fcc.microplan.domain.model.AcademicProject
import mx.buap.fcc.microplan.domain.model.ProtectedBlock
import mx.buap.fcc.microplan.domain.model.ProtectedBlockType
import mx.buap.fcc.microplan.domain.model.Subtask
import mx.buap.fcc.microplan.domain.model.SyncStatus
import mx.buap.fcc.microplan.domain.model.TimeFormatters

data class RebalanceResult(
  val updatedSubtasks: List<Subtask>,
  val hasOverCapacityDays: Boolean,
  val overCapacityDayEpochs: Set<Long>
)

/**
 * Distribuye y rebalancea subtareas académicas respetando:
 * 1. Bloques protegidos inmunes (Ocio Diurno Protegido 05:00 PM - 06:00 PM) y Horario de Clases.
 * 2. Hora límite de estudio antes de la desconexión nocturna (idealmente <= 19:00 h).
 * 3. Capacidad máxima diaria de Pomodoros (etiquetando "Carga Ajustada" si se supera).
 */
class RebalanceScheduleUseCase {

  fun rebalance(
    subtasks: List<Subtask>,
    projectsById: Map<String, AcademicProject>,
    protectedBlocks: List<ProtectedBlock>,
    todayEpochDay: Long,
    dailyPomodoroCapacity: Int,
    disconnectAlertMinutes: Int,
    nowMillis: Long = System.currentTimeMillis(),
    minStartEpochDay: Long = todayEpochDay
  ): RebalanceResult {
    val safeCapacity = dailyPomodoroCapacity.coerceAtLeast(1)
    val completed = subtasks.filter { it.isCompleted }
    val incomplete = subtasks.filter { !it.isCompleted }

    // Ordenar pendientes: primero fecha de entrega más cercana, luego sortOrder.
    val orderedPending = incomplete.sortedWith(
      compareBy<Subtask>(
        { projectsById[it.projectId]?.dueDateEpochDay ?: it.assignedEpochDay },
        { it.assignedEpochDay },
        { it.sortOrder }
      )
    )

    // Carga inicial por día contando tareas ya completadas en cada fecha.
    val loadByDay = mutableMapOf<Long, Int>()
    completed.forEach { task ->
      loadByDay[task.assignedEpochDay] = (loadByDay[task.assignedEpochDay] ?: 0) + task.estimatedPomodoros
    }

    val assignedList = mutableListOf<Subtask>()
    for (task in orderedPending) {
      val preferredDay = task.assignedEpochDay.coerceAtLeast(minStartEpochDay)
      val projectDueDay = (projectsById[task.projectId]?.dueDateEpochDay ?: preferredDay)
        .coerceAtLeast(preferredDay)

      // Buscar día óptimo desde preferredDay hasta dueDateEpochDay sin rebasar capacidad.
      var chosenDay = preferredDay
      val currentDayLoad = loadByDay[preferredDay] ?: 0
      if (currentDayLoad + task.estimatedPomodoros > safeCapacity) {
        val candidate = (preferredDay..projectDueDay).firstOrNull { day ->
          (loadByDay[day] ?: 0) + task.estimatedPomodoros <= safeCapacity
        }
        if (candidate != null) {
          chosenDay = candidate
        }
      }

      loadByDay[chosenDay] = (loadByDay[chosenDay] ?: 0) + task.estimatedPomodoros
      assignedList += task.copy(assignedEpochDay = chosenDay)
    }

    // Recalcular carga final por día para marcar alertas "Carga Ajustada" y asignar franjas horarias libres.
    val finalDayLoads = mutableMapOf<Long, Int>()
    (completed + assignedList).forEach { task ->
      finalDayLoads[task.assignedEpochDay] = (finalDayLoads[task.assignedEpochDay] ?: 0) + task.estimatedPomodoros
    }

    val overCapacityDays = finalDayLoads.filterValues { it > safeCapacity }.keys
    val dayOccupiedSlots = mutableMapOf<Long, MutableList<Pair<Int, Int>>>()

    // Registrar bloques completados del día en las franjas ocupadas
    completed.forEach { done ->
      val list = dayOccupiedSlots.getOrPut(done.assignedEpochDay) { mutableListOf() }
      val duration = (done.estimatedPomodoros.coerceAtLeast(1) * 30).coerceAtMost(90)
      list += done.scheduledSlotMinutes to (done.scheduledSlotMinutes + duration)
    }

    val finalizedPending = assignedList.map { task ->
      val day = task.assignedEpochDay
      val occupied = dayOccupiedSlots.getOrPut(day) { mutableListOf() }
      val blockDuration = (task.estimatedPomodoros.coerceAtLeast(1) * 30).coerceAtMost(90)
      val chosenStart = pickAvailableSlot(
        existingTaskIntervals = occupied,
        protectedBlocks = protectedBlocks,
        blockDurationMinutes = blockDuration,
        disconnectAlertMinutes = disconnectAlertMinutes
      )
      occupied += chosenStart to (chosenStart + blockDuration)

      val overLoaded = (day in overCapacityDays) || task.estimatedPomodoros >= 3
      val newSlotLabel = TimeFormatters.formatMinutesAs12Hour(chosenStart)

      val changed = chosenStart != task.scheduledSlotMinutes ||
        newSlotLabel != task.scheduledSlotLabel ||
        overLoaded != task.isOverCapacityAlert ||
        subtasks.firstOrNull { it.id == task.id }?.assignedEpochDay != day

      if (changed) {
        task.copy(
          scheduledSlotMinutes = chosenStart,
          scheduledSlotLabel = newSlotLabel,
          isOverCapacityAlert = overLoaded,
          syncStatus = if (task.syncStatus == SyncStatus.SYNCED) SyncStatus.PENDING_SYNC else task.syncStatus,
          updatedAt = nowMillis
        )
      } else {
        task
      }
    }

    return RebalanceResult(
      updatedSubtasks = (completed + finalizedPending).sortedWith(compareBy({ it.assignedEpochDay }, { it.scheduledSlotMinutes }, { it.sortOrder })),
      hasOverCapacityDays = overCapacityDays.isNotEmpty(),
      overCapacityDayEpochs = overCapacityDays
    )
  }

  fun reschedulePendingTodayToTomorrow(
    allSubtasks: List<Subtask>,
    projectsById: Map<String, AcademicProject>,
    protectedBlocks: List<ProtectedBlock>,
    todayEpochDay: Long,
    dailyPomodoroCapacity: Int,
    disconnectAlertMinutes: Int,
    nowMillis: Long = System.currentTimeMillis()
  ): RebalanceResult {
    val tomorrow = todayEpochDay + 1L
    val shifted = allSubtasks.map { task ->
      if (!task.isCompleted && task.assignedEpochDay <= todayEpochDay) {
        task.copy(
          assignedEpochDay = tomorrow,
          syncStatus = SyncStatus.PENDING_SYNC,
          updatedAt = nowMillis
        )
      } else {
        task
      }
    }
    return rebalance(
      subtasks = shifted,
      projectsById = projectsById,
      protectedBlocks = protectedBlocks,
      todayEpochDay = todayEpochDay,
      dailyPomodoroCapacity = dailyPomodoroCapacity,
      disconnectAlertMinutes = disconnectAlertMinutes,
      nowMillis = nowMillis,
      minStartEpochDay = tomorrow
    )
  }

  private fun pickAvailableSlot(
    existingTaskIntervals: List<Pair<Int, Int>>,
    protectedBlocks: List<ProtectedBlock>,
    blockDurationMinutes: Int,
    disconnectAlertMinutes: Int
  ): Int {
    // Franjas vespertinas preferentes para terminar antes de las 19:00 h y nunca tras la desconexión.
    val candidateStarts = listOf(
      14 * 60 + 30, // 02:30 PM
      15 * 60 + 30, // 03:30 PM
      16 * 60,      // 04:00 PM
      18 * 60,      // 06:00 PM
      18 * 60 + 30, // 06:30 PM
      19 * 60,      // 07:00 PM
      20 * 60,      // 08:00 PM
      21 * 60,      // 09:00 PM
      9 * 60,       // 09:00 AM (fines de semana o sin clase)
      11 * 60       // 11:00 AM
    )

    val blockingIntervals = protectedBlocks
      .filter { it.isImmune || it.type == ProtectedBlockType.CLASS_SCHEDULE || it.type == ProtectedBlockType.DAYTIME_LEISURE }
      .map { it.startMinutes to it.endMinutes } + existingTaskIntervals

    for (start in candidateStarts) {
      val end = start + blockDurationMinutes
      if (end > disconnectAlertMinutes) continue
      val overlaps = blockingIntervals.any { (bStart, bEnd) ->
        start < bEnd && end > bStart
      }
      if (!overlaps) return start
    }

    // Respaldo seguro antes de la hora de desconexión evitando bloques inmunes
    val fallback = (18 * 60).coerceAtMost((disconnectAlertMinutes - blockDurationMinutes).coerceAtLeast(8 * 60))
    val immuneConflict = protectedBlocks.filter { it.isImmune }.any { block ->
      fallback < block.endMinutes && (fallback + blockDurationMinutes) > block.startMinutes
    }
    return if (immuneConflict) 18 * 60 else fallback
  }
}
