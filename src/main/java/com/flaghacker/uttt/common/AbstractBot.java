package com.flaghacker.uttt.common;

public abstract class AbstractBot implements Bot
{
	private boolean running;

	protected void startRunning()
	{
		if (running)
			throw new IllegalStateException();

		running = true;
	}

	public boolean running()
	{
		return running;
	}

	@Override
	public void timeUp()
	{
		if (running)
			throw new IllegalStateException();

		running = false;
	}
}
