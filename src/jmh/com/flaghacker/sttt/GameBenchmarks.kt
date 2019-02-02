package com.flaghacker.sttt

import com.flaghacker.sttt.bots.MCTSBot
import com.flaghacker.sttt.bots.RandomBot
import com.flaghacker.sttt.games.BotGame
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.infra.Blackhole
import java.util.*
import java.util.concurrent.TimeUnit

open class GameBenchmarks {
	@Benchmark
	@BenchmarkMode(Mode.AverageTime)
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	fun averageGame(blackhole: Blackhole) {
		val game = BotGame(MCTSBot(Random(51476), 50_000), RandomBot(Random(401797280)))
		game.setLogLevel(BotGame.LogLevel.NONE).setRandomSeed(20).setShuffling(false).run()
	}
}

