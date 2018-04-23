package com.flaghacker.sttt

import com.flaghacker.sttt.bots.MMBot
import com.flaghacker.sttt.bots.RandomBot
import com.flaghacker.sttt.games.BotGame
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import java.util.concurrent.TimeUnit

open class GameBenchmarks {
	@Benchmark
	@BenchmarkMode(Mode.AverageTime)
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	fun averageGame() {
		val game = BotGame(MMBot(6), RandomBot(401797280))
		game.setLogLevel(BotGame.LogLevel.NONE).setRandomSeed(20).setShuffling(false).run()
	}
}

