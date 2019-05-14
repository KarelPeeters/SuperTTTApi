package com.flaghacker.sttt

import com.flaghacker.sttt.bots.MMBot
import com.flaghacker.sttt.common.Board
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

open class MMBenchmarks {
	@Benchmark
	@BenchmarkMode(Mode.AverageTime)
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	fun averageGame(blackhole: Blackhole) {
		val mv = MMBot(12).move(Board())
		blackhole.consume(mv)
	}
}

