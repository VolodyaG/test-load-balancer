package com.test.balancer.registry

import com.test.balancer.provider.Provider

class RandomProvidersRegistry : ProvidersRegistry() {
    override fun List<Provider>.pickNext() = random()
}

class RoundRobbinProvidersRegistry : ProvidersRegistry() {
    private var lastPickIndex = -1

    override fun List<Provider>.pickNext(): Provider {
        lastPickIndex = (lastPickIndex + 1) % size
        return get(lastPickIndex)
    }
}
