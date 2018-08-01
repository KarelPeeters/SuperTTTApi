package com.flaghacker.sttt.bots

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Timer
import java.util.*

class RandomBot : Bot {
	private val random: Random

	constructor() {
		random = Random()
	}

	constructor(seed: Long) {
		random = Random(seed)
	}

	override fun move(board: Board, timer: Timer): Byte? {
		val moves = board.availableMoves
		return moves[random.nextInt(moves.size)]
	}

	override fun toString() = "RandomBot"
}
