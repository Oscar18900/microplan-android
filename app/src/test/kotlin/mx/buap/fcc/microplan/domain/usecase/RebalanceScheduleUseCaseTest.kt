package mx.buap.fcc.microplan.domain.usecase

import mx.buap.fcc.microplan.domain.model.AcademicProject
import mx.buap.fcc.microplan.domain.model.ProjectCategoryIcon
import mx.buap.fcc.microplan.domain.model.ProtectedBlock
import mx.buap.fcc.microplan.domain.model.ProtectedBlockType
import mx.buap.fcc.microplan.domain.model.Subtask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RebalanceScheduleUseCaseTest {

  private val useCase = RebalanceScheduleUseCase()
  private val today = LocalDate.of(2026, 11, 24).toEpochDay()

  private val defaultBlocks = listOf(
    ProtectedBlock(
      id = "leisure",
      title = "Ocio Diurno Protegido",
      type = ProtectedBlockType.DAYTIME_LEISURE,
      daysLabel = "Lun - Vie",
      startMinutes = 17 * 60, // 5:00 PM
      endMinutes = 18 * 60,   // 6:00 PM
      description = "Descanso",
      tags = listOf("Paseo sin pantallas"),
      isImmune = true
    ),
    ProtectedBlock(
      id = "classes",
      title = "Horario de Clases",
      type = ProtectedBlockType.CLASS_SCHEDULE,
      daysLabel = "Lun - Vie",
      startMinutes = 8 * 60,  // 8:00 AM
      endMinutes = 14 * 60,   // 2:00 PM
      description = "Clases",
      tags = emptyList(),
      isImmune = false
    )
  )

  @Test
  fun `rebalance protects immune leisure block and avoids class hours`() {
    val project = AcademicProject(
      id = "p1",
      title = "Ensayo",
      courseName = "Seminario",
      dueDateEpochDay = today + 2,
      categoryIcon = ProjectCategoryIcon.BOOK,
      createdAtEpochDay = today
    )
    val subtasks = listOf(
      Subtask("s1", "p1", "Bloque A", today, 17 * 60, "05:00 PM", 2, isCompleted = false, sortOrder = 0),
      Subtask("s2", "p1", "Bloque B", today, 10 * 60, "10:00 AM", 2, isCompleted = false, sortOrder = 1)
    )

    val result = useCase.rebalance(
      subtasks = subtasks,
      projectsById = mapOf("p1" to project),
      protectedBlocks = defaultBlocks,
      todayEpochDay = today,
      dailyPomodoroCapacity = 4,
      disconnectAlertMinutes = 22 * 60 + 30
    )

    result.updatedSubtasks.forEach { task ->
      val start = task.scheduledSlotMinutes
      val end = start + (task.estimatedPomodoros * 30).coerceAtMost(90)
      // Must not overlap immune 17:00-18:00
      assertFalse(start < 18 * 60 && end > 17 * 60)
      // Must not overlap class hours 08:00-14:00
      assertFalse(start < 14 * 60 && end > 8 * 60)
    }
  }

  @Test
  fun `rebalance shifts extra load to lighter future days and flags over capacity`() {
    val project = AcademicProject(
      id = "p1",
      title = "Proyecto Grande",
      courseName = "Ingeniería",
      dueDateEpochDay = today + 2,
      categoryIcon = ProjectCategoryIcon.DATABASE,
      createdAtEpochDay = today
    )
    val subtasks = listOf(
      Subtask("s1", "p1", "Tarea 1", today, 14 * 60 + 30, "02:30 PM", 3, isCompleted = false, sortOrder = 0),
      Subtask("s2", "p1", "Tarea 2", today, 16 * 60, "04:00 PM", 2, isCompleted = false, sortOrder = 1),
      Subtask("s3", "p1", "Tarea 3", today, 18 * 60, "06:00 PM", 3, isCompleted = false, sortOrder = 2)
    )

    val result = useCase.rebalance(
      subtasks = subtasks,
      projectsById = mapOf("p1" to project),
      protectedBlocks = defaultBlocks,
      todayEpochDay = today,
      dailyPomodoroCapacity = 4,
      disconnectAlertMinutes = 22 * 60 + 30
    )

    // Total 8 Pomodoros should spread across days rather than leaving all 3 on today
    val onToday = result.updatedSubtasks.filter { it.assignedEpochDay == today }
    assertTrue(onToday.size < 3)
    // Tasks with 3 Pomodoros receive Carga Ajustada alert
    assertTrue(result.updatedSubtasks.first { it.id == "s1" }.isOverCapacityAlert)
  }

  @Test
  fun `reschedule pending today to tomorrow moves incomplete today tasks forward`() {
    val project = AcademicProject(
      id = "p1",
      title = "Ensayo",
      courseName = "Seminario",
      dueDateEpochDay = today + 3,
      categoryIcon = ProjectCategoryIcon.BOOK,
      createdAtEpochDay = today
    )
    val subtasks = listOf(
      Subtask("s-done", "p1", "Ya hecha", today, 14 * 60 + 30, "02:30 PM", 1, completedPomodoros = 1, isCompleted = true, sortOrder = 0),
      Subtask("s-pending", "p1", "Pendiente hoy", today, 18 * 60, "06:00 PM", 2, isCompleted = false, sortOrder = 1)
    )

    val result = useCase.reschedulePendingTodayToTomorrow(
      allSubtasks = subtasks,
      projectsById = mapOf("p1" to project),
      protectedBlocks = defaultBlocks,
      todayEpochDay = today,
      dailyPomodoroCapacity = 4,
      disconnectAlertMinutes = 22 * 60 + 30
    )

    val done = result.updatedSubtasks.first { it.id == "s-done" }
    val moved = result.updatedSubtasks.first { it.id == "s-pending" }
    assertEquals(today, done.assignedEpochDay)
    assertEquals(today + 1, moved.assignedEpochDay)
  }
}
