package com.wefit.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wefit.app.ui.admin.AdminUsersScreen
import com.wefit.app.ui.analytics.AnalyticsScreen
import com.wefit.app.ui.assignments.AssignmentProgressScreen
import com.wefit.app.ui.assignments.AssignmentsScreen
import com.wefit.app.ui.assignments.CreateAssignmentScreen
import com.wefit.app.ui.dashboard.DashboardScreen
import com.wefit.app.ui.login.LoginScreen
import com.wefit.app.ui.login.RegisterScreen
import com.wefit.app.ui.notifications.NotificationsScreen
import com.wefit.app.ui.profile.ProfileScreen
import com.wefit.app.ui.sections.SectionDetailScreen
import com.wefit.app.ui.sections.SectionMembersScreen
import com.wefit.app.ui.sections.SectionsScreen
import com.wefit.app.ui.settings.SettingsScreen
import com.wefit.app.ui.tracking.TrackingScreen
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object Sections : Screen("sections")
    object Assignments : Screen("assignments")
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")
    object Analytics : Screen("analytics")
    object AdminUsers : Screen("admin_users")
    object Settings : Screen("settings")

    object CreateAssignment : Screen("create_assignment/{sectionId}") {
        fun route(sectionId: Int = -1) = "create_assignment/$sectionId"
    }

    object SectionDetail : Screen("section_detail/{sectionId}/{isOwner}") {
        fun route(sectionId: Int, isOwner: Boolean) = "section_detail/$sectionId/$isOwner"
    }

    object SectionMembers : Screen("section_members/{sectionId}/{isOwner}") {
        fun route(sectionId: Int, isOwner: Boolean) = "section_members/$sectionId/$isOwner"
    }

    object AssignmentProgress : Screen("assignment_progress/{assignmentId}") {
        fun route(assignmentId: Int) = "assignment_progress/$assignmentId"
    }

    object Tracking : Screen("tracking/{assignmentId}/{exerciseType}/{exerciseName}")

    fun trackingRoute(assignmentId: Int, exerciseType: String, exerciseName: String): String {
        val encodedName = URLEncoder.encode(exerciseName, "UTF-8")
        return "tracking/$assignmentId/$exerciseType/$encodedName"
    }
}

@Composable
fun WeFitNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                WeFitBottomBar(currentRoute = currentRoute) { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onLoggedOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToSections = { navController.navigate(Screen.Sections.route) },
                    onNavigateToAssignments = { navController.navigate(Screen.Assignments.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToCreateAssignment = { navController.navigate(Screen.CreateAssignment.route()) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToAdminUsers = { navController.navigate(Screen.AdminUsers.route) },
                    onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Sections.route) {
                val authStateViewModel: AuthStateViewModel = viewModel()
                val storedRole by authStateViewModel.role.collectAsState()
                SectionsScreen(
                    userRole = storedRole ?: "STUDENT",
                    onSectionSelected = { sectionId, isOwner ->
                        navController.navigate(Screen.SectionDetail.route(sectionId, isOwner))
                    }
                )
            }
            composable(
                route = Screen.SectionDetail.route,
                arguments = listOf(
                    navArgument("sectionId") { type = NavType.IntType },
                    navArgument("isOwner") { type = NavType.BoolType }
                )
            ) { backStackEntry ->
                val sectionId = backStackEntry.arguments?.getInt("sectionId") ?: 0
                val isOwner = backStackEntry.arguments?.getBoolean("isOwner") ?: false
                SectionDetailScreen(
                    sectionId = sectionId,
                    isOwner = isOwner,
                    onCreateAssignment = { sId -> navController.navigate(Screen.CreateAssignment.route(sId)) },
                    onAssignmentSelected = { assignmentId -> navController.navigate(Screen.AssignmentProgress.route(assignmentId)) },
                    onViewMembers = { sId, owner -> navController.navigate(Screen.SectionMembers.route(sId, owner)) }
                )
            }
            composable(
                route = Screen.SectionMembers.route,
                arguments = listOf(
                    navArgument("sectionId") { type = NavType.IntType },
                    navArgument("isOwner") { type = NavType.BoolType }
                )
            ) { backStackEntry ->
                val sectionId = backStackEntry.arguments?.getInt("sectionId") ?: 0
                val isOwner = backStackEntry.arguments?.getBoolean("isOwner") ?: false
                SectionMembersScreen(sectionId = sectionId, isOwner = isOwner)
            }
            composable(Screen.Assignments.route) {
                AssignmentsScreen(
                    onAssignmentSelected = { assignmentId, exerciseType, exerciseName ->
                        navController.navigate(Screen.Tracking.trackingRoute(assignmentId, exerciseType, exerciseName))
                    }
                )
            }
            composable(
                route = Screen.CreateAssignment.route,
                arguments = listOf(navArgument("sectionId") { type = NavType.IntType })
            ) { backStackEntry ->
                val sectionId = backStackEntry.arguments?.getInt("sectionId") ?: -1
                CreateAssignmentScreen(
                    preselectedSectionId = if (sectionId == -1) null else sectionId,
                    onCreated = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.AssignmentProgress.route,
                arguments = listOf(navArgument("assignmentId") { type = NavType.IntType })
            ) { backStackEntry ->
                val assignmentId = backStackEntry.arguments?.getInt("assignmentId") ?: 0
                AssignmentProgressScreen(assignmentId = assignmentId)
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen()
            }
            composable(Screen.Profile.route) {
                ProfileScreen()
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(Screen.AdminUsers.route) {
                AdminUsersScreen()
            }
            composable(
                route = Screen.Tracking.route,
                arguments = listOf(
                    navArgument("assignmentId") { type = NavType.IntType },
                    navArgument("exerciseType") { type = NavType.StringType },
                    navArgument("exerciseName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val assignmentId = backStackEntry.arguments?.getInt("assignmentId") ?: 0
                val exerciseType = backStackEntry.arguments?.getString("exerciseType") ?: "plank"
                val encodedName = backStackEntry.arguments?.getString("exerciseName") ?: "Exercise"
                val exerciseName = URLDecoder.decode(encodedName, "UTF-8")

                TrackingScreen(
                    assignmentId = assignmentId,
                    exerciseName = exerciseName,
                    exerciseType = exerciseType,
                    onDone = { navController.popBackStack(Screen.Dashboard.route, inclusive = false) }
                )
            }
        }
    }
}