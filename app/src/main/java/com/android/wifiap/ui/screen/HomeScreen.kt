package com.android.wifiap.ui.screen

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val activity = (context as Activity)
    val scope = rememberCoroutineScope()
    Column {
        Text(text = "Hello world")
    }
}