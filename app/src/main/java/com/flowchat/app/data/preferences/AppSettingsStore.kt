package com.flowchat.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val memoryEnabled: Boolean = true,
    val powerModeEnabled: Boolean = false,
    val webSearchDisclosureAccepted: Boolean = false,
    val installId: String = ""
)

@Singleton
class AppSettingsStore private constructor(
    private val preferences: SharedPreferences?,
    initial: AppSettings
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE),
        initial = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).loadSettings()
    )

    internal constructor(initial: AppSettings) : this(null, initial)

    private val mutableState = MutableStateFlow(initial)
    val state: StateFlow<AppSettings> = mutableState.asStateFlow()

    fun setMemoryEnabled(enabled: Boolean) = update(MemoryEnabledKey, enabled) { copy(memoryEnabled = enabled) }

    fun setPowerModeEnabled(enabled: Boolean) =
        update(PowerModeEnabledKey, enabled) { copy(powerModeEnabled = enabled) }

    fun acceptWebSearchDisclosure() =
        update(WebSearchDisclosureKey, true) { copy(webSearchDisclosureAccepted = true) }

    private fun update(key: String, value: Boolean, transform: AppSettings.() -> AppSettings) {
        mutableState.value = mutableState.value.transform()
        preferences?.edit()?.putBoolean(key, value)?.apply()
    }

    private companion object {
        const val PreferencesName = "app_controls"
        const val MemoryEnabledKey = "memory_enabled"
        const val PowerModeEnabledKey = "power_mode_enabled"
        const val AppUsageEnabledKey = "app_usage_tool_enabled"
        const val RecentActivityEnabledKey = "recent_activity_tool_enabled"
        const val OpenAppEnabledKey = "open_app_tool_enabled"
        const val DeviceAssistantEnabledKey = "device_assistant_enabled"
        const val FloatingAssistantEnabledKey = "floating_assistant_enabled"
        const val ForceStopEnabledKey = "force_stop_tool_enabled"
        const val WebSearchDisclosureKey = "web_search_disclosure_accepted"
        const val InstallIdKey = "install_id"

        fun SharedPreferences.loadSettings(): AppSettings {
            val installId = getString(InstallIdKey, null)?.takeIf { it.isNotBlank() }
                ?: UUID.randomUUID().toString().also { edit().putString(InstallIdKey, it).apply() }
            val powerModeEnabled = if (contains(PowerModeEnabledKey)) {
                getBoolean(PowerModeEnabledKey, false)
            } else {
                val migrated = getBoolean(AppUsageEnabledKey, false) ||
                    getBoolean(RecentActivityEnabledKey, false) ||
                    getBoolean(DeviceAssistantEnabledKey, false) ||
                    getBoolean(FloatingAssistantEnabledKey, false) ||
                    getBoolean(ForceStopEnabledKey, false)
                edit().putBoolean(PowerModeEnabledKey, migrated).apply()
                migrated
            }
            return AppSettings(
                memoryEnabled = getBoolean(MemoryEnabledKey, true),
                powerModeEnabled = powerModeEnabled,
                webSearchDisclosureAccepted = getBoolean(WebSearchDisclosureKey, false),
                installId = installId
            )
        }
    }
}
