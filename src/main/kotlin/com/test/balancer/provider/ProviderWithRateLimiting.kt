package com.test.balancer.provider

import com.test.balancer.Config
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ProviderWithRateLimiting(private val instance: Provider) : Provider by instance {
    private val semaphore = Semaphore(permits = Config.MAX_CONCURENCY_PER_PROVIDER)

    override suspend fun get() = semaphore.withPermit {
        instance.get()
    }
}