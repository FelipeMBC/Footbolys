package com.fmarquez.footboly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.fmarquez.footboly.navigation.Screen
import com.fmarquez.footboly.navigation.setupFootballNavigation
import com.fmarquez.footboly.vm.FutbolViewModel

class MainActivity : ComponentActivity() {

    private val vm: FutbolViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                FutbolApp(vm = vm)
            }
        }
    }
}

@Composable
fun FutbolApp(vm: FutbolViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.TEAM_SELECTION.route
    ) {
        setupFootballNavigation(
            navHostController = navController,
            viewModel = vm
        )
    }
}