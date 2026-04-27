package io.github.seyud.weave.core.integration

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

interface ZygiskNextDenylistPolicy {
    suspend fun isActive(): Boolean
    suspend fun getWhitelistMode(): Boolean?
    suspend fun setWhitelistMode(enabled: Boolean): Boolean
}

internal data class ZygiskNextShellResult(
    val code: Int,
    val out: List<String>,
) {
    val accepted: Boolean
        get() = code == 0 || code == 1
}

internal interface ZygiskNextShellRunner {
    fun run(command: String): ZygiskNextShellResult
}

private object LibSuZygiskNextShellRunner : ZygiskNextShellRunner {
    override fun run(command: String): ZygiskNextShellResult {
        val result = Shell.cmd(command).exec()
        return ZygiskNextShellResult(
            code = result.code,
            out = result.out,
        )
    }
}

internal class ShellZygiskNextDenylistPolicyImpl(
    private val shellRunner: ZygiskNextShellRunner = LibSuZygiskNextShellRunner,
) : ZygiskNextDenylistPolicy {

    private val ZYGISKD_PATH = "/data/adb/modules/zygisksu/bin/zygiskd"
    private val DENYLIST_POLICY_FILE = "/data/adb/zygisksu/denylist_policy"
    private val INJECT_STATE_RUNNING = 1
    private val ROOT_IMPL_MAGISK = 4

    @Volatile
    private var activeCache: Boolean? = null

    override suspend fun isActive(): Boolean = withContext(Dispatchers.IO) {
        activeCache
            ?.let { return@withContext it }

        return@withContext synchronized(this@ShellZygiskNextDenylistPolicyImpl) {
            activeCache
                ?.let { return@synchronized it }

            val isActive = readStatusMap()
                ?.let(::isActiveStatus)
                ?: false
            activeCache = isActive
            isActive
        }
    }

    override suspend fun getWhitelistMode(): Boolean? = withContext(Dispatchers.IO) {
        if (!isActive()) {
            return@withContext null
        }
        return@withContext when (readPolicyValue()?.lowercase(Locale.ROOT)) {
            "1", "true", "whitelist" -> true
            "0", "false", "default" -> false
            else -> null
        }
    }

    override suspend fun setWhitelistMode(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!isActive()) {
            return@withContext false
        }
        val policy = if (enabled) "whitelist" else "default"
        shellRunner.run("$ZYGISKD_PATH denylist-policy $policy").accepted
    }

    private fun readStatusMap(): Map<String, String>? {
        val result = shellRunner.run("$ZYGISKD_PATH status")
        if (!result.accepted) {
            return null
        }

        return result.out
            .asSequence()
            .mapNotNull { line ->
                val separator = line.indexOf(':')
                if (separator <= 0) {
                    return@mapNotNull null
                }
                val key = line.substring(0, separator).trim()
                if (key.isEmpty()) {
                    return@mapNotNull null
                }
                key to line.substring(separator + 1).trim()
            }
            .toMap()
    }

    private fun isActiveStatus(status: Map<String, String>): Boolean {
        val injectState = status["inject_state"]?.toIntOrNull()
        val rootImpl = status["root_impl"]?.toIntOrNull()
        return injectState == INJECT_STATE_RUNNING && rootImpl == ROOT_IMPL_MAGISK
    }

    private fun readPolicyValue(): String? {
        val result = shellRunner.run("cat '$DENYLIST_POLICY_FILE' 2>/dev/null")
        if (!result.accepted) {
            return null
        }
        return result.out
            .firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
}

object ShellZygiskNextDenylistPolicy : ZygiskNextDenylistPolicy by ShellZygiskNextDenylistPolicyImpl()
