package com.flaghacker.uttt.games;

import com.flaghacker.uttt.bots.RandomBot;
import com.flaghacker.uttt.common.Board;
import com.flaghacker.uttt.common.Bot;
import com.flaghacker.uttt.common.Coord;

import static com.flaghacker.uttt.common.Board.ENEMY;
import static com.flaghacker.uttt.common.Board.PLAYER;

public class RandomEnemyGame
{
	private Bot player;
	private Bot random;

	private int count = 1;
	private int timePerMove = - 1;

	public RandomEnemyGame(Bot player)
	{
		this.player = player;
		random = new RandomBot();
	}

	public void run()
	{
		double[] results = new double[3];

		for (int i = 0; i < count; i++)
		{
			printm(String.format("starting game %d, %f%%", i, (double) i/count));

			Board board = new Board();

			int nextRound = 0;
			while (! board.isDone())
			{
				prints("Round #" + nextRound++);

				long start = System.currentTimeMillis();
				boolean[] runTimeUp = {true};

				Thread thread = new Thread(() -> {

					while (System.currentTimeMillis() - start < (long) timePerMove)
					{
						try
						{
							Thread.sleep((long) timePerMove - (System.currentTimeMillis() - start));
						}
						catch (InterruptedException e)
						{
							//NOP
						}
					}
					if (runTimeUp[0])
					{
						player.timeUp();
					}

				});
				thread.start();

				Coord pMove = player.move(board.copy());
				runTimeUp[0] = false;

				prints("player move: " + pMove);
				board.play(pMove, PLAYER);

				if (board.isDone())
					continue;

				Coord rMove = random.move(board.copy());
				prints("random move: " + rMove);
				board.play(rMove, ENEMY);

				prints(board);
			}

			prints("done, won: " + board.wonBy());
			results[board.wonBy() + 1]++;
		}

		printm("Results:");
		printm("Win:\t" + results[2] / count);
		printm("Tie:\t" + results[1] / count);
		printm("Loss:\t" + results[0] / count);
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
