package com.vone.statepage

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.vone.statepage.sample.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "StatePageSample",
    ) {
        App()
    }
}