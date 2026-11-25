package mx.buap.fcc.microplan.domain.usecase

import mx.buap.fcc.microplan.domain.model.StreakDayRecord
import mx.buap.fcc.microplan.domain.model.WeekDayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakEvaluatorUseCaseTest {

  private val useCase = StreakEvaluatorUseCase()

  @Test
  fun `rescheduling balance at curfew increments streak and grants XP`() {
    val (newStreak, newXp) = useCase.onCurfewSleepRescheduled(
      currentStreakDays = 5,
      currentXp = 340,
      alreadyResolvedToday = false
    )

    assertEquals(6, newStreak)
    assertEquals(380, newXp)
  }

  @Test
  fun `repeated curfew reschedule on same day does not double count streak`() {
    val (newStreak, newXp) = useCase.onCurfewSleepRescheduled(
      currentStreakDays = 6,
      currentXp = 380,
      alreadyResolvedToday = true
    )

    assertEquals(6, newStreak)
    assertEquals(395, newXp)
  }

  @Test
  fun `breaking streak via emergency mode resets streak to zero while preserving XP`() {
    val (newStreak, newXp) = useCase.onStreakBroken(currentXp = 340)

    assertEquals(0, newStreak)
    assertEquals(340, newXp)
  }

  @Test
  fun `toggling task completion adds and subtracts pomodoro XP`() {
    val xpCompleted = useCase.calculateXpAfterTaskToggle(
      wasCompleted = false,
      estimatedPomodoros = 2,
      currentXp = 300
    )
    assertEquals(350, xpCompleted)

    val xpUnchecked = useCase.calculateXpAfterTaskToggle(
      wasCompleted = true,
      estimatedPomodoros = 2,
      currentXp = xpCompleted
    )
    assertEquals(300, xpUnchecked)
  }

  @Test
  fun `weekly indicator marks previous days, TODAY, and pending future days`() {
    // 2026-11-26 is Thursday
    val thursday = LocalDate.of(2026, 11, 26).toEpochDay()
    val history = mapOf(
      (thursday - 3) to StreakDayRecord(thursday - 3, 2, true, false, true, 50),
      (thursday - 2) to StreakDayRecord(thursday - 2, 3, true, false, true, 65),
      (thursday - 1) to StreakDayRecord(thursday - 1, 1, false, true, false, 25)
    )

    val indicators = useCase.buildWeeklyIndicators(
      todayEpochDay = thursday,
      historyByDay = history,
      currentStreakDays = 0
    )

    assertEquals(7, indicators.size)
    assertEquals(listOf("L", "M", "X", "J", "V", "S", "D"), indicators.map { it.dayLetter })
    assertEquals(WeekDayStatus.COMPLETED, indicators[0].status)
    assertEquals(WeekDayStatus.COMPLETED, indicators[1].status)
    assertEquals(WeekDayStatus.MISSED, indicators[2].status)
    assertEquals(WeekDayStatus.TODAY, indicators[3].status)
    assertEquals(WeekDayStatus.PENDING, indicators[4].status)
    assertEquals(WeekDayStatus.PENDING, indicators[5].status)
    assertEquals(WeekDayStatus.PENDING, indicators[6].status)
  }

  @Test
  fun `gamification summary unlocks streak 5 and AI pioneer medals when earned`() {
    val summary = useCase.evaluateGamification(
      xpPoints = 340,
      currentStreakDays = 5,
      hasAiProject = true,
      hasProtectedLeisure = true,
      respectedCurfewCount = 0
    )

    assertEquals(3, summary.level)
    assertEquals(40, summary.xpInCurrentLevel)
    assertTrue(summary.medals.first { it.id == "streak_5" }.isUnlocked)
    assertTrue(summary.medals.first { it.id == "protected_leisure" }.isUnlocked)
    assertTrue(summary.medals.first { it.id == "ai_pioneer" }.isUnlocked)
    assertFalse(summary.medals.first { it.id == "zero_all_nighters" }.isUnlocked)
  }
}
