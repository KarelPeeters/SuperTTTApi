package com.flaghacker.sttt

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Player
import com.flaghacker.sttt.common.Player.*
import com.flaghacker.sttt.common.toCoord
import com.flaghacker.sttt.common.toPair
import org.apache.commons.lang3.SerializationUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import java.util.*

fun playedBoard(moves: List<Byte>): Board {
	val board = Board()
	for (move in moves) board.play(move)
	return board
}

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

@RunWith(Theories::class)
class BoardTest {
	companion object {
		@DataPoints @JvmField val players = arrayOf(Player.PLAYER, Player.ENEMY)
	}

	@Test
	fun testOther() {
		assertEquals(PLAYER, ENEMY.other())
		assertEquals(ENEMY, PLAYER.other())
	}

	@Test(expected = IllegalArgumentException::class)
	fun testOtherNeutralFails() {
		NEUTRAL.other()
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
		assertEquals(board, SerializationUtils.clone(board))
	}

	@Test
	fun testDoubleFlip() {
		val board = randomBoard(Random(0), 10)
		assertEquals(board, board.flip().flip())
	}

	@Theory
	fun testOtherSymmetry(player: Player) {
		assertEquals("test player.other symmetry", player, player.other().other())
	}

	@Theory
	fun testManhattanWin(player: Player) {
		for (om in 0..8) {
			for (i in 0..2) {
				assertEquals("horizontal $i in macro $om", player,
						playedBoard(player, 0 + 3 * i + 9 * om, 1 + 3 * i + 9 * om, 2 + 3 * i + 9 * om).macro(om.toByte()))
				assertEquals("vertical $i in macro $om", player,
						playedBoard(player, 0 + i + 9 * om, 3 + i + 9 * om, 6 + i + 9 * om).macro(om.toByte()))
			}
		}
	}

	@Theory
	fun testDiagonalWin(player: Player) {
		for (om in 0..8) {
			assertEquals("diagonal / in macro $om", player,
					playedBoard(player, 0 + 9 * om, 4 + 9 * om, 8 + 9 * om).macro(om.toByte()))
			assertEquals("diagonal \\ in macro $om", player,
					playedBoard(player, 2 + 9 * om, 4 + 9 * om, 6 + 9 * om).macro(om.toByte()))
		}
	}

	@Theory
	fun testFullWin(player: Player) {
		assertEquals("macro top line", player, playedBoard(player, 0, 1, 2, 9, 10, 11, 18, 19, 20).wonBy)
	}
}

private fun playedBoard(player: Player, vararg moves: Int): Board {
	val board = Array(9, { Array(9, { Player.NEUTRAL }) })
	for (move in moves) board[move.toPair().first][move.toPair().second] = player
	return Board(board, player, moves.last().toByte())
}
