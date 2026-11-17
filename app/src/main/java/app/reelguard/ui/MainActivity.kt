package app.reelguard.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import app.reelguard.R
import app.reelguard.accessibility.GuardAccessibilityService
import app.reelguard.detection.CompatibilityProfile
import com.google.android.material.materialswitch.MaterialSwitch

/** Plain View-based setup dashboard. No account, network, or runtime permissions exist. */
class MainActivity : AppCompatActivity() {
    private lateinit var preferences: GuardPreferences
    private lateinit var protectionSwitch: MaterialSwitch
    private lateinit var diagnosticsSwitch: MaterialSwitch
    private lateinit var dmModeSwitch: MaterialSwitch
    private lateinit var serviceStatus: TextView
    private lateinit var protectionSummary: TextView
    private lateinit var consent: CheckBox
    private lateinit var accessibilitySettingsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        preferences = GuardPreferences(this)

        protectionSwitch = findViewById(R.id.protection_switch)
        diagnosticsSwitch = findViewById(R.id.diagnostics_switch)
        dmModeSwitch = findViewById(R.id.dm_mode_switch)
        serviceStatus = findViewById(R.id.service_status)
        protectionSummary = findViewById(R.id.protection_summary)
        consent = findViewById(R.id.accessibility_consent)
        accessibilitySettingsButton = findViewById(R.id.open_accessibility_settings)

        protectionSwitch.setOnCheckedChangeListener { _, enabled ->
            preferences.protectionEnabled = enabled
            updateProtectionSummary(enabled)
        }
        diagnosticsSwitch.setOnCheckedChangeListener { _, enabled ->
            preferences.diagnosticsEnabled = enabled
        }
        consent.setOnCheckedChangeListener { _, accepted ->
            accessibilitySettingsButton.isEnabled = accepted && !isAccessibilityServiceEnabled()
        }
        accessibilitySettingsButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // The starter profile is deliberately empty. Keep the exception false until a
        // future phone-tested profile is added and the UI can enable this control.
        dmModeSwitch.isEnabled = CompatibilityProfile.UNVERIFIED.isDmExceptionVerified
        dmModeSwitch.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val serviceEnabled = isAccessibilityServiceEnabled()
        serviceStatus.setText(if (serviceEnabled) R.string.service_enabled else R.string.service_disabled)

        protectionSwitch.setOnCheckedChangeListener(null)
        protectionSwitch.isChecked = preferences.protectionEnabled
        protectionSwitch.setOnCheckedChangeListener { _, enabled ->
            preferences.protectionEnabled = enabled
            updateProtectionSummary(enabled)
        }
        updateProtectionSummary(preferences.protectionEnabled)

        diagnosticsSwitch.setOnCheckedChangeListener(null)
        diagnosticsSwitch.isChecked = preferences.diagnosticsEnabled
        diagnosticsSwitch.setOnCheckedChangeListener { _, enabled ->
            preferences.diagnosticsEnabled = enabled
        }

        dmModeSwitch.isChecked = false
        dmModeSwitch.isEnabled = false
        preferences.dmModeEnabled = false
        accessibilitySettingsButton.isEnabled = consent.isChecked && !serviceEnabled
    }

    private fun updateProtectionSummary(enabled: Boolean) {
        protectionSummary.setText(
            if (enabled) R.string.protection_enabled_summary else R.string.protection_disabled_summary,
        )
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val manager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val component = ComponentName(this, GuardAccessibilityService::class.java)
        return manager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                info.resolveInfo.serviceInfo.packageName == component.packageName &&
                    info.resolveInfo.serviceInfo.name == component.className
            }
    }
}
