package mx.buap.fcc.microplan.ui.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.buap.fcc.microplan.domain.model.AcademicProject
import mx.buap.fcc.microplan.domain.model.ProjectCategoryIcon
import mx.buap.fcc.microplan.domain.model.Subtask
import mx.buap.fcc.microplan.domain.model.TimeFormatters
import mx.buap.fcc.microplan.ui.components.MicroPlanCard
import mx.buap.fcc.microplan.ui.components.ModuleTopBar
import mx.buap.fcc.microplan.ui.components.StatusPill
import mx.buap.fcc.microplan.ui.theme.MicroPlanTheme
import java.time.LocalDate

@Composable
fun PlanScreen(
  viewModel: PlanViewModel,
  onNavigateToProfile: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val colors = MicroPlanTheme.colors
  val snackbarHostState = remember { SnackbarHostState() }
  val todayEpochDay = remember { LocalDate.now().toEpochDay() }

  LaunchedEffect(uiState.statusMessage) {
    uiState.statusMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
    Column(modifier = Modifier.fillMaxSize()) {
      ModuleTopBar(
        title = "Plan",
        leadingIcon = Icons.Rounded.Schedule,
        streakDays = uiState.streakDays,
        onProfileClick = onNavigateToProfile
      )

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          Column(modifier = Modifier.padding(horizontal = 2.dp)) {
            Text(
              text = "Entregas Académicas",
              color = colors.textPrimary,
              fontSize = 23.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
              text = "Vista general de proyectos y cadencia de estudio",
              color = colors.textSecondary,
              fontSize = 13.sp
            )
          }
        }

        // Input Card for Manual or Local AI Breakdown
        item {
          MicroPlanCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "ACTIVIDAD A DESGLOSAR",
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
              )
              StatusPill(
                text = "Modo Local",
                backgroundColor = colors.tealSoft,
                textColor = colors.tealProtection
              )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
              value = uiState.activityPrompt,
              onValueChange = viewModel::onPromptChange,
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              placeholder = {
                Text("Ej. Ensayo final de neurobiología...", color = colors.textSecondary, fontSize = 13.sp)
              },
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surfaceSubtle,
                focusedBorderColor = colors.primaryBlue,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
              )
            )
            Spacer(Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = { viewModel.openManualProjectDialog(true) },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.border)
              ) {
                Text("+ Tarea Manual", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              }

              Button(
                onClick = viewModel::breakdownWithAi,
                enabled = !uiState.isProcessingAi,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlue),
                contentPadding = PaddingValues(horizontal = 10.dp)
              ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Desglosar con IA", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
              }
            }
          }
        }

        // Section Header
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp, start = 2.dp, end = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "CURSOS EN CURSO (${uiState.projects.size})",
              color = colors.textPrimary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.6.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "${uiState.nearestDeadlineDays} días restantes",
                color = colors.textSecondary,
                fontSize = 12.sp
              )
              StatusPill(
                text = "Rebalancear",
                backgroundColor = colors.primaryBlueSoft,
                textColor = colors.primaryBlue,
                leadingIcon = Icons.Rounded.Sync,
                onClick = viewModel::rebalanceAll
              )
            }
          }
        }

        items(uiState.projects, key = { it.id }) { project ->
          ProjectAccordionCard(
            project = project,
            todayEpochDay = todayEpochDay,
            isExpanded = project.id in uiState.expandedProjectIds,
            onToggleExpand = { viewModel.toggleProjectExpanded(project.id) },
            onToggleSubtask = viewModel::toggleSubtaskCompletion,
            onAddSubtask = { viewModel.openAddSubtaskDialog(project.id) },
            onDeleteProject = { viewModel.deleteProject(project.id) }
          )
        }
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
    )
  }

  if (uiState.showManualProjectDialog) {
    ManualProjectDialog(
      initialTitle = uiState.activityPrompt,
      onDismiss = { viewModel.openManualProjectDialog(false) },
      onConfirm = viewModel::createManualProject
    )
  }

  uiState.addSubtaskForProjectId?.let { projectId ->
    AddSubtaskDialog(
      onDismiss = { viewModel.openAddSubtaskDialog(null) },
      onConfirm = { title, days, pomodoros ->
        viewModel.addManualSubtask(projectId, title, days, pomodoros)
      }
    )
  }
}

