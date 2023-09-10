package com.flaghacker.sttt

import bots.MCTSBot
import bots.MMBot
import bots.RandomBot
import games.BotGame
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.infra.Blackhole
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.random.RandomGenerator

open class GameBenchmarks {
	@Benchmark
	@BenchmarkMode(Mode.AverageTime)
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	fun averageGame(blackhole: Blackhole) {
		val game = BotGame(MCTSBot(50_000), RandomBot())
		game.setLogLevel(BotGame.LogLevel.NONE).setRandomSeed(20).setShuffling(false).run()
	}

	@Benchmark
	@BenchmarkMode(Mode.AverageTime)
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	fun averageMM() {
		val game = BotGame(MMBot(6), RandomBot(Random(401797280)))
		game.setLogLevel(BotGame.LogLevel.NONE).setRandomSeed(20).setShuffling(false).run()
	}
}

