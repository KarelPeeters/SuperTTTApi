package com.flaghacker.sttt

import com.flaghacker.sttt.common.Board
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.*

class BoardCopyTest {
	@ParameterizedTest
	@MethodSource("data")
	fun testCopySeparate(board: Board) {
		val copy = board.copy()
		assertBoardEquals(board, copy)

		playRandom(copy, Random(1), 15)
		assertNotEquals(board, copy)
	}

	@ParameterizedTest
	@MethodSource("data")
	fun testCopyEqualsHashcode(board: Board) {
		val copy = board.copy()

		assertTrue(copy == board && board == copy)
		assertEquals(board.hashCode().toLong(), copy.hashCode().toLong())
	}

	companion object {
		@JvmStatic
		@Suppress("unused")
		fun data() = (0..9).map { randomBoard(Random(0), it) }
	}
}
