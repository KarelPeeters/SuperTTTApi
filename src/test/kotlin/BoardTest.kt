package com.flaghacker.sttt

import common.Board
import common.Player
import common.Player.*
import common.toCoord
import org.apache.commons.lang3.SerializationUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.math.abs

// Truncate to 8 bit and sign extend
private fun IntArray.fix() = map { it.fix() }.toIntArray()
private fun List<Int>.fix() = toIntArray().fix()
private fun Int.fix() = toByte().toInt()

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
		when (coord.idxToCoord().toInt()) {
			in playerMoves -> player.char
			in enemyMoves -> player.other().char
			else -> ' '
		}
	}

	val lastMoveConverted = lastMove.toByte().coordToIdx()
	val lastMoveChar = chars[lastMoveConverted]
	require(lastMoveChar != ' ') {
		"lastMove must be an actual move"
	}
	chars[lastMoveConverted] = lastMoveChar.lowercaseChar()

	return Board(String(chars))
}

/**
 * These moves represent the following grid, won by neither player:
 *   XXO
 *   OXX
 *   XOO
 */
private val TIE_MOVES_A = listOf(1, 3, 4, 6, 8)
private val TIE_MOVES_B = listOf(0, 2, 5, 7)

private val COMPACT_STRING_PAIRS = listOf(
		"                                                                                 " to intArrayOf(),
		"                                                                          x      " to intArrayOf(130),
		"                                  X                                   o          " to intArrayOf(55, 119),
		"    x             O                            X                                 " to intArrayOf(82, 32, 4),
		"                         X   OX                                       o          " to intArrayOf(51, 50, 39, 119),
		"        OXO                                                     X               x" to intArrayOf(113, 17, 16, 8, 136),
		"               O     X           o             O           X    X                " to intArrayOf(113, 22, 101, 82, 35, 54),
		"Ox  O O  X                          X                 X                          " to intArrayOf(16, 4, 64, 6, 96, 0, 1),
		" X           O         X     o           X   O       O                     X     " to intArrayOf(37, 80, 1, 20, 69, 88, 131, 50),
		"       X   O             xX          X                             O   XO O      " to intArrayOf(120, 130, 40, 128, 7, 116, 65, 18, 39),
		"     X  X                         O               O  XO           X  X     o   O " to intArrayOf(5, 85, 88, 135, 115, 55, 118, 96, 8, 131),
		"      O   O      X                 O                  X  X   X       O  xX      O" to intArrayOf(103, 118, 96, 6, 99, 56, 129, 17, 24, 136, 128),
		"                X         X             o                 X  O  OO   XXO      OX " to intArrayOf(118, 103, 119, 120, 135, 113, 23, 114, 40, 134, 100, 68),
		"      X                         X  O            X  O         XO   O O  XOx     X " to intArrayOf(120, 128, 6, 104, 135, 115, 53, 86, 103, 117, 83, 56, 129),
		"XO O    oX X           OO    X               X      X        X O    O            " to intArrayOf(18, 38, 103, 112, 0, 1, 16, 3, 50, 37, 87, 117, 80, 8)
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
		board.play(move)
		assertEquals(PLAYER, board.tile(move))
	}

	@Test
	fun testSerializedEquals() {
		val board = randomBoard(Random(0), 10)
		assertBoardEquals(board, SerializationUtils.clone(board))
	}

	@TestPlayersAndMacros
	fun testManhattanWin(player: Player, om: Int) {
		val otherOm = (om + 1) % 8
		for (i in 0..2) {
			assertEquals(player, playedBoard(player,
				intArrayOf(0 + 3 * i + (om shl 4), 1 + 3 * i + (om shl 4), 2 + 3 * i + (om shl 4)).fix(),
				intArrayOf(0 + (otherOm shl 4), 1 + (otherOm shl 4), 3 + (otherOm shl 4)).fix(),
				(if (player == PLAYER) (otherOm shl 4) else (3 * i + (om shl 4))).fix()
			).macro(om)) { "diagonal / in macro $om" }
			assertEquals(player, playedBoard(player,
				intArrayOf(0 + i + (om shl 4), 3 + i + (om shl 4), 6 + i + (om shl 4)).fix(),
				intArrayOf(0 + (otherOm shl 4), 1 + (otherOm shl 4), 3 + (otherOm shl 4)).fix(),
				(if (player == PLAYER) (otherOm shl 4) else (i + (om shl 4))).fix()
			).macro(om)) { "diagonal \\ in macro $om" }
		}
	}

	@TestPlayersAndMacros
	fun testDiagonalWin(player: Player, om: Int) {
		val otherOm = (om + 1) % 8
		assertEquals(player, playedBoard(player,
				intArrayOf(0 + (om shl 4), 4 + (om shl 4), 8 + (om shl 4)).fix(),
				intArrayOf(0 + (otherOm shl 4), 1 + (otherOm shl 4), 3 + (otherOm shl 4)).fix(),
			(if (player == PLAYER) (otherOm shl 4) else (om shl 4)).fix()
		).macro(om)) { "diagonal / in macro $om" }
		assertEquals(player, playedBoard(player,
			intArrayOf(2 + (om shl 4), 4 + (om shl 4), 6 + (om shl 4)).fix(),
			intArrayOf(0 + (otherOm shl 4), 1 + (otherOm shl 4), 3 + (otherOm shl 4)).fix(),
			(if (player == PLAYER) (otherOm shl 4) else (2 + (om shl 4))).fix()
		).macro(om)) { "diagonal \\ in macro $om" }
	}

	@Test
	fun testFullWinX() {
		val board = playedBoard(
			PLAYER,
			intArrayOf(6, 7, 8, 6 + 16, 7 + 16, 8 + 16, 6 + 32, 7 + 32),
			intArrayOf(0, 1, 2, 0 + 16, 1 + 16, 2 + 16, 0 + 32, 1 + 32),
			2
		)
		board.play((8 + 32).toByte())
		assertEquals(PLAYER, board.wonBy) {"macro top line"}
	}

	@Test
	fun testFullWinO() {
		val board = playedBoard(
			ENEMY,
			intArrayOf(6, 7, 8, 6 + 16, 7 + 16, 8 + 16, 6 + 32, 7 + 32),
			intArrayOf(0, 1, 2, 0 + 16, 1 + 16, 2 + 16, 0 + 32, 1 + 32, 3 /* extra move to balance */),
			2
		)
		board.play((8 + 32).toByte())
		assertEquals(ENEMY, board.wonBy) {"macro top line"}
	}

	@TestPlayersAndMacros
	fun testFullMacro(player: Player, om: Int) {
		//fill the macro and direct the next move to that macro
		val otherOm = (om + 1) % 8
		val movesFullMacro = (((om shl 4)) until ((om shl 4) + 9)).toArray().fix()
		val movesOther = (0..8).map {
			if(it != om) ((it shl 4) + om) else ((otherOm shl 4) + otherOm)
		}.toIntArray().fix()

		val board = playedBoard(
			player, movesFullMacro, movesOther,
			(if (player == PLAYER) ((otherOm shl 4) + om) else ((om shl 4) + om)).fix()
		)

		assertEquals(81 - 9 - 9, board.availableMoves.size) {
			"freeplay"
		}
	}

	@TestPlayersAndMacros
	fun testFullMixedMacro(player: Player, om: Int) {
		val otherOm = (om + 1) % 8
		val lastMove = ((otherOm shl 4) + om).fix()
		val movesA = TIE_MOVES_A.map { ((om shl 4) + it)}.fix()
		val movesB = (TIE_MOVES_B.map { ((om shl 4) + it) } + lastMove).fix()
		val board = playedBoard(
			player, if (player == PLAYER) movesA else movesB, if (player == PLAYER) movesB else movesA, lastMove
		)

		assertEquals(81 - 10, board.availableMoves.size) { "freeplay" }
	}

	@Test
	fun testNeutralWin() {
		val board = playedBoard(
				PLAYER,
				(0 until 9).flatMap { om -> (if (om%2 == 0) TIE_MOVES_A else TIE_MOVES_B)
					.map { (om shl 4) + it } }.fix(),
				(0 until 9).flatMap { om -> (if (om%2 == 0) TIE_MOVES_B else TIE_MOVES_A)
					.map { (om shl 4) + it } }.fix(),
				1
		)

		//assertEquals(NEUTRAL, board.wonBy) // disabled because random winner is chosen as tiebreaker
		assertTrue(board.isDone)
		for (i in 0..<9) assertTrue(board.macro(0) == NEUTRAL)
	}

	@Test
	fun testWinFullBoard() {
		val player = PLAYER
		//fill macros 0..5 without winning anything
		val pFill = (0 until 6).flatMap { om -> TIE_MOVES_B.map { (om shl 4) + it } }
		val eFill = (0 until 6).flatMap { om -> TIE_MOVES_A.map { (om shl 4) + it } }

		//almost fill row y = 6
		val eNotWin = (0 until 2).map { x -> toCoord(x, 7).toInt() }
		val pWin = (0 until 8).map { x -> toCoord(x, 6).toInt() }

		val lastMove = 8 // point to bottom right corner (part of TIE_MOVES_A mask)
		val board = playedBoard(player, (pFill + pWin).toIntArray(), (eFill + eNotWin).toIntArray(), lastMove)

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
				val moves = board.availableMoves
				val move = moves[random.nextInt(moves.size)].coordToIdx()
				counts[move]++
			}

			val moves = board.availableMoves

			assertTrue(counts.withIndex().all { (i, c) -> c == 0 || i.idxToCoord() in moves }) { "should only return available moves" }
			assertTrue(moves.all { i -> counts[i.coordToIdx()] > 0 }) { "should return every available move" }

			val maxDev = counts
					.filter { it != 0 }
					.map { abs(it.toDouble() / testCount - 1.0 / moves.size) }
					.max()
			assertTrue(maxDev < 0.05) { "should be uniformly distributed" }

			board.play(moves[random.nextInt(moves.size)])
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