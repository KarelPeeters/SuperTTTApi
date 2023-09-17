package bots

import common.Board
import common.Bot
import common.Coord
import kotlin.random.Random

class DilutedBot(
    private val bot: Bot,       //Bot to dilute
    private val ratio: Double   //Ratio of the moves played by the real bot / total moves
) : Bot {
    private val randomBot = RandomBot()

    override fun move(board: Board, percentDone: (Int) -> Unit): Coord {
        val isRealMove = Random.nextDouble() < ratio
        return if (isRealMove) bot.move(board, percentDone) else randomBot.move(board, percentDone)
    }

    override fun reset() {}
    override fun cancel() {}
    override fun toString(): String {
        return "$bot@$ratio"
    }
}
