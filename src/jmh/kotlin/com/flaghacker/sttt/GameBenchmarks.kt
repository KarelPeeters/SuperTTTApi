package com.flaghacker.sttt

import com.flaghacker.sttt.bots.MMBot
import com.flaghacker.sttt.bots.RandomBot
import com.flaghacker.sttt.games.BotGame
import com.flaghacker.sttt.games.OldBotGame
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import java.util.concurrent.TimeUnit


fun main(args:Array<String>) {

    Runner(OptionsBuilder()
            .include(".*" + GameBenchmarks::class.simpleName + ".*")
            .warmupIterations(5)
            .measurementIterations(5)
            .forks(1)
            .build()).run()
}

open class GameBenchmarks{
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    fun averageKotlinGame(){
        val game = BotGame(MMBot(5), RandomBot(401797280))
        game.setLogLevel(BotGame.LogLevel.NONE).setRandomSeed(20).setShuffling(false).run()
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    fun averageJavaGame(){
        val game = OldBotGame(MMBot(5), RandomBot(401797280))
        game.setCount(1).setDetailedLogging(false).setShuffling(false).run()
    }
}

