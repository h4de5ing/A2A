package com.android.wifiap

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.android.wifiap.other.SoftApCallback
import com.android.wifiap.other.isWifiApEnabled
import com.android.wifiap.other.startTethering
import com.android.wifiap.other.stopTethering
import com.android.wifiap.ui.theme.WIFIAPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WIFIAPTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Greeting()
                }
            }
        }
    }
}

@Composable
fun Greeting() {
    val context = LocalContext.current
    val activity = (context as Activity)
    val scope = rememberCoroutineScope()
    var isWifiApEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isWifiApEnabled = isWifiApEnabled(context)
        println("wifi状态:${isWifiApEnabled}")
    }
    Column {
        Button(onClick = {
            startTethering(context)
        }) {
            Text(text = "开启热点")
        }
        Button(onClick = {
            stopTethering(context)
        }) {
            Text(text = "关闭热点")
        }
        Button(onClick = {
            SoftApCallback.registerSoftApCallback(context)
        }) {
            Text(text = "获取AP信息")
        }
    }
}