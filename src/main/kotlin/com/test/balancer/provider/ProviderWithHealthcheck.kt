package com.test.balancer.provider

import com.test.balancer.Config
import kotlinx.coroutines.*

class ProviderWithHealthcheck(private val instance: Provider) : Provider by instance {
    private var isHealthy: Boolean = false
    private var lastCheckResult: Boolean = false

    override suspend fun isAvailable(): Boolean {
        return instance.isAvailable() && isHealthy
    }

    private val checkScheduler = CoroutineScope(Dispatchers.IO).launch {
        while (isActive) {
            doHealthCheck()
            delay(Config.HEALTHCHECK_PERIOD)
        }
    }

    private suspend fun doHealthCheck() {
        val result = runCatching { instance.check() }.getOrElse { false }

        // Ensures that provider is considered healthy only if two consecutive checks were true
        isHealthy = lastCheckResult && result
        lastCheckResult = result
    }
}