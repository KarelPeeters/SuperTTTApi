package com.flaghacker.uttt.common;

import java.util.Random;

public class Util
{
	private static int nextId = 0;
	private static Random random = new Random();

	public static Random loggedRandom()
	{
		int seed = random.nextInt();
		System.err.println(String.format("random #%d seed: %d", nextId++, seed));
		return new Random(seed);
	}
}
