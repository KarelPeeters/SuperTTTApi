package com.flaghacker.uttt.games;

import com.flaghacker.uttt.common.Board;
import com.flaghacker.uttt.common.Bot;
import com.flaghacker.uttt.common.Coord;
import com.flaghacker.uttt.common.Player;
import com.flaghacker.uttt.common.Util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.flaghacker.uttt.common.Player.NEUTRAL;
import static com.flaghacker.uttt.common.Player.PLAYER;

public class BotGame
{
	private List<Bot> bots;
	private Random random = Util.loggedRandom();

	private int count = 1;
	private int timePerMove = 500;
	private boolean logging;
	private boolean shuffling;

	public BotGame(Bot p1, Bot p2)
	{
		bots = Collections.unmodifiableList(Arrays.asList(p1, p2));
	}

	public void run()
	{
		int[] results = new int[3];

		for (int i = 0; i < count; i++)
		{
			if (count <= 100 || i % (count / 100) == 0)
				printm(String.format("starting game %d; %.4f", i, (double) i / count));

			boolean swapped = shuffling && random.nextBoolean();
			Bot p1 = bots.get(swapped ? 1 : 0);
			Bot p2 = bots.get(swapped ? 0 : 1);

			Board board = new Board();

			int nextRound = 0;
			while (! board.isDone())
			{
				prints("Round #" + nextRound++);

				Coord pMove = Util.moveBotWithTimeOut(p1, board.copy(), timePerMove);
				prints("p1 move: " + pMove);
				board.play(pMove);

				if (board.isDone())
					continue;

				Coord rMove = Util.moveBotWithTimeOut(p2, board.copy(), timePerMove);
				prints("p2 move: " + rMove);
				board.play(rMove);

				prints(board);
			}

			Player wonBy = board.wonBy();
			if (wonBy != NEUTRAL && swapped)
				wonBy = wonBy.other();

			prints("done, won: " + wonBy);
			results[wonBy == PLAYER ? 0 : (wonBy == NEUTRAL ? 1 : 2)]++;
		}

		printm("Results:");
		printm("Player 1 Win:\t" + (double) results[2] / count);
		printm("Tie:\t\t\t" + (double) results[1] / count);
		printm("Player 2 Win:\t" + (double) results[0] / count);
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

	public BotGame setDetailedLogging(boolean logging)
	{
		this.logging = logging;
		return this;
	}

	public BotGame setCount(int count)
	{
		this.count = count;
		this.logging = count == 1;
		return this;
	}

	public BotGame setTimePerMove(int time)
	{
		this.timePerMove = time;
		return this;
	}

	public BotGame setShuffling(boolean shuffling)
	{
		this.shuffling = shuffling;
		return this;
	}
}
