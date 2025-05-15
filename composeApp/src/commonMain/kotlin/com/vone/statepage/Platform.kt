package com.vone.statepage

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform