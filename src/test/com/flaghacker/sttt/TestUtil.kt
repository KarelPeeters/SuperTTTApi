package com.flaghacker.sttt

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Player
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals

fun assertBoardMatches(exp: Expected, board: Board) {
	assertEquals(jsonToBoardPlayer(exp.nextPlayer), board.nextPlayer)
	assertEquals(jsonToBoardPlayer(exp.wonBy), board.wonBy)
	assertEquals(exp.done, board.isDone)
	assertEquals(exp.lastMove, board.lastMove)
	Assertions.assertArrayEquals(exp.availableMoves.apply { sort() }, board.availableMoves.apply { sort() })

	for (o in 0 until 81)
		assertEquals(jsonToBoardPlayer(exp.tiles[o]), board.tile(o.toByte()))
	for (om in 0 until 9)
		assertEquals(jsonToBoardPlayer(exp.macros[om]), board.macro(om.toByte()))
}

fun assertBoardEquals(expected: Board, actual: Board) {
	assertBoardMatches(expected.toExpected(), actual)
}

private const val PLAYER_JSON = 1
private const val ENEMY_JSON = -1
private const val NEUTRAL_JSON = 0

fun jsonToBoardPlayer(jsonPlayer: Int) = when (jsonPlayer) {
	PLAYER_JSON -> Player.PLAYER
	ENEMY_JSON -> Player.ENEMY
	NEUTRAL_JSON -> Player.NEUTRAL
	else -> throw IllegalArgumentException("$jsonPlayer is not a valid JSON player")
}

fun boardToJSONPlayer(boardPlayer: Player) = when (boardPlayer) {
	Player.PLAYER -> PLAYER_JSON
	Player.ENEMY -> ENEMY_JSON
	Player.NEUTRAL -> NEUTRAL_JSON
	else -> throw IllegalArgumentException("$boardPlayer is not a valid JSON player")
}

class State(val move: Byte? = null, val expected: Expected)

class Expected(
		val tiles: IntArray,
		val macros: IntArray,
		val nextPlayer: Int,
		val lastMove: Byte? = null,
		val wonBy: Int,
		val availableMoves: ByteArray,
		val done: Boolean
)

fun Board.toExpected() = Expected(
		nextPlayer = boardToJSONPlayer(nextPlayer),
		wonBy = boardToJSONPlayer(wonBy),
		availableMoves = availableMoves,
		lastMove = lastMove,
		done = isDone,
		macros = IntArray(9) { boardToJSONPlayer(macro(it.toByte())) },
		tiles = IntArray(81) { boardToJSONPlayer(tile(it.toByte())) }
)