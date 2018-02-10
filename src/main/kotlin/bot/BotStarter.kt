package bot

import com.flaghacker.sttt.bots.MMBot

fun main(args : Array<String>) {
     val parser = BotParser(MMBot(2))
     parser.run()
}
