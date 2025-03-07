package com.example.memoryleakes

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {

    // Eliminamos la referencia estática a la actividad

    // Handler sin fuga: evitamos reprogramar el runnable de forma infinita
    private val safeHandler = Handler(Looper.getMainLooper())
    private val safeRunnable = Runnable {
        // Realiza alguna tarea puntual sin reprogramarse
    }

    // ClickCounter usa WeakReference para evitar retener la actividad
    private val clickCounter = ClickCounter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Programamos el runnable para que se ejecute una sola vez (o se cancele en onDestroy)
        safeHandler.postDelayed(safeRunnable, 60000)

        // Usamos lifecycleScope para que la coroutine se cancele al destruirse la actividad
        startSafeCoroutine()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LeakyComposeUI(clickCounter)
                }
            }
        }
    }

    private fun startSafeCoroutine() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(10000)
                val sharedPrefs = getSharedPreferences("LeakyPrefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().putLong("lastCheck", System.currentTimeMillis()).apply()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Eliminamos callbacks pendientes para evitar fugas
        safeHandler.removeCallbacks(safeRunnable)
        // Liberamos la referencia en ClickCounter
        clickCounter.clearActivityRef()
    }

    // Se utiliza WeakReference para evitar retener la actividad
    class ClickCounter {
        var count = mutableStateOf(0)
        private var activityRef: WeakReference<MainActivity>? = null

        fun increment(activity: MainActivity) {
            activityRef = WeakReference(activity)
            count.value++
        }

        fun clearActivityRef() {
            activityRef?.clear()
        }
    }
}

@Composable
fun LeakyComposeUI(clickCounter: MainActivity.ClickCounter) {
    val context = LocalContext.current
    val clickCount = remember { clickCounter.count }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Memory Leak Demo",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Clicks: ${clickCount.value}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (context is MainActivity) {
                    clickCounter.increment(context)
                }
            }
        ) {
            Text("Incrementar contador")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (context is MainActivity) {
                    context.finish()
                }
            }
        ) {
            Text("Cerrar actividad")
        }
    }
}
