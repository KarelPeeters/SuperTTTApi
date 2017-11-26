package com.flaghacker.sttt.common;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Util
{
	private static int nextId = 0;
	private static Random random = new Random();
	private static int[] seeds = {401797280,0};

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

	public static Coord moveBotWithTimeOut(Bot bot, Board board, long time)
	{
		Timer timer = new Timer(time);
		timer.start();
		return bot.move(board, timer);
	}

	public static Byte moveKotlinBotWithTimeOut(KotlinBot bot, KotlinBoard board, long time)
	{
		Timer timer = new Timer(time);
		timer.start();
		return bot.move(board, timer);
	}

	public static Future<Coord> moveBotWithTimeOutAsync(
			ExecutorService executor, final Bot bot, final Board board, long time)
	{
		final Timer timer = new Timer(time);
		timer.start();

		final Future<Coord> future = executor.submit(new Callable<Coord>()
		{
			@Override
			public Coord call() throws Exception
			{
				return bot.move(board, timer);
			}
		});

		return new Future<Coord>()
		{
			boolean cancelled;

			@Override
			public boolean cancel(boolean mayInterruptIfRunning)
			{
				if (mayInterruptIfRunning)
				{
					future.cancel(true);
					timer.interrupt();
					cancelled = true;
				}

				return cancelled;
			}

			@Override
			public boolean isCancelled()
			{
				return cancelled;
			}

			@Override
			public boolean isDone()
			{
				return future.isDone();
			}

			@Override
			public Coord get() throws InterruptedException, ExecutionException
			{
				return future.get();
			}

			@Override
			public Coord get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
					TimeoutException
			{
				return future.get(timeout, unit);
			}
		};
	}
}
