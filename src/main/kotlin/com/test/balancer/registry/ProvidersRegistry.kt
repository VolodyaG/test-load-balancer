package com.test.balancer.registry

import com.test.balancer.health.ProviderWithHealthcheck
import com.test.balancer.provider.Provider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class ProvidersRegistry {
    private val providers = mutableListOf<ProviderWithHealthcheck>()
    private val mutex = Mutex()

    suspend fun nextAvailableProvider(): Provider? {
        return mutex.withLock {
            val healthyProviders = providers.filter { it.isHealthy }
            if (healthyProviders.isEmpty()) null else healthyProviders.pickNext()
        }
    }

    suspend fun addProvider(provider: Provider) {
        mutex.withLock {
            check(providers.size < 10) { "Maximum number of providers has been reached" }
            providers.add(ProviderWithHealthcheck(provider))
        }
    }

    suspend fun removeProvider(providerId: String) {
        mutex.withLock {
            providers.removeIf { it.id == providerId }
        }
    }

    suspend fun countAvailableProviders(): Int {
        return mutex.withLock {
            providers.count { it.isHealthy }
        }
    }

    protected abstract fun List<Provider>.pickNext(): Provider
}

