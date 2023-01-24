package com.test.balancer

import com.test.balancer.provider.Provider
import com.test.balancer.provider.ProviderWithHealthcheck
import com.test.balancer.provider.ProviderWithRateLimiting
import com.test.balancer.registry.ProvidersRegistry
import java.util.concurrent.atomic.AtomicInteger

class LoadBalancer(
    private val providersRegistry: ProvidersRegistry,
) {
    private val requestsInProgress = AtomicInteger()

    suspend fun get(): String = executeWithRateLimiting {
        providersRegistry.nextAvailableProvider()?.get() ?: error("Providers are not available")
    }

    suspend fun addProvider(provider: Provider) {
        val megaProvider = ProviderWithHealthcheck(ProviderWithRateLimiting(provider))
        providersRegistry.addProvider(megaProvider)
    }

    private suspend fun <T> executeWithRateLimiting(action: suspend () -> T): T {
        return try {
            val requestsCount = requestsInProgress.incrementAndGet()
            val availableProviders = providersRegistry.countAvailableProviders()

            check(requestsCount <= Config.MAX_CONCURENCY_PER_PROVIDER * availableProviders) {
                "Load Balancer capacity is reached its limits"
            }
            action()
        } finally {
            requestsInProgress.decrementAndGet()
        }
    }
}

