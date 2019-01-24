package com.flaghacker.sttt

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Player
import com.flaghacker.sttt.common.Player.*
import com.flaghacker.sttt.common.toCoord
import com.flaghacker.sttt.common.toPair
import org.apache.commons.lang3.SerializationUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

fun randomBoard(rand: Random, moveCount: Int): Board {
	return playRandom(Board(), rand, moveCount)
}

fun playRandom(startBoard: Board, rand: Random, moveCount: Int): Board {
	var i = 0
	while (i < moveCount && !startBoard.isDone) {
		val available = startBoard.availableMoves
		startBoard.play(available[rand.nextInt(available.size)])
		i++
	}
	return startBoard
}

private fun playedBoard(player: Player, playerMoves: IntArray, enemyMoves: IntArray, lastMove: Int): Board {
	val board = Array(9) { Array(9) { Player.NEUTRAL } }
	for (move in playerMoves) board[move.toPair().first][move.toPair().second] = player
	for (move in enemyMoves) board[move.toPair().first][move.toPair().second] = player.other()

	return Board(board, player, lastMove.toByte())
}

private fun playedBoard(player: Player, moves: IntArray, lastMove: Int) =
		playedBoard(player, moves, IntArray(0), lastMove)

private fun playedBoard(player: Player, vararg moves: Int) =
		playedBoard(player, moves, IntArray(0), moves.last())

class BoardTest {
	@Test
	fun testOther() {
		assertEquals(PLAYER, ENEMY.other())
		assertEquals(ENEMY, PLAYER.other())
		assertThrows<IllegalArgumentException> { NEUTRAL.other() }
	}

	@Test
	fun testPlayRemembered() {
		val move = toCoord(4, 4)
		val board = Board()
		val player = board.nextPlayer

		board.play(move)
		assertEquals(player, board.tile(move))
	}

	@Test
	fun testSerializedEquals() {
		val board = randomBoard(Random(0), 10)
		assertBoardEquals(board, SerializationUtils.clone(board))
	}

	@Test
	fun testDoubleFlip() {
		val board = randomBoard(Random(0), 10)
		assertBoardEquals(board, board.flip().flip())
	}

	@TestPlayersAndMacros
	fun testManhattanWin(player: Player, om: Int) {
		for (i in 0..2) {
			assertEquals(player, playedBoard(player, 0 + 3 * i + 9 * om, 1 + 3 * i + 9 * om, 2 + 3 * i + 9 * om).macro(om.toByte())) {
				"horizontal $i in macro $om"
			}
			assertEquals(player, playedBoard(player, 0 + i + 9 * om, 3 + i + 9 * om, 6 + i + 9 * om).macro(om.toByte())) {
				"vertical $i in macro $om"
			}
		}
	}

	@TestPlayersAndMacros
	fun testDiagonalWin(player: Player, om: Int) {
		assertEquals(player, playedBoard(player, 0 + 9 * om, 4 + 9 * om, 8 + 9 * om).macro(om.toByte())) {
			"diagonal / in macro $om"
		}
		assertEquals(player, playedBoard(player, 2 + 9 * om, 4 + 9 * om, 6 + 9 * om).macro(om.toByte())) {
			"diagonal \\ in macro $om"
		}
	}

	@TestPlayers
	fun testFullWin(player: Player) {
		assertEquals(player, playedBoard(player, 0, 1, 2, 9, 10, 11, 18, 19, 20).wonBy) {
			"macro top line"
		}
	}

	@TestPlayersAndMacros
	fun testFullMacro(player: Player, om: Int) {
		//fill the macro and direct the next move to that macrp
		val moves = ((9 * om) until (9 * om + 9)).toArray()
		val lastMove = (8 - om) * 9 + om
		val board = playedBoard(player, moves, lastMove)

		assertEquals(81 - 9, board.availableMoves.size) { "freeplay" }
	}

	@TestPlayersAndMacros
	fun testFullMixedMacro(player: Player, om: Int) {
		val pMoves = listOf(0, 1, 4, 5, 6)
		val eMoves = listOf(2, 3, 7, 8)
		val lastMove = (8 - om) * 9 + om

		val board = playedBoard(
				player,
				pMoves.map { om * 9 + it }.toIntArray(),
				eMoves.map { om * 9 + it }.toIntArray(),
				lastMove
		)

		assertEquals(81 - 9, board.availableMoves.size) { "freeplay" }
	}
}

private fun IntProgression.toArray() = toList().toIntArray()