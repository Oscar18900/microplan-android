package mx.buap.fcc.microplan.ui.profile

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.buap.fcc.microplan.domain.model.ProtectedBlock
import mx.buap.fcc.microplan.domain.model.ProtectedBlockType
import mx.buap.fcc.microplan.domain.model.SleepRigidity
import mx.buap.fcc.microplan.domain.model.TimeFormatters
import mx.buap.fcc.microplan.ui.components.MicroPlanCard
import mx.buap.fcc.microplan.ui.components.ModuleTopBar
import mx.buap.fcc.microplan.ui.components.StatusPill
import mx.buap.fcc.microplan.ui.components.TimeMinutesPickerDialog
import mx.buap.fcc.microplan.ui.theme.MicroPlanTheme

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val colors = MicroPlanTheme.colors
  val snackbarHostState = remember { SnackbarHostState() }
  var expandedGuides by remember { mutableStateOf(false) }

  LaunchedEffect(uiState.syncStatusMessage) {
    uiState.syncStatusMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessage()
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
    Column(modifier = Modifier.fillMaxSize()) {
      ModuleTopBar(
        title = "Profile",
        leadingIcon = Icons.Rounded.NightsStay,
        streakDays = null,
        onProfileClick = { viewModel.setEditProfileDialog(true) }
      )

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // 1. Identity Card (Stitch Screen 7)
        item {
          MicroPlanCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.BottomEnd) {
                  Box(
                    modifier = Modifier
                      .size(50.dp)
                      .clip(CircleShape)
                      .background(colors.primaryBlue),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = uiState.initials,
                      color = Color.White,
                      fontWeight = FontWeight.Bold,
                      fontSize = 16.sp
                    )
                  }
                  Box(
                    modifier = Modifier
                      .offset(x = 1.dp, y = 1.dp)
                      .size(16.dp)
                      .clip(CircleShape)
                      .background(colors.successGreen)
                      .border(2.dp, colors.surface, CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                  }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                  Text(
                    text = uiState.fullName,
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = uiState.careerGroup,
                    color = colors.textSecondary,
                    fontSize = 12.sp
                  )
                }
              }

              IconButton(
                onClick = { viewModel.setEditProfileDialog(true) },
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(colors.surfaceSubtle)
              ) {
                Icon(Icons.Rounded.Edit, contentDescription = "Editar perfil", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
              }
            }
          }
        }

        // 2. Dark Mode Switch
        item {
          MicroPlanCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primaryBlueSoft),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = colors.primaryBlue, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Modo Oscuro Nocturno",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                  Text(
                    text = "Baja fatiga visual para uso vespertino",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                  )
                }
              }
              Switch(
                checked = uiState.darkModeEnabled,
                onCheckedChange = viewModel::setDarkMode,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.primaryBlue)
              )
            }
          }
        }

        // 3. Sleep Hygiene and Curfew Card
        item {
          MicroPlanCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(30.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(colors.tealSoft),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Rounded.NightsStay, contentDescription = null, tint = colors.tealProtection, modifier = Modifier.size(17.dp))
              }
              Spacer(Modifier.width(8.dp))
              Text(
                text = "Higiene del Sueño y Toque de Queda",
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(Modifier.height(10.dp))

            SettingsRow(
              title = "Meta diaria de sueño",
              subtitle = "Recomendado ≥ 7h para consolidación cognitiva",
              pillText = "${uiState.sleepGoalHours} horas",
              pillBg = colors.tealPill,
              pillFg = colors.tealProtection,
              pillIcon = Icons.Rounded.KeyboardArrowDown,
              onClick = { viewModel.setSleepGoalDialog(true) }
            )
            HorizontalDivider(color = colors.surfaceSubtle)

            SettingsRow(
              title = "Alerta de desconexión nocturna",
              subtitle = "Atenuación lumínica y parada de estímulos",
              pillText = TimeFormatters.formatMinutesAs12Hour(uiState.disconnectAlertMinutes),
              pillBg = colors.amberSoft,
              pillFg = colors.amberWarning,
              pillIcon = Icons.Rounded.Tune,
              onClick = { viewModel.setDisconnectTimeDialog(true) }
            )
            HorizontalDivider(color = colors.surfaceSubtle)

            SettingsRow(
              title = "Hora límite para dormir",
              subtitle = "Toque de queda infranqueable",
              pillText = TimeFormatters.formatMinutesAs12Hour(uiState.curfewBedtimeMinutes),
              pillBg = colors.dangerSoft,
              pillFg = colors.dangerRed,
              pillIcon = Icons.Rounded.Tune,
              onClick = { viewModel.setCurfewTimeDialog(true) }
            )
            HorizontalDivider(color = colors.surfaceSubtle)

            SettingsRow(
              title = "Nivel de rigidez",
              subtitle = uiState.rigidity.description,
              pillText = uiState.rigidity.label,
              pillBg = colors.primaryBlueSoft,
              pillFg = colors.primaryBlue,
              pillIcon = Icons.Rounded.Tune,
              onClick = { viewModel.setRigidityDialog(true) }
            )
          }
        }

        // 4. Weekly Protected Blocks Card (CRUD)
        item {
          MicroPlanCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(30.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(colors.tealSoft),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = colors.tealProtection, modifier = Modifier.size(17.dp))
              }
              Spacer(Modifier.width(8.dp))
              Text(
                text = "Bloques Protegidos Semanales",
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(Modifier.height(10.dp))

            uiState.protectedBlocks.forEachIndexed { index, block ->
              ProtectedBlockRow(
                block = block,
                onEdit = { viewModel.openBlockEditor(block) }
              )
              if (index < uiState.protectedBlocks.lastIndex) {
                HorizontalDivider(color = colors.surfaceSubtle)
              }
            }

            Spacer(Modifier.height(10.dp))
            Text(
              text = "+ Agregar horario protegido",
              color = colors.primaryBlue,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { viewModel.openBlockEditor(null) }
                .padding(vertical = 4.dp)
            )
          }
        }

        // 5. Medals & XP Progress
        uiState.gamification?.let { gamification ->
          item {
            MicroPlanCard {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = colors.amberWarning, modifier = Modifier.size(20.dp))
                  Spacer(Modifier.width(8.dp))
                  Text(
                    text = "Medallas y Progreso",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
                StatusPill(
                  text = "Nivel ${gamification.level} · ${gamification.xpPoints} XP",
                  backgroundColor = colors.primaryBlueSoft,
                  textColor = colors.primaryBlue
                )
              }
              Spacer(Modifier.height(10.dp))
              LinearProgressIndicator(
                progress = { gamification.xpInCurrentLevel.toFloat() / gamification.xpForNextLevel.toFloat() },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(7.dp)
                  .clip(RoundedCornerShape(50)),
                color = colors.primaryBlue,
                trackColor = colors.surfaceSubtle,
                strokeCap = StrokeCap.Round
              )
              Spacer(Modifier.height(12.dp))
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                gamification.medals.chunked(2).forEach { rowMedals ->
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowMedals.forEach { medal ->
                      Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (medal.isUnlocked) colors.tealSoft else colors.surfaceSubtle,
                        border = BorderStroke(1.dp, if (medal.isUnlocked) colors.tealBorder else colors.border)
                      ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                          Text(
                            text = if (medal.isUnlocked) "🏅 ${medal.title}" else "🔒 ${medal.title}",
                            color = if (medal.isUnlocked) colors.tealProtection else colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                          )
                          Spacer(Modifier.height(2.dp))
                          Text(
                            text = medal.description,
                            color = colors.textSecondary,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // 6. Quick Sleep Hygiene & Focus Guides
        item {
          MicroPlanCard {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { expandedGuides = !expandedGuides },
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = colors.primaryBlue, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                  text = "Guías Rápidas de Higiene del Sueño y Enfoque",
                  color = colors.textPrimary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              Icon(
                imageVector = if (expandedGuides) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.textSecondary
              )
            }
            AnimatedVisibility(visible = expandedGuides) {
              Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GuidePoint("1. Regla ≥ 7 horas", "Dormir menos de 7h reduce tu retención de memoria y eleva la somnolencia diurna.")
                GuidePoint("2. Ocio Diurno Blindado", "Proteger 1 hora de descanso sin pantallas en la tarde evita la procrastinación vengativa nocturna.")
                GuidePoint("3. Desconexión a las 10:30 PM", "Usa 'Reprogramar saldo y Dormir' en Wellness en lugar de desvelarte; mantendrás tu racha.")
              }
            }
          }
        }

        // 7. Offline-First Sync Readiness & Logout
        item {
          MicroPlanCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CloudQueue, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                  Text(
                    text = "Modo Offline-First Activo",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                  Text(
                    text = "${uiState.pendingSyncCount} eventos en cola local (listo para Fase 2)",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                  )
                }
              }
              StatusPill(
                text = "Revisar cola",
                backgroundColor = colors.surfaceSubtle,
                textColor = colors.primaryBlue,
                onClick = viewModel::checkSyncQueue
              )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
              onClick = viewModel::logout,
              modifier = Modifier.fillMaxWidth().height(44.dp),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, colors.dangerBorder),
              colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.dangerSoft)
            ) {
              Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = colors.dangerRed, modifier = Modifier.size(17.dp))
              Spacer(Modifier.width(6.dp))
              Text("Cerrar sesión", color = colors.dangerRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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

  // Dialogs
  if (uiState.showEditProfileDialog) {
    EditProfileDialog(
      initialName = uiState.fullName,
      initialCareer = uiState.careerGroup,
      onDismiss = { viewModel.setEditProfileDialog(false) },
      onConfirm = viewModel::updateProfile
    )
  }

  if (uiState.showSleepGoalDialog) {
    SleepGoalDialog(
      currentHours = uiState.sleepGoalHours,
      onDismiss = { viewModel.setSleepGoalDialog(false) },
      onConfirm = viewModel::updateSleepGoal
    )
  }

  if (uiState.showDisconnectTimeDialog) {
    TimeMinutesPickerDialog(
      title = "Alerta de desconexión nocturna",
      initialMinutes = uiState.disconnectAlertMinutes,
      onDismiss = { viewModel.setDisconnectTimeDialog(false) },
      onConfirm = viewModel::updateDisconnectAlert
    )
  }

  if (uiState.showCurfewTimeDialog) {
    TimeMinutesPickerDialog(
      title = "Hora límite para dormir",
      initialMinutes = uiState.curfewBedtimeMinutes,
      onDismiss = { viewModel.setCurfewTimeDialog(false) },
      onConfirm = viewModel::updateCurfewBedtime
    )
  }

  if (uiState.showRigidityDialog) {
    RigidityDialog(
      current = uiState.rigidity,
      onDismiss = { viewModel.setRigidityDialog(false) },
      onConfirm = viewModel::updateRigidity
    )
  }

  if (uiState.editingProtectedBlock != null || uiState.showNewBlockDialog) {
    ProtectedBlockEditorDialog(
      existing = uiState.editingProtectedBlock,
      onDismiss = viewModel::closeBlockEditor,
      onSave = viewModel::saveProtectedBlock,
      onDelete = uiState.editingProtectedBlock?.let { block -> { viewModel.deleteProtectedBlock(block.id) } }
    )
  }
}

