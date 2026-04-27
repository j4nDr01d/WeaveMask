package io.github.seyud.weave.ui.superuser

import io.github.seyud.weave.core.Config
import io.github.seyud.weave.core.integration.ShellZygiskNextDenylistPolicy
import io.github.seyud.weave.core.integration.ZygiskNextDenylistPolicy

internal data class SuperuserModeApplyResult(
    val appliedMode: Int,
    val success: Boolean,
    val zygiskNextActive: Boolean,
)

internal fun superuserModeUsesWhitelist(mode: Int): Boolean = isWhitelistMode(mode)

internal fun whitelistEnabledToSuperuserMode(enabled: Boolean): Int =
    if (enabled) Config.Value.SU_MODE_WHITELIST else Config.Value.SU_MODE_BLACKLIST

internal class SuperuserModeSyncCoordinator(
    private val zygiskNextPolicy: ZygiskNextDenylistPolicy = ShellZygiskNextDenylistPolicy,
) {

    suspend fun isZygiskNextActive(): Boolean = zygiskNextPolicy.isActive()

    suspend fun resolveMode(currentMode: Int): Int {
        if (!zygiskNextPolicy.isActive()) {
            return normalizeSuperuserListMode(currentMode)
        }
        val whitelistEnabled = zygiskNextPolicy.getWhitelistMode() ?: return normalizeSuperuserListMode(currentMode)
        return whitelistEnabledToSuperuserMode(whitelistEnabled)
    }

    suspend fun applyMode(requestedMode: Int): SuperuserModeApplyResult {
        val normalizedMode = normalizeSuperuserListMode(requestedMode)
        val whitelistEnabled = superuserModeUsesWhitelist(normalizedMode)
        if (!zygiskNextPolicy.isActive()) {
            return SuperuserModeApplyResult(
                appliedMode = normalizedMode,
                success = true,
                zygiskNextActive = false,
            )
        }

        val currentWhitelistMode = zygiskNextPolicy.getWhitelistMode()

        if (currentWhitelistMode != null && currentWhitelistMode == whitelistEnabled) {
            return SuperuserModeApplyResult(
                appliedMode = normalizedMode,
                success = true,
                zygiskNextActive = true,
            )
        }

        val success = zygiskNextPolicy.setWhitelistMode(whitelistEnabled)
        return SuperuserModeApplyResult(
            appliedMode = normalizedMode,
            success = success,
            zygiskNextActive = true,
        )
    }
}
