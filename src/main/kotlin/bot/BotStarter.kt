package bot

import com.flaghacker.sttt.bots.MMBot
import com.flaghacker.sttt.games.RiddlesIOGame

fun main(args : Array<String>) {
     RiddlesIOGame(MMBot(2)).run()
}
