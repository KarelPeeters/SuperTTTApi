package com.flaghacker.sttt.common

import boardToJSON
import checkMatch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*

val random = Random(0)

@RunWith(Parameterized::class)
class MultiBoardTest(private val board: Board) {
	@Test
	fun testCopySeparate() {
		val origJSON = boardToJSON(board)
		val copy = board.copy()

		BoardTest.playRandom(copy, random, 15)
		assertNotEquals(board, copy)
		checkMatch(board, origJSON)
	}

	@Test
	fun testCopyEqualsHashcode() {
		val copy = board.copy()

		assertTrue(copy == board && board == copy)
		assertEquals(board.hashCode().toLong(), copy.hashCode().toLong())
	}

	companion object {
		@JvmStatic
		@Parameterized.Parameters
		fun data() = (0..9).map { BoardTest.randomBoard(random,it) }
	}
}
