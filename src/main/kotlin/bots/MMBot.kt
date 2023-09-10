package bots

import common.Board
import common.Bot
import common.Coord
import kotlin.Float.Companion.NEGATIVE_INFINITY
import kotlin.math.max

private const val TILE_FACTOR = 1
private const val MACRO_FACTOR = 32

private const val CENTER_FACTOR = 4
private const val CORNER_FACTOR = 2
private const val EDGE_FACTOR = 1

class MMBot(private val depth: Int) : Bot {
	init {
		require(depth >= 1) { "depth must be >= 1, was $depth" }
	}

	override fun move(board: Board): Coord {
		if (board.isDone) throw Exception("No moves remain")

		return negaMax(
			board, 0.0F, depth,
			java.lang.Float.NEGATIVE_INFINITY, java.lang.Float.POSITIVE_INFINITY,
			if (board.nextPlayX) 1 else -1
		).move
	}

	private class ValuedMove(val move: Coord, val value: Float)

	private fun negaMax(board: Board, cValue: Float, depth: Int, a: Float, b: Float, player: Int): ValuedMove {
		if (depth == 0 || board.isDone) return ValuedMove(board.lastMove, player * cValue)

		var bestValue = NEGATIVE_INFINITY
		var bestMove: Coord? = null
		var newA = a

		var stopSearch = false
		board.forAvailableMoves { move ->
			if (!stopSearch){
				val child = board.copy()

				//Calculate the new score
				var childValue = cValue + TILE_FACTOR * factor(move.toInt() and 0b1111) * factor((move.toInt() shr 4) and 0b1111) * player
				if (child.play(move)) {
					if (child.isDone) childValue = java.lang.Float.POSITIVE_INFINITY * player
					else childValue += MACRO_FACTOR * factor((move.toInt() shr 4) and 0b1111) * player
				}

				//Check if the (global) value of this child is better than the previous best child
				val value = -negaMax(child, childValue, depth - 1, -b, -newA, -player).value
				if (value > bestValue || bestMove == null) {
					bestValue = value
					bestMove = child.lastMove
				}
				newA = max(newA, value)
				if (newA >= b) stopSearch = true
			}
		}

		return ValuedMove(bestMove!!, bestValue)
	}

	private fun factor(os: Int) = when {
		os == 4 -> CENTER_FACTOR
		os % 2 == 0 -> CORNER_FACTOR
		else -> EDGE_FACTOR
	}

	override fun toString() = "MMBot(d=$depth)"
}