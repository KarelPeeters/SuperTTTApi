package bot

import com.flaghacker.sttt.bots.MMBot
import com.flaghacker.sttt.games.RiddlesIOGame

fun main(args : Array<String>) {
     val parser = RiddlesIOGame(MMBot(2))
     parser.run()
}
