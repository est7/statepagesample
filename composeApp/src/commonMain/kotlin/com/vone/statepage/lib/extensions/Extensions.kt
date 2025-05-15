package com.vone.statepage.lib.extensions

import io.github.aakira.napier.Napier


typealias Log = Napier

fun Log.d(tag: String, message: String) {
    Napier.d(tag = tag, message = message)
}
