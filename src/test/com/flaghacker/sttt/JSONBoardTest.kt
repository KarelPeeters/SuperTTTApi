package com.flaghacker.sttt

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.JSONBoard
import com.flaghacker.sttt.common.toJSON
import org.junit.jupiter.api.Test
import java.util.*

class JSONBoardTest {
	@Test
	fun testEmpty() {
		val board = Board()
		val result = JSONBoard.fromJSON(board.toJSON())

		assertBoardEquals(board, result)
	}

	@Test
	fun testRandom() {
		val board = randomBoard(Random(0), 10)
		val result = JSONBoard.fromJSON(board.toJSON())

		assertBoardEquals(board, result)
	}
}