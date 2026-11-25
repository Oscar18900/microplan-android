package mx.buap.fcc.microplan.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.buap.fcc.microplan.domain.model.TimeFormatters
import mx.buap.fcc.microplan.ui.components.MicroPlanCard
import mx.buap.fcc.microplan.ui.components.StatusPill
import mx.buap.fcc.microplan.ui.components.TimeMinutesPickerDialog
import mx.buap.fcc.microplan.ui.theme.MicroPlanTheme

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val colors = MicroPlanTheme.colors
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.background)
      .statusBarsPadding()
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (uiState.mode == AuthMode.LOGIN) {
        Spacer(Modifier.height(12.dp))
        // App badge icon matching Screen 1
        Box(contentAlignment = Alignment.TopEnd) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(colors.primaryBlue),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.NightsStay,
              contentDescription = "MicroPlan",
              tint = Color.White,
              modifier = Modifier.size(30.dp)
            )
          }
          Box(
            modifier = Modifier
              .offset(x = 3.dp, y = (-3).dp)
              .size(14.dp)
              .clip(CircleShape)
              .background(colors.tealProtection)
              .border(2.dp, colors.background, CircleShape)
          )
        }
        Spacer(Modifier.height(16.dp))
        Text(
          text = "Bienvenido de nuevo",
          color = colors.textPrimary,
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
          text = "Microplanifica tus entregas y protege tu descanso",
          color = colors.textSecondary,
          fontSize = 14.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(22.dp))
      } else {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Crea tu cuenta",
            color = colors.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = "Configura tu acceso y tu meta saludable de sueño en un paso",
            color = colors.textSecondary,
            fontSize = 14.sp
          )
          Spacer(Modifier.height(16.dp))
          AuthSegmentedSwitcher(
            selectedMode = uiState.mode,
            onSelectMode = viewModel::setMode
          )
          Spacer(Modifier.height(16.dp))
        }
      }

      if (uiState.mode == AuthMode.LOGIN) {
        MicroPlanCard {
          AuthSegmentedSwitcher(
            selectedMode = uiState.mode,
            onSelectMode = viewModel::setMode
          )
          Spacer(Modifier.height(18.dp))

          AuthInputLabel("CORREO ELECTRÓNICO")
          Spacer(Modifier.height(6.dp))
          AuthStyledTextField(
            value = uiState.loginEmail,
            onValueChange = viewModel::onLoginEmailChange,
            placeholder = "estudiante@correo.edu.mx",
            leadingIcon = Icons.Rounded.Email,
            keyboardType = KeyboardType.Email
          )
          Spacer(Modifier.height(14.dp))

          AuthInputLabel("CONTRASEÑA")
          Spacer(Modifier.height(6.dp))
          AuthStyledTextField(
            value = uiState.loginPassword,
            onValueChange = viewModel::onLoginPasswordChange,
            placeholder = "••••••••••••",
            leadingIcon = Icons.Rounded.Lock,
            isPassword = true,
            passwordVisible = uiState.passwordVisible,
            onTogglePassword = viewModel::togglePasswordVisibility
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 6.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.End
          ) {
            Text(
              text = "¿Olvidaste tu contraseña?",
              color = colors.primaryBlue,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.clickable { viewModel.setForgotPasswordDialog(true) }
            )
          }

          if (uiState.errorMessage != null) {
            ErrorBanner(uiState.errorMessage!!)
            Spacer(Modifier.height(10.dp))
          }

          Button(
            onClick = viewModel::submitLogin,
            enabled = !uiState.isLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlue)
          ) {
            if (uiState.isLoading) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
              Text("Iniciar sesión", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
              Spacer(Modifier.width(6.dp))
              Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
          }
        }
      } else {
        MicroPlanCard {
          AuthInputLabel("NOMBRE COMPLETO")
          Spacer(Modifier.height(6.dp))
          AuthStyledTextField(
            value = uiState.registerFullName,
            onValueChange = viewModel::onRegisterFullNameChange,
            placeholder = "Ej. Angel García",
            keyboardType = KeyboardType.Text
          )
          Spacer(Modifier.height(12.dp))

          AuthInputLabel("CORREO ELECTRÓNICO")
          Spacer(Modifier.height(6.dp))
          AuthStyledTextField(
            value = uiState.registerEmail,
            onValueChange = viewModel::onRegisterEmailChange,
            placeholder = "angel@correo.edu.mx",
            keyboardType = KeyboardType.Email
          )
          Spacer(Modifier.height(12.dp))

          AuthInputLabel("CONTRASEÑA")
          Spacer(Modifier.height(6.dp))
          AuthStyledTextField(
            value = uiState.registerPassword,
            onValueChange = viewModel::onRegisterPasswordChange,
            placeholder = "Mínimo 8 caracteres",
            isPassword = true,
            passwordVisible = uiState.passwordVisible,
            onTogglePassword = viewModel::togglePasswordVisibility
          )
        }

        Spacer(Modifier.height(14.dp))

        // Sleep Goal Card (Screen 2)
        MicroPlanCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(colors.tealSoft),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Rounded.NightsStay,
                  contentDescription = null,
                  tint = colors.tealProtection,
                  modifier = Modifier.size(17.dp)
                )
              }
              Spacer(Modifier.width(10.dp))
              Text(
                text = "Meta Inicial de Sueño",
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
            StatusPill(
              text = "Mínimo 7h",
              backgroundColor = colors.successGreenSoft,
              textColor = colors.successGreen
            )
          }

          Spacer(Modifier.height(14.dp))

          val sleepOptions = listOf(7.0f, 7.5f, 8.0f)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            sleepOptions.forEach { hours ->
              val isSelected = uiState.sleepGoalHours == hours
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .height(40.dp)
                  .clickable { viewModel.onSleepGoalSelected(hours) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) colors.successGreen else colors.surfaceSubtle,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, colors.border)
              ) {
                Row(
                  modifier = Modifier.fillMaxSize(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Rounded.Check,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                  }
                  Text(
                    text = "$hours hrs",
                    color = if (isSelected) Color.White else colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }
          }

          Spacer(Modifier.height(12.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(colors.background)
              .border(1.dp, colors.border, RoundedCornerShape(12.dp))
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Rounded.AccessTime,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text = "Hora límite de desconexión",
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }
            StatusPill(
              text = TimeFormatters.formatMinutesAs12Hour(uiState.disconnectAlertMinutes),
              backgroundColor = colors.dangerSoft,
              textColor = colors.dangerRed,
              leadingIcon = Icons.Rounded.Edit,
              onClick = { viewModel.setTimePickerDialog(true) }
            )
          }
        }

        if (uiState.errorMessage != null) {
          Spacer(Modifier.height(12.dp))
          ErrorBanner(uiState.errorMessage!!)
        }

        Spacer(Modifier.height(14.dp))
        Button(
          onClick = viewModel::submitRegister,
          enabled = !uiState.isLoading,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlue)
        ) {
          if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
          } else {
            Text("Crear cuenta y comenzar", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
          }
        }
      }
    }

    Spacer(Modifier.height(24.dp))
    val footerText = buildAnnotatedString {
      withStyle(SpanStyle(color = colors.textSecondary)) {
        append(if (uiState.mode == AuthMode.LOGIN) "¿No tienes cuenta aún? " else "¿Ya tienes una cuenta? ")
      }
      withStyle(SpanStyle(color = colors.primaryBlue, fontWeight = FontWeight.Bold)) {
        append(if (uiState.mode == AuthMode.LOGIN) "Regístrate aquí" else "Inicia sesión")
      }
    }
    Text(
      text = footerText,
      fontSize = 13.sp,
      modifier = Modifier
        .padding(bottom = 12.dp)
        .clickable {
          viewModel.setMode(if (uiState.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN)
        }
    )
  }

  if (uiState.showForgotPasswordDialog) {
    AlertDialog(
      onDismissRequest = { viewModel.setForgotPasswordDialog(false) },
      containerColor = colors.surface,
      title = { Text("Cuenta Local Offline", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "En esta fase offline-first tu cuenta reside localmente en SQLite. Puedes probar la app con la cuenta demo precargada:\n\nCorreo: estudiante@correo.edu.mx\nContraseña: contraseña123",
          color = colors.textSecondary,
          fontSize = 13.sp
        )
      },
      confirmButton = {
        TextButton(onClick = { viewModel.setForgotPasswordDialog(false) }) {
          Text("Entendido", color = colors.primaryBlue, fontWeight = FontWeight.SemiBold)
        }
      }
    )
  }

  if (uiState.showTimePickerDialog) {
    TimeMinutesPickerDialog(
      title = "Hora límite de desconexión",
      initialMinutes = uiState.disconnectAlertMinutes,
      onDismiss = { viewModel.setTimePickerDialog(false) },
      onConfirm = viewModel::onDisconnectMinutesSelected
    )
  }
}

