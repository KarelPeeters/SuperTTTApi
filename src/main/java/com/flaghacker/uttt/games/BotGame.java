package com.flaghacker.uttt.games;

import com.flaghacker.uttt.common.Board;
import com.flaghacker.uttt.common.Bot;
import com.flaghacker.uttt.common.Coord;
import com.flaghacker.uttt.common.Util;

import static com.flaghacker.uttt.common.Board.ENEMY;
import static com.flaghacker.uttt.common.Board.PLAYER;

public class BotGame
{
	private Bot p1;
	private Bot p2;

	private int count = 1;
	private int timePerMove = - 1;

	public BotGame(Bot p1, Bot p2)
	{
		this.p1 = p1;
		this.p2 = p2;
	}

	public void run()
	{
		double[] results = new double[3];

		for (int i = 0; i < count; i++)
		{
			if (count <= 100 || i % (count / 100) == 0)
				printm(String.format("starting game %d; %.4f", i, (double) i/count));

			Board board = new Board();

			int nextRound = 0;
			while (! board.isDone())
			{
				prints("Round #" + nextRound++);

				Coord pMove = Util.moveBotWithTimeOut(p1,board.copy(),timePerMove);
				prints("p1 move: " + pMove);
				board.play(pMove, PLAYER);

				if (board.isDone())
					continue;

				Coord rMove = Util.moveBotWithTimeOut(p2,board.copy(),timePerMove);
				prints("p2 move: " + rMove);
				board.play(rMove, ENEMY);

				prints(board);
			}

			prints("done, won: " + board.wonBy());
			results[board.wonBy() + 1]++;
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
