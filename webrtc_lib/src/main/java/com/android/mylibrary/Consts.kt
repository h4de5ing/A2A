package com.android.mylibrary

import kotlinx.coroutines.flow.MutableStateFlow

object Consts {
    var targetSn: String = ""
    val frameCountFlow = MutableStateFlow(0L)
}