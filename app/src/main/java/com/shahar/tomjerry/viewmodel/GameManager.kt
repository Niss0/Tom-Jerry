package com.shahar.tomjerry.viewmodel

class GameManager( val rows: Int = 6,  val cols: Int = 3) {


    // activeObstacles set is removed as requested

    enum class CellContent {
        EMPTY, JERRY, OBSTACLE
    }
    // Game matrix - 0 for empty, 1 for Jerry, 2 for obstacles
    private val gameMatrix = Array(rows) { Array(cols) { CellContent.EMPTY } }

    // Jerry's position - initially at bottom center
    private var jerryRow = rows - 1
    private var jerryCol = 1  // Middle column
    private var lives = 3

    init {
        // Place Jerry in starting position
        gameMatrix[jerryRow][jerryCol] = CellContent.JERRY
    }

    fun moveJerryLeft(): Boolean {
        if (jerryCol <= 0) return false

        // Clear current position
        gameMatrix[jerryRow][jerryCol] = CellContent.EMPTY

        // Move left
        jerryCol--

        // Set new position
        gameMatrix[jerryRow][jerryCol] = CellContent.JERRY

        // Collision check will be done in GameActivity after this move
        return true
    }

    fun moveJerryRight(): Boolean {
        if (jerryCol >= cols - 1) return false

        // Clear current position
        gameMatrix[jerryRow][jerryCol] = CellContent.EMPTY

        // Move right
        jerryCol++

        // Set new position
        gameMatrix[jerryRow][jerryCol] = CellContent.JERRY

        // Collision check will be done in GameActivity after this move
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


    fun moveObstaclesDown() {
        for (row in rows - 2 downTo 0) {
            for (col in 0 until cols) {
                // If there is an obstacle in the current cell
                if (gameMatrix[row][col] == CellContent.OBSTACLE) {
                    gameMatrix[row+1][col] = CellContent.OBSTACLE
                    gameMatrix[row][col] = CellContent.EMPTY
                }
            }
        }

        for (col in 0 until cols) {
            val bottomRow = rows - 1
            if (gameMatrix[bottomRow][col] == CellContent.OBSTACLE) {
                if (col != jerryCol) {
                    gameMatrix[bottomRow][col] = CellContent.EMPTY
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

    fun handleCollision(): Pair<Int, Int> {
        // Collision happens if Jerry's cell contains an obstacle
            val collisionPosition = Pair(jerryRow, jerryCol)

            // Remove obstacle from game matrix at collision position
            gameMatrix[jerryRow][jerryCol] = CellContent.JERRY // Jerry remains after collision

            decreaseLives()

            return collisionPosition  // Return the position of collision
        }

    // Simplified checkCollision: checks if Jerry's cell contains an obstacle
    fun checkCollision(): Boolean {
        // Collision happens if Jerry's cell contains an obstacle
        return gameMatrix[jerryRow][jerryCol] == CellContent.OBSTACLE
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
}
