package com.vone.statepage.sample.data

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 *
 * @author: est8
 * @date: 4/30/25
 */
object ContactsService {
    const val pageSize = 10

    suspend fun loadDataById(id: String, page: Int): PersistentList<SampleUserInfo> {
        delay(1000) // 模拟网络延迟
        // 模拟数据加载
        // 有 30% 的概率第一页返回空数据
        // 第三页必返回 7 个,模拟加载完的情况
        val data = if (page == 1 && Random.nextFloat() < 0.3f) {
            persistentListOf()
        } else if (page == 3) {
            persistentListOf<SampleUserInfo>(
                SampleUserInfo(
                    "User 1,page =  $page",
                    Random.nextInt(18, 60),
                    "user1_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                ), SampleUserInfo(
                    "User 2,page =  $page",
                    Random.nextInt(18, 60),
                    "user2_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                ), SampleUserInfo(
                    "User 3,page =  $page",
                    Random.nextInt(18, 60),
                    "user3_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                ), SampleUserInfo(
                    "User 4,page =  $page",
                    Random.nextInt(18, 60),
                    "user4_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                ), SampleUserInfo(
                    "User 5,page =  $page",
                    Random.nextInt(18, 60),
                    "user5_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                ), SampleUserInfo(
                    "User 6,page =  $page",
                    Random.nextInt(18, 60),
                    "user6_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                ), SampleUserInfo(
                    "User 7,page =  $page",
                    Random.nextInt(18, 60),
                    "user7_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                )
            )
        } else {
            persistentListOf(
                SampleUserInfo(
                    "User 1,page =  $page",
                    Random.nextInt(18, 60),
                    "user1_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                ), SampleUserInfo(
                    "User 2,page =  $page",
                    Random.nextInt(18, 60),
                    "user2_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                ), SampleUserInfo(
                    "User 3,page =  $page",
                    Random.nextInt(18, 60),
                    "user3_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                ), SampleUserInfo(
                    "User 4,page =  $page",
                    Random.nextInt(18, 60),
                    "user4_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                ), SampleUserInfo(
                    "User 5,page =  $page",
                    Random.nextInt(18, 60),
                    "user5_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                ), SampleUserInfo(
                    "User 6,page =  $page",
                    Random.nextInt(18, 60),
                    "user6_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                ), SampleUserInfo(
                    "User 7,page =  $page",
                    Random.nextInt(18, 60),
                    "user7_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                ), SampleUserInfo(
                    "User 8,page =  $page",
                    Random.nextInt(18, 60),
                    "user8_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                ), SampleUserInfo(
                    "User 9,page =  $page",
                    Random.nextInt(18, 60),
                    "user9_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                ), SampleUserInfo(
                    "User 10,page =  $page",
                    Random.nextInt(18, 60),
                    "user10_$page@example.com",
                    "https://example.com/avatar$id.jpg"
                )
            )
        }
        return data
    }
}
