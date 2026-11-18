package com.sukisu.ultra.data.repository

import com.sukisu.ultra.data.model.KernelModule
import com.sukisu.ultra.data.model.KernelModuleCollection
import com.sukisu.ultra.data.model.KernelModuleLastResult

interface KernelModuleRepository {
    suspend fun getModules(): Result<KernelModuleCollection>

    suspend fun install(path: String): Result<Unit>

    suspend fun remove(id: String): Result<Unit>

    suspend fun enable(id: String): Result<Unit>

    suspend fun disable(id: String): Result<Unit>

    suspend fun setAutoload(id: String, enabled: Boolean): Result<Unit>

    suspend fun setParams(id: String, params: String): Result<Unit>

    suspend fun load(id: String): Result<Unit>

    suspend fun getLog(id: String): Result<KernelModuleLastResult?>
}

class KernelModuleCommandException(val code: Int, message: String) : Exception(message)