@Composable
private fun ProjectAccordionCard(
  project: AcademicProject,
  todayEpochDay: Long,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  onToggleSubtask: (String) -> Unit,
  onAddSubtask: () -> Unit,
  onDeleteProject: () -> Unit
) {
  val colors = MicroPlanTheme.colors
  val categoryIcon = when (project.categoryIcon) {
    ProjectCategoryIcon.BOOK -> Icons.AutoMirrored.Rounded.MenuBook
    ProjectCategoryIcon.DATABASE -> Icons.Rounded.Storage
    ProjectCategoryIcon.LIGHTBULB -> Icons.Rounded.Lightbulb
    ProjectCategoryIcon.ASSIGNMENT -> Icons.Rounded.TaskAlt
  }

  MicroPlanCard {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onToggleExpand),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.primaryBlueSoft),
          contentAlignment = Alignment.Center
        ) {
          Icon(categoryIcon, contentDescription = null, tint = colors.primaryBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
          Text(
            text = project.title,
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = project.courseName,
            color = colors.textSecondary,
            fontSize = 12.sp
          )
        }
      }

      Spacer(Modifier.width(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StatusPill(
          text = "Plazo: ${project.daysRemaining(todayEpochDay)} Días",
          backgroundColor = colors.primaryBlueSoft,
          textColor = colors.primaryBlue
        )
        Icon(
          imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
          contentDescription = if (isExpanded) "Colapsar" else "Expandir",
          tint = colors.textSecondary,
          modifier = Modifier.size(22.dp)
        )
      }
    }

    AnimatedVisibility(visible = isExpanded) {
      Column(modifier = Modifier.padding(top = 12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${project.totalPomodoros} Pomodoros tot.${if (project.isAiGenerated) " · Desglose local" else ""}",
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
          IconButton(onClick = onDeleteProject, modifier = Modifier.size(24.dp)) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = "Eliminar proyecto",
              tint = colors.textSecondary,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        Spacer(Modifier.height(8.dp))
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.background)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          project.subtasks.forEach { subtask ->
            SubtaskPlanRow(
              subtask = subtask,
              todayEpochDay = todayEpochDay,
              onToggle = { onToggleSubtask(subtask.id) }
            )
          }
        }

        Spacer(Modifier.height(10.dp))
        Text(
          text = "+ Agregar subtarea manual",
          color = colors.textSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onAddSubtask)
            .padding(vertical = 4.dp, horizontal = 2.dp)
        )

        Spacer(Modifier.height(6.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = "Distribución calculada para finalizar antes de las 19:00 h.",
            color = colors.textSecondary,
            fontSize = 11.sp
          )
        }
      }
    }
  }
}

@Composable
private fun SubtaskPlanRow(
  subtask: Subtask,
  todayEpochDay: Long,
  onToggle: () -> Unit
) {
  val colors = MicroPlanTheme.colors
  val dayPrefix = TimeFormatters.relativeDayLabel(subtask.assignedEpochDay, todayEpochDay)
  val pomText = if (subtask.estimatedPomodoros == 1) "1 Pomodoro" else "${subtask.estimatedPomodoros} Pomodoros"
  val badgeLabel = if (subtask.isOverCapacityAlert && !subtask.isCompleted) "$pomText (Carga Ajustada)" else pomText

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onToggle),
    shape = RoundedCornerShape(10.dp),
    color = colors.surface,
    border = BorderStroke(1.dp, colors.border)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(
              when {
                subtask.isCompleted -> colors.successGreen
                subtask.isOverCapacityAlert -> colors.dangerRed
                else -> colors.tealProtection
              }
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = "$dayPrefix: ${subtask.title}",
          color = if (subtask.isCompleted) colors.textSecondary else colors.textPrimary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(Modifier.width(6.dp))
      StatusPill(
        text = badgeLabel,
        backgroundColor = if (subtask.isOverCapacityAlert && !subtask.isCompleted) colors.dangerSoft else colors.surfaceSubtle,
        textColor = if (subtask.isOverCapacityAlert && !subtask.isCompleted) colors.dangerRed else colors.textSecondary
      )
    }
  }
}

@Composable
private fun ManualProjectDialog(
  initialTitle: String,
  onDismiss: () -> Unit,
  onConfirm: (title: String, course: String, days: Int, pomodoros: Int) -> Unit
) {
  val colors = MicroPlanTheme.colors
  var title by remember(initialTitle) { mutableStateOf(initialTitle) }
  var course by remember { mutableStateOf("") }
  var dueDaysText by remember { mutableStateOf("4") }
  var pomodoros by remember { mutableIntStateOf(4) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.surface,
    title = { Text("Nueva Entrega Manual", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Título de la entrega") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = course,
          onValueChange = { course = it },
          label = { Text("Materia o asignatura") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = dueDaysText,
          onValueChange = { dueDaysText = it.filter { ch -> ch.isDigit() } },
          label = { Text("Plazo en días (ej. 4)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Pomodoros estimados:", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pomodoros = (pomodoros - 1).coerceAtLeast(1) }) { Text("-") }
            Text("$pomodoros", color = colors.primaryBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedButton(onClick = { pomodoros = (pomodoros + 1).coerceAtMost(15) }) { Text("+") }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(title, course, dueDaysText.toIntOrNull() ?: 4, pomodoros)
        },
        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlue)
      ) {
        Text("Guardar y Planificar", color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar", color = colors.textSecondary)
      }
    }
  )
}

@Composable
private fun AddSubtaskDialog(
  onDismiss: () -> Unit,
  onConfirm: (title: String, dueInDays: Int, pomodoros: Int) -> Unit
) {
  val colors = MicroPlanTheme.colors
  var title by remember { mutableStateOf("") }
  var dayOffset by remember { mutableIntStateOf(0) }
  var pomodoros by remember { mutableIntStateOf(1) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.surface,
    title = { Text("Agregar Subtarea Manual", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Descripción de la micro-tarea") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Día asignado (0=Hoy):", color = colors.textPrimary, fontSize = 13.sp)
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { dayOffset = (dayOffset - 1).coerceAtLeast(0) }) { Text("-") }
            Text("+$dayOffset d", color = colors.textPrimary, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { dayOffset = (dayOffset + 1).coerceAtMost(14) }) { Text("+") }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Pomodoros:", color = colors.textPrimary, fontSize = 13.sp)
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pomodoros = (pomodoros - 1).coerceAtLeast(1) }) { Text("-") }
            Text("$pomodoros", color = colors.primaryBlue, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { pomodoros = (pomodoros + 1).coerceAtMost(6) }) { Text("+") }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(title, dayOffset, pomodoros) },
        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlue)
      ) {
        Text("Agregar", color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar", color = colors.textSecondary)
      }
    }
  )
}
