package com.fmarquez.footboly.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.fmarquez.footboly.screens.MatchConfigScreen
import com.fmarquez.footboly.screens.MatchLiveScreen
import com.fmarquez.footboly.screens.MatchTimelineScreen
import com.fmarquez.footboly.screens.PlayerStatsScreen
import com.fmarquez.footboly.screens.PlayersMasterScreen
import com.fmarquez.footboly.screens.ReporteScreen
import com.fmarquez.footboly.screens.TeamSelectionScreen
import com.fmarquez.footboly.vm.FutbolViewModel

enum class Screen(val route: String) {
    TEAM_SELECTION("team_selection"),
    PLAYERS_MASTER("players_master"),
    MATCH_CONFIG("match_config"),
    MATCH_LIVE("match_live"),
    PLAYER_STATS("player_stats"),
    MATCH_TIMELINE("match_timeline"),
    REPORT_SCREEN("report_screen"),
}

fun NavGraphBuilder.setupFootballNavigation(
    navHostController: NavHostController,
    viewModel: FutbolViewModel
) {
    composable(route = Screen.TEAM_SELECTION.route) {
        TeamSelectionScreen(
            vm = viewModel,
            navHostController = navHostController
        )
    }

    composable(route = Screen.PLAYERS_MASTER.route) {
        PlayersMasterScreen(
            vm = viewModel,
            navHostController = navHostController
        )
    }

    composable(route = Screen.MATCH_CONFIG.route) {
        MatchConfigScreen(
            vm = viewModel,
            navHostController = navHostController
        )
    }

    composable(route = Screen.MATCH_LIVE.route) {
        MatchLiveScreen(
            vm = viewModel,
            navHostController = navHostController
        )
    }

    composable(route = Screen.PLAYER_STATS.route) {
        PlayerStatsScreen(
            vm = viewModel,
            navHostController = navHostController
        )
    }

    composable(route = Screen.MATCH_TIMELINE.route) {
        MatchTimelineScreen(
            vm = viewModel,
            navHostController = navHostController
        )
    }

    composable(route = Screen.REPORT_SCREEN.route) {
        ReporteScreen(
            vm = viewModel,
            navHostController = navHostController
        )
    }
}