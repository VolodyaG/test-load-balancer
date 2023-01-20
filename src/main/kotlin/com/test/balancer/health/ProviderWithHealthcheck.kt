package com.test.balancer.health

import com.test.balancer.Config
import com.test.balancer.provider.Provider
import kotlinx.coroutines.*

class ProviderWithHealthcheck(private val instance: Provider) : Provider by instance {
    var isHealthy: Boolean = false
        private set

    private var lastCheckResult: Boolean = false

    private val checkScheduler = CoroutineScope(Dispatchers.Default).launch {
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