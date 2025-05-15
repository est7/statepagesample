package com.vone.statepage.sample.di

import com.vone.statepage.sample.presention.SealedListStateViewModel
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModelOf

/**
 *
 * @author: est8
 * @date: 5/15/25
 */
val contactsModule = module {
    viewModelOf(::SealedListStateViewModel)
}