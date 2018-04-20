package com.flaghacker.sttt.common

enum class Player(val niceString: String) {
	PLAYER("X"),
	ENEMY("O"),
	NEUTRAL(" ");

	fun other(): Player = when(this) {
		PLAYER -> ENEMY
		ENEMY -> PLAYER
		else -> throw IllegalArgumentException("player should be one of [PLAYER, ENEMY]; was " + this)
	}

	fun otherWithNeutral(): Player = if (this == NEUTRAL) NEUTRAL else this.other()

	companion object {
		fun fromNiceString(string: String): Player {
			Player.values().filter { it.niceString == string }.forEach { return it }
			throw IllegalArgumentException("$string is not a valid Player")
		}
	}
}
