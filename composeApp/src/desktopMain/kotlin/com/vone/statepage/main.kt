package com.vone.statepage

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.vone.statepage.sample.App
import com.vone.statepage.sample.di.appModule
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(appModule)
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "StatePageSample",
    ) {
        App()
    }
}