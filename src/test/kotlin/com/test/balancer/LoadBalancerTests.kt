package com.test.balancer

import com.test.balancer.provider.SimpleProvider
import com.test.balancer.registry.RandomProvidersRegistry
import com.test.balancer.registry.RoundRobbinProvidersRegistry
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LoadBalancerTests : AnnotationSpec() {
    @BeforeAll
    fun prepare() {
        mockkObject(Config).also {
            every { Config.HEALTHCHECK_PERIOD }.returns(10.milliseconds)
            every { Config.MAX_CONCURENCY_PER_PROVIDER }.returns(7)
        }
    }

    @AfterAll
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    suspend fun `Should pass request to a single healthy provider`() {
        val deadProvider = spyk(SimpleProvider("1")).also {
            coEvery { it.check() }.returns(false)
        }

        val providerRegistry = RoundRobbinProvidersRegistry().apply {
            addProvider(deadProvider)
            addProvider(SimpleProvider("2"))
        }
        deadProvider.waiUntilCheckIsCalled(times = 2)

        val loadBalancer = LoadBalancer(providerRegistry)
        val response = loadBalancer.get()

        response.shouldBe("2")
    }

    @Test
    suspend fun `Should successfully pass 7 simultaneous request to a single provider`() {
        val provider = spyk(SimpleProvider("1"))
        coEvery { provider.get() }.coAnswers { delay(500); provider.id }

        val providerRegistry = RandomProvidersRegistry().apply {
            addProvider(provider)
        }
        provider.waiUntilCheckIsCalled(times = 2)

        val loadBalancer = LoadBalancer(providerRegistry)

        val response = withTimeout(1.seconds) {
            (0 until 7).map {
                async { loadBalancer.get() }
            }.awaitAll()
        }

        response.shouldHaveSize(7)
        response.shouldContainOnly(provider.id)
    }

    @Test
    suspend fun `Should fail to pass 8th simultaneous request to a single provider`() {
        val provider = spyk(SimpleProvider("1"))
        coEvery { provider.get() }.coAnswers { delay(500); provider.id }

        val providerRegistry = RandomProvidersRegistry().apply {
            addProvider(provider)
        }
        provider.waiUntilCheckIsCalled(times = 2)

        val loadBalancer = LoadBalancer(providerRegistry)

        val error = shouldThrow<IllegalStateException> {
            withTimeout(1.seconds) {
                (0 until 8).map {
                    async { loadBalancer.get() }
                }.awaitAll()
            }
        }.message
        error.shouldBe("Load Balancer capacity is reached its limits")
    }

    @Test
    suspend fun `Should respond with correct error if there is no providers `() {
        val lb = LoadBalancer(RandomProvidersRegistry())

        val error = shouldThrow<IllegalStateException> {
            lb.get()
        }.message

        error.shouldBe("Load Balancer capacity is reached its limits")
    }

    @Test
    suspend fun `Should respond with correct error if there is no healthy providers `() {
        val deadProvider = spyk(SimpleProvider("1")).also {
            coEvery { it.check() }.returns(false)
        }

        val providerRegistry = RandomProvidersRegistry().apply {
            addProvider(deadProvider)
        }
        deadProvider.waiUntilCheckIsCalled(times = 2)

        val loadBalancer = LoadBalancer(providersRegistry = providerRegistry)

        val error = shouldThrow<IllegalStateException> {
            loadBalancer.get()
        }.message

        error.shouldBe("Load Balancer capacity is reached its limits")
    }
}