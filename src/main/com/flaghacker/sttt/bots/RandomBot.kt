package com.flaghacker.sttt.bots

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Coord
import com.flaghacker.sttt.common.Timer
import java.util.*

class RandomBot(seed: Long? = null) : Bot {
	private val random = if (seed != null) Random(seed) else Random()

	fun move(board: Board): Coord? {
		val moves = board.availableMoves
		if (moves.isEmpty()) return null
		return moves[random.nextInt(moves.size)]
	}

	override fun move(board: Board, timer: Timer): Coord? = move(board)

	override fun toString() = "RandomBot"
}
