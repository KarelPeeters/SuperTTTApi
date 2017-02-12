package com.flaghacker.uttt.common;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class Util
{
	private static int nextId = 0;
	private static Random random = new Random();
	private static int[] seeds = {};

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

	private static ScheduledExecutorService timeOutService;

	public static Coord moveBotWithTimeOut(final Bot bot, Board board, long time)
	{
		checkAndInitExecutor();

		final boolean[] runTimeUp = {true};
		timeOutService.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				if (runTimeUp[0])
				{
					runTimeUp[0] = false;
					bot.timeUp();
				}
			}
		}, time, TimeUnit.MILLISECONDS);
		Coord move = bot.move(board);
		runTimeUp[0] = false;
		return move;
	}

	public static Future<Coord> moveBotWithTimeOutAsync(ExecutorService executor,
														final Bot bot, final Board board, final long time)
	{
		checkAndInitExecutor();

		return executor.submit(new Callable<Coord>()
		{
			@Override
			public Coord call() throws Exception
			{
				return moveBotWithTimeOut(bot, board, time);
			}
		});
	}

	private static void checkAndInitExecutor()
	{
		if (timeOutService != null)
			return;

		timeOutService = new ScheduledThreadPoolExecutor(1, new ThreadFactory()
		{
			@Override
			public Thread newThread(Runnable runnable)
			{
				Thread thread = new Thread(runnable);
				thread.setDaemon(true);
				thread.setName("Util executor thread");
				return thread;
			}
		});
	}
}
