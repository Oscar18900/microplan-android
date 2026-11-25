package mx.buap.fcc.microplan.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import mx.buap.fcc.microplan.domain.model.SleepRigidity
import mx.buap.fcc.microplan.domain.model.UserPreferences

private val Context.microPlanDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "microplan_preferences"
)

class UserPreferencesDataSource(private val context: Context) {

  private object Keys {
    val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
    val CURRENT_USER_NAME = stringPreferencesKey("current_user_name")
    val CURRENT_USER_EMAIL = stringPreferencesKey("current_user_email")
    val CAREER_GROUP = stringPreferencesKey("career_group")
    val SLEEP_GOAL_HOURS = floatPreferencesKey("sleep_goal_hours")
    val DISCONNECT_ALERT_MINUTES = intPreferencesKey("disconnect_alert_minutes")
    val CURFEW_BEDTIME_MINUTES = intPreferencesKey("curfew_bedtime_minutes")
    val RIGIDITY = stringPreferencesKey("sleep_rigidity")
    val DAILY_POMODORO_CAPACITY = intPreferencesKey("daily_pomodoro_capacity")
    val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
    val XP_POINTS = intPreferencesKey("xp_points")
    val CURRENT_STREAK_DAYS = intPreferencesKey("current_streak_days")
    val TODAY_CURFEW_RESOLVED_DAY = longPreferencesKey("today_curfew_resolved_day")
    val EMERGENCY_ACTIVE_TODAY = booleanPreferencesKey("emergency_active_today")
    val SEED_INITIALIZED = booleanPreferencesKey("seed_initialized")
  }

  val preferencesFlow: Flow<UserPreferences> = context.microPlanDataStore.data.map { prefs ->
    val rigidityName = prefs[Keys.RIGIDITY] ?: SleepRigidity.BALANCED.name
    val rigidity = runCatching { SleepRigidity.valueOf(rigidityName) }.getOrDefault(SleepRigidity.BALANCED)
    UserPreferences(
      isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: false,
      currentUserId = prefs[Keys.CURRENT_USER_ID] ?: "",
      currentUserName = prefs[Keys.CURRENT_USER_NAME] ?: "Angel García",
      currentUserEmail = prefs[Keys.CURRENT_USER_EMAIL] ?: "estudiante@correo.edu.mx",
      careerGroup = prefs[Keys.CAREER_GROUP] ?: "Ciencias de la Computación · Grupo Piloto",
      sleepGoalHours = prefs[Keys.SLEEP_GOAL_HOURS] ?: 7.5f,
      disconnectAlertMinutes = prefs[Keys.DISCONNECT_ALERT_MINUTES] ?: (22 * 60 + 30),
      curfewBedtimeMinutes = prefs[Keys.CURFEW_BEDTIME_MINUTES] ?: (23 * 60),
      rigidity = rigidity,
      dailyPomodoroCapacity = prefs[Keys.DAILY_POMODORO_CAPACITY] ?: 4,
      darkModeEnabled = prefs[Keys.DARK_MODE_ENABLED] ?: false,
      xpPoints = prefs[Keys.XP_POINTS] ?: 340,
      currentStreakDays = prefs[Keys.CURRENT_STREAK_DAYS] ?: 5,
      todayCurfewResolvedEpochDay = prefs[Keys.TODAY_CURFEW_RESOLVED_DAY] ?: -1L,
      emergencyModeActiveToday = prefs[Keys.EMERGENCY_ACTIVE_TODAY] ?: false,
      seedInitialized = prefs[Keys.SEED_INITIALIZED] ?: false
    )
  }

  suspend fun getCurrent(): UserPreferences = preferencesFlow.first()

  suspend fun setLoggedInUser(
    userId: String,
    fullName: String,
    email: String,
    careerGroup: String,
    sleepGoalHours: Float,
    disconnectAlertMinutes: Int,
    curfewBedtimeMinutes: Int,
    rigidity: SleepRigidity
  ) {
    context.microPlanDataStore.edit { prefs ->
      prefs[Keys.IS_LOGGED_IN] = true
      prefs[Keys.CURRENT_USER_ID] = userId
      prefs[Keys.CURRENT_USER_NAME] = fullName
      prefs[Keys.CURRENT_USER_EMAIL] = email
      prefs[Keys.CAREER_GROUP] = careerGroup
      prefs[Keys.SLEEP_GOAL_HOURS] = sleepGoalHours
      prefs[Keys.DISCONNECT_ALERT_MINUTES] = disconnectAlertMinutes
      prefs[Keys.CURFEW_BEDTIME_MINUTES] = curfewBedtimeMinutes
      prefs[Keys.RIGIDITY] = rigidity.name
    }
  }

  suspend fun updateProfileInfo(fullName: String, careerGroup: String) {
    context.microPlanDataStore.edit { prefs ->
      prefs[Keys.CURRENT_USER_NAME] = fullName
      prefs[Keys.CAREER_GROUP] = careerGroup
    }
  }

  suspend fun updateSleepSettings(
    sleepGoalHours: Float,
    disconnectAlertMinutes: Int,
    curfewBedtimeMinutes: Int,
    rigidity: SleepRigidity
  ) {
    context.microPlanDataStore.edit { prefs ->
      prefs[Keys.SLEEP_GOAL_HOURS] = sleepGoalHours
      prefs[Keys.DISCONNECT_ALERT_MINUTES] = disconnectAlertMinutes
      prefs[Keys.CURFEW_BEDTIME_MINUTES] = curfewBedtimeMinutes
      prefs[Keys.RIGIDITY] = rigidity.name
    }
  }

  suspend fun setDarkMode(enabled: Boolean) {
    context.microPlanDataStore.edit { prefs ->
      prefs[Keys.DARK_MODE_ENABLED] = enabled
    }
  }

  suspend fun updateGamification(
    xpPoints: Int,
    currentStreakDays: Int,
    resolvedEpochDay: Long? = null,
    emergencyActiveToday: Boolean? = null
  ) {
    context.microPlanDataStore.edit { prefs ->
      prefs[Keys.XP_POINTS] = xpPoints.coerceAtLeast(0)
      prefs[Keys.CURRENT_STREAK_DAYS] = currentStreakDays.coerceAtLeast(0)
      if (resolvedEpochDay != null) {
        prefs[Keys.TODAY_CURFEW_RESOLVED_DAY] = resolvedEpochDay
      }
      if (emergencyActiveToday != null) {
        prefs[Keys.EMERGENCY_ACTIVE_TODAY] = emergencyActiveToday
      }
    }
  }

  suspend fun markSeedInitialized() {
    context.microPlanDataStore.edit { prefs ->
      prefs[Keys.SEED_INITIALIZED] = true
      if (prefs[Keys.XP_POINTS] == null) prefs[Keys.XP_POINTS] = 340
      if (prefs[Keys.CURRENT_STREAK_DAYS] == null) prefs[Keys.CURRENT_STREAK_DAYS] = 5
    }
  }

  suspend fun logout() {
    context.microPlanDataStore.edit { prefs ->
      prefs[Keys.IS_LOGGED_IN] = false
      prefs[Keys.CURRENT_USER_ID] = ""
    }
  }
}
