package com.test.balancer.provider

import com.test.balancer.Config
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

interface Provider {
    val id: String

    suspend fun get(): String
    suspend fun check(): Boolean
}

class SimpleProvider(override val id: String) : Provider {
    private val semaphore = Semaphore(permits = Config.MAX_CONCURENCY_PER_PROVIDER)

    override suspend fun get() = semaphore.withPermit { id }
    override suspend fun check() = true
}

