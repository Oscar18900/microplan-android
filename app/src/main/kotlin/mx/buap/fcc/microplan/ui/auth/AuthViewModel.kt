package mx.buap.fcc.microplan.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.buap.fcc.microplan.domain.repository.AuthRepository

enum class AuthMode {
  LOGIN,
  REGISTER
}

data class AuthUiState(
  val mode: AuthMode = AuthMode.LOGIN,
  val loginEmail: String = "estudiante@correo.edu.mx",
  val loginPassword: String = "contraseña123",
  val registerFullName: String = "",
  val registerEmail: String = "",
  val registerPassword: String = "",
  val sleepGoalHours: Float = 7.5f,
  val disconnectAlertMinutes: Int = 22 * 60 + 30,
  val passwordVisible: Boolean = false,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val showForgotPasswordDialog: Boolean = false,
  val showTimePickerDialog: Boolean = false
)

class AuthViewModel(
  private val authRepository: AuthRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  fun setMode(mode: AuthMode) {
    _uiState.update { it.copy(mode = mode, errorMessage = null, passwordVisible = false) }
  }

  fun onLoginEmailChange(value: String) {
    _uiState.update { it.copy(loginEmail = value, errorMessage = null) }
  }

  fun onLoginPasswordChange(value: String) {
    _uiState.update { it.copy(loginPassword = value, errorMessage = null) }
  }

  fun onRegisterFullNameChange(value: String) {
    _uiState.update { it.copy(registerFullName = value, errorMessage = null) }
  }

  fun onRegisterEmailChange(value: String) {
    _uiState.update { it.copy(registerEmail = value, errorMessage = null) }
  }

  fun onRegisterPasswordChange(value: String) {
    _uiState.update { it.copy(registerPassword = value, errorMessage = null) }
  }

  fun onSleepGoalSelected(hours: Float) {
    _uiState.update { it.copy(sleepGoalHours = hours.coerceAtLeast(7.0f), errorMessage = null) }
  }

  fun onDisconnectMinutesSelected(minutes: Int) {
    _uiState.update { it.copy(disconnectAlertMinutes = minutes, showTimePickerDialog = false) }
  }

  fun togglePasswordVisibility() {
    _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
  }

  fun setForgotPasswordDialog(visible: Boolean) {
    _uiState.update { it.copy(showForgotPasswordDialog = visible) }
  }

  fun setTimePickerDialog(visible: Boolean) {
    _uiState.update { it.copy(showTimePickerDialog = visible) }
  }

  fun submitLogin() {
    val state = _uiState.value
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val result = authRepository.login(state.loginEmail, state.loginPassword)
      _uiState.update {
        it.copy(
          isLoading = false,
          errorMessage = result.exceptionOrNull()?.message
        )
      }
    }
  }

  fun submitRegister() {
    val state = _uiState.value
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val result = authRepository.register(
        fullName = state.registerFullName,
        email = state.registerEmail,
        password = state.registerPassword,
        sleepGoalHours = state.sleepGoalHours,
        disconnectAlertMinutes = state.disconnectAlertMinutes
      )
      _uiState.update {
        it.copy(
          isLoading = false,
          errorMessage = result.exceptionOrNull()?.message
        )
      }
    }
  }
}
