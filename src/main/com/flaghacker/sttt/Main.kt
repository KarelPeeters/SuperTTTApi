import com.flaghacker.sttt.bots.MMBot
import com.flaghacker.sttt.games.BotGame

fun main(args: Array<String>) {
    BotGame(MMBot(4), MMBot(4))
            .setCount(100)
            .setShuffling(true)
            .setTimePerMove(56)
            .setLogLevel(BotGame.LogLevel.BASIC)
            .run()
}
