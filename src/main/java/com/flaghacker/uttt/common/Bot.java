package com.flaghacker.uttt.common;

import java.io.Serializable;

public interface Bot extends Serializable
{
	Coord move(Board board);

	void timeUp();
}
