package com.mauricekuehl.appblock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import com.mauricekuehl.appblock.MainActivity
import com.mauricekuehl.appblock.formatTime

internal class BlockingOverlay(private val service: AccessibilityService) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var overlayView: View? = null
    private var shownPackage: String? = null

    fun show(
        packageName: String,
        appLabel: String,
        endMinute: Int,
        onOverlayFailure: () -> Unit,
    ) {
        if (overlayView != null && shownPackage == packageName) return
        hide()

        val view = createOverlayView(appLabel, endMinute)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            title = "App Block: $appLabel"
        }

        try {
            windowManager.addView(view, params)
            overlayView = view
            shownPackage = packageName
            view.isFocusableInTouchMode = true
            view.requestFocus()
        } catch (_: RuntimeException) {
            overlayView = null
            shownPackage = null
            onOverlayFailure()
        }
    }

    fun hide() {
        val view = overlayView ?: return
        overlayView = null
        shownPackage = null
        runCatching { windowManager.removeViewImmediate(view) }
    }

    private fun createOverlayView(appLabel: String, endMinute: Int): View {
        val root = FrameLayout(service).apply {
            setBackgroundColor(BACKGROUND_COLOR)
            isClickable = true
            isFocusable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            contentDescription = "$appLabel is blocked until ${formatTime(endMinute)}"
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    goHome()
                    true
                } else {
                    false
                }
            }
        }

        val content = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), dp(32), dp(32), dp(32))
        }
        root.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ),
        )

        content.addView(TextView(service).apply {
            text = "Focus time"
            textSize = 18f
            setTextColor(PRIMARY_COLOR)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(12), dp(20), dp(12))
            background = roundedBackground(PRIMARY_CONTAINER_COLOR, 28)
        })

        content.addView(space(28))
        content.addView(TextView(service).apply {
            text = "$appLabel is taking a break"
            textSize = 30f
            setTextColor(TEXT_COLOR)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }, matchWidth())

        content.addView(space(12))
        content.addView(TextView(service).apply {
            text = "Your block window ends at ${formatTime(endMinute)}."
            textSize = 18f
            setTextColor(SECONDARY_TEXT_COLOR)
            gravity = Gravity.CENTER
        }, matchWidth())

        content.addView(space(36))
        content.addView(Button(service).apply {
            text = "Go to home screen"
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(PRIMARY_COLOR)
            setOnClickListener { goHome() }
        }, buttonLayout())

        content.addView(space(10))
        content.addView(Button(service).apply {
            text = "Change schedule"
            textSize = 16f
            isAllCaps = false
            setTextColor(PRIMARY_COLOR)
            backgroundTintList = ColorStateList.valueOf(PRIMARY_CONTAINER_COLOR)
            setOnClickListener {
                hide()
                service.startActivity(Intent(service, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                })
            }
        }, buttonLayout())

        return root
    }

    private fun goHome() {
        hide()
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    private fun space(heightDp: Int) = Space(service).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    )

    private fun buttonLayout() = LinearLayout.LayoutParams(dp(240), dp(52))

    private fun roundedBackground(color: Int, radiusDp: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun dp(value: Int): Int = (value * service.resources.displayMetrics.density).toInt()

    private companion object {
        const val BACKGROUND_COLOR = 0xFFF7F9F5.toInt()
        const val PRIMARY_COLOR = 0xFF21573A.toInt()
        const val PRIMARY_CONTAINER_COLOR = 0xFFBEEBCC.toInt()
        const val TEXT_COLOR = 0xFF191C1A.toInt()
        const val SECONDARY_TEXT_COLOR = 0xFF414942.toInt()
    }
}
