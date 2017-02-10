package com.flaghacker.uttt.common;

public abstract class AbstractBot implements Bot
{
	private static final long serialVersionUID = -306352116984750635L;

	private boolean running;

	protected void startRunning()
	{
		if (running)
			throw new IllegalStateException();

		running = true;
	}

	protected boolean running()
	{
		return running;
	}

	@Override
	public void timeUp()
	{
		running = false;
	}
}
