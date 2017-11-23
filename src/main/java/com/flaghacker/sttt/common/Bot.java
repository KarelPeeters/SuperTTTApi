package com.flaghacker.sttt.common;

import java.io.Serializable;

public interface Bot extends Serializable
{
	Coord move(Board board, Timer timer);
}
