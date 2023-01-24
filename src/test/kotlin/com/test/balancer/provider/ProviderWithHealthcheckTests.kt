package com.test.balancer.provider

import com.test.balancer.Config
import com.test.balancer.waiUntilCheckIsCalled
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.*
import kotlin.time.Duration.Companion.milliseconds

class ProviderWithHealthcheckTests : AnnotationSpec() {
    @BeforeAll
    fun prepare() {
        mockkObject(Config).also {
            every { Config.HEALTHCHECK_PERIOD }.returns(300.milliseconds)
        }
    }

    @AfterAll
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    suspend fun `Should initialize provider as unhealthy until it is not checked`() {
        val provider = SimpleProvider("1")
        val providerWithHealthcheck = ProviderWithHealthcheck(provider)

        providerWithHealthcheck.isAvailable().shouldBeFalse()
    }

    @Test
    suspend fun `Should consider provider as healthy after it passes the check`() {
        val provider = spyk(SimpleProvider("1"))
        val providerWithHealthcheck = ProviderWithHealthcheck(provider)

        provider.waiUntilCheckIsCalled(times = 2)

        providerWithHealthcheck.isAvailable().shouldBeTrue()
    }

    @Test
    suspend fun `Should made provider unhealthy if check is failed even once`() {
        val provider = spyk(SimpleProvider("1")).also {
            coEvery { it.check() }.returns(false)
        }
        val providerWithHealthcheck = ProviderWithHealthcheck(provider)

        provider.waiUntilCheckIsCalled(times = 1)

        providerWithHealthcheck.isAvailable().shouldBeFalse()
    }

    @Test
    suspend fun `Should still consider provider unhealthy after one successful check`() {
        val provider = spyk(SimpleProvider("1")).also {
            coEvery { it.check() }.returns(false)
        }
        val providerWithHealthcheck = ProviderWithHealthcheck(provider)

        provider.waiUntilCheckIsCalled(times = 1)
        providerWithHealthcheck.isAvailable().shouldBeFalse()

        coEvery { provider.check() }.returns(true)
        provider.waiUntilCheckIsCalled(times = 2)

        providerWithHealthcheck.isAvailable().shouldBeFalse()
    }

    @Test
    suspend fun `Should made provider healthy when two consecutive checks are successful`() {
        val provider = spyk(SimpleProvider("1")).also {
            coEvery { it.check() }.returns(false)
        }

        val providerWithHealthcheck = ProviderWithHealthcheck(provider)
        provider.waiUntilCheckIsCalled(times = 1)

        providerWithHealthcheck.isAvailable().shouldBeFalse()

        coEvery { provider.check() }.returns(true)
        provider.waiUntilCheckIsCalled(times = 3)

        providerWithHealthcheck.isAvailable().shouldBeTrue()
    }
}