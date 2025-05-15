package com.vone.statepage

import androidx.compose.ui.window.ComposeUIViewController
import com.vone.statepage.sample.App
import com.vone.statepage.sample.di.appModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        startKoin {
            modules(appModule)
        }
    }
) { App() }