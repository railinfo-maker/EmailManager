package com.emailmanager.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emailmanager.app.ui.EmailManagerApp
import com.emailmanager.app.ui.EmailManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            // Blue top bar -> light status-bar icons; white bottom bar -> dark navigation icons.
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            EmailManagerTheme {
                val vm: EmailViewModel = viewModel()
                EmailManagerApp(vm)
            }
        }
    }
}
