package com.flaghacker.uttt.common;

public abstract class TimedBot implements Bot
{
	private static final long serialVersionUID = -306352116984750635L;

	private boolean running;

	protected abstract Coord calcMove(Board board);

	@Override
	public Coord move(Board board)
	{
		if (running)
			throw new IllegalStateException();
		running = true;

		Coord move = calcMove(board);
		running = false;

		return move;
	}

	@Override
	public void timeUp()
	{
		running = false;
	}

	protected boolean running()
	{
		return running;
	}
}
