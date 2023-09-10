package common

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

	fun bool(): Boolean {
		if (this == NEUTRAL) throw IllegalArgumentException("Function not allowed for neutral")
		return this == PLAYER
	}

	companion object {
		fun legalChar(char: Char) = entries.any { it.char == char }
		fun fromChar(char: Char) = entries.find { it.char == char }
				?: throw IllegalArgumentException("$char is not a valid Player")
	}
}
