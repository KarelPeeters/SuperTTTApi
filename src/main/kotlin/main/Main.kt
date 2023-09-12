package main

import bots.DilutedBot
import bots.MCTSBot
import bots.MMBot
import games.BotGame

fun main() {
	val bot1 = MCTSBot(1000*25)
	val bot2 = MCTSBot(6)

	BotGameTiming(bot1, bot2).setCount(1000).setShuffling(true).run()
}