@Composable
private fun AuthSegmentedSwitcher(
  selectedMode: AuthMode,
  onSelectMode: (AuthMode) -> Unit
) {
  val colors = MicroPlanTheme.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(colors.surfaceSubtle)
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    listOf(AuthMode.LOGIN to "Iniciar Sesión", AuthMode.REGISTER to "Registrarse").forEach { (mode, label) ->
      val selected = selectedMode == mode
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(9.dp))
          .background(if (selected) colors.surface else Color.Transparent)
          .clickable { onSelectMode(mode) }
          .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = label,
          color = if (selected) colors.primaryBlue else colors.textSecondary,
          fontSize = 13.sp,
          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
      }
    }
  }
}

@Composable
private fun AuthInputLabel(text: String) {
  Text(
    text = text,
    color = MicroPlanTheme.colors.textPrimary,
    fontSize = 11.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.6.sp
  )
}

@Composable
private fun AuthStyledTextField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  keyboardType: KeyboardType = KeyboardType.Text,
  isPassword: Boolean = false,
  passwordVisible: Boolean = false,
  onTogglePassword: (() -> Unit)? = null
) {
  val colors = MicroPlanTheme.colors
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    placeholder = { Text(placeholder, color = colors.textSecondary, fontSize = 13.sp) },
    leadingIcon = if (leadingIcon != null) {
      { Icon(leadingIcon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp)) }
    } else null,
    trailingIcon = if (isPassword && onTogglePassword != null) {
      {
        IconButton(onClick = onTogglePassword) {
          Icon(
            imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
            contentDescription = "Mostrar u ocultar contraseña",
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    } else null,
    visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
    keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
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
}

@Composable
private fun ErrorBanner(message: String) {
  val colors = MicroPlanTheme.colors
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(colors.dangerSoft)
      .border(1.dp, colors.dangerBorder, RoundedCornerShape(10.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Text(text = message, color = colors.dangerRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
  }
}
