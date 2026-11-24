package com.kazewell.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.kazewell.app.ui.HomeScreen
import com.kazewell.app.ui.KazeTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            KazeTheme {
                HomeScreen(vm)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.ensureRunning()
    }
}
