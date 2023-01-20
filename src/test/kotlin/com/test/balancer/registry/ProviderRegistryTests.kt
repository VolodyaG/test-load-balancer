package com.test.balancer.registry

import com.test.balancer.Config
import com.test.balancer.provider.SimpleProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class ProviderRegistryTests : AnnotationSpec() {
    @BeforeAll
    fun prepare() {
        mockkObject(Config).also {
            every { Config.HEALTHCHECK_PERIOD }.returns(10.milliseconds)
        }
    }

    @AfterAll
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    suspend fun `Should return random provider`() {
        val registry = RandomProvidersRegistry().apply {
            addProvider(SimpleProvider("1"))
            addProvider(SimpleProvider("2"))
        }
        waitForHealthCheck()

        val provider = registry.nextAvailableProvider()?.id
        provider.shouldBeIn("1", "2")
    }

    @Test
    suspend fun `Should iterate with round-robbin over providers`() {
        val registry = RoundRobbinProvidersRegistry().apply {
            addProvider(SimpleProvider("1"))
            addProvider(SimpleProvider("2"))
            addProvider(SimpleProvider("3"))
        }
        waitForHealthCheck()

        val result = (0 until 4).mapNotNull {
            registry.nextAvailableProvider()?.id
        }
        result.shouldContainInOrder("1", "2", "3", "1")
    }

    @Test
    suspend fun `Should iterate with round-robbin over providers skipping dead`() {
        val deadProvider = spyk(SimpleProvider("2")).also {
            coEvery { it.check() }.returns(false)
        }

        val registry = RoundRobbinProvidersRegistry().apply {
            addProvider(SimpleProvider("1"))
            addProvider(deadProvider)
            addProvider(SimpleProvider("3"))
        }

        waitForHealthCheck()

        val result = (0 until 4).mapNotNull {
            registry.nextAvailableProvider()?.id
        }

        result.shouldContainInOrder("1", "3", "1", "3")
    }

    @Test
    suspend fun `Should add and remove provider from registry`() {
        val provider = SimpleProvider("1")

        listOf(
            RandomProvidersRegistry(),
            RoundRobbinProvidersRegistry(),
        ).forEach {
            it.addProvider(provider)
            waitForHealthCheck()

            it.countAvailableProviders().shouldBe(1)

            it.removeProvider(provider.id)
            it.countAvailableProviders().shouldBe(0)
        }
    }


    @Test
    suspend fun `Should return null if no providers are registered`() {
        listOf(
            RandomProvidersRegistry(),
            RoundRobbinProvidersRegistry(),
        ).forEach {
            it.nextAvailableProvider().shouldBeNull()
        }
    }

    @Test
    suspend fun `Should return null if only unhealthy providers are registered`() {
        val deadProvider = spyk(SimpleProvider("1")).also {
            coEvery { it.check() }.returns(false)
        }

        val registry = RoundRobbinProvidersRegistry().apply { addProvider(deadProvider) }
        waitForHealthCheck()

        registry.nextAvailableProvider().shouldBeNull()
    }

    @Test
    suspend fun `Should prohibit adding more than 10 providers in registry`() {
        val registry = RandomProvidersRegistry().apply {
            repeat(10) {
                addProvider(SimpleProvider(it.toString()))
            }
        }

        val error = shouldThrow<IllegalStateException> {
            registry.addProvider(SimpleProvider("11"))
        }.message

        error.shouldBe("Maximum number of providers has been reached")
    }

    private suspend fun waitForHealthCheck() {
        delay(100) // Think about something smarter and more reliable
    }
}