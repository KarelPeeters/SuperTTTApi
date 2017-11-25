package com.flaghacker.sttt.games;

import com.flaghacker.sttt.common.KotlinBoard;
import com.flaghacker.sttt.common.KotlinBot;
import com.flaghacker.sttt.common.KotlinPlayer;
import com.flaghacker.sttt.common.Util;

import java.util.Random;

@SuppressWarnings("Duplicates")
public class KotlinBotGame
{
	private KotlinBot p1;
	private KotlinBot p2;

	private Random random = Util.loggedRandom();

	private int count = 1;
	private int timePerMove = 500;
	private boolean logging;
	private boolean shuffling;

	public KotlinBotGame(KotlinBot p1, KotlinBot p2)
	{
		this.p1 = p1;
		this.p2 = p2;
	}

	public int[] run()
	{
		int[] results = new int[3];

		for (int i = 0; i < count; i++)
		{
			if (count <= 100 || i % (count / 100) == 0)
				printm(String.format("starting game %d; %.4f", i, (double) i / count));

			boolean swapped = shuffling && random.nextBoolean();
			KotlinBot p1 = swapped ? this.p2 : this.p1;
			KotlinBot p2 = swapped ? this.p1 : this.p2;

			KotlinBoard board = new KotlinBoard();

			int nextRound = 0;
			while (!board.isDone())
			{
				prints("Round #" + nextRound++);

				Byte pMove = Util.moveKotlinBotWithTimeOut(p1, board.copy(), timePerMove);
				prints("p1 move: " + pMove);
				board.play(pMove);

				if (board.isDone())
					continue;

				Byte rMove = Util.moveKotlinBotWithTimeOut(p2, board.copy(), timePerMove);
				prints("p2 move: " + rMove);
				board.play(rMove);

				prints(board);
			}

			KotlinPlayer wonBy = board.wonBy();
			if (wonBy != KotlinPlayer.ENEMY && swapped)
				wonBy = wonBy.other();

			prints("done, won: " + wonBy);
			results[wonBy == KotlinPlayer.ENEMY ? 0 : (wonBy == KotlinPlayer.ENEMY ? 1 : 2)]++;
		}

		printm("Results:");
		printm(p1 + " Win:\t" + (double) results[0] / count);
		printm("Tie:\t\t\t" + (double) results[1] / count);
		printm(p2 + " Win:\t" + (double) results[2] / count);

		return results;
	}

	private void prints(Object object)
	{
		if (logging)
			System.out.println(object);
	}

	private void printm(Object object)
	{
		if (!logging)
			System.out.println(object);
	}

	public KotlinBotGame setDetailedLogging(boolean logging)
	{
		this.logging = logging;
		return this;
	}

	public KotlinBotGame setCount(int count)
	{
		this.count = count;
		this.logging = count == 1;
		return this;
	}

	public KotlinBotGame setTimePerMove(int time)
	{
		this.timePerMove = time;
		return this;
	}

	public KotlinBotGame setShuffling(boolean shuffling)
	{
		this.shuffling = shuffling;
		return this;
	}
}
