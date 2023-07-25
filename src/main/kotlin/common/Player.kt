package com.flaghacker.sttt.common

enum class Player(val char: Char) {
	PLAYER('X'),
	ENEMY('O'),
	NEUTRAL(' ');

	fun otherWithNeutral(): Player = if (this == NEUTRAL) NEUTRAL else this.other()
	fun other(): Player = when (this) {
		PLAYER -> ENEMY
		ENEMY -> PLAYER
		else -> throw IllegalArgumentException("player should be one of [PLAYER, ENEMY]; was $this")
	}

	companion object {
		fun legalChar(char: Char) = values().any { it.char == char }
		fun fromChar(char: Char) = values().find { it.char == char }
				?: throw IllegalArgumentException("$char is not a valid Player")
	}
}
