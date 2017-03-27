package com.flaghacker.uttt.common;

public enum Player
{
	NEUTRAL(" "),
	PLAYER("X"),
	ENEMY("O"),
	;

	private String niceString;

	Player(String niceString)
	{
		this.niceString = niceString;
	}

	public Player other()
	{
		if (this == PLAYER)
			return ENEMY;
		else if (this == ENEMY)
			return PLAYER;
		else
			throw new IllegalArgumentException("player should be one of [PLAYER, ENEMY]; was " + this);
	}

	public String toNiceString()
	{
		return niceString;
	}

	public static Player fromNiceString(String string)
	{
		for (Player player : values())
			if (player.toNiceString().equals(string))
				return player;

		throw new IllegalArgumentException(string + " is not a valid Player");
	}
}
