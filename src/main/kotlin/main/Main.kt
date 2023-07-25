package com.flaghacker.sttt.main

import com.flaghacker.sttt.bots.DilutedBot
import com.flaghacker.sttt.bots.MMBotFloat
import com.flaghacker.sttt.bots.RandomBot
import com.flaghacker.sttt.games.BotGame

fun main() {
/*	val bot = DilutedBot(MMBot(5),0.83)
	val randBot = RandomBot()

	BotGame(bot,randBot).setCount(1000).setShuffling(true).run()*/
	//BotGame(MMBot(5),MMBotFloat(5)).setCount(10000).setShuffling(true).run()

	//val bot = DilutedBot(MMBot(5),0.83) //76
	val bot = DilutedBot(MMBotFloat(5),0.83) //76
	//val bot = DilutedBot(MMBotFloat(5),0.83)
	val randBot = RandomBot()

	BotGame(bot,randBot).setCount(10000).setShuffling(true).run()
}