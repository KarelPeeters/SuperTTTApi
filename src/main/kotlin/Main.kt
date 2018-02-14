import com.flaghacker.sttt.bots.RandomBot
import com.flaghacker.sttt.bots.mcts.MCTSBot
import com.flaghacker.sttt.bots.mcts.Settings
import com.flaghacker.sttt.games.BotGame

fun main(args: Array<String>) {
    BotGame(MCTSBot(Settings.standard()), RandomBot())
            .setCount(1)
            .setShuffling(true)
            .setTimePerMove(56)
            .setLogLevel(BotGame.LogLevel.NONE)
            .run()
}
