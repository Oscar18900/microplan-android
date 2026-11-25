package mx.buap.fcc.microplan.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.buap.fcc.microplan.domain.model.Subtask
import mx.buap.fcc.microplan.ui.components.MicroPlanCard
import mx.buap.fcc.microplan.ui.components.ModuleTopBar
import mx.buap.fcc.microplan.ui.components.StatusPill
import mx.buap.fcc.microplan.ui.components.WeeklyStreakRow
import mx.buap.fcc.microplan.ui.theme.MicroPlanTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
  viewModel: DashboardViewModel,
  onNavigateToWellness: () -> Unit,
  onNavigateToProfile: () -> Unit,
  onStartFocusTask: (String) -> Unit
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val colors = MicroPlanTheme.colors
  val animatedProgress by animateFloatAsState(targetValue = uiState.completionProgress, label = "todayProgress")

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.background)
  ) {
    ModuleTopBar(
      title = "Dashboard",
      leadingIcon = Icons.Rounded.NightsStay,
      streakDays = null,
      onProfileClick = onNavigateToProfile
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Card 1: Streak + Compact Level & XP
      item {
        MicroPlanCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Racha de Cumplimiento",
              color = colors.textSecondary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
            uiState.gamification?.let { gamification ->
              StatusPill(
                text = "Nv. ${gamification.level} · ${gamification.xpPoints} XP",
                backgroundColor = colors.primaryBlueSoft,
                textColor = colors.primaryBlue,
                leadingIcon = Icons.Rounded.Stars
              )
            }
          }

          Spacer(Modifier.height(6.dp))
          Text(
            text = "🔥 ${uiState.streakDays} días seguidos",
            color = colors.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = if (uiState.emergencyActiveToday) {
              "Modo emergencia activo hoy. Reprograma a la hora de desconexión para recuperar tu racha mañana."
            } else {
              "Has completado tus bloques diarios y respetado tu desconexión nocturna."
            },
            color = colors.textSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
          )

          Spacer(Modifier.height(14.dp))
          WeeklyStreakRow(indicators = uiState.weeklyIndicators)

          uiState.gamification?.let { gamification ->
            Spacer(Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = gamification.levelTitle,
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "${gamification.xpInCurrentLevel} / ${gamification.xpForNextLevel} XP",
                color = colors.primaryBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(
              progress = { gamification.xpInCurrentLevel.toFloat() / gamification.xpForNextLevel.toFloat() },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
              color = colors.primaryBlue,
              trackColor = colors.surfaceSubtle,
              strokeCap = StrokeCap.Round
            )
          }
        }
      }

      // Card 2: Micro-tareas de Hoy
      item {
        MicroPlanCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Rounded.AssignmentTurnedIn,
                contentDescription = null,
                tint = colors.primaryBlue,
                modifier = Modifier.size(20.dp)
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = "Micro-tareas de Hoy",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
            Text(
              text = "${uiState.completedTodayCount} de ${uiState.todayTasks.size} completadas",
              color = colors.primaryBlue,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          Spacer(Modifier.height(12.dp))
          LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
              .fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(50)),
            color = colors.successGreen,
            trackColor = colors.surfaceSubtle,
            strokeCap = StrokeCap.Round
          )

          Spacer(Modifier.height(12.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            StatusPill(
              text = "${uiState.completedTodayCount} Terminadas",
              backgroundColor = colors.successGreenSoft,
              textColor = colors.successGreen,
              leadingIcon = Icons.Rounded.Check
            )
            val pomLabel = if (uiState.pendingPomodorosCount == 1) "1 Pomodoro" else "${uiState.pendingPomodorosCount} Pomodoros"
            StatusPill(
              text = "⏳ ${uiState.pendingTodayCount} Pendiente ($pomLabel)",
              backgroundColor = colors.amberSoft,
              textColor = colors.amberWarning
            )
          }

          Spacer(Modifier.height(12.dp))

          if (uiState.todayTasks.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.tealSoft)
                .padding(14.dp)
            ) {
              Text(
                text = "¡Agenda libre hoy! Revisa tu bloque protegido de ocio o agrega una entrega en Plan.",
                color = colors.tealProtection,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }
          } else {
            uiState.todayTasks.forEachIndexed { index, task ->
              TodayTaskChecklistRow(
                task = task,
                onToggle = { viewModel.toggleSubtask(task.id) },
                onStartFocus = { onStartFocusTask(task.id) }
              )
              if (index < uiState.todayTasks.lastIndex) {
                HorizontalDivider(color = colors.border, thickness = 1.dp)
              }
            }
          }

          Spacer(Modifier.height(14.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onNavigateToWellness)
              .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Ver cronograma del día en Wellness",
              color = colors.primaryBlue,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(4.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
              contentDescription = null,
              tint = colors.primaryBlue,
              modifier = Modifier.size(15.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TodayTaskChecklistRow(
  task: Subtask,
  onToggle: () -> Unit,
  onStartFocus: () -> Unit
) {
  val colors = MicroPlanTheme.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onToggle)
      .padding(vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (task.isCompleted) {
        Box(
          modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.successGreen),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = "Completada",
            tint = Color.White,
            modifier = Modifier.size(15.dp)
          )
        }
      } else {
        Box(
          modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surface)
            .border(2.dp, colors.border, RoundedCornerShape(6.dp))
        )
      }

      Spacer(Modifier.width(12.dp))
      Text(
        text = task.title,
        color = if (task.isCompleted) colors.textSecondary else colors.textPrimary,
        fontSize = 14.sp,
        fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium,
        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(Modifier.width(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      if (task.isCompleted) {
        StatusPill(
          text = "Listo",
          backgroundColor = colors.successGreenSoft,
          textColor = colors.successGreen
        )
      } else {
        StatusPill(
          text = "Pendiente · ${task.scheduledSlotLabel}",
          backgroundColor = colors.amberSoft,
          textColor = colors.amberWarning
        )
        IconButton(
          onClick = onStartFocus,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = "Enfocar en Focus",
            tint = colors.primaryBlue,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
