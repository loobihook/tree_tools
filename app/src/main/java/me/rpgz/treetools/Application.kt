package me.rpgz.treetools
import android.app.Application

import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.ConsolePrinter
import com.elvishew.xlog.printer.Printer

import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val androidPrinter: Printer =  AndroidPrinter(true) // Printer that print the log using android.util.Log
        val consolePrinter: Printer = ConsolePrinter() // Printer that print the log to console using System.out
        val config = LogConfiguration.Builder()
            .logLevel(LogLevel.ALL)
            .build()

        XLog.init(
            config,
            androidPrinter,
            consolePrinter)
    }
}
