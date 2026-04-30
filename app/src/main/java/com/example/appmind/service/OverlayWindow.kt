package com.example.appmind.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
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

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layout = createOverlayLayout()
        overlayView = layout

        val params = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            format = PixelFormat.TRANSLUCENT
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
        }

        windowManager?.addView(layout, params)

        // Auto-focus the input field
        val input = layout.findViewById<EditText>(inputFieldId)
        input.postDelayed({
            input.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }, 300)
    }

    @SuppressLint("DefaultLocale")
    private fun createOverlayLayout(): android.widget.LinearLayout {
        val root = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#66000000"))
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setOnClickListener { /* consume clicks, prevent touch passthrough */ }
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
            textSize = 22f
            setTextColor(Color.parseColor("#1A1A1A"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
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
            gravity = Gravity.TOP
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#F5F5F5"))
                cornerRadius = 12f
            }
            background = bg
        }

        val buttonRow = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 28, 0, 0)
            gravity = Gravity.CENTER
        }

        val cancelBtn = TextView(context).apply {
            text = "取消"
            textSize = 16f
            setTextColor(Color.parseColor("#999999"))
            gravity = Gravity.CENTER
            setPadding(32, 14, 32, 14)
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#F0F0F0"))
                cornerRadius = 24f
            }
            background = bg
            setOnClickListener {
                hideKeyboard(inputField)
                onCancel()
            }
        }

        val confirmBtn = TextView(context).apply {
            text = "确认打开"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(32, 14, 32, 14)
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#4A90D9"))
                cornerRadius = 24f
            }
            background = bg
            setOnClickListener {
                hideKeyboard(inputField)
                val answer = inputField.text.toString().trim()
                onConfirm(answer)
            }
        }

        buttonRow.addView(cancelBtn)
        val spacer = View(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(20, 1)
        }
        buttonRow.addView(spacer)
        buttonRow.addView(confirmBtn)

        card.addView(appLabel)
        card.addView(questionLabel)
        card.addView(inputField)
        card.addView(buttonRow)

        root.addView(card)

        val cardLp = card.layoutParams as android.widget.LinearLayout.LayoutParams
        cardLp.width = (context.resources.displayMetrics.widthPixels * 0.85).toInt()

        return root
    }

    private fun hideKeyboard(view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun dismiss() {
        try {
            overlayView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        overlayView = null
        windowManager = null
    }
}
