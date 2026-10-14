package com.emailmanager.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.emailmanager.app.EmailViewModel
import kotlinx.coroutines.launch

private enum class Screen { HOME, IMPORT }

@Composable
fun EmailManagerApp(vm: EmailViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show every message from the ViewModel; a newer message replaces the one on screen.
    LaunchedEffect(Unit) {
        vm.messages.collect { msg ->
            snackbar.currentSnackbarData?.dismiss()
            scope.launch { snackbar.showSnackbar(msg, duration = SnackbarDuration.Short) }
        }
    }

    when (screen) {
        Screen.HOME -> HomeScreen(vm, snackbar, onOpenImport = { screen = Screen.IMPORT })
        Screen.IMPORT -> {
            BackHandler { screen = Screen.HOME }
            ImportScreen(vm, snackbar, onDone = { screen = Screen.HOME })
        }
    }
}
