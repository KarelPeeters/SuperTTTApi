package main

import bots.DilutedBot
import bots.MCTSBot
import bots.MMBot
import games.BotGame

fun main() {
	val bot = DilutedBot(MMBot(5),0.83)
	val randBot = MCTSBot(3000)

	BotGame(bot, randBot).setCount(10000).setShuffling(true).run()
}