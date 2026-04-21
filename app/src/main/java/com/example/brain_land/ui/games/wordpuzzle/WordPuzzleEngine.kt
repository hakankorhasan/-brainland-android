package com.example.brain_land.ui.games.wordpuzzle

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Models  (mirrors WPGameState.swift enums + structs)
// ─────────────────────────────────────────────────────────────────────────────

enum class WPLetterStatus { CORRECT, PRESENT, ABSENT, EMPTY }

data class WPLetterResult(val letter: String, val status: WPLetterStatus)
data class WPGuessRow(val results: List<WPLetterResult>)

// ─────────────────────────────────────────────────────────────────────────────
// WordPuzzleLocalEngine  (mirrors WordPuzzleLocalEngine.swift - singleton)
// Deterministic word selection: same level → same word
// ─────────────────────────────────────────────────────────────────────────────

object WordPuzzleLocalEngine {

    private const val APP_SALT = 7523L
    private const val PREFS_KEY_LEVEL     = "wordPuzzle_currentLevel"
    private const val PREFS_KEY_COMPLETED = "wordPuzzle_completedLevels"
    private const val PREFS_NAME          = "wordpuzzle_prefs"

    // word lists grouped by length
    private var wordsByLength: Map<Int, List<String>> = emptyMap()
    // (length, start_level, end_level)
    private var groupRanges: List<Triple<Int, Int, Int>> = emptyList()
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        wordsByLength = buildWordGroups(context)
        groupRanges   = buildGroupRanges()
        initialized   = true
    }

    // Deterministic word for level — never changes
    fun wordForLevel(level: Int): String {
        val (list, offset) = wordListAndOffset(level)
        if (list.isEmpty()) return "ERROR"
        val shuffled = seededShuffle(list, APP_SALT)
        return shuffled[offset % shuffled.size]
    }

    fun wordLengthForLevel(level: Int): Int =
        groupInfo(level)?.first ?: 5

    fun difficultyForLevel(level: Int): String =
        groupInfo(level)?.second ?: "medium"

    // 2-pass Wordle evaluation (mirrors Swift evaluateGuess)
    fun evaluateGuess(guess: String, answer: String): List<WPLetterResult> {
        val g = guess.uppercase().map { it.toString() }
        val a = answer.uppercase().map { it.toString() }
        val n = a.size

        val results    = MutableList(n) { WPLetterStatus.ABSENT }
        val answerUsed = MutableList(n) { false }

        // Pass 1: correct (green)
        for (i in 0 until n) {
            if (g[i] == a[i]) { results[i] = WPLetterStatus.CORRECT; answerUsed[i] = true }
        }
        // Pass 2: present (yellow)
        for (i in 0 until n) {
            if (results[i] == WPLetterStatus.CORRECT) continue
            for (j in 0 until n) {
                if (!answerUsed[j] && g[i] == a[j]) {
                    results[i] = WPLetterStatus.PRESENT; answerUsed[j] = true; break
                }
            }
        }
        return g.zip(results).map { (l, s) -> WPLetterResult(l, s) }
    }

    // Hint: random unrevealed, non-green position
    fun nextHint(word: String, revealed: Set<Int>, greenPositions: Set<Int>): Pair<String, Int>? {
        val letters = word.uppercase().map { it.toString() }
        val candidates = letters.indices.filter { it !in revealed && it !in greenPositions }
        val pos = candidates.randomOrNull() ?: return null
        return Pair(letters[pos], pos)
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    fun persistedCurrentLevel(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(PREFS_KEY_LEVEL, 1).coerceAtLeast(1)

    fun setPersistedLevel(context: Context, level: Int) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(PREFS_KEY_LEVEL, level).apply()

    fun markLevelCompleted(context: Context, level: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(PREFS_KEY_LEVEL, 1)
        if (level >= current) prefs.edit().putInt(PREFS_KEY_LEVEL, level + 1).apply()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun groupInfo(level: Int): Pair<Int, String>? {
        for ((length, start, end) in groupRanges) {
            if (level in start..end) return Pair(length, diffLabel(length))
        }
        val last = groupRanges.lastOrNull() ?: return null
        return Pair(last.first, diffLabel(last.first))
    }

    private fun diffLabel(length: Int) = when (length) {
        3    -> "Easy"
        4    -> "Medium"
        5    -> "Hard"
        6    -> "Expert"
        else -> "Master"
    }

    private fun wordListAndOffset(level: Int): Pair<List<String>, Int> {
        for ((length, start, end) in groupRanges) {
            if (level in start..end) {
                return Pair(wordsByLength[length] ?: emptyList(), level - start)
            }
        }
        val last = groupRanges.lastOrNull() ?: return Pair(emptyList(), 0)
        val list  = wordsByLength[last.first] ?: emptyList()
        val offset = (level - last.second) % list.size.coerceAtLeast(1)
        return Pair(list, offset)
    }

    private fun buildGroupRanges(): List<Triple<Int, Int, Int>> {
        val ranges = mutableListOf<Triple<Int, Int, Int>>()
        var cur = 1
        for (len in listOf(3, 4, 5, 6, 7)) {
            val words = wordsByLength[len] ?: continue
            if (words.isEmpty()) continue
            ranges.add(Triple(len, cur, cur + words.size - 1))
            cur += words.size
        }
        return ranges
    }

    private fun buildWordGroups(context: Context): Map<Int, List<String>> {
        val content = context.assets.open("1000-most-common-words.txt")
            .bufferedReader().readText()
        val groups = mutableMapOf<Int, MutableList<String>>()
        content.lines()
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() && it.all { c -> c.isLetter() } }
            .forEach { word ->
                if (word.length in 3..7)
                    groups.getOrPut(word.length) { mutableListOf() }.add(word)
            }
        return groups
    }

    // Fisher-Yates with LCG — mirrors Swift seededShuffle
    private fun seededShuffle(list: List<String>, seed: Long): List<String> {
        val result = list.toMutableList()
        var rng = seed
        for (i in result.size - 1 downTo 1) {
            rng = rng * 6364136223846793005L + 1442695040888963407L
            val j = (rng.and(Long.MAX_VALUE) % (i + 1)).toInt()
            val tmp = result[i]; result[i] = result[j]; result[j] = tmp
        }
        return result
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WPGameState  (mirrors WPGameState.swift ObservableObject → Compose state)
// ─────────────────────────────────────────────────────────────────────────────

class WPGameState(
    private val context: Context,
    private val scope: CoroutineScope,
    initialLevel: Int = 1
) {
    // ── Observed state (all drive recomposition) ──────────────────────────────
    var levelNumber     by mutableIntStateOf(initialLevel)
    var wordLength      by mutableIntStateOf(3)
    var difficulty      by mutableStateOf("Easy")
    var maxGuesses      by mutableIntStateOf(5)

    var guesses         by mutableStateOf<List<WPGuessRow>>(emptyList())
    var attemptsUsed    by mutableIntStateOf(0)
    var solved          by mutableStateOf(false)
    var failed          by mutableStateOf(false)
    var answer          by mutableStateOf<String?>(null)

    var currentInput    by mutableStateOf<List<String?>>(emptyList())
    var selectedCell    by mutableStateOf<Int?>(null)

    var keyboardStatus  by mutableStateOf<Map<String, WPLetterStatus>>(emptyMap())

    var shakeRow        by mutableStateOf(false)
    var revealingRow    by mutableStateOf<Int?>(null)
    var showResult      by mutableStateOf(false)
    var elapsedSeconds  by mutableIntStateOf(0)
    var hintsUsed       by mutableIntStateOf(0)
    var hintReveals     by mutableStateOf<Map<Int, String>>(emptyMap())
    var alertMessage    by mutableStateOf<String?>(null)

    private var word: String = ""
    private var timerJob: Job? = null

    val isInputComplete get() = currentInput.all { it != null }
    val canUseHint      get() = hintsUsed < wordLength && !solved && !failed
    val hintLabel       get() = "${wordLength - hintsUsed}"
    val maxHints        get() = wordLength

    // ── Load level ───────────────────────────────────────────────────────────
    fun loadLevel() {
        WordPuzzleLocalEngine.init(context)
        word       = WordPuzzleLocalEngine.wordForLevel(levelNumber)
        wordLength = WordPuzzleLocalEngine.wordLengthForLevel(levelNumber)
        difficulty = WordPuzzleLocalEngine.difficultyForLevel(levelNumber)
        resetLocalState()
        android.util.Log.d("WordPuzzle", "Level $levelNumber loaded: $wordLength letters ($difficulty) — word: $word")
    }

    // ── Submit guess ─────────────────────────────────────────────────────────
    fun submitGuess() {
        val guess = currentInput.filterNotNull().joinToString("")
        if (guess.length != wordLength) { triggerShake(); return }

        val result   = WordPuzzleLocalEngine.evaluateGuess(guess, word)
        val rowIndex = guesses.size
        revealingRow = rowIndex
        guesses      = guesses + WPGuessRow(result)
        attemptsUsed++
        selectedCell = null

        // Update keyboard
        val kbCopy = keyboardStatus.toMutableMap()
        for (lr in result) updateKb(lr.letter, lr.status, kbCopy)
        keyboardStatus = kbCopy

        val isSolved = result.all { it.status == WPLetterStatus.CORRECT }
        val isFailed = !isSolved && attemptsUsed >= maxGuesses

        scope.launch {
            delay(600)
            revealingRow = null
            currentInput = MutableList(wordLength) { null }

            if (isSolved || isFailed) {
                solved = isSolved; failed = isFailed
                stopTimer()
                if (isFailed) answer = word
                if (isSolved) WordPuzzleLocalEngine.markLevelCompleted(context, levelNumber)
                showResult = true
            }
        }
    }

    // ── Input ────────────────────────────────────────────────────────────────
    fun selectCell(index: Int) {
        if (index !in 0 until wordLength) return
        if (solved || failed) return
        if (hintReveals.containsKey(index)) return
        selectedCell = index
    }

    fun typeLetter(letter: String) {
        if (solved || failed) return
        val cell = selectedCell
        val input = currentInput.toMutableList()
        if (cell != null && cell < wordLength && !hintReveals.containsKey(cell)) {
            input[cell] = letter.uppercase()
            currentInput = input
            selectedCell = input.indexOfFirst { it == null }.takeIf { it >= 0 }
        } else {
            val first = input.indexOfFirst { it == null }
            if (first >= 0 && !hintReveals.containsKey(first)) {
                input[first] = letter.uppercase()
                currentInput = input
                selectedCell = input.indexOfFirst { it == null }.takeIf { it >= 0 }
            }
        }
    }

    fun deleteLetter() {
        if (solved || failed) return
        val cell = selectedCell ?: return
        if (hintReveals.containsKey(cell)) return
        val input = currentInput.toMutableList()
        if (input[cell] != null) { input[cell] = null; currentInput = input }
    }

    // ── Hint ─────────────────────────────────────────────────────────────────
    fun useHint() {
        if (!canUseHint) { alertMessage = "No more hints available."; return }
        val greenPositions = buildSet<Int> {
            guesses.forEach { row -> row.results.forEachIndexed { i, lr -> if (lr.status == WPLetterStatus.CORRECT) add(i) } }
        }
        val hint = WordPuzzleLocalEngine.nextHint(word, hintReveals.keys, greenPositions) ?: return
        hintsUsed++
        val newReveals = hintReveals.toMutableMap().also { it[hint.second] = hint.first }
        hintReveals = newReveals
        val input = currentInput.toMutableList(); input[hint.second] = hint.first; currentInput = input
        val kbCopy = keyboardStatus.toMutableMap()
        kbCopy[hint.first] = WPLetterStatus.CORRECT
        keyboardStatus = kbCopy
        if (selectedCell == hint.second) selectedCell = input.indexOfFirst { it == null }.takeIf { it >= 0 }
    }

    // ── Next / Reset ─────────────────────────────────────────────────────────
    fun nextLevel() { levelNumber++; loadLevel() }

    fun resetSession() { levelNumber = levelNumber; loadLevel() }

    fun startTimerIfNeeded() { if (timerJob == null || timerJob?.isActive == false) startTimer() }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun resetLocalState() {
        guesses       = emptyList()
        attemptsUsed  = 0
        solved        = false
        failed        = false
        answer        = null
        currentInput  = MutableList(wordLength) { null }
        selectedCell  = null
        keyboardStatus= emptyMap()
        showResult    = false
        elapsedSeconds= 0
        hintsUsed     = 0
        hintReveals   = emptyMap()
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        elapsedSeconds = 0
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (!solved && !failed) elapsedSeconds++
            }
        }
    }

    private fun stopTimer() { timerJob?.cancel(); timerJob = null }

    private fun triggerShake() {
        shakeRow = true
        scope.launch { delay(500); shakeRow = false }
    }

    private fun updateKb(letter: String, status: WPLetterStatus, kb: MutableMap<String, WPLetterStatus>) {
        val key = letter.uppercase()
        when (status) {
            WPLetterStatus.CORRECT  -> kb[key] = WPLetterStatus.CORRECT
            WPLetterStatus.PRESENT  -> if (kb[key] != WPLetterStatus.CORRECT) kb[key] = WPLetterStatus.PRESENT
            WPLetterStatus.ABSENT   -> if (!kb.containsKey(key)) kb[key] = WPLetterStatus.ABSENT
            else                    -> {}
        }
    }
}
