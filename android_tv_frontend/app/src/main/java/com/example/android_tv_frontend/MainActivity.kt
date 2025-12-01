package com.example.android_tv_frontend

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.SoundEffectConstants
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider

/**
 * Main Activity for Android TV Number Guessing — TV
 * TV-optimized layout using focusable controls and DPAD navigation.
 * This XML-free programmatic UI ensures precise focus order and styles per Ocean Professional theme.
 */
class MainActivity : FragmentActivity() {

    private lateinit var vm: GameViewModel

    // Colors per Ocean Professional
    private val colorPrimary = Color.parseColor("#2563EB")
    private val colorSecondary = Color.parseColor("#F59E0B")
    private val colorSuccess = Color.parseColor("#F59E0B")
    private val colorError = Color.parseColor("#EF4444")
    private val colorBg = Color.parseColor("#f9fafb")
    private val colorSurface = Color.parseColor("#ffffff")
    private val colorText = Color.parseColor("#111827")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(this)[GameViewModel::class.java]

        // Root with subtle blue-to-gray gradient background
        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#e6eaf7"), colorBg)
            )
        }

        // Center card container
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(32), dp(32), dp(32))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(colorSurface)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.parseColor("#e5e7eb"))
            }
            // Shadow like effect where supported
            if (Build.VERSION.SDK_INT >= 21) {
                elevation = dp(8).toFloat()
            }
        }

        val title = TextView(this).apply {
            text = "Number Guessing — TV"
            setTextColor(colorText)
            textSize = 28f
        }

        val inputDisplay = TextView(this).apply {
            text = ""
            setTextColor(colorPrimary)
            textSize = 40f
            setPadding(0, dp(8), 0, dp(8))
            // Make focusable to allow DPAD highlighting and selection
            isFocusable = true
            isFocusableInTouchMode = true
            background = focusableSurfaceBackground()
            setPadding(dp(16), dp(12), dp(16), dp(12))
            contentDescription = "Current input"
        }

        val feedbackText = TextView(this).apply {
            text = ""
            setTextColor(colorText)
            textSize = 22f
            setPadding(0, dp(6), 0, dp(6))
            contentDescription = "Feedback"
        }

        val attemptsText = TextView(this).apply {
            text = "Attempts: 0"
            setTextColor(colorText)
            textSize = 20f
            setPadding(0, dp(6), 0, dp(16))
            contentDescription = "Attempts counter"
        }

        // Grid for digits and actions (3x4 for digits plus one row for actions)
        val keypadGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Build 3x4 keypad: 1-9, then row [Clear, 0, Delete]
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("Clear", "0", "Delete"),
        )

        val keypadButtons = mutableListOf<Button>()

        for (r in rows) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(8)
                }
            }
            r.forEach { label ->
                val btn = buildTvButton(label).apply {
                    val weight = 1f
                    layoutParams = LinearLayout.LayoutParams(0, dp(72), weight).apply {
                        marginStart = dp(8)
                        marginEnd = dp(8)
                    }
                    setOnClickListener {
                        playClick()
                        when (label) {
                            "Clear" -> vm.clearInput()
                            "Delete" -> vm.deleteDigit()
                            else -> {
                                val d = label.toIntOrNull()
                                if (d != null) vm.appendDigit(d)
                            }
                        }
                    }
                }
                keypadButtons.add(btn)
                row.addView(btn)
            }
            keypadGrid.addView(row)
        }

        // Action buttons row: Submit and New Game
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
            }
        }

        val submitBtn = buildTvButton("Submit", accent = true).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(72), 1f).apply {
                marginStart = dp(8)
                marginEnd = dp(8)
            }
            setOnClickListener {
                playClick()
                vm.submitGuess()
            }
        }

        val newGameBtn = buildTvButton("New Game").apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(72), 1f).apply {
                marginStart = dp(8)
                marginEnd = dp(8)
            }
            setOnClickListener {
                playClick()
                vm.newGame()
            }
        }

        actionsRow.addView(submitBtn)
        actionsRow.addView(newGameBtn)

        // Assemble card
        card.addView(title)
        card.addView(inputDisplay)
        card.addView(feedbackText)
        card.addView(attemptsText)
        card.addView(keypadGrid)
        card.addView(actionsRow)

        root.addView(card)
        setContentView(root)

        // Observe view model
        vm.input.observe(this) { input ->
            inputDisplay.text = if (input.isEmpty()) "—" else input
        }
        vm.feedback.observe(this) { msg ->
            feedbackText.text = msg
            if (msg == "Correct!") {
                feedbackText.setTextColor(colorSuccess)
            } else if (msg == "Too low" || msg == "Too high" || msg.startsWith("Invalid") || msg.startsWith("Enter")) {
                feedbackText.setTextColor(colorError)
            } else {
                feedbackText.setTextColor(colorText)
            }
        }
        vm.attempts.observe(this) { attempts ->
            attemptsText.text = "Attempts: $attempts"
        }

        // Initialize focus on first keypad button for DPAD usability
        keypadButtons.firstOrNull()?.requestFocus()

        // Configure DPAD navigation order (top-to-bottom, left-to-right)
        setLinearNavigation(keypadGrid)
        setRowNavigation(actionsRow)

        // When game over, move focus to New Game to encourage restart
        vm.gameOver.observe(this) { over ->
            if (over) {
                newGameBtn.requestFocus()
            }
        }

        // Start a new game on first load
        if (savedInstanceState == null) {
            vm.newGame()
        }
    }

    // PUBLIC_INTERFACE
    /**
     * Configure DPAD navigation for a vertical LinearLayout containing horizontal rows.
     * Ensures logical TV navigation: left-right within row, up-down between rows.
     */
    fun setLinearNavigation(grid: LinearLayout) {
        val rows = grid.children.filterIsInstance<LinearLayout>().toList()
        for (i in rows.indices) {
            val row = rows[i]
            val buttons = row.children.filterIsInstance<View>().toList()
            // Horizontal navigation within the row
            for (j in buttons.indices) {
                val v = buttons[j]
                val left = buttons.getOrNull(j - 1)
                val right = buttons.getOrNull(j + 1)
                ViewCompat.setAccessibilityHeading(v, false)
                // Use pre-API 26 compatible focus APIs
                v.isFocusable = true
                v.isFocusableInTouchMode = true
                if (left != null) v.setNextFocusLeftId(left.idOrAssign())
                if (right != null) v.setNextFocusRightId(right.idOrAssign())

                // Up/Down navigation to same column in adjacent rows
                val upRow = rows.getOrNull(i - 1)
                val downRow = rows.getOrNull(i + 1)
                val up = upRow?.children?.filterIsInstance<View>()?.elementAtOrNull(j)
                val down = downRow?.children?.filterIsInstance<View>()?.elementAtOrNull(j)
                if (up != null) v.setNextFocusUpId(up.idOrAssign())
                if (down != null) v.setNextFocusDownId(down.idOrAssign())
            }
        }
    }

    // PUBLIC_INTERFACE
    /** Configure left-right navigation for a single horizontal row. */
    fun setRowNavigation(row: LinearLayout) {
        val buttons = row.children.filterIsInstance<View>().toList()
        for (j in buttons.indices) {
            val v = buttons[j]
            val left = buttons.getOrNull(j - 1)
            val right = buttons.getOrNull(j + 1)
            // Use pre-API 26 compatible focus APIs
            v.isFocusable = true
            v.isFocusableInTouchMode = true
            if (left != null) v.setNextFocusLeftId(left.idOrAssign())
            if (right != null) v.setNextFocusRightId(right.idOrAssign())
        }
        // Up navigation will be naturally handled by Android focusing previous focusable.
    }

    private fun View.idOrAssign(): Int {
        if (id == View.NO_ID) id = View.generateViewId()
        return id
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    /** Build a TV-friendly button with proper focus states and rounded corners. */
    private fun buildTvButton(textLabel: String, accent: Boolean = false): Button {
        val btn = Button(this).apply {
            text = textLabel
            isAllCaps = false
            textSize = 22f
            setTextColor(if (accent) Color.WHITE else colorText)
            isFocusable = true
            isFocusableInTouchMode = true
            background = tvButtonBackground(accent)
        }
        return btn
    }

    /** Simple sound feedback on button press if available. */
    private fun playClick() {
        // TV devices often play standard click; explicitly request it
        window.decorView.playSoundEffect(SoundEffectConstants.CLICK)
    }

    /** Build focusable surface background for input display. */
    private fun focusableSurfaceBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#f3f4f6"))
            cornerRadius = dp(12).toFloat()
            setStroke(dp(2), Color.parseColor("#e5e7eb"))
        }
    }

    /** Background with stateful feedback for TV buttons. */
    private fun tvButtonBackground(accent: Boolean): android.graphics.drawable.StateListDrawable {
        val normal = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(if (accent) colorPrimary else Color.WHITE)
            cornerRadius = dp(12).toFloat()
            setStroke(dp(2), if (accent) colorPrimary else Color.parseColor("#e5e7eb"))
        }
        val focused = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(if (accent) colorPrimary else Color.WHITE)
            cornerRadius = dp(12).toFloat()
            // Use secondary color as focus ring
            setStroke(dp(4), colorSecondary)
        }
        val pressed = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(if (accent) darken(colorPrimary) else Color.parseColor("#f3f4f6"))
            cornerRadius = dp(12).toFloat()
            setStroke(dp(2), if (accent) darken(colorPrimary) else Color.parseColor("#d1d5db"))
        }

        return android.graphics.drawable.StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressed)
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(intArrayOf(), normal)
        }
    }

    private fun darken(color: Int, factor: Float = 0.85f): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt().coerceAtLeast(0)
        val g = (Color.green(color) * factor).toInt().coerceAtLeast(0)
        val b = (Color.blue(color) * factor).toInt().coerceAtLeast(0)
        return Color.argb(a, r, g, b)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Let the individual focusable views handle DPAD by default, only handle BACK here.
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
