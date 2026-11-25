package mx.buap.fcc.microplan.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.buap.fcc.microplan.di.AppContainer
import mx.buap.fcc.microplan.di.SimpleViewModelFactory
import mx.buap.fcc.microplan.domain.model.UserPreferences
import mx.buap.fcc.microplan.ui.auth.AuthScreen
import mx.buap.fcc.microplan.ui.auth.AuthViewModel
import mx.buap.fcc.microplan.ui.dashboard.DashboardScreen
import mx.buap.fcc.microplan.ui.dashboard.DashboardViewModel
import mx.buap.fcc.microplan.ui.focus.FocusScreen
import mx.buap.fcc.microplan.ui.focus.FocusViewModel
import mx.buap.fcc.microplan.ui.plan.PlanScreen
import mx.buap.fcc.microplan.ui.plan.PlanViewModel
import mx.buap.fcc.microplan.ui.profile.ProfileScreen
import mx.buap.fcc.microplan.ui.profile.ProfileViewModel
import mx.buap.fcc.microplan.ui.theme.MicroPlanTheme
import mx.buap.fcc.microplan.ui.wellness.WellnessScreen
import mx.buap.fcc.microplan.ui.wellness.WellnessViewModel

enum class BottomTab(
  val route: String,
  val label: String,
  val icon: ImageVector
) {
  DASHBOARD("dashboard", "Dashboard", Icons.Rounded.Dashboard),
  PLAN("plan", "Plan", Icons.Rounded.CalendarMonth),
  FOCUS("focus", "Focus", Icons.Rounded.Timer),
  WELLNESS("wellness", "Wellness", Icons.Rounded.NightsStay),
  PROFILE("profile", "Profile", Icons.Rounded.Person)
}

@Composable
fun MicroPlanApp(container: AppContainer) {
  val prefs by container.preferencesDataSource.preferencesFlow.collectAsStateWithLifecycle(
    initialValue = UserPreferences()
  )
  val context = LocalContext.current

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { }
  )

  LaunchedEffect(prefs.isLoggedIn) {
    if (prefs.isLoggedIn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  MicroPlanTheme(darkTheme = prefs.darkModeEnabled) {
    if (!prefs.isLoggedIn) {
      val authViewModel: AuthViewModel = viewModel(
        factory = SimpleViewModelFactory { AuthViewModel(container.authRepository) }
      )
      AuthScreen(viewModel = authViewModel)
    } else {
      MainTabbedShell(container = container)
    }
  }
}

@Composable
private fun MainTabbedShell(container: AppContainer) {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: BottomTab.DASHBOARD.route
  val colors = MicroPlanTheme.colors

  fun navigateToTab(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  fun openFocusWithTask(subtaskId: String) {
    navController.navigate("focus?subtaskId=$subtaskId") {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
    }
  }

  Scaffold(
    containerColor = colors.background,
    modifier = Modifier.fillMaxSize().statusBarsPadding(),
    bottomBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(colors.surface)
          .navigationBarsPadding()
      ) {
        HorizontalDivider(color = colors.border, thickness = 1.dp)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .padding(horizontal = 6.dp),
          horizontalArrangement = Arrangement.SpaceAround,
          verticalAlignment = Alignment.CenterVertically
        ) {
          BottomTab.entries.forEach { tab ->
            val selected = currentRoute.startsWith(tab.route)
            val tint = if (selected) colors.primaryBlue else colors.textSecondary
            Column(
              modifier = Modifier
                .weight(1f)
                .clickable {
                  if (tab == BottomTab.FOCUS) navigateToTab("focus") else navigateToTab(tab.route)
                }
                .padding(vertical = 6.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = tint,
                modifier = Modifier.size(22.dp)
              )
              Spacer(Modifier.height(3.dp))
              Text(
                text = tab.label,
                color = tint,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
              )
            }
          }
        }
      }
    }
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      NavHost(
        navController = navController,
        startDestination = BottomTab.DASHBOARD.route
      ) {
        composable(BottomTab.DASHBOARD.route) {
          val vm: DashboardViewModel = viewModel(
            factory = SimpleViewModelFactory {
              DashboardViewModel(
                authRepository = container.authRepository,
                plannerRepository = container.plannerRepository,
                wellnessRepository = container.wellnessRepository,
                streakEvaluator = container.streakEvaluatorUseCase
              )
            }
          )
          DashboardScreen(
            viewModel = vm,
            onNavigateToWellness = { navigateToTab(BottomTab.WELLNESS.route) },
            onNavigateToProfile = { navigateToTab(BottomTab.PROFILE.route) },
            onStartFocusTask = ::openFocusWithTask
          )
        }

        composable(BottomTab.PLAN.route) {
          val vm: PlanViewModel = viewModel(
            factory = SimpleViewModelFactory {
              PlanViewModel(
                authRepository = container.authRepository,
                plannerRepository = container.plannerRepository
              )
            }
          )
          PlanScreen(
            viewModel = vm,
            onNavigateToProfile = { navigateToTab(BottomTab.PROFILE.route) }
          )
        }

        composable(
          route = "focus?subtaskId={subtaskId}",
          arguments = listOf(
            navArgument("subtaskId") {
              type = NavType.StringType
              nullable = true
              defaultValue = null
            }
          )
        ) { backStack ->
          val subtaskId = backStack.arguments?.getString("subtaskId")
          val vm: FocusViewModel = viewModel(
            factory = SimpleViewModelFactory {
              FocusViewModel(
                authRepository = container.authRepository,
                plannerRepository = container.plannerRepository,
                focusRepository = container.focusRepository,
                telemetryRepository = container.telemetryRepository
              )
            }
          )
          FocusScreen(
            viewModel = vm,
            initialSubtaskId = subtaskId,
            onNavigateToProfile = { navigateToTab(BottomTab.PROFILE.route) }
          )
        }

        composable(BottomTab.WELLNESS.route) {
          val vm: WellnessViewModel = viewModel(
            factory = SimpleViewModelFactory {
              WellnessViewModel(
                authRepository = container.authRepository,
                plannerRepository = container.plannerRepository,
                wellnessRepository = container.wellnessRepository
              )
            }
          )
          WellnessScreen(
            viewModel = vm,
            onStartFocusTask = ::openFocusWithTask,
            onNavigateToProfile = { navigateToTab(BottomTab.PROFILE.route) }
          )
        }

        composable(BottomTab.PROFILE.route) {
          val vm: ProfileViewModel = viewModel(
            factory = SimpleViewModelFactory {
              ProfileViewModel(
                authRepository = container.authRepository,
                plannerRepository = container.plannerRepository,
                wellnessRepository = container.wellnessRepository,
                telemetryRepository = container.telemetryRepository,
                syncManager = container.syncManager,
                streakEvaluator = container.streakEvaluatorUseCase
              )
            }
          )
          ProfileScreen(viewModel = vm)
        }
      }
    }
  }
}
