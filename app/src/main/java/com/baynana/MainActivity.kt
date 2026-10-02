package com.baynana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baynana.core.security.DeviceLockManager
import com.baynana.core.security.DeviceUnauthorizedScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val isDeviceAuthorized = DeviceLockManager.verifyOrRegister(this)
        setContent {
            if (!isDeviceAuthorized) {
                DeviceUnauthorizedScreen()
            } else {
                MainApp()
            }
        }
    }
}
