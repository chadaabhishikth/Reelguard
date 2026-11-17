package app.reelguard.diagnostics

import android.content.Context
import app.reelguard.domain.GuardCommand
import app.reelguard.domain.GuardSignal
import app.reelguard.domain.GuardState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small, opt-in-safe local trace for lifecycle debugging.
 *
 * Entries deliberately contain no accessibility-node text, resource IDs, package names
 * other than the fixed Instagram policy scope, usernames, captions, or content markers.
 */
class LocalDiagnostics(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun record(signal: GuardSignal, state: GuardState, command: GuardCommand) {
        val timestamp = timestampFormat.format(Date())
        val entry = "$timestamp signal=${signal.name} state=${state.name} command=${command.name}"
        val entries = loadEntries().takeLast(MAX_ENTRIES - 1) + entry
        preferences.edit().putStringSet(KEY_ENTRIES, entries.toSet()).apply()
    }

    fun exportText(): String = loadEntries().joinToString(separator = "\n")

    fun clear() {
        preferences.edit().remove(KEY_ENTRIES).apply()
    }

    private fun loadEntries(): List<String> =
        preferences.getStringSet(KEY_ENTRIES, emptySet()).orEmpty().sorted()

    private companion object {
        const val PREFERENCES_NAME = "local_diagnostics"
        const val KEY_ENTRIES = "entries"
        const val MAX_ENTRIES = 100
        val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
    }
}
