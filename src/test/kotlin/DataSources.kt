package com.flaghacker.sttt

import common.Player
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

@Target(AnnotationTarget.FUNCTION)
@ParameterizedTest
@EnumSource(Player::class, names = ["PLAYER", "ENEMY"])
annotation class TestPlayers


@Target(AnnotationTarget.FUNCTION)
@ParameterizedTest
@MethodSource("com.flaghacker.sttt.DataSourcesKt#playersAndMacros")
annotation class TestPlayersAndMacros

@Suppress("unused")
private fun playersAndMacros() = sequence {
	for (player in arrayOf(Player.PLAYER, Player.ENEMY))
		for (om in 0 until 9)
			yield(arrayOf(player, om))
}.toList()