@Composable
private fun SettingsRow(
  title: String,
  subtitle: String,
  pillText: String,
  pillBg: Color,
  pillFg: Color,
  pillIcon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit
) {
  val colors = MicroPlanTheme.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 11.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
      Text(text = title, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
      Text(text = subtitle, color = colors.textSecondary, fontSize = 11.sp)
    }
    StatusPill(
      text = pillText,
      backgroundColor = pillBg,
      textColor = pillFg,
      leadingIcon = pillIcon,
      onClick = onClick
    )
  }
}

@Composable
private fun ProtectedBlockRow(
  block: ProtectedBlock,
  onEdit: () -> Unit
) {
  val colors = MicroPlanTheme.colors
  val isLeisure = block.type == ProtectedBlockType.DAYTIME_LEISURE
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onEdit)
      .padding(vertical = 11.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(RoundedCornerShape(9.dp))
          .background(if (isLeisure) colors.tealSoft else colors.surfaceSubtle),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = when (block.type) {
            ProtectedBlockType.DAYTIME_LEISURE -> Icons.Rounded.Park
            ProtectedBlockType.CLASS_SCHEDULE -> Icons.Rounded.School
            ProtectedBlockType.CUSTOM -> Icons.Rounded.Lock
          },
          contentDescription = null,
          tint = if (isLeisure) colors.tealProtection else colors.textSecondary,
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(Modifier.width(10.dp))
      Column {
        Text(text = block.title, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(
          text = "${block.daysLabel} · ${block.timeRangeLabel()}",
          color = colors.textSecondary,
          fontSize = 11.sp
        )
      }
    }

    if (block.isImmune) {
      StatusPill(
        text = "Inmune",
        backgroundColor = colors.tealPill,
        textColor = colors.tealProtection,
        onClick = onEdit
      )
    } else {
      IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
        Icon(Icons.Rounded.Edit, contentDescription = "Editar bloque", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
      }
    }
  }
}

