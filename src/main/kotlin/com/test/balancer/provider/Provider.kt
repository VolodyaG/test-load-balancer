package com.test.balancer.provider

interface Provider {
    val id: String

    suspend fun get(): String
    suspend fun check(): Boolean
    suspend fun isAvailable(): Boolean
}

class SimpleProvider(override val id: String) : Provider {
    override suspend fun get() = id
    override suspend fun check() = true
    override suspend fun isAvailable() = true
}

