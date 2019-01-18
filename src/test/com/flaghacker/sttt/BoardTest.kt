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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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

private fun playedBoard(player: Player, vararg moves: Int): Board {
	val board = Array(9) { Array(9) { Player.NEUTRAL } }
	for (move in moves) board[move.toPair().first][move.toPair().second] = player
	return Board(board, player, moves.last().toByte())
}

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

	@ParameterizedTest
	@EnumSource(Player::class, names = ["PLAYER", "ENEMY"])
	fun testManhattanWin(player: Player) {
		for (om in 0..8) {
			for (i in 0..2) {
				assertEquals(player, playedBoard(player, 0 + 3 * i + 9 * om, 1 + 3 * i + 9 * om, 2 + 3 * i + 9 * om).macro(om.toByte())) {
					"horizontal $i in macro $om"
				}
				assertEquals(player, playedBoard(player, 0 + i + 9 * om, 3 + i + 9 * om, 6 + i + 9 * om).macro(om.toByte())) {
					"vertical $i in macro $om"
				}
			}
		}
	}

	@ParameterizedTest
	@EnumSource(Player::class, names = ["PLAYER", "ENEMY"])
	fun testDiagonalWin(player: Player) {
		for (om in 0..8) {
			assertEquals(player, playedBoard(player, 0 + 9 * om, 4 + 9 * om, 8 + 9 * om).macro(om.toByte())) {
				"diagonal / in macro $om"
			}
			assertEquals(player, playedBoard(player, 2 + 9 * om, 4 + 9 * om, 6 + 9 * om).macro(om.toByte())) {
				"diagonal \\ in macro $om"
			}
		}
	}

	@ParameterizedTest
	@EnumSource(Player::class, names = ["PLAYER", "ENEMY"])
	fun testFullWin(player: Player) {
		assertEquals(player, playedBoard(player, 0, 1, 2, 9, 10, 11, 18, 19, 20).wonBy) {
			"macro top line"
		}
	}
}

