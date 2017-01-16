package com.flaghacker.uttt.common;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Util
{
	private static int nextId = 0;
	private static Random random = new Random();
	private static int[] seeds = {};

	private static final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

	public static Random loggedRandom()
	{
		int seed;
		if (nextId >= seeds.length)
		{
			seed = random.nextInt();
			System.err.println(String.format("random #%d seed: %d", nextId, seed));
		}
		else
		{
			seed = seeds[nextId];
			System.err.println(String.format("picked %d seed: %d", nextId, seed));
		}

		nextId++;
		return new Random(seed);
	}

	public static Coord moveBotWithTimeOut(Bot bot, Board board, int time)
	{
		final boolean[] runTimeUp = {true};
		exec.schedule(() ->
		{
			if (runTimeUp[0])
			{
				runTimeUp[0] = false;
				bot.timeUp();
			}
		}, time, TimeUnit.MILLISECONDS);
		Coord move = bot.move(board);
		runTimeUp[0] = false;
		return move;
	}
}