@Composable
private fun GuidePoint(title: String, body: String) {
  val colors = MicroPlanTheme.colors
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(colors.surfaceSubtle)
      .padding(10.dp)
  ) {
    Text(text = title, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(2.dp))
    Text(text = body, color = colors.textSecondary, fontSize = 11.sp, lineHeight = 16.sp)
  }
}

@Composable
private fun EditProfileDialog(
  initialName: String,
  initialCareer: String,
  onDismiss: () -> Unit,
  onConfirm: (String, String) -> Unit
) {
  val colors = MicroPlanTheme.colors
  var name by remember(initialName) { mutableStateOf(initialName) }
  var career by remember(initialCareer) { mutableStateOf(initialCareer) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.surface,
    title = { Text("Editar Perfil", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre completo") }, singleLine = true)
        OutlinedTextField(value = career, onValueChange = { career = it }, label = { Text("Carrera / Grupo") }, singleLine = true)
      }
    },
    confirmButton = {
      Button(onClick = { onConfirm(name, career) }, colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlue)) {
        Text("Guardar", color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar", color = colors.textSecondary) }
    }
  )
}

@Composable
private fun SleepGoalDialog(
  currentHours: Float,
  onDismiss: () -> Unit,
  onConfirm: (Float) -> Unit
) {
  val colors = MicroPlanTheme.colors
  var selected by remember(currentHours) { mutableStateOf(currentHours) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.surface,
    title = { Text("Meta diaria de sueño", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
    text = {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(7.0f, 7.5f, 8.0f).forEach { hrs ->
          FilterChip(
            selected = selected == hrs,
            onClick = { selected = hrs },
            label = { Text("$hrs h") },
            modifier = Modifier.weight(1f)
          )
        }
      }
    },
    confirmButton = {
      Button(onClick = { onConfirm(selected) }, colors = ButtonDefaults.buttonColors(containerColor = colors.tealProtection)) {
        Text("Guardar", color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar", color = colors.textSecondary) }
    }
  )
}

@Composable
private fun RigidityDialog(
  current: SleepRigidity,
  onDismiss: () -> Unit,
  onConfirm: (SleepRigidity) -> Unit
) {
  val colors = MicroPlanTheme.colors
  var selected by remember(current) { mutableStateOf(current) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.surface,
    title = { Text("Nivel de rigidez nocturna", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SleepRigidity.entries.forEach { option ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selected = option }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(selected = selected == option, onClick = { selected = option })
            Column {
              Text(option.label, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
              Text(option.description, color = colors.textSecondary, fontSize = 11.sp)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = { onConfirm(selected) }, colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlue)) {
        Text("Aplicar", color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar", color = colors.textSecondary) }
    }
  )
}

@Composable
private fun ProtectedBlockEditorDialog(
  existing: ProtectedBlock?,
  onDismiss: () -> Unit,
  onSave: (String?, String, ProtectedBlockType, String, Int, Int, String, String, Boolean) -> Unit,
  onDelete: (() -> Unit)?
) {
  val colors = MicroPlanTheme.colors
  var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
  var days by remember(existing) { mutableStateOf(existing?.daysLabel ?: "Lun - Vie") }
  var startMinutes by remember(existing) { mutableIntStateOf(existing?.startMinutes ?: 17 * 60) }
  var endMinutes by remember(existing) { mutableIntStateOf(existing?.endMinutes ?: 18 * 60) }
  var description by remember(existing) { mutableStateOf(existing?.description ?: "Espacio libre protegido") }
  var tagsText by remember(existing) { mutableStateOf(existing?.tags?.joinToString(", ") ?: "Descanso, Sin pantallas") }
  var isImmune by remember(existing) { mutableStateOf(existing?.isImmune ?: true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.surface,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (existing == null) "Nuevo Horario Protegido" else "Editar Horario",
          color = colors.textPrimary,
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp
        )
        if (onDelete != null) {
          IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar", tint = colors.dangerRed)
          }
        }
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Nombre del bloque") }, singleLine = true)
        OutlinedTextField(value = days, onValueChange = { days = it }, label = { Text("Días (ej. Lun - Vie)") }, singleLine = true)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = { startMinutes = ((startMinutes + 30) % 1440) },
            modifier = Modifier.weight(1f)
          ) {
            Text("Ini: ${TimeFormatters.formatMinutesAs12Hour(startMinutes)}", fontSize = 11.sp)
          }
          OutlinedButton(
            onClick = { endMinutes = ((endMinutes + 30) % 1440) },
            modifier = Modifier.weight(1f)
          ) {
            Text("Fin: ${TimeFormatters.formatMinutesAs12Hour(endMinutes)}", fontSize = 11.sp)
          }
        }
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción") }, singleLine = true)
        OutlinedTextField(value = tagsText, onValueChange = { tagsText = it }, label = { Text("Etiquetas (separadas por coma)") }, singleLine = true)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Bloque Inmune al auto-agendado", color = colors.textPrimary, fontSize = 12.sp)
          Switch(checked = isImmune, onCheckedChange = { isImmune = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            existing?.id,
            title,
            existing?.type ?: if (isImmune) ProtectedBlockType.DAYTIME_LEISURE else ProtectedBlockType.CUSTOM,
            days,
            startMinutes,
            endMinutes,
            description,
            tagsText,
            isImmune
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlue)
      ) {
        Text("Guardar", color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar", color = colors.textSecondary) }
    }
  )
}
