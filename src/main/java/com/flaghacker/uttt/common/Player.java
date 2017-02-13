package com.flaghacker.uttt.common;

public enum Player
{
	NEUTRAL,
	PLAYER,
	ENEMY,
	;

	public Player other()
	{
		if (this == PLAYER)
			return ENEMY;
		else if (this == ENEMY)
			return PLAYER;
		else
			throw new IllegalArgumentException("player should be one of [PLAYER, ENEMY]; was " + this);
	}
}
