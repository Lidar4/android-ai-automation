package com.example.myautomationapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val bg = Color.rgb(10, 15, 28)
    private val panel = Color.rgb(22, 31, 49)
    private val accent = Color.rgb(89, 155, 255)
    private val muted = Color.rgb(157, 172, 195)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(page)
        }

        page.addView(label("MY AUTOMATION", 12, accent, true))
        page.addView(label("Control Center", 30, Color.WHITE, true).withTop(6))
        page.addView(label("Your personal automation workspace", 14, muted).withTop(4))

        val statusCard = card().apply {
            addView(label("SYSTEM STATUS", 11, muted, true))
            addView(label("Ready to configure", 21, Color.WHITE, true).withTop(8))
            addView(label("Accessibility service can be enabled from settings. AI task execution is not connected yet.", 13, muted).withTop(6))
            addView(statusPill("SETUP REQUIRED").withTop(14))
        }.withTop(24)
        page.addView(statusCard)

        page.addView(sectionTitle("Quick actions").withTop(24))
        val settingsButton = actionButton("Enable Accessibility Service", "Open Android accessibility settings")
        settingsButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        page.addView(settingsButton.withTop(12))

        val infoButton = actionButton("About this build", "See what is available in this preview")
        infoButton.setOnClickListener {
            android.widget.Toast.makeText(
                this,
                "Preview build: dashboard UI is ready. AI planning and automation engine are not connected yet.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
        page.addView(infoButton.withTop(10))

        page.addView(sectionTitle("Workspace").withTop(26))
        page.addView(infoCard("01", "Task planner", "A place for future AI-generated task plans.", "COMING NEXT").withTop(12))
        page.addView(infoCard("02", "Automation history", "Completed and recent actions will appear here.", "COMING NEXT").withTop(10))
        page.addView(infoCard("03", "AI connection", "No AI provider key or live AI connection is configured in this build.", "NOT CONNECTED").withTop(10))

        page.addView(label("MY AUTOMATION APP  •  PREVIEW BUILD", 10, muted, true).apply {
            gravity = Gravity.CENTER
        }.withTop(28))

        setContentView(scroll)
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setLineSpacing(dp(2).toFloat(), 1f)
        }

    private fun sectionTitle(text: String) = label(text, 19, Color.WHITE, true)

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(panel, 18)
    }

    private fun statusPill(text: String) = TextView(this).apply {
        this.text = text
        textSize = 10f
        setTextColor(Color.rgb(255, 211, 128))
        setPadding(dp(10), dp(7), dp(10), dp(7))
        background = rounded(Color.rgb(73, 57, 35), 30)
    }

    private fun actionButton(title: String, subtitle: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = rounded(panel, 14)
        addView(label(title, 15, Color.WHITE, true))
        addView(label(subtitle, 12, muted).withTop(5))
        isClickable = true
        isFocusable = true
        val out = android.util.TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, out, true)
        foreground = getDrawable(out.resourceId)
    }

    private fun infoCard(number: String, title: String, description: String, status: String) = card().apply {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(label(number, 12, accent, true))
            addView(label(title, 16, Color.WHITE, true).apply {
                setPadding(dp(12), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        addView(row)
        addView(label(description, 12, muted).withTop(8))
        addView(label(status, 10, accent, true).withTop(10))
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun View.withTop(margin: Int): View {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(margin) }
        return this
    }
}
