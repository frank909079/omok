package com.frank.omok.game

const val BOARD_SIZE = 15

data class Point(val row: Int, val col: Int)

class Board private constructor(
    private val cells: Array<Array<Stone>>,
    val moves: List<Point>
) {
    constructor() : this(
        Array(BOARD_SIZE) { Array(BOARD_SIZE) { Stone.EMPTY } },
        emptyList()
    )

    fun stoneAt(row: Int, col: Int): Stone = cells[row][col]

    fun isEmpty(row: Int, col: Int): Boolean = cells[row][col] == Stone.EMPTY

    fun place(row: Int, col: Int, stone: Stone): Board {
        val newCells = Array(BOARD_SIZE) { r -> cells[r].copyOf() }
        newCells[row][col] = stone
        return Board(newCells, moves + Point(row, col))
    }
}
