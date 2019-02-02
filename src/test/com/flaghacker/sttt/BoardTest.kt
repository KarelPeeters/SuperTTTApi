package com.flaghacker.sttt

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Player
import com.flaghacker.sttt.common.Player.*
import com.flaghacker.sttt.common.toCoord
import com.flaghacker.sttt.common.toPair
import org.apache.commons.lang3.SerializationUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.math.abs

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

/**
 * These moves represent the following grid, won by neither player:
 *   XXO
 *   OXX
 *   XOO
 */
private val P_MOVES = listOf(0, 1, 4, 5, 6)
private val E_MOVES = listOf(2, 3, 7, 8)

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
		val lastMove = (8 - om) * 9 + om

		val board = playedBoard(
				player,
				P_MOVES.map { om * 9 + it }.toIntArray(),
				E_MOVES.map { om * 9 + it }.toIntArray(),
				lastMove
		)

		assertEquals(81 - 9, board.availableMoves.size) { "freeplay" }
	}

	@TestPlayers
	fun testNeutralWin(player: Player) {
		val board = playedBoard(
				player,
				(0 until 9).flatMap { om -> P_MOVES.map { om * 9 + it } }.toIntArray(),
				(0 until 9).flatMap { om -> E_MOVES.map { om * 9 + it } }.toIntArray(),
				0
		)

		assertEquals(NEUTRAL, board.wonBy)
		assertTrue(board.isDone)
	}

	@Test//Players
	//player: Player
	fun testWinFullBoard() {
		val player = PLAYER
		//fill macros 0-5 without winning anything
		val pFill = (0 until 6).flatMap { om -> P_MOVES.map { om * 9 + it } }
		val eFill = (0 until 6).flatMap { om -> E_MOVES.map { om * 9 + it } }

		//almost fill y = 6
		val pWin = (0 until 8).map { x -> toCoord(x, 6).toInt() }
		//point to bottom right corner
		val lastMove = 8

		val board = playedBoard(player, (pFill + pWin).toIntArray(), eFill.toIntArray(), lastMove)

		//win the board
		board.play(toCoord(8, 6))
		assertEquals(player, board.wonBy)
	}

	@Test
	fun testRandomAvailableMoveDistribution() {
		val testCount = 100_000

		val random = Random(42)
		val board = Board()

		repeat(40) {
			val counts = IntArray(81)
			repeat(testCount) {
				val move = board.randomAvailableMove(random).toInt()
				counts[move]++
			}

			val moves = board.availableMoves

			assertTrue(counts.withIndex().all { (i, c) -> c == 0 || i.toByte() in moves }) { "should only return available moves" }
			assertTrue(moves.all { i -> counts[i.toInt()] > 0 }) { "should return every available move" }

			val maxDev = counts
					.filter { it != 0 }
					.map { abs(it.toDouble() / testCount - 1.0 / moves.size) }
					.max() ?: return
			assertTrue(maxDev < 0.05) { "should be uniformly distributed" }

			board.play(board.randomAvailableMove(random))
		}
	}

	@Test
	fun availableMovesVariants() {
		val random = Random(12)
		val board = Board()

		while (!board.isDone) {
			val t1 = board.availableMoves
			val t2 = board.availableMoves { it }.toByteArray()

			assertArrayEquals(t1, t2)
			board.play(board.randomAvailableMove(random))
		}
	}
}

private fun IntProgression.toArray() = toList().toIntArray()