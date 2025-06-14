package com.shahar.tomjerry.viewmodel

import android.util.Log

class GameManager( val rows: Int = 9,  val cols: Int = 5) {


    // activeObstacles set is removed as requested

    enum class
    CellContent {
        EMPTY, OBSTACLE, COIN
    }

    enum class InteractionResult {
        NONE, COIN_COLLECTED, OBSTACLE_COLLISION
    }

    private val gameMatrix = Array(rows) { Array(cols) { CellContent.EMPTY } }
    var isBonusOnCooldown = false
        private set
    private var jerryRow = rows - 1  // Jerry's position - initially at bottom center
    private var jerryCol = 2  // Middle column
    private var lives = 3
    private var coinsCollected: Int = 0
    private var distanceTraveled: Long = 0L

    fun moveJerryLeft(): Boolean {
        if (jerryCol <= 0) return false
        jerryCol--
        return true
    }

    fun moveJerryRight(): Boolean {
        if (jerryCol >= cols - 1) return false
        jerryCol++
        return true
    }

    fun getJerryColumn(): Int {
        return jerryCol
    }

    // Added obstacle at a random top cell if empty
    fun addObstacle() {
        val randomCol = (0 until cols).random()
        // Only add obstacle if the top cell is empty
        if (gameMatrix[0][randomCol] == CellContent.EMPTY) {
            gameMatrix[0][randomCol] = CellContent.OBSTACLE
        }
    }


    // In GameManager.kt
    fun handleInteractionsAtJerryLocation(): InteractionResult {
        val cellContent = gameMatrix[jerryRow][jerryCol]
        distanceTraveled++

        return when (cellContent) {
            CellContent.COIN -> {
                coinsCollected++
                gameMatrix[jerryRow][jerryCol] = CellContent.EMPTY // Remove coin
                InteractionResult.COIN_COLLECTED
            }
            CellContent.OBSTACLE -> {
                val livesBefore = lives // Log lives before for clarity
                decreaseLives()
                gameMatrix[jerryRow][jerryCol] = CellContent.EMPTY // Obstacle disappears
                InteractionResult.OBSTACLE_COLLISION
            }
            CellContent.EMPTY -> {
                InteractionResult.NONE
            }
        }
    }

    internal fun clearOffScreenItemsFromBottomRow() {
        val bottomRowIndex = rows - 1
        for (col in 0 until cols) {
            val cellContent = gameMatrix[bottomRowIndex][col]
            if (cellContent == CellContent.OBSTACLE || cellContent == CellContent.COIN) {
                val isJerryAtThisCell = (jerryRow == bottomRowIndex && jerryCol == col)
                if (!isJerryAtThisCell) {
                    gameMatrix[bottomRowIndex][col] = CellContent.EMPTY
                }

            }
        }
    }

    fun moveObstaclesAndCoinsDown() {
        // Move existing obstacles and coins down, row by row from bottom-up
        for (row in rows - 2 downTo 0) { // Iterate from second to last data row upwards
            for (col in 0 until cols) { // Using col to avoid confusion with class property 'cols'
                val currentCellContent = gameMatrix[row][col]
                val targetCellContent = gameMatrix[row + 1][col]

                when (currentCellContent) {
                    CellContent.OBSTACLE -> {
                        val isTargetJerry = (row + 1 == jerryRow && col == jerryCol)
                        if (targetCellContent == CellContent.EMPTY || targetCellContent == CellContent.COIN || isTargetJerry) {
                            gameMatrix[row + 1][col] = CellContent.OBSTACLE
                            gameMatrix[row][col] = CellContent.EMPTY
                        }
                    }
                    CellContent.COIN -> {
                        if (targetCellContent == CellContent.EMPTY) {
                            gameMatrix[row + 1][col] = CellContent.COIN
                            gameMatrix[row][col] = CellContent.EMPTY
                        }
                    }
                    CellContent.EMPTY  -> {
                        // Nothing to move from these cells in this pass
                    }
                }
            }
        }
    }


    private fun decreaseLives(): Int {
        lives--
        return lives
    }

    fun isGameOver(): Boolean {
        return lives <= 0
    }

    fun resetLives() {
        lives = 3
    }

    fun getDistanceTraveled(): Long {
        return distanceTraveled
    }
    fun getAllCoinPositions(): Set<Pair<Int, Int>> {
        val coinPositions = mutableSetOf<Pair<Int, Int>>()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (gameMatrix[row][col] == CellContent.COIN) {
                    coinPositions.add(Pair(row, col))
                }
            }
        }
        return coinPositions
    }

    fun resetGameMetrics() {
        lives = 3 // Assuming you want to reset lives here too
        coinsCollected = 0
        distanceTraveled = 0L
        // Potentially clear the gameMatrix too if not done elsewhere for a new game
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                gameMatrix[r][c] = CellContent.EMPTY
            }
        }
        // Re-place Jerry if needed (though his position is usually reset by GameActivity or GameManager init)
        jerryRow = rows - 1
        jerryCol = cols / 2
        Log.d("GameManager", "Game metrics reset. Lives: $lives, Coins: $coinsCollected, Distance: $distanceTraveled")
    }

    // Function to get all obstacle positions for UI update
    fun getAllObstaclePositions(): Set<Pair<Int, Int>> {
        val obstaclePositions = mutableSetOf<Pair<Int, Int>>()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (gameMatrix[row][col] == CellContent.OBSTACLE) {
                    obstaclePositions.add(Pair(row, col))
                }
            }
        }
        return obstaclePositions
    }

    // Function to get Jerry's position for UI update
    fun getJerryPosition(): Pair<Int, Int> {
        return Pair(jerryRow, jerryCol)
    }

    fun getRemainingHearts(): Int {
        return lives
    }


    // Coins Logic

    fun getCoinsCollectedCount(): Int {
        return coinsCollected
    }

    fun addCoin() {
        val randomCol = (0 until cols).random() // Get a random column
        // Only add a coin if the top cell in that column is currently EMPTY
        // to avoid spawning on top of a new obstacle or another coin.
        if (gameMatrix[0][randomCol] == CellContent.EMPTY) {
            gameMatrix[0][randomCol] = CellContent.COIN
        }
        Log.d("GameManager_Coins", "ADD_COIN: Coin placed at (0, $randomCol)")
    }

    fun canActivateBonus(): Boolean {
        return !isBonusOnCooldown
    }

    fun setBonusCooldown(isOnCooldown: Boolean) {
        this.isBonusOnCooldown = isOnCooldown
    }
}
