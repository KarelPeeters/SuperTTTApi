package com.flaghacker.uttt.games;

import com.flaghacker.uttt.common.Board;
import com.flaghacker.uttt.common.Bot;
import com.flaghacker.uttt.common.Coord;
import com.flaghacker.uttt.common.Util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.flaghacker.uttt.common.Board.ENEMY;
import static com.flaghacker.uttt.common.Board.PLAYER;

public class BotGame
{
	private List<Bot> bots;
	private Random random = Util.loggedRandom();

	private int count = 1;
	private int timePerMove = - 1;

	public BotGame(Bot p1, Bot p2)
	{
		bots = Collections.unmodifiableList(Arrays.asList(p1, p2));
	}

	public void run()
	{
		double[] results = new double[3];

		for (int i = 0; i < count; i++)
		{
			if (count <= 100 || i % (count / 100) == 0)
				printm(String.format("starting game %d; %.4f", i, (double) i / count));

			boolean swapped = random.nextBoolean();
			Bot p1 = bots.get(swapped ? 1 : 0);
			Bot p2 = bots.get(swapped ? 0 : 1);

			Board board = new Board();

			int nextRound = 0;
			while (! board.isDone())
			{
				prints("Round #" + nextRound++);

				Coord pMove = Util.moveBotWithTimeOut(p1, board.copy(), timePerMove);
				prints("p1 move: " + pMove);
				board.play(pMove, PLAYER);

				if (board.isDone())
					continue;

				Coord rMove = Util.moveBotWithTimeOut(p2, board.copy(), timePerMove);
				prints("p2 move: " + rMove);
				board.play(rMove, ENEMY);

				prints(board);
			}

			prints("done, won: " + (swapped ? - 1 : 1) * board.wonBy());
			results[(swapped ? - 1 : 1) * board.wonBy() + 1]++;
		}

		printm("Results:");
		printm("Player 1 Win:\t" + results[2] / count);
		printm("Tie:\t\t\t" + results[1] / count);
		printm("Player 2 Win:\t" + results[0] / count);
	}

	private void prints(Object object)
	{
		if (count == 1)
			System.out.println(object);
	}

	private void printm(Object object)
	{
		if (count > 1)
			System.out.println(object);
	}

	public void setCount(int count)
	{
		this.count = count;
	}

	public void setTimePerMove(int time)
	{
		this.timePerMove = time;
	}
}
