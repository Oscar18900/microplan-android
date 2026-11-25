package mx.buap.fcc.microplan.data.remote

import kotlinx.serialization.Serializable
import mx.buap.fcc.microplan.domain.model.ProjectCategoryIcon

@Serializable
data class AiBreakdownRequestDto(
  val activityPrompt: String,
  val dailyPomodoroCapacity: Int,
  val availableDaysBeforeDeadline: Int = 4,
  val preferredEndHour24: Int = 19
)

@Serializable
data class AiSubtaskDto(
  val title: String,
  val dayOffset: Int,
  val estimatedPomodoros: Int
)

@Serializable
data class AiBreakdownResponseDto(
  val projectTitle: String,
  val suggestedCourse: String,
  val recommendedDeadlineDays: Int,
  val categoryHint: String,
  val sourceMode: String,
  val subtasks: List<AiSubtaskDto>
)

/**
 * Interfaz lista para conectarse a un endpoint de IA (p. ej. Gemini / Backend REST) en Fase 2.
 */
interface AiBreakdownService {
  suspend fun breakdownActivity(request: AiBreakdownRequestDto): Result<AiBreakdownResponseDto>
}

/**
 * Motor heurístico local y plantillas académicas realistas para operar 100% offline.
 */
class LocalHeuristicAiBreakdownService : AiBreakdownService {

  override suspend fun breakdownActivity(request: AiBreakdownRequestDto): Result<AiBreakdownResponseDto> {
    val cleanPrompt = request.activityPrompt.trim()
    if (cleanPrompt.isBlank()) {
      return Result.failure(IllegalArgumentException("Describe la actividad académica para desglosar."))
    }

    val lower = cleanPrompt.lowercase()
    val response = when {
      lower.containsAny("ensayo", "redac", "reseña", "artículo", "articulo", "tesis", "monografía") ->
        AiBreakdownResponseDto(
          projectTitle = cleanPrompt.replaceFirstChar { it.uppercase() },
          suggestedCourse = "Seminario de Investigación",
          recommendedDeadlineDays = 4,
          categoryHint = ProjectCategoryIcon.BOOK.name,
          sourceMode = "LOCAL_HEURISTIC",
          subtasks = listOf(
            AiSubtaskDto("Delimitar tesis y fuentes académicas", dayOffset = 0, estimatedPomodoros = 2),
            AiSubtaskDto("Estructurar esquema de argumentos y citas", dayOffset = 1, estimatedPomodoros = 2),
            AiSubtaskDto("Redactar borrador completo sin distracciones", dayOffset = 2, estimatedPomodoros = 3),
            AiSubtaskDto("Revisión de estilo, coherencia y formato APA", dayOffset = 3, estimatedPomodoros = 1)
          )
        )

      lower.containsAny("base de datos", "sql", "código", "codigo", "program", "app", "software", "práctica", "practica", "api") ->
        AiBreakdownResponseDto(
          projectTitle = cleanPrompt.replaceFirstChar { it.uppercase() },
          suggestedCourse = "Ingeniería de Software / BD",
          recommendedDeadlineDays = 5,
          categoryHint = ProjectCategoryIcon.DATABASE.name,
          sourceMode = "LOCAL_HEURISTIC",
          subtasks = listOf(
            AiSubtaskDto("Analizar requisitos y modelar arquitectura", dayOffset = 0, estimatedPomodoros = 2),
            AiSubtaskDto("Implementar lógica central y persistencia", dayOffset = 1, estimatedPomodoros = 2),
            AiSubtaskDto("Ejecutar pruebas unitarias y depurar casos borde", dayOffset = 3, estimatedPomodoros = 2),
            AiSubtaskDto("Documentar entrega y preparar demostración", dayOffset = 4, estimatedPomodoros = 1)
          )
        )

      lower.containsAny("examen", "parcial", "cuestionario", "repaso", "cálculo", "calculo", "álgebra", "algebra", "física", "fisica") ->
        AiBreakdownResponseDto(
          projectTitle = cleanPrompt.replaceFirstChar { it.uppercase() },
          suggestedCourse = "Preparación de Evaluación",
          recommendedDeadlineDays = 4,
          categoryHint = ProjectCategoryIcon.LIGHTBULB.name,
          sourceMode = "LOCAL_HEURISTIC",
          subtasks = listOf(
            AiSubtaskDto("Mapear temario y detectar temas críticos", dayOffset = 0, estimatedPomodoros = 1),
            AiSubtaskDto("Resolver bloque de ejercicios guiados", dayOffset = 1, estimatedPomodoros = 2),
            AiSubtaskDto("Simulacro cronometrado sin apuntes", dayOffset = 2, estimatedPomodoros = 2),
            AiSubtaskDto("Repaso ligero final antes de las 19:00 h", dayOffset = 3, estimatedPomodoros = 1)
          )
        )

      lower.containsAny("exposición", "exposicion", "presentación", "presentacion", "diapositivas", "proyecto") ->
        AiBreakdownResponseDto(
          projectTitle = cleanPrompt.replaceFirstChar { it.uppercase() },
          suggestedCourse = "Proyectos Académicos",
          recommendedDeadlineDays = 4,
          categoryHint = ProjectCategoryIcon.ASSIGNMENT.name,
          sourceMode = "LOCAL_HEURISTIC",
          subtasks = listOf(
            AiSubtaskDto("Definir objetivos clave y guion de la entrega", dayOffset = 0, estimatedPomodoros = 1),
            AiSubtaskDto("Investigar evidencias y datos de respaldo", dayOffset = 1, estimatedPomodoros = 2),
            AiSubtaskDto("Diseñar material visual y síntesis de láminas", dayOffset = 2, estimatedPomodoros = 2),
            AiSubtaskDto("Ensayar exposición cronometrada", dayOffset = 3, estimatedPomodoros = 1)
          )
        )

      else ->
        AiBreakdownResponseDto(
          projectTitle = cleanPrompt.replaceFirstChar { it.uppercase() },
          suggestedCourse = "Curso Universitario",
          recommendedDeadlineDays = 3,
          categoryHint = ProjectCategoryIcon.LIGHTBULB.name,
          sourceMode = "LOCAL_HEURISTIC",
          subtasks = listOf(
            AiSubtaskDto("Leer rúbrica y definir alcance mínimo", dayOffset = 0, estimatedPomodoros = 1),
            AiSubtaskDto("Desarrollar bloque principal de contenido", dayOffset = 1, estimatedPomodoros = 2),
            AiSubtaskDto("Revisar calidad y empaquetar entrega final", dayOffset = 2, estimatedPomodoros = 2)
          )
        )
    }

    return Result.success(response)
  }

  private fun String.containsAny(vararg keywords: String): Boolean =
    keywords.any { this.contains(it) }
}
