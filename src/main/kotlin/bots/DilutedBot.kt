package com.flaghacker.sttt.bots

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Bot
import com.flaghacker.sttt.common.Coord
import kotlin.random.Random

class DilutedBot(
		private val bot: Bot,       //Bot to dilute
		private val ratio: Double   //Ratio of the moves played by the real bot / total moves
) : Bot {
	private val randomBot = RandomBot()

	override fun move(board: Board): Coord? {
		val isRealMove = Random.nextDouble()<ratio
		return if (isRealMove) bot.move(board) else randomBot.move(board)
	}

	override fun toString(): String {
		return "$bot@$ratio"
	}
}
