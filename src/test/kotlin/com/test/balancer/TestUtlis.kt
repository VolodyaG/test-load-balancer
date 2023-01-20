package com.test.balancer

import com.test.balancer.provider.Provider
import io.kotest.assertions.until.fixed
import io.kotest.assertions.until.until
import io.mockk.coVerify
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

suspend fun Provider.waiUntilCheckIsCalled(times: Int, timeout: Duration = 5.seconds) {
    until(duration = timeout, interval = 100.milliseconds.fixed()) {
        runCatching { coVerify(atLeast = times) { this@waiUntilCheckIsCalled.check() } }.isSuccess
    }
}