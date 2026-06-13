package com.rishabhships.renew.sample

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rishabhships.renew.sample.ui.DemoScreen
import com.rishabhships.renew.sample.ui.DemoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RenewSampleTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val vm: DemoViewModel = viewModel()
                    val state by vm.uiState.collectAsState()
                    DemoScreen(
                        state = state,
                        onEvent = vm::onIntent,
                        onReset = vm::reset,
                    )
                }
            }
        }
    }
}

@Composable
private fun RenewSampleTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(context)
    } else {
        lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}
