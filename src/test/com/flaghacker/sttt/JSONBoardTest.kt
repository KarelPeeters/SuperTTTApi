package com.flaghacker.sttt

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.JSONBoard
import com.flaghacker.sttt.common.toJSON
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class JSONBoardTest {
	@Test
	fun testEmpty() {
		val board = Board()
		val result = JSONBoard.fromJSON(board.toJSON())

		assertEquals(board, result)
	}

	@Test
	fun testRandom() {
		val board = randomBoard(Random(), 10)
		val result = JSONBoard.fromJSON(board.toJSON())

		assertEquals(board, result)
	}
}