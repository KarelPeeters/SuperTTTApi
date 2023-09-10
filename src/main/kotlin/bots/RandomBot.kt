package bots

import common.Board
import common.Bot
import common.Coord
import java.util.*

class RandomBot(private val random: Random = Random()) : Bot {
	override fun move(board: Board): Coord {
		val moves = board.availableMoves
		if (moves.isEmpty()) throw Exception("No moves remain")
		return moves[random.nextInt(moves.size)]
	}

	override fun toString() = "RandomBot"
}
