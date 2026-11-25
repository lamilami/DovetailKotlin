package bg.workshop.dovetail

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import bg.workshop.dovetail.ui.AppRoot
import bg.workshop.dovetail.ui.theme.DovetailTheme

class MainActivity : ComponentActivity() {
    private val repository get() = (application as DovetailApp).repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // При настройка ръцете са заети — екранът не трябва да угасва.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { DovetailTheme { AppRoot(repository) } }
    }

    override fun onStop() {
        super.onStop()
        repository.flush()
    }
}
