package app.reelguard.ui

import android.content.Context

/** Preferences contain only local feature choices; no Instagram data is persisted. */
class GuardPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var protectionEnabled: Boolean
        get() = preferences.getBoolean(KEY_PROTECTION_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_PROTECTION_ENABLED, value).apply()

    var diagnosticsEnabled: Boolean
        get() = preferences.getBoolean(KEY_DIAGNOSTICS_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_DIAGNOSTICS_ENABLED, value).apply()

    var dmModeEnabled: Boolean
        get() = preferences.getBoolean(KEY_DM_MODE_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_DM_MODE_ENABLED, value).apply()

    companion object {
        private const val PREFERENCES_NAME = "guard_preferences"
        private const val KEY_PROTECTION_ENABLED = "protection_enabled"
        private const val KEY_DIAGNOSTICS_ENABLED = "diagnostics_enabled"
        private const val KEY_DM_MODE_ENABLED = "dm_mode_enabled"
    }
}
