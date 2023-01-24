package com.test.balancer.registry

import com.test.balancer.provider.SimpleProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.spyk

class ProviderRegistryTests : AnnotationSpec() {
    @Test
    suspend fun `Should return random provider`() {
        val registry = RandomProvidersRegistry().apply {
            addProvider(SimpleProvider("1"))
            addProvider(SimpleProvider("2"))
        }

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

        val result = (0 until 4).mapNotNull {
            registry.nextAvailableProvider()?.id
        }
        result.shouldContainInOrder("1", "2", "3", "1")
    }

    @Test
    suspend fun `Should iterate with round-robbin over providers skipping unavailable providers`() {
        val unavailableProvider = spyk(SimpleProvider("2")).also {
            coEvery { it.isAvailable() }.returns(false)
        }

        val registry = RoundRobbinProvidersRegistry().apply {
            addProvider(SimpleProvider("1"))
            addProvider(unavailableProvider)
            addProvider(SimpleProvider("3"))
        }

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
            it.nextAvailableProvider()?.id.shouldBe(provider.id)

            it.removeProvider(provider.id)
            it.nextAvailableProvider().shouldBeNull()
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
    suspend fun `Should return null if only unavailable providers are registered`() {
        val deadProvider = spyk(SimpleProvider("1")).also {
            coEvery { it.isAvailable() }.returns(false)
        }

        val registry = RoundRobbinProvidersRegistry().apply {
            addProvider(deadProvider)
        }

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
}