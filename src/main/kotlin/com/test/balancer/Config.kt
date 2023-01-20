package com.test.balancer

import kotlin.time.Duration.Companion.seconds

object Config {
    val MAX_CONCURENCY_PER_PROVIDER = 10
    val HEALTHCHECK_PERIOD = 1.seconds
}