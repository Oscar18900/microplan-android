package mx.buap.fcc.microplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import mx.buap.fcc.microplan.ui.navigation.MicroPlanApp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val container = (application as MicroPlanApplication).container
    setContent {
      MicroPlanApp(container = container)
    }
  }
}
