package mx.buap.fcc.microplan.ui.wellness

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.buap.fcc.microplan.domain.model.TimeFormatters
import mx.buap.fcc.microplan.ui.components.MicroPlanCard
import mx.buap.fcc.microplan.ui.components.ModuleTopBar
import mx.buap.fcc.microplan.ui.components.StatusPill
import mx.buap.fcc.microplan.ui.theme.MicroPlanTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WellnessScreen(
  viewModel: WellnessViewModel,
  onStartFocusTask: (String) -> Unit,
  onNavigateToProfile: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val colors = MicroPlanTheme.colors
  val snackbarHostState = remember { SnackbarHostState() }
  var showEmergencyConfirmDialog by remember { mutableStateOf(false) }

  LaunchedEffect(uiState.statusMessage) {
    uiState.statusMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
    Column(modifier = Modifier.fillMaxSize()) {
      ModuleTopBar(
        title = "Wellness",
        leadingIcon = Icons.Rounded.NightsStay,
        streakDays = uiState.streakDays,
        onProfileClick = onNavigateToProfile,
        onBellClick = viewModel::triggerTestDisconnectNotification
      )

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Circadian Summary Header Card
        item {
          MicroPlanCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top
            ) {
              Column {
                Text(
                  text = "Agenda de Hoy y Descanso",
                  color = colors.textPrimary,
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                  text = "Ciclo Circadiano · Protección de Sueño ≥ 7h",
                  color = colors.textSecondary,
                  fontSize = 12.sp
                )
              }
              Icon(
                imageVector = Icons.Rounded.NightsStay,
                contentDescription = null,
                tint = colors.tealProtection,
                modifier = Modifier.size(22.dp)
              )
            }

            Spacer(Modifier.height(12.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceSubtle)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Rounded.Alarm, contentDescription = null, tint = colors.primaryBlue, modifier = Modifier.size(16.dp))
                Text(
                  text = "Meta: ${uiState.sleepGoalHours}h sueño (Toque: ${uiState.curfewLabel})",
                  color = colors.textPrimary,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium
                )
              }
              StatusPill(
                text = if (uiState.emergencyModeActiveToday) "Emergencia" else "Protegido",
                backgroundColor = if (uiState.emergencyModeActiveToday) colors.dangerSoft else colors.tealPill,
                textColor = if (uiState.emergencyModeActiveToday) colors.dangerRed else colors.tealProtection
              )
            }
          }
        }

        // Chronological Timeline Items
        items(uiState.timelineEntries) { entry ->
          when (entry) {
            is TimelineEntry.TaskEntry -> {
              TimelineTaskItem(
                entry = entry,
                onPlayClick = { onStartFocusTask(entry.subtask.id) }
              )
            }

            is TimelineEntry.ProtectedLeisureEntry -> {
              TimelineProtectedLeisureItem(entry = entry)
            }
          }
        }

        // Disconnection Limit Divider
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(colors.dangerRed),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Rounded.NightsStay, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
              HorizontalDivider(color = colors.dangerBorder, thickness = 1.dp)
              StatusPill(
                text = "Hora límite de desconexión (${uiState.disconnectLabel})",
                backgroundColor = colors.dangerSoft,
                textColor = colors.dangerRed,
                modifier = Modifier.border(1.dp, colors.dangerBorder, RoundedCornerShape(50))
              )
            }
          }
        }

        // Night Disconnect Interactive Card
        item {
          MicroPlanCard {
            Text(
              text = "¡${uiState.disconnectLabel}! Hora de desconectar",
              color = colors.textPrimary,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = if (uiState.isCurfewResolvedToday) {
                "Tu saldo pendiente ya fue reprogramado para mañana. ¡Descansa tranquilo!"
              } else {
                "Has alcanzado el límite para proteger tu meta de ${uiState.sleepGoalHours}h de sueño."
              },
              color = colors.textSecondary,
              fontSize = 12.sp,
              lineHeight = 17.sp
            )

            Spacer(Modifier.height(12.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.tealSoft)
                .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
              Text(
                text = "Horas acumuladas: ${uiState.gainedSleepLabel}",
                color = colors.tealProtection,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
            }

            Spacer(Modifier.height(12.dp))
            Button(
              onClick = viewModel::rescheduleBalanceAndSleep,
              modifier = Modifier.fillMaxWidth().height(46.dp),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlue)
            ) {
              Text(
                text = "Reprogramar saldo y Dormir",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
              )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
              onClick = { showEmergencyConfirmDialog = true },
              modifier = Modifier.fillMaxWidth().height(42.dp),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, colors.border),
              colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.surfaceSubtle)
            ) {
              Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = colors.dangerRed, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(
                text = "Modo emergencia: Pierde tu racha",
                color = colors.dangerRed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
              )
            }
          }
        }
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
    )
  }

  if (showEmergencyConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showEmergencyConfirmDialog = false },
      containerColor = colors.surface,
      title = { Text("¿Activar Modo Emergencia?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "Continuar trabajando tras tu hora límite de desconexión reiniciará tu racha actual a 0 días. Si prefieres proteger tu descanso, usa 'Reprogramar saldo y Dormir'.",
          color = colors.textSecondary,
          fontSize = 13.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showEmergencyConfirmDialog = false
            viewModel.activateEmergencyMode()
          },
          colors = ButtonDefaults.buttonColors(containerColor = colors.dangerRed)
        ) {
          Text("Asumir pérdida de racha", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showEmergencyConfirmDialog = false }) {
          Text("Proteger descanso", color = colors.primaryBlue)
        }
      }
    )
  }
}

