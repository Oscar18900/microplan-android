package mx.buap.fcc.microplan.domain.usecase

import mx.buap.fcc.microplan.domain.model.DayStreakIndicator
import mx.buap.fcc.microplan.domain.model.GamificationSummary
import mx.buap.fcc.microplan.domain.model.Medal
import mx.buap.fcc.microplan.domain.model.StreakDayRecord
import mx.buap.fcc.microplan.domain.model.WeekDayStatus
import java.time.LocalDate

class StreakEvaluatorUseCase {

  fun evaluateGamification(
    xpPoints: Int,
    currentStreakDays: Int,
    hasAiProject: Boolean,
    hasProtectedLeisure: Boolean,
    respectedCurfewCount: Int
  ): GamificationSummary {
    val safeXp = xpPoints.coerceAtLeast(0)
    val level = (safeXp / XP_PER_LEVEL) + 1
    val xpInLevel = safeXp % XP_PER_LEVEL
    val levelTitle = when (level) {
      1 -> "Estudiante Enfocado"
      2 -> "Planificador Constante"
      3 -> "Guardián del Sueño"
      4 -> "Estratega Circadiano"
      else -> "Maestro del Enfoque"
    }

    val medals = listOf(
      Medal(
        id = "streak_5",
        title = "Racha 5 Días",
        description = "Mantén 5 días seguidos cumpliendo bloques y descanso",
        isUnlocked = currentStreakDays >= 5
      ),
      Medal(
        id = "protected_leisure",
        title = "Descanso Blindado",
        description = "Reserva y protege un bloque diario de ocio sin pantallas",
        isUnlocked = hasProtectedLeisure
      ),
      Medal(
        id = "zero_all_nighters",
        title = "Cero Desvelos",
        description = "Respeta la alerta de desconexión nocturna al dormir",
        isUnlocked = respectedCurfewCount >= 1
      ),
      Medal(
        id = "ai_pioneer",
        title = "Pionero MicroPlan",
        description = "Desglosa una entrega académica con asistencia inteligente",
        isUnlocked = hasAiProject
      )
    )

    return GamificationSummary(
      xpPoints = safeXp,
      level = level,
      levelTitle = levelTitle,
      xpInCurrentLevel = xpInLevel,
      xpForNextLevel = XP_PER_LEVEL,
      medals = medals
    )
  }

  fun buildWeeklyIndicators(
    todayEpochDay: Long,
    historyByDay: Map<Long, StreakDayRecord>,
    currentStreakDays: Int
  ): List<DayStreakIndicator> {
    val today = LocalDate.ofEpochDay(todayEpochDay)
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val dayLetters = listOf("L", "M", "X", "J", "V", "S", "D")

    return dayLetters.mapIndexed { index, letter ->
      val dayDate = monday.plusDays(index.toLong())
      val epoch = dayDate.toEpochDay()
      val record = historyByDay[epoch]
      val status = when {
        epoch == todayEpochDay -> {
          if (record?.respectedCurfew == true && record.streakMaintained) {
            WeekDayStatus.COMPLETED
          } else {
            WeekDayStatus.TODAY
          }
        }
        epoch > todayEpochDay -> WeekDayStatus.PENDING
        record != null -> {
          if (record.streakMaintained && !record.emergencyUsed) {
            WeekDayStatus.COMPLETED
          } else {
            WeekDayStatus.MISSED
          }
        }
        else -> {
          val daysAgo = (todayEpochDay - epoch).toInt()
          if (daysAgo in 1..currentStreakDays) WeekDayStatus.COMPLETED else WeekDayStatus.MISSED
        }
      }
      DayStreakIndicator(dayLetter = letter, epochDay = epoch, status = status)
    }
  }

  fun calculateXpAfterTaskToggle(
    wasCompleted: Boolean,
    estimatedPomodoros: Int,
    currentXp: Int
  ): Int {
    val delta = estimatedPomodoros.coerceAtLeast(1) * XP_PER_POMODORO
    return if (!wasCompleted) currentXp + delta else (currentXp - delta).coerceAtLeast(0)
  }

  fun onPomodoroCompleted(currentXp: Int): Int = currentXp + XP_PER_POMODORO

  fun onCurfewSleepRescheduled(
    currentStreakDays: Int,
    currentXp: Int,
    alreadyResolvedToday: Boolean
  ): Pair<Int, Int> {
    return if (!alreadyResolvedToday) {
      (currentStreakDays + 1) to (currentXp + XP_CURFEW_PROTECTED)
    } else {
      currentStreakDays to (currentXp + 15)
    }
  }

  fun onStreakBroken(currentXp: Int): Pair<Int, Int> = 0 to currentXp

  companion object {
    const val XP_PER_POMODORO = 25
    const val XP_CURFEW_PROTECTED = 40
    const val XP_PER_LEVEL = 150
  }
}
