package com.flaghacker.sttt.common;

public class Timer
{
	private final long time;

	private long start = -1;
	private boolean interrupted;

	public Timer(long time)
	{
		this.time = time;
	}

	public void start()
	{
		this.start = System.currentTimeMillis();
	}

	public boolean started()
	{
		return start != -1;
	}

	public void interrupt()
	{
		this.interrupted = true;
	}

	public boolean isInterrupted()
	{
		return interrupted;
	}

	public long timeLeft()
	{
		if (!started())
			throw new IllegalStateException("this Timer has not been started yet");

		long left = time - (System.currentTimeMillis() - start);
		return (left > 0) && !interrupted ? left : 0;
	}

	public boolean running()
	{
		return timeLeft() > 0;
	}
}
