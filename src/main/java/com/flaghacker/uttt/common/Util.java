package com.flaghacker.uttt.common;

import java.util.Random;

public class Util
{
	private static int nextId = 0;
	private static Random random = new Random();

	private static int[] seeds = {-394761231, -1744249032};

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
}
