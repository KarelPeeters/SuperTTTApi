package com.flaghacker.sttt.common

fun fromNiceString(string: String): KotlinPlayer {
    KotlinPlayer.values().filter { it.toNiceString() == string }.forEach { return it }
    throw IllegalArgumentException(string + " is not a valid KotlinPlayer")
}

enum class KotlinPlayer(val niceString: String, val value: Int) {
    NEUTRAL(" ",0),
    PLAYER("X",1),
    ENEMY("O",2);

    fun other(): KotlinPlayer = when {
        this == PLAYER -> ENEMY
        this == ENEMY -> PLAYER
        else -> throw IllegalArgumentException("player should be one of [PLAYER, ENEMY]; was " + this)
    }

    fun otherWithNeutral(): KotlinPlayer = if (this == NEUTRAL) NEUTRAL else this.other()
    fun toNiceString(): String = niceString
}
