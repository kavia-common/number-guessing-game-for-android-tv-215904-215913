package com.example.android_tv_frontend

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.random.Random

/**
 * PUBLIC_INTERFACE
 * GameViewModel manages the game state for the number guessing game.
 * It holds target number, current input, attempts, feedback message, and game over flag.
 */
class GameViewModel : ViewModel() {

    private val _target = MutableLiveData(generateTarget())
    private val _input = MutableLiveData("")
    private val _attempts = MutableLiveData(0)
    private val _feedback = MutableLiveData("")
    private val _gameOver = MutableLiveData(false)

    val target: LiveData<Int> get() = _target
    val input: LiveData<String> get() = _input
    val attempts: LiveData<Int> get() = _attempts
    val feedback: LiveData<String> get() = _feedback
    val gameOver: LiveData<Boolean> get() = _gameOver

    private fun generateTarget(): Int = Random.nextInt(1, 101)

    // PUBLIC_INTERFACE
    /** Append a digit (0-9) to the current input; ignores leading zeros. */
    fun appendDigit(d: Int) {
        if (_gameOver.value == true) return
        if (d !in 0..9) return
        // Limit input length to 3 digits for 1..100
        val current = _input.value.orEmpty()
        if (current.length >= 3) return
        if (current.isEmpty() && d == 0) return // no leading zero
        _input.value = current + d.toString()
    }

    // PUBLIC_INTERFACE
    /** Delete the last digit if present. */
    fun deleteDigit() {
        if (_gameOver.value == true) return
        val current = _input.value.orEmpty()
        if (current.isNotEmpty()) {
            _input.value = current.dropLast(1)
        }
    }

    // PUBLIC_INTERFACE
    /** Clear the entire current input. */
    fun clearInput() {
        if (_gameOver.value == true) return
        _input.value = ""
    }

    // PUBLIC_INTERFACE
    /** Submit the current input as a guess and update feedback & attempts. */
    fun submitGuess() {
        if (_gameOver.value == true) return
        val current = _input.value.orEmpty()
        if (current.isBlank()) {
            _feedback.value = "Enter a number between 1 and 100"
            return
        }
        val guess = current.toIntOrNull()
        if (guess == null || guess !in 1..100) {
            _feedback.value = "Invalid number (1-100)"
            return
        }
        val newAttempts = (_attempts.value ?: 0) + 1
        _attempts.value = newAttempts

        val targetNum = _target.value ?: return
        when {
            guess < targetNum -> _feedback.value = "Too low"
            guess > targetNum -> _feedback.value = "Too high"
            else -> {
                _feedback.value = "Correct!"
                _gameOver.value = true
            }
        }
        // Keep input to show last guess
    }

    // PUBLIC_INTERFACE
    /** Start a new game by reinitializing state. */
    fun newGame() {
        _target.value = generateTarget()
        _input.value = ""
        _attempts.value = 0
        _feedback.value = "New game started"
        _gameOver.value = false
    }
}
