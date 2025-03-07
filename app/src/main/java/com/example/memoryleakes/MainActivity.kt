package com.example.memoryleakes

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Ejemplo 1: Campo estático que hace referencia a la actividad
    companion object {
        // Esta referencia estática a la actividad provoca un Memory Leak
        private var activityInstance: MainActivity? = null
    }

    // Ejemplo 2: Handler que retiene una referencia implícita a la actividad
    private val leakyHandler = Handler(Looper.getMainLooper())
    private val leakyRunnable = object : Runnable {
        override fun run() {
            // Esta runnable usa implícitamente una referencia a la actividad
            // y se programa para ejecutarse mucho después de que la actividad sea destruida
            leakyHandler.postDelayed(this, 60000)
        }
    }

    // Ejemplo 3: Objeto que almacena referencias a contextos
    private val clickCounter = ClickCounter()

    // Ejemplo 4: Coroutine scope con trabajo que nunca termina
    private val leakyJob = Job()
    private val leakyScope = CoroutineScope(Dispatchers.IO + leakyJob)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ejemplo 1: Asignar la actividad a una referencia estática
        activityInstance = this

        // Ejemplo 2: Iniciar el handler con leak
        leakyHandler.postDelayed(leakyRunnable, 60000)

        // Ejemplo 4: Iniciar coroutine que nunca termina
        startLeakyCoroutine()

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

    private fun startLeakyCoroutine() {
        leakyScope.launch {
            // Esta coroutine mantiene una referencia a la actividad a través del scope
            // y nunca termina, incluso cuando la actividad es destruida
            while (true) {
                delay(10000)
                // Usa el contexto de la actividad, lo que impide que se recolecte
                val sharedPrefs = getSharedPreferences("LeakyPrefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().putLong("lastCheck", System.currentTimeMillis()).apply()
            }
        }
    }

    // Clase que demuestra cómo una referencia fuerte a un contexto puede causar leaks
    inner class ClickCounter {
        var count = mutableStateOf(0)
        private var activityRef: MainActivity? = null

        fun increment(activity: MainActivity) {
            // Guarda una referencia fuerte a la actividad en lugar de una WeakReference
            activityRef = activity
            count.value++
        }
    }

    // Nota: Intencionalmente NO limpiamos las referencias en onDestroy()
    // para demostrar los memory leaks. En una aplicación real, deberíamos hacer:
    //
    // override fun onDestroy() {
    //     super.onDestroy()
    //     activityInstance = null
    //     leakyHandler.removeCallbacks(leakyRunnable)
    //     leakyJob.cancel()
    //     clickCounter.activityRef = null
    // }
}

@Composable
fun LeakyComposeUI(clickCounter: MainActivity.ClickCounter) {
    // Ejemplo 3: Captura de contexto en composable que se pasa a un objeto que lo retiene
    val context = LocalContext.current as MainActivity
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
                // Pasa una referencia a la actividad al clickCounter
                clickCounter.increment(context)
            }
        ) {
            Text("Incrementar contador")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // Cerrar la actividad para provocar los memory leaks
                context.finish()
            }
        ) {
            Text("Cerrar actividad (generar leaks)")
        }
    }
}