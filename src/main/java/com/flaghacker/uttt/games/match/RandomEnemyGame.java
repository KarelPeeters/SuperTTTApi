package com.flaghacker.uttt.games.match;

import com.flaghacker.uttt.bots.RandomBot;
import com.flaghacker.uttt.common.Board;
import com.flaghacker.uttt.common.Bot;
import com.flaghacker.uttt.common.Coord;

import static com.flaghacker.uttt.common.Board.ENEMY;
import static com.flaghacker.uttt.common.Board.PLAYER;

public class RandomEnemyGame
{
	private Board board;
	private Bot player;
	private Bot random;

	public RandomEnemyGame(Bot player)
	{
		this.board = new Board();
		this.player = player;
		random = new RandomBot();
	}

	public void run()
	{
		int i = 0;
		while (!board.isDone())
		{
			System.out.println("Round #" + i++);

			Coord pMove = player.move(board.copy());
			System.out.println("player move: " + pMove);
			board.play(pMove, PLAYER);

			if (board.isDone())
				continue;

			Coord rMove = random.move(board.copy());
			System.out.println("random move: " + rMove);
			board.play(rMove, ENEMY);

			System.out.println(board);
		}

		System.out.println("done, won: " + board.wonBy());
	}
}
