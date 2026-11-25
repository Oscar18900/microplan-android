package mx.buap.fcc.microplan.ui.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.buap.fcc.microplan.BuildConfig
import mx.buap.fcc.microplan.ui.components.MicroPlanCard
import mx.buap.fcc.microplan.ui.components.ModuleTopBar
import mx.buap.fcc.microplan.ui.components.StatusPill
import mx.buap.fcc.microplan.ui.theme.MicroPlanTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
  viewModel: FocusViewModel,
  initialSubtaskId: String?,
  onNavigateToProfile: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val colors = MicroPlanTheme.colors
  val snackbarHostState = remember { SnackbarHostState() }
  val view = LocalView.current

  LaunchedEffect(initialSubtaskId) {
    if (!initialSubtaskId.isNullOrBlank()) {
      viewModel.selectSubtask(initialSubtaskId)
    }
  }

  LaunchedEffect(uiState.statusMessage) {
    uiState.statusMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  // Escudo Anti-Redes: mantiene la pantalla activa mientras corre la sesión Pomodoro
  DisposableEffect(uiState.antiSocialShieldEnabled, uiState.isRunning) {
    view.keepScreenOn = uiState.antiSocialShieldEnabled && uiState.isRunning
    onDispose { view.keepScreenOn = false }
  }

  Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
    Column(modifier = Modifier.fillMaxSize()) {
      ModuleTopBar(
        title = "Focus",
        leadingIcon = Icons.Rounded.Timer,
        streakDays = uiState.streakDays,
        onProfileClick = onNavigateToProfile
      )

      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Status & Zen Pill row matching Screen 6
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (uiState.isRunning) colors.tealProtection else colors.textSecondary)
            )
            Text(
              text = if (uiState.isRunning) "EN CURSO • BLOQUE ${uiState.currentBlockNumber}" else "LISTO • BLOQUE ${uiState.currentBlockNumber}",
              color = colors.tealProtection,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.6.sp
            )
          }

          StatusPill(
            text = if (uiState.antiSocialShieldEnabled) "Modo Zen Activo" else "Escudo Pausado",
            backgroundColor = colors.primaryBlueSoft,
            textColor = colors.primaryBlue,
            leadingIcon = Icons.Rounded.Bolt
          )
        }

        Column(modifier = Modifier.padding(horizontal = 2.dp)) {
          Text(
            text = if (uiState.isRunning) "Sesión de Enfoque Activa" else "Temporizador Pomodoro Adaptativo",
            color = colors.textPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(Modifier.height(2.dp))
          Text(
            text = if (uiState.antiSocialShieldEnabled) {
              "Aislamiento cognitivo activado. Evita cambiar a redes sociales."
            } else {
              "Activa el Escudo Anti-Redes para proteger tu concentración."
            },
            color = colors.textSecondary,
            fontSize = 12.sp
          )
        }

        // Selector de duración adaptativa: 25 / 45 / 50 min
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(25, 45, 50).forEach { mins ->
            val selected = uiState.selectedDurationMinutes == mins
            FilterChip(
              selected = selected,
              onClick = { viewModel.selectDuration(mins) },
              label = {
                Text(
                  text = "$mins min",
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  modifier = Modifier.fillMaxWidth(),
                  textAlign = TextAlign.Center
                )
              },
              modifier = Modifier.weight(1f)
            )
          }
        }

        // Selector horizontal de micro-tarea activa
        if (uiState.availableTasks.isNotEmpty()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            uiState.availableTasks.take(6).forEach { task ->
              val isSelected = uiState.selectedSubtask?.id == task.id
              Surface(
                modifier = Modifier.clickable { viewModel.selectSubtask(task.id) },
                shape = RoundedCornerShape(50),
                color = if (isSelected) colors.primaryBlue else colors.surface,
                border = BorderStroke(1.dp, if (isSelected) colors.primaryBlue else colors.border)
              ) {
                Text(
                  text = task.title,
                  color = if (isSelected) Color.White else colors.textPrimary,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
              }
            }
          }
        }

        // Main Timer Ring Card
        MicroPlanCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            StatusPill(
              text = "Bloque de Trabajo · ${uiState.selectedDurationMinutes} min",
              backgroundColor = colors.surfaceSubtle,
              textColor = colors.textPrimary
            )
            Text(
              text = "⚡ Alta Frecuencia",
              color = colors.tealProtection,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          Spacer(Modifier.height(14.dp))
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = uiState.selectedSubtask?.title ?: "Sesión de estudio enfocada",
              color = colors.textPrimary,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
              text = uiState.courseSubtitle,
              color = colors.textSecondary,
              fontSize = 12.sp,
              textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))
            PomodoroProgressRing(
              progress = uiState.progressFraction,
              countdownText = uiState.formattedCountdown,
              blockLabel = "BLOQUE ${uiState.currentBlockNumber} DE ${uiState.totalBlocksForTask}",
              ringColor = colors.tealProtection,
              trackColor = colors.surfaceSubtle,
              textColor = colors.textPrimary,
              subTextColor = colors.textSecondary
            )

            Spacer(Modifier.height(20.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = viewModel::startOrPauseTimer,
                modifier = Modifier.weight(1.4f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (uiState.isRunning) colors.amberWarning else colors.primaryBlue
                )
              ) {
                Icon(
                  imageVector = if (uiState.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  text = if (uiState.isRunning) "Pausar" else "Iniciar Enfoque",
                  color = Color.White,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 14.sp
                )
              }

              OutlinedButton(
                onClick = viewModel::resetTimer,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.border)
              ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Reiniciar", color = colors.textPrimary, fontWeight = FontWeight.Medium)
              }
            }
          }
        }

        // Escudo Anti-Redes Card
        MicroPlanCard(
          containerColor = if (uiState.antiSocialShieldEnabled) colors.tealSoft else colors.surface,
          borderColor = if (uiState.antiSocialShieldEnabled) colors.tealBorder else colors.border
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (uiState.antiSocialShieldEnabled) colors.tealPill else colors.surfaceSubtle),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (uiState.antiSocialShieldEnabled) Icons.Rounded.Shield else Icons.Rounded.DoNotDisturbOn,
                  contentDescription = null,
                  tint = if (uiState.antiSocialShieldEnabled) colors.tealProtection else colors.textSecondary
                )
              }
              Spacer(Modifier.width(12.dp))
              Column {
                Text(
                  text = "Escudo Anti-Redes (Local)",
                  color = colors.textPrimary,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Mantiene la pantalla activa y bloquea distracciones visuales.",
                  color = colors.textSecondary,
                  fontSize = 11.sp
                )
              }
            }
            Switch(
              checked = uiState.antiSocialShieldEnabled,
              onCheckedChange = viewModel::toggleShield,
              colors = SwitchDefaults.colors(checkedTrackColor = colors.tealProtection)
            )
          }
        }

        if (BuildConfig.DEBUG) {
          OutlinedButton(
            onClick = viewModel::simulatePomodoroEndDebug,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, colors.primaryBlue.copy(alpha = 0.5f))
          ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = colors.primaryBlue, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
              text = "Simular fin de Pomodoro (Debug)",
              color = colors.primaryBlue,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        Spacer(Modifier.height(16.dp))
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
    )
  }

  if (uiState.showCompletionSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = viewModel::dismissCompletionSheet,
      sheetState = sheetState,
      containerColor = colors.surface,
      shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp)
          .padding(bottom = 28.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(colors.tealSoft),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.Celebration,
              contentDescription = null,
              tint = colors.tealProtection,
              modifier = Modifier.size(24.dp)
            )
          }
          Column {
            Text(
              text = "¡Pomodoro Terminado!",
              color = colors.textPrimary,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${uiState.selectedDurationMinutes} minutos cumplidos con éxito",
              color = colors.tealProtection,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        Spacer(Modifier.height(14.dp))
        Text(
          text = "El bloque de ${uiState.selectedDurationMinutes} min concluyó. ¿Has terminado por completo esta micro-meta o requieres una extensión antes de pausar?",
          color = colors.textSecondary,
          fontSize = 13.sp,
          lineHeight = 19.sp
        )

        Spacer(Modifier.height(14.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceSubtle)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(Icons.Rounded.EditNote, contentDescription = null, tint = colors.primaryBlue, modifier = Modifier.size(18.dp))
          Text(
            text = uiState.selectedSubtask?.title ?: "Sesión de enfoque",
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(Modifier.height(16.dp))
        Button(
          onClick = viewModel::completeTaskFromSheet,
          modifier = Modifier.fillMaxWidth().height(48.dp),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = colors.tealProtection)
        ) {
          Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Sí, marcar como Completada", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }

        Spacer(Modifier.height(10.dp))
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = viewModel::extendAnotherBlock),
          shape = RoundedCornerShape(12.dp),
          color = colors.amberSoft,
          border = BorderStroke(1.dp, colors.amberWarning.copy(alpha = 0.35f))
        ) {
          Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = colors.amberWarning, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
              text = "No, necesito otro bloque (+25 min)",
              color = colors.amberWarning,
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp
            )
          }
        }

        Spacer(Modifier.height(14.dp))
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = viewModel::pauseAndLoseStreak)
            .padding(vertical = 6.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "Pausar por ahora y tomar descanso",
            color = colors.textSecondary,
            fontSize = 12.sp
          )
          Text(
            text = "pero pierde tu racha",
            color = colors.dangerRed,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }
}

@Composable
private fun PomodoroProgressRing(
  progress: Float,
  countdownText: String,
  blockLabel: String,
  ringColor: Color,
  trackColor: Color,
  textColor: Color,
  subTextColor: Color
) {
  val animatedSweep by animateFloatAsState(targetValue = (1f - progress) * 360f, label = "ringSweep")
  Box(
    modifier = Modifier.size(190.dp),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
      val strokeWidth = 14.dp.toPx()
      drawArc(
        color = trackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )
      drawArc(
        color = ringColor,
        startAngle = -90f,
        sweepAngle = animatedSweep.coerceAtLeast(6f),
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = countdownText,
        color = textColor,
        fontSize = 36.sp,
        fontWeight = FontWeight.ExtraBold
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = blockLabel,
        color = subTextColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp
      )
    }
  }
}
