package com.flaghacker.sttt.bots

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Coord
import java.util.*

class RandomBot(private val random: Random = Random()) : Bot {
	override fun move(board: Board): Coord? {
		val moves = board.availableMoves
		if (moves.isEmpty())
			return null
		return moves[random.nextInt(moves.size)]
	}

	override fun toString() = "RandomBot"
}
