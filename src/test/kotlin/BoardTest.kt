package com.flaghacker.sttt

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Player
import com.flaghacker.sttt.common.Player.*
import com.flaghacker.sttt.common.toCoord
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

private fun playedBoard(vararg moves: Int): Board {
	val board = Board()
	for (move in moves)
		board.play(move.toByte())
	return board
}

private fun playedBoard(player: Player, playerMoves: IntArray, enemyMoves: IntArray, lastMove: Int): Board {
	val chars = CharArray(81) { coord ->
		when (coord) {
			in playerMoves -> player.char
			in enemyMoves -> player.other().char
			else -> ' '
		}
	}

	val lastMoveChar = chars[lastMove]
	require(lastMoveChar != ' ') { "lastMove must be an actual move" }
	chars[lastMove] = lastMoveChar.lowercaseChar()

	return Board(String(chars))
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
private val TIE_P_MOVES = listOf(0, 1, 4, 5, 6)
private val TIE_E_MOVES = listOf(2, 3, 7, 8)

private val COMPACT_STRING_PAIRS = listOf(
		"                                                                                 " to intArrayOf(),
		"                                                                          x      " to intArrayOf(74),
		"                                  X                                   o          " to intArrayOf(34, 70),
		"    x             O                            X                                 " to intArrayOf(47, 18, 4),
		"                         X   OX                                       o          " to intArrayOf(30, 29, 25, 70),
		"        OXO                                                     X               x" to intArrayOf(64, 10, 9, 8, 80),
		"               O     X           o             O           X    X                " to intArrayOf(64, 15, 59, 47, 21, 33),
		"Ox  O O  X                          X                 X                          " to intArrayOf(9, 4, 36, 6, 54, 0, 1),
		" X           O         X     o           X   O       O                     X     " to intArrayOf(23, 45, 1, 13, 41, 53, 75, 29),
		"       X   O             xX          X                             O   XO O      " to intArrayOf(71, 74, 26, 72, 7, 67, 37, 11, 25),
		"     X  X                         O               O  XO           X  X     o   O " to intArrayOf(5, 50, 53, 79, 66, 34, 69, 54, 8, 75),
		"      O   O      X                 O                  X  X   X       O  xX      O" to intArrayOf(61, 69, 54, 6, 57, 35, 73, 10, 17, 80, 72),
		"                X         X             o                 X  O  OO   XXO      OX " to intArrayOf(69, 61, 70, 71, 79, 64, 16, 65, 26, 78, 58, 40),
		"      X                         X  O            X  O         XO   O O  XOx     X " to intArrayOf(71, 72, 6, 62, 79, 66, 32, 51, 61, 68, 48, 35, 73),
		"XO O    oX X           OO    X               X      X        X O    O            " to intArrayOf(11, 24, 61, 63, 0, 1, 9, 3, 29, 23, 52, 68, 45, 8)
)

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
		val board = playedBoard(player, 6, 7, 8, 15, 16, 17, 24, 25)
		board.play(65)
		board.play(26)
		assertEquals(player, board.wonBy) {
			"macro top line"
		}
	}

	@TestPlayersAndMacros
	fun testFullMacro(player: Player, om: Int) {
		//fill the macro and direct the next move to that macrp
		val moves = ((9 * om) until (9 * om + 9)).toArray()
		val lastMove = om * 9 + om
		val board = playedBoard(player, moves, lastMove)

		assertEquals(81 - 9, board.availableMoves.size) { "freeplay" }
	}

	@TestPlayersAndMacros
	fun testFullMixedMacro(player: Player, om: Int) {
		val lastMove = om * 9 + om
		val board = playedBoard(
				player,
				TIE_P_MOVES.map { om * 9 + it }.toIntArray(),
				TIE_E_MOVES.map { om * 9 + it }.toIntArray(),
				lastMove
		)

		assertEquals(81 - 9, board.availableMoves.size) { "freeplay" }
	}

	@TestPlayers
	fun testNeutralWin(player: Player) {
		val board = playedBoard(
				player,
				(0 until 9).flatMap { om -> TIE_P_MOVES.map { om * 9 + it } }.toIntArray(),
				(0 until 9).flatMap { om -> TIE_E_MOVES.map { om * 9 + it } }.toIntArray(),
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
		val pFill = (0 until 6).flatMap { om -> TIE_P_MOVES.map { om * 9 + it } }
		val eFill = (0 until 6).flatMap { om -> TIE_E_MOVES.map { om * 9 + it } }

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
					.max()
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

	@Test
	fun toCompactString() {
		for ((str, moves) in COMPACT_STRING_PAIRS) {
			val moveBoard = playedBoard(*moves)

			assertEquals(str, moveBoard.toCompactString())
		}
	}

	@Test
	fun fromCompactString() {
		for ((str, moves) in COMPACT_STRING_PAIRS) {
			val moveBoard = playedBoard(*moves)
			val strBoard = Board(str)

			assertEquals(moveBoard, strBoard)
		}
	}
}

private fun IntProgression.toArray() = toList().toIntArray()