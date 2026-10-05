package com.baynana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baynana.core.security.DeviceLockManager
import com.baynana.core.security.DeviceUnauthorizedScreen
import com.baynana.data.local.sync.SyncBridge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val isDeviceAuthorized = DeviceLockManager.verifyOrRegister(this)
        // قناة المزامنة (ح٢٢): تُثبَّت إن كانت مضبوطة، ولا تفعل شيئًا إن لم تكن — ولا إرسال هنا.
        SyncBridge.install(applicationContext)
        setContent {
            if (!isDeviceAuthorized) {
                DeviceUnauthorizedScreen()
            } else {
                MainApp()
            }
        }
    }
}
