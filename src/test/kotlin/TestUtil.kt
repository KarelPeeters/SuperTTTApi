package com.flaghacker.sttt

import common.Board
import common.Coord
import common.Player
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals

fun Int.idxToCoord(): Coord = (((this / 9) shl 4) or (this % 9)).toByte()
fun Byte.idxToCoord(): Coord = (((this / 9) shl 4) or (this % 9)).toByte()
fun Coord.coordToIdx(): Int {
	val idx = this.toInt() and 0xFF  // remove sign extension
	val om  = idx shr 4		 		  // top bits
	val os  = idx and 0b1111  		  // lower bits
	return om*9 + os
}

fun assertBoardMatches(exp: Expected, board: Board) {
	assertEquals(Player.fromChar(exp.nextPlayer), boolToBoardPlayer(board.nextPlayX))
	assertEquals(exp.compactString, board.toCompactString())
	assertEquals(exp.done, board.isDone)
	assertEquals(exp.lastMove, board.lastMove)
	Assertions.assertArrayEquals(exp.availableMoves.apply { sort() }, board.availableMoves.apply { sort() })

	for (o in 0 until 81)
		assertEquals(Player.fromChar(exp.tiles[o]), board.tile(o.idxToCoord()))
	for (om in 0 until 9)
		assertEquals(Player.fromChar(exp.macros[om]), board.macro(om.toByte()))
}

fun assertBoardEquals(expected: Board, actual: Board) {
	assertBoardMatches(expected.toExpected(), actual)
}

private const val PLAYER_JSON = 1
private const val ENEMY_JSON = -1
private const val NEUTRAL_JSON = 0

fun boolToBoardPlayer(nextPlayX: Boolean) = if (nextPlayX) Player.PLAYER else Player.ENEMY

fun boardToJSONPlayer(boardPlayer: Player) = when (boardPlayer) {
	Player.PLAYER -> PLAYER_JSON
	Player.ENEMY -> ENEMY_JSON
	Player.NEUTRAL -> NEUTRAL_JSON
	else -> throw IllegalArgumentException("$boardPlayer is not a valid JSON player")
}

class State(val move: Byte, val expected: Expected)

class Expected(
		val nextPlayer: Char,
		val availableMoves: Array<Byte>,
		val lastMove: Byte,
		val tiles: CharArray,
		val macros: CharArray,
		val done: Boolean,
		val compactString: String

)

fun Board.toExpected() = Expected(
		nextPlayer = boolToBoardPlayer(nextPlayX).char,
		availableMoves = availableMoves,
		lastMove = lastMove,
		done = isDone,
		macros = CharArray(9) { macro(it.toByte().idxToCoord()).char },
		tiles = CharArray(81) { tile(it.toByte().idxToCoord()).char },
		compactString = toCompactString()
)