package app.reelguard.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import app.reelguard.R

/**
 * Accessibility-owned overlays for the two policy outcomes. They are removed whenever
 * protection ends, the service stops, or another app comes to the foreground.
 */
class ReelOverlayController(
    private val service: AccessibilityService,
    private val onBackRequested: () -> Unit,
) {
    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlay: View? = null

    fun showBlockedViewer() {
        showOverlay(
            title = service.getString(R.string.blocked_reels_title),
            summary = service.getString(R.string.blocked_reels_summary),
            darkenFully = true,
        )
    }

    fun showDmGuard() {
        showOverlay(
            title = service.getString(R.string.shared_reel_title),
            summary = service.getString(R.string.shared_reel_summary),
            darkenFully = false,
        )
    }

    fun clear() {
        val view = overlay ?: return
        overlay = null
        runCatching { windowManager.removeViewImmediate(view) }
    }

    private fun showOverlay(title: String, summary: String, darkenFully: Boolean) {
        clear()

        val root = FrameLayout(service).apply {
            // A clickable FrameLayout consumes touches in the viewer area while still
            // dispatching the explicit Back button to its child.
            isClickable = true
            isFocusable = false
            setBackgroundColor(if (darkenFully) Color.rgb(18, 18, 20) else 0xB0000000.toInt())
        }

        val card = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Color.rgb(38, 38, 44))
            }
        }
        card.addView(label(title, 20, Color.WHITE))
        card.addView(label(summary, 15, 0xFFDADAE2.toInt()).apply {
            setPadding(0, dp(8), 0, dp(12))
        })
        card.addView(Button(service).apply {
            text = service.getString(R.string.back_to_chat)
            setOnClickListener { onBackRequested() }
        })

        root.addView(
            card,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            ).apply {
                setMargins(dp(16), dp(24), dp(16), 0)
            },
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            title = "ReelGuard overlay"
        }

        if (runCatching { windowManager.addView(root, params) }.isSuccess) {
            overlay = root
        }
    }

    private fun label(text: String, sizeSp: Int, color: Int): TextView =
        TextView(service).apply {
            this.text = text
            textSize = sizeSp.toFloat()
            setTextColor(color)
        }

    private fun dp(value: Int): Int =
        (value * service.resources.displayMetrics.density).toInt()
}
