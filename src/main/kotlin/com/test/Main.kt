package com.test

import com.test.balancer.LoadBalancer
import com.test.balancer.provider.SimpleProvider
import com.test.balancer.registry.RoundRobbinProvidersRegistry
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
fun main(args: Array<String>) = runBlocking {
    val providersRegistry = RoundRobbinProvidersRegistry().apply {
        repeat(10) { addProvider(SimpleProvider("$it")) }
    }
    delay(3.seconds) // Wait for healthcheck

    val lb = LoadBalancer(providersRegistry = providersRegistry)

    withContext(Dispatchers.Default) {
        val requests = 125 // Add delay(100) in SimpleProvider.get() to simulate IO time

        val (results, duration) = measureTimedValue {
            (0 until requests).map {
                async { runCatching { lb.get() } }
            }.awaitAll()
        }
        val rejected = results.count { it.isFailure }

        println(
            "Executed $requests requests in ${duration.inWholeMilliseconds}ms. $rejected requests were rejected"
        )
    }
}