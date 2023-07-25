package com.flaghacker.sttt.bots

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Coord
import com.flaghacker.sttt.common.Player
import java.lang.Float.NEGATIVE_INFINITY
import java.lang.Float.POSITIVE_INFINITY
import java.lang.Math.max

private const val TILE_FACTOR = 1
private const val MACRO_FACTOR = 32

private const val CENTER_FACTOR = 4
private const val CORNER_FACTOR = 2
private const val EDGE_FACTOR = 1

class MMBot(private val depth: Int) : Bot {
	init {
		require(depth >= 1) { "depth must be >= 1, was $depth" }
	}

	override fun move(board: Board): Coord? {
		if (board.isDone)
			return null

		return negaMax(
				board, 0.0F, depth,
				NEGATIVE_INFINITY, POSITIVE_INFINITY,
				playerSign(board.nextPlayer)
		).move
	}

	private class ValuedMove(val move: Coord, val value: Float)

	private fun negaMax(board: Board, cValue: Float, depth: Int, a: Float, b: Float, player: Int): ValuedMove {
		if (depth == 0 || board.isDone) return ValuedMove(board.lastMove!!, player * cValue)

		var bestValue = NEGATIVE_INFINITY
		var bestMove: Coord? = null
		var newA = a

		for (move in board.availableMoves) {
			val child = board.copy()

			//Calculate the new score
			var childValue = cValue + TILE_FACTOR*factor(move % 9) * factor(move / 9) * player
			if (child.play(move)) {
				if (child.isDone) childValue = POSITIVE_INFINITY * player
				else childValue += MACRO_FACTOR * factor(move / 9) * player
			}

			//Check if the (global) value of this child is better then the previous best child
			val value = -negaMax(child, childValue, depth - 1, -b, -newA, -player).value
			if (value > bestValue || bestMove == null) {
				bestValue = value
				bestMove = child.lastMove
			}
			newA = max(newA, value)
			if (newA >= b) break
		}

		return ValuedMove(bestMove!!, bestValue)
	}

	private fun playerSign(player: Player) = when (player) {
		Player.NEUTRAL -> 0
		Player.PLAYER -> 1
		Player.ENEMY -> -1
	}

	private fun factor(os: Int) = when {
		os == 4 -> CENTER_FACTOR
		os % 2 == 0 -> CORNER_FACTOR
		else -> EDGE_FACTOR
	}

	override fun toString() = "MMBotFloat32"
}
