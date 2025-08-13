package com.android.mylibrary

import android.text.TextUtils
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private val FILE_NAME = Thread.currentThread().stackTrace[2].fileName
private var tag = Log::class.simpleName
private var isShow = true
private var logFile = "/sdcard/log.txt"
fun Any.initLog(initIsShow: Boolean, initTag: String) {
    isShow = initIsShow
    tag = initTag
}

fun Any.initLogFile(file: String) {
    logFile = file
}

//日志格式:TAG (类名:行数)[调用对象类型$方法名称] 日志内容
private fun buildMessage(
    className: String,
    msg: Array<out String>
): Pair<String, String> {
    fun location(simpleName: String): String {
        var offset = 0
        val stackTrace = Thread.currentThread().stackTrace
        while (stackTrace[offset].fileName != FILE_NAME) offset++
        while (stackTrace[offset].fileName == FILE_NAME) offset++
        val stackTraceElement = stackTrace[offset]
        val clazz =
            if (simpleName == stackTraceElement.fileName.substringBefore(".")) "" else "$simpleName#"
        return "(${stackTraceElement.fileName}:${stackTraceElement.lineNumber})[$clazz${stackTraceElement.methodName}]"
    }

    val location = location(className)
    val txt = msg.joinToString(":")
    return Pair(location, txt)
}

fun Any.logV() = log(VERBOSE, buildMessage(javaClass.simpleName, arrayOf("$this")))
fun Any.logD() = log(DEBUG, buildMessage(javaClass.simpleName, arrayOf("$this")))
fun Any.logI() = log(INFO, buildMessage(javaClass.simpleName, arrayOf("$this")))
fun Any.logW() = log(WARN, buildMessage(javaClass.simpleName, arrayOf("$this")))
fun Any.logE() = log(ERROR, buildMessage(javaClass.simpleName, arrayOf("$this")))
fun Any.logA() = log(ASSERT, buildMessage(javaClass.simpleName, arrayOf("$this")))
fun Any.logJ() = log(JSON, buildMessage(javaClass.simpleName, arrayOf("$this")))

fun Any.logX() = log(XML, buildMessage(javaClass.simpleName, arrayOf("$this")))
fun Any.logF() {
    try {
        this.logD()
        File(logFile).appendText("${this}\n")
    } catch (_: Exception) {
    }
}

private const val VERBOSE = 2
private const val DEBUG = 3
private const val INFO = 4
private const val WARN = 5
private const val ERROR = 6
private const val ASSERT = 7
private const val JSON = 8
private const val XML = 9
private fun log(level: Int, context: Pair<String, String>) {
    if (isShow) {
        if (level == VERBOSE
            || level == DEBUG
            || level == INFO
            || level == WARN
            || level == ERROR
            || level == ASSERT
        ) {
            printDefault(level, context.first + " " + context.second)
        } else if (level == JSON) {
            printJson(context)
        }
    }
}

//处理超过4000行
private fun printDefault(level: Int, message: String) {
    printLine(true)
    val maxLength = 4000
    val countOfSub: Int = message.length
    if (countOfSub > maxLength) {
        var i = 0
        while (i < countOfSub) {
            if (i + maxLength < countOfSub) {
                printSub(level, message.substring(i, i + maxLength))
            } else {
                printSub(level, message.substring(i, countOfSub))
            }
            i += maxLength
        }
    } else {
        printSub(level, message)
    }
    printLine(false)
}

private fun printSub(level: Int, message: String) {
    when (level) {
        VERBOSE -> Log.v(tag, message)
        DEBUG -> Log.d(tag, message)
        INFO -> Log.i(tag, message)
        WARN -> Log.w(tag, message)
        ERROR -> Log.e(tag, message)
        ASSERT -> Log.wtf(tag, message)
    }
}

private const val JSON_INDENT = 4
private val LINE_SEPARATOR = System.lineSeparator()

private fun printJson(context: Pair<String, String>) {
    var head = context.first
    var json = context.second
    var message = ""
    if (TextUtils.isEmpty(json)) {
        "Empty/Null json content".logE()
    } else {
        message = try {
            when {
                json.startsWith("{") -> JSONObject(json).toString(JSON_INDENT)
                json.startsWith("[") -> JSONArray(json).toString(JSON_INDENT)
                else -> json
            }
        } catch (e: Exception) {
            json
        }
        printLine(true)
        printSub(DEBUG, "|${head}")
        val lines = message.split(LINE_SEPARATOR.toRegex()).toTypedArray()
        for (line in lines) {
            printSub(DEBUG, "|$line")
        }
        printLine(false)
    }
}

private fun printLine(isTop: Boolean) {
    if (isTop)
        Log.d(
            tag,
            "╔═══════════════════════════════════════════════════════════════════════════════════════"
        ) else
        Log.d(
            tag,
            "╚═══════════════════════════════════════════════════════════════════════════════════════"
        )
}