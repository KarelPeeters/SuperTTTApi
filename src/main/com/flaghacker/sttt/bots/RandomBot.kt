package com.flaghacker.sttt.bots

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Coord
import com.flaghacker.sttt.common.Timer
import java.util.*

class RandomBot(seed: Long? = null) : Bot {
	private val random = if (seed != null) Random(seed) else Random()

	override fun move(board: Board, timer: Timer): Coord? = board.randomAvailableMove(random)

	override fun toString() = "RandomBot"
}
