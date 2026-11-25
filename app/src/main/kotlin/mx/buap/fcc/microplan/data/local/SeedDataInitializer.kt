package mx.buap.fcc.microplan.data.local

import androidx.room.withTransaction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mx.buap.fcc.microplan.data.local.entity.LocalUserEntity
import mx.buap.fcc.microplan.data.local.entity.ProjectEntity
import mx.buap.fcc.microplan.data.local.entity.ProtectedBlockEntity
import mx.buap.fcc.microplan.data.local.entity.StreakDayEntity
import mx.buap.fcc.microplan.data.local.entity.SubtaskEntity
import mx.buap.fcc.microplan.domain.model.ProjectCategoryIcon
import mx.buap.fcc.microplan.domain.model.ProtectedBlockType
import mx.buap.fcc.microplan.domain.model.SleepRigidity
import mx.buap.fcc.microplan.domain.model.SyncStatus
import java.security.MessageDigest
import java.time.LocalDate

object PasswordHasher {
  fun sha256(raw: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }
}

class SeedDataInitializer(
  private val database: MicroPlanDatabase,
  private val prefs: UserPreferencesDataSource
) {
  private val mutex = Mutex()

  suspend fun ensureSeeded() {
    mutex.withLock {
      val currentPrefs = prefs.getCurrent()
      if (currentPrefs.seedInitialized && database.projectDao().countProjects() > 0) {
        return
      }

      val now = System.currentTimeMillis()
      val today = LocalDate.now().toEpochDay()

      database.withTransaction {
        if (database.userDao().countUsers() == 0) {
          database.userDao().upsert(
            LocalUserEntity(
              id = "user-demo-angel",
              fullName = "Angel García",
              email = "estudiante@correo.edu.mx",
              passwordHash = PasswordHasher.sha256("contraseña123"),
              careerGroup = "Ciencias de la Computación · Grupo Piloto",
              initials = "AG",
              sleepGoalHours = 7.5f,
              disconnectAlertMinutes = 22 * 60 + 30,
              curfewBedtimeMinutes = 23 * 60,
              rigidity = SleepRigidity.BALANCED,
              remoteId = null,
              syncStatus = SyncStatus.LOCAL_ONLY,
              updatedAt = now
            )
          )
        }

        if (database.protectedBlockDao().countBlocks() == 0) {
          database.protectedBlockDao().upsertAll(
            listOf(
              ProtectedBlockEntity(
                id = "block-leisure-default",
                title = "Ocio Diurno Protegido",
                type = ProtectedBlockType.DAYTIME_LEISURE,
                daysLabel = "Lun - Vie",
                startMinutes = 17 * 60, // 05:00 PM
                endMinutes = 18 * 60,   // 06:00 PM
                description = "Pausa activa & recarga cognitiva",
                tagsSerialized = "Paseo sin pantallas|Hidratación",
                isImmune = true,
                remoteId = null,
                syncStatus = SyncStatus.LOCAL_ONLY,
                updatedAt = now
              ),
              ProtectedBlockEntity(
                id = "block-classes-default",
                title = "Horario de Clases",
                type = ProtectedBlockType.CLASS_SCHEDULE,
                daysLabel = "Lun - Vie",
                startMinutes = 8 * 60,  // 08:00 AM
                endMinutes = 14 * 60,   // 02:00 PM
                description = "Carga académica presencial",
                tagsSerialized = "Clases BUAP",
                isImmune = false,
                remoteId = null,
                syncStatus = SyncStatus.LOCAL_ONLY,
                updatedAt = now
              )
            )
          )
        }

        if (database.projectDao().countProjects() == 0) {
          val projEssayId = "proj-ensayo-metodologias"
          val projDbId = "proj-practica-bd"
          val projNeuroId = "proj-diseno-cognitivo"

          database.projectDao().upsertAll(
            listOf(
              ProjectEntity(
                id = projEssayId,
                title = "Ensayo de Metodologías",
                courseName = "Seminario de Investigación",
                dueDateEpochDay = today + 3L,
                categoryIcon = ProjectCategoryIcon.BOOK,
                isAiGenerated = true,
                createdAtEpochDay = today - 1L,
                remoteId = null,
                syncStatus = SyncStatus.LOCAL_ONLY,
                updatedAt = now
              ),
              ProjectEntity(
                id = projDbId,
                title = "Práctica de Base de Datos",
                courseName = "Sistemas Distribuidos",
                dueDateEpochDay = today + 6L,
                categoryIcon = ProjectCategoryIcon.DATABASE,
                isAiGenerated = false,
                createdAtEpochDay = today,
                remoteId = null,
                syncStatus = SyncStatus.LOCAL_ONLY,
                updatedAt = now
              ),
              ProjectEntity(
                id = projNeuroId,
                title = "Diseño Experimental Cognitivo",
                courseName = "Neurobiología II",
                dueDateEpochDay = today + 12L,
                categoryIcon = ProjectCategoryIcon.LIGHTBULB,
                isAiGenerated = false,
                createdAtEpochDay = today,
                remoteId = null,
                syncStatus = SyncStatus.LOCAL_ONLY,
                updatedAt = now
              )
            )
          )

          database.subtaskDao().upsertAll(
            listOf(
              SubtaskEntity(
                id = "sub-1",
                projectId = projEssayId,
                title = "Definir objetivos",
                assignedEpochDay = today,
                scheduledSlotMinutes = 14 * 60 + 30,
                scheduledSlotLabel = "02:30 PM",
                estimatedPomodoros = 1,
                completedPomodoros = 1,
                isCompleted = true,
                isOverCapacityAlert = false,
                sortOrder = 0,
                syncStatus = SyncStatus.LOCAL_ONLY,
                updatedAt = now
              ),
              SubtaskEntity(
                id = "sub-2",
                projectId = projEssayId,
                title = "Investigar fuentes primarias",
                assignedEpochDay = today,
                scheduledSlotMinutes = 16 * 60,
                scheduledSlotLabel = "04:00 PM",
                estimatedPomodoros = 2,
                completedPomodoros = 2,
                isCompleted = true,
                isOverCapacityAlert = false,
                sortOrder = 1,
                syncStatus = SyncStatus.LOCAL_ONLY,
                updatedAt = now
              ),
              SubtaskEntity(
                id = "sub-3",
                projectId = projEssayId,
                title = "Redactar borrador",
                assignedEpochDay = today,
                scheduledSlotMinutes = 18 * 60,
                scheduledSlotLabel = "06:00 PM",
                estimatedPomodoros = 2,
                completedPomodoros = 0,
                isCompleted = false,
                isOverCapacityAlert = false,
                sortOrder = 2,
                syncStatus = SyncStatus.LOCAL_ONLY,
                updatedAt = now
              ),
              SubtaskEntity(
                id = "sub-4",
                projectId = projEssayId,
                title = "Integrar marco teórico y citas",
                assignedEpochDay = today + 1L,
                scheduledSlotMinutes = 16 * 60,
                scheduledSlotLabel = "04:00 PM",
                estimatedPomodoros = 3,
                completedPomodoros = 0,
                isCompleted = false,
                isOverCapacityAlert = true,
                sortOrder = 3,
                syncStatus = SyncStatus.LOCAL_ONLY,
                updatedAt = now
              ),
              SubtaskEntity(
                id = "sub-5",
                projectId = projEssayId,
                title = "Revisión final y cierre APA",
                assignedEpochDay = today + 2L,
                scheduledSlotMinutes = 18 * 60,
                scheduledSlotLabel = "06:00 PM",
                estimatedPomodoros = 1,
                completedPomodoros = 0,
                isCompleted = false,
                isOverCapacityAlert = false,
                sortOrder = 4,
                syncStatus = SyncStatus.LOCAL_ONLY,
                updatedAt = now
              ),
              SubtaskEntity(
                id = "sub-6",
                projectId = projDbId,
                title = "Normalizar esquema relacional (3FN)",
                assignedEpochDay = today + 2L,
                scheduledSlotMinutes = 16 * 60,
                scheduledSlotLabel = "04:00 PM",
                estimatedPomodoros = 2,
                completedPomodoros = 0,
                isCompleted = false,
                isOverCapacityAlert = false,
                sortOrder = 0,
                syncStatus = SyncStatus.LOCAL_ONLY,
                updatedAt = now
              ),
              SubtaskEntity(
                id = "sub-7",
                projectId = projDbId,
                title = "Implementar índices y consultas SQL",
                assignedEpochDay = today + 4L,
                scheduledSlotMinutes = 16 * 60,
                scheduledSlotLabel = "04:00 PM",
                estimatedPomodoros = 2,
                completedPomodoros = 0,
                isCompleted = false,
                isOverCapacityAlert = false,
                sortOrder = 1,
                syncStatus = SyncStatus.LOCAL_ONLY,
                updatedAt = now
              ),
              SubtaskEntity(
                id = "sub-8",
                projectId = projDbId,
                title = "Documentar pruebas de concurrencia",
                assignedEpochDay = today + 5L,
                scheduledSlotMinutes = 18 * 60,
                scheduledSlotLabel = "06:00 PM",
                estimatedPomodoros = 1,
                completedPomodoros = 0,
                isCompleted = false,
                isOverCapacityAlert = false,
                sortOrder = 2,
                syncStatus = SyncStatus.LOCAL_ONLY,
                updatedAt = now
              ),
              SubtaskEntity(
                id = "sub-9",
                projectId = projNeuroId,
                title = "Definir hipótesis y variables de estudio",
                assignedEpochDay = today + 6L,
                scheduledSlotMinutes = 16 * 60,
                scheduledSlotLabel = "04:00 PM",
                estimatedPomodoros = 2,
                completedPomodoros = 0,
                isCompleted = false,
                isOverCapacityAlert = false,
                sortOrder = 0,
                syncStatus = SyncStatus.LOCAL_ONLY,
                updatedAt = now
              ),
              SubtaskEntity(
                id = "sub-10",
                projectId = projNeuroId,
                title = "Diseño del instrumento y análisis piloto",
                assignedEpochDay = today + 9L,
                scheduledSlotMinutes = 18 * 60,
                scheduledSlotLabel = "06:00 PM",
                estimatedPomodoros = 2,
                completedPomodoros = 0,
                isCompleted = false,
                isOverCapacityAlert = false,
                sortOrder = 1,
                syncStatus = SyncStatus.LOCAL_ONLY,
                updatedAt = now
              )
            )
          )
        }

        if (database.streakHistoryDao().getAll().isEmpty()) {
          val seedStreakDays = (1L..5L).map { offset ->
            StreakDayEntity(
              epochDay = today - offset,
              completedTasksCount = 3,
              respectedCurfew = true,
              emergencyUsed = false,
              streakMaintained = true,
              xpEarned = 65,
              syncStatus = SyncStatus.LOCAL_ONLY,
              updatedAt = now
            )
          }
          database.streakHistoryDao().upsertAll(seedStreakDays)
        }
      }

      prefs.markSeedInitialized()
    }
  }
}
