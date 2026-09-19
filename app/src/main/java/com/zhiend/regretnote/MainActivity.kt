package com.zhiend.regretnote

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zhiend.regretnote.ui.UndoApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Undo is dark-only, so the system bars always need *light* icons.
        // Without this the platform follows the (light) theme and paints dark
        // icons over the near-black canvas — invisible clock, invisible battery.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            UndoApp()
        }
        // Notification permission is deliberately *not* requested here. Asking on
        // first launch put a system dialog over the app before the user had seen
        // a single screen; the reminder switch in Settings asks for it at the
        // moment it becomes relevant.
    }
}
