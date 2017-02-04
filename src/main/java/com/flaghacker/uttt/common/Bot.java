package com.flaghacker.uttt.common;

public interface Bot
{
	Coord move(Board board);

	void timeUp();
}
