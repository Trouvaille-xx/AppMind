package com.example.appmind.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.*

class OverlayWindow(
    private val context: Context,
    private val appName: String,
    private val question: String,
    private val onConfirm: (answer: String) -> Unit,
    private val onCancel: () -> Unit
) {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val inputFieldId = View.generateViewId()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isDismissed = false

    fun show() {
        if (isDismissed) return
        mainHandler.post {
            showInternal()
        }
    }

    private fun showInternal() {
        if (isDismissed) return
        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val layout = createOverlayLayout()
            overlayView = layout

            val params = WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                format = PixelFormat.TRANSLUCENT
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                gravity = Gravity.CENTER
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
            }

            windowManager?.addView(layout, params)

            // Request focus after adding
            mainHandler.postDelayed({
                try {
                    val input = layout.findViewById<EditText>(inputFieldId)
                    input?.requestFocus()
                    // Don't auto-show keyboard - let user tap
                } catch (_: Exception) {}
            }, 100)
        } catch (e: Exception) {
            e.printStackTrace()
            dismiss()
        }
    }

    fun dismiss() {
        mainHandler.post {
            if (isDismissed) return@post
            isDismissed = true
            try {
                overlayView?.let { v ->
                    windowManager?.removeView(v)
                }
            } catch (_: Exception) {}
            overlayView = null
            windowManager = null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayLayout(): android.widget.FrameLayout {
        val root = android.widget.FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#99000000"))
            isClickable = true
            isFocusable = true
            // Tap outside card to cancel
            setOnClickListener {
                dismiss()
                onCancel()
            }
        }

        val card = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(40, 40, 40, 40)
            gravity = Gravity.CENTER_HORIZONTAL
            val drawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 24f
            }
            background = drawable
            elevation = 12f
            isClickable = true
            isFocusable = true
            // Prevent clicks from passing through to root
            setOnClickListener { }
        }

        val appLabel = TextView(context).apply {
            text = "📱 $appName"
            textSize = 16f
            setTextColor(Color.parseColor("#666666"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val questionLabel = TextView(context).apply {
            text = question
            textSize = 20f
            setTextColor(Color.parseColor("#1A1A1A"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val inputField = EditText(context).apply {
            id = inputFieldId
            hint = "请输入你的想法..."
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            setHintTextColor(Color.parseColor("#999999"))
            setPadding(24, 16, 24, 16)
            minLines = 2
            maxLines = 4
            gravity = Gravity.TOP or Gravity.START
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#F5F5F5"))
                cornerRadius = 12f
            }
            background = bg
            isFocusable = true
            isFocusableInTouchMode = true
            // Handle back key to cancel
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    dismiss()
                    onCancel()
                    true
                } else false
            }
        }

        val buttonRow = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 24, 0, 0)
            gravity = Gravity.CENTER
        }

        val cancelBtn = TextView(context).apply {
            text = "取消 (点空白处也可)"
            textSize = 15f
            setTextColor(Color.parseColor("#999999"))
            gravity = Gravity.CENTER
            setPadding(24, 12, 24, 12)
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#F0F0F0"))
                cornerRadius = 24f
            }
            background = bg
            isClickable = true
            isFocusable = true
            setOnClickListener {
                onCancel()
            }
        }

        val confirmBtn = TextView(context).apply {
            text = "确认打开"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(24, 12, 24, 12)
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#4A90D9"))
                cornerRadius = 24f
            }
            background = bg
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val answer = inputField.text.toString().trim()
                onConfirm(answer)
            }
        }

        buttonRow.addView(cancelBtn)
        buttonRow.addView(View(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(16, 1)
        })
        buttonRow.addView(confirmBtn)

        card.addView(appLabel)
        card.addView(questionLabel)
        card.addView(inputField)
        card.addView(buttonRow)

        val cardLp = android.widget.FrameLayout.LayoutParams(
            (context.resources.displayMetrics.widthPixels * 0.88).toInt(),
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
        cardLp.gravity = Gravity.CENTER
        root.addView(card, cardLp)

        // Catch key events for back button
        root.isFocusableInTouchMode = true
        root.requestFocus()
        root.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dismiss()
                onCancel()
                true
            } else false
        }

        return root
    }
}
