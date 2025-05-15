package com.vone.statepage.sample.data

import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 *
 * @author: est8
 * @date: 4/30/25
 */
class ContactsRepository {
    fun loadDataById(id: String, page: Int): Flow<Result<PersistentList<SampleUserInfo>>> = flow {
        try {
            // 模拟网络请求失败
            // 随机生成10%概率的故障
            if (Random.nextFloat() < 0.1f) {
                delay(200)
                emit(Result.failure(RuntimeException("Random failure for testing")))
            } else {
                val data = ContactsService.loadDataById(id, page)
                emit(Result.success(data))
            }
        } catch (e: Exception) {
            delay(200)
            emit(Result.failure(e))
        }
    }


    companion object {
        private var instance: ContactsRepository? = null

        fun getSingleInstance(): ContactsRepository {
            if (instance == null) {
                instance = ContactsRepository()
            }
            return instance!!
        }
    }
}
