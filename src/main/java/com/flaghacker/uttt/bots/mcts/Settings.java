package com.flaghacker.uttt.bots.mcts;

import java.io.Serializable;

public class Settings implements Serializable
{
	private static final long serialVersionUID = 2364109102744587333L;

	private final boolean log;

	private final double branchWeight;
	private final boolean tryLose;

	public Settings(boolean log, double branchWeight, boolean tryLose)
	{
		this.log = log;
		this.branchWeight = branchWeight;
		this.tryLose = tryLose;
	}

	public boolean log()
	{
		return log;
	}

	public double branchWeight()
	{
		return branchWeight;
	}

	public boolean tryLose()
	{
		return tryLose;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static Settings standard()
	{
		return builder().build();
	}

	public static class Builder
	{
		private boolean log = false;
		private double branchWeight = Math.sqrt(2);
		private boolean tryLose = false;

		public Builder log(boolean log)
		{
			this.log = log;
			return this;
		}

		public Builder branchWeight(double branchWeight)
		{
			this.branchWeight = branchWeight;
			return this;
		}

		public Builder tryLose(boolean tryLose)
		{
			this.tryLose = tryLose;
			return this;
		}

		public Settings build()
		{
			return new Settings(log, branchWeight, tryLose);
		}
	}
}
