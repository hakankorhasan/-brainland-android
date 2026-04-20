package com.example.brain_land.data

// Game types matching iOS ContentView GameType enum
enum class GameType(val displayName: String, val gameId: String) {
    PIPE_CONNECT("Pipe Connect", "pipeConnect"),
    LASER_PUZZLE("Laser Puzzle", "laserPuzzle"),
    HIDDEN_PAIR("Hidden Pair", "hiddenPair"),
    BINARY_PUZZLE("Binary Puzzle", "binaryPuzzle"),
    NONOGRAM("Pixel Excavation", "nonogram"),
    SLITHERLINK("Slitherlink", "slitherlink"),
    BLOCK_FIT("Block Fit", "blockFit"),
    CRYPTO_CAGE("Crypto-Cage", "cryptoCage"),
    NEURAL_LINK("Neural Link", "neuralLink"),
    GALACTIC_BEACONS("Galactic Beacons", "galacticBeacons"),
    NUMBER_CIRCUIT("Number Circuit", "numberCircuit"),
    WORD_PUZZLE("Word Puzzle", "wordPuzzle"),
    PATH_CLEARING("Path Clearing", "pathClearing"),
    LIQUID_SORT("Liquid Sort", "liquidSort"),
    TILT_MAZE("Tilt Maze", "tiltMaze");

    // Card background colour (hex) — mirrors iOS cardColor
    val cardColorHex: String get() = when (this) {
        PIPE_CONNECT     -> "AFABE5"
        LASER_PUZZLE     -> "7E7DDC"
        HIDDEN_PAIR      -> "D495BB"
        BINARY_PUZZLE    -> "F2F0F7"
        NONOGRAM         -> "12141F"
        SLITHERLINK      -> "1D1B29"
        BLOCK_FIT        -> "AFABE5"
        CRYPTO_CAGE      -> "0D2137"
        NEURAL_LINK      -> "0A1628"
        GALACTIC_BEACONS -> "0D0B2E"
        NUMBER_CIRCUIT   -> "0A1A2E"
        WORD_PUZZLE      -> "0D3B2E"
        PATH_CLEARING    -> "0A1628"
        LIQUID_SORT      -> "1A0A3E"
        TILT_MAZE        -> "0D1F15"
    }

    // Light card = dark text on card
    val isLightCard: Boolean get() = when (this) {
        PIPE_CONNECT, HIDDEN_PAIR, BINARY_PUZZLE, BLOCK_FIT -> true
        else -> false
    }

    // Emoji icon fallback (each game gets a unique emoji)
    val emoji: String get() = when (this) {
        PIPE_CONNECT     -> "🔧"
        LASER_PUZZLE     -> "🔦"
        HIDDEN_PAIR      -> "🃏"
        BINARY_PUZZLE    -> "01"
        NONOGRAM         -> "🖼️"
        SLITHERLINK      -> "🐍"
        BLOCK_FIT        -> "🧩"
        CRYPTO_CAGE      -> "🔐"
        NEURAL_LINK      -> "🧠"
        GALACTIC_BEACONS -> "🛸"
        NUMBER_CIRCUIT   -> "🔢"
        WORD_PUZZLE      -> "📝"
        PATH_CLEARING    -> "🌿"
        LIQUID_SORT      -> "💧"
        TILT_MAZE        -> "🌀"
    }

    companion object {
        fun from(gameTypeStr: String): GameType? = values().firstOrNull {
            it.gameId == gameTypeStr.trimStart('.')
        }
        fun allTypes(): List<GameType> = values().toList()
    }
}

// Leaderboard models
data class LeaderboardPlayer(
    val uid: String = "",
    val rank: Int = 0,
    val displayName: String = "",
    val score: Int = 0,
    val avatarUrl: String? = null,
    val tier: String = "bronze"
)

data class LeaderboardResponse(
    val players: List<LeaderboardPlayer> = emptyList(),
    val myScore: Int = 0,
    val myRank: Int = 0,
    val myTier: String = "bronze"
)