@Composable
private fun TimelineTaskItem(
  entry: TimelineEntry.TaskEntry,
  onPlayClick: () -> Unit
) {
  val colors = MicroPlanTheme.colors
  val subtask = entry.subtask
  val isDone = subtask.isCompleted

  Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.width(24.dp).fillMaxHeight()
    ) {
      Box(
        modifier = Modifier
          .size(22.dp)
          .clip(CircleShape)
          .background(if (isDone) colors.successGreen else colors.primaryBlue),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isDone) Icons.Rounded.Check else Icons.Rounded.PlayArrow,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(13.dp)
        )
      }
      Box(
        modifier = Modifier
          .width(2.dp)
          .weight(1f)
          .padding(top = 4.dp)
          .background(colors.border)
      )
    }

    Spacer(Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f).padding(bottom = 6.dp)) {
      Text(
        text = if (entry.isNextPending && !isDone) "${subtask.scheduledSlotLabel} • Próximo" else subtask.scheduledSlotLabel,
        color = if (entry.isNextPending && !isDone) colors.primaryBlue else colors.textSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(Modifier.height(6.dp))
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
      ) {
        if (isDone) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.successGreen),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                  text = subtask.title,
                  color = colors.textSecondary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium,
                  textDecoration = TextDecoration.LineThrough
                )
              }
              StatusPill(
                text = "Completado",
                backgroundColor = colors.tealPill,
                textColor = colors.tealProtection
              )
            }
            Spacer(Modifier.height(4.dp))
            Text(
              text = "Subtarea de ${entry.projectName}",
              color = colors.textSecondary,
              fontSize = 11.sp,
              modifier = Modifier.padding(start = 26.dp)
            )
          }
        } else {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              if (entry.isNextPending) {
                StatusPill(
                  text = "SIGUIENTE BLOQUE",
                  backgroundColor = colors.primaryBlueSoft,
                  textColor = colors.primaryBlue
                )
                Spacer(Modifier.height(6.dp))
              }
              Text(
                text = subtask.title,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(Modifier.height(2.dp))
              val pomText = if (subtask.estimatedPomodoros == 1) "1 Pomodoro" else "${subtask.estimatedPomodoros} Pomodoros"
              Text(
                text = "$pomText • pendiente",
                color = colors.tealProtection,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }
            IconButton(
              onClick = onPlayClick,
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(colors.primaryBlue)
            ) {
              Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Iniciar bloque en Focus",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineProtectedLeisureItem(entry: TimelineEntry.ProtectedLeisureEntry) {
  val colors = MicroPlanTheme.colors
  val block = entry.block

  Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.width(24.dp).fillMaxHeight()
    ) {
      Box(
        modifier = Modifier
          .size(22.dp)
          .clip(CircleShape)
          .background(colors.tealPill)
          .border(1.dp, colors.tealProtection, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.tealProtection, modifier = Modifier.size(12.dp))
      }
      Box(
        modifier = Modifier
          .width(2.dp)
          .weight(1f)
          .padding(top = 4.dp)
          .background(colors.border)
      )
    }

    Spacer(Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f).padding(bottom = 6.dp)) {
      Text(
        text = "${TimeFormatters.formatMinutesAs12Hour(block.startMinutes)} • Descanso Blindado",
        color = colors.tealProtection,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(Modifier.height(6.dp))
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.tealSoft,
        border = BorderStroke(1.dp, colors.tealBorder)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "Bloque Protegido de Ocio Diurno",
            color = colors.tealProtection,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(Modifier.height(2.dp))
          Text(
            text = block.description,
            color = colors.tealProtection.copy(alpha = 0.85f),
            fontSize = 11.sp
          )
          if (block.tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              block.tags.forEach { tag ->
                StatusPill(
                  text = tag,
                  backgroundColor = colors.tealPill,
                  textColor = colors.tealProtection
                )
              }
            }
          }
        }
      }
    }
  }
}
