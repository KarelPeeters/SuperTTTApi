package com.flaghacker.uttt.games;

import com.flaghacker.uttt.common.Board;
import com.flaghacker.uttt.common.Bot;
import com.flaghacker.uttt.common.Coord;
import com.flaghacker.uttt.common.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.flaghacker.uttt.common.Board.ENEMY;
import static com.flaghacker.uttt.common.Board.PLAYER;

public class ConsoleGame
{
	private final Bot bot;
	private final Scanner scanner;
	private int time = 5000;

	private List<Board> history = new ArrayList<>();
	private Board curr = new Board();

	public ConsoleGame(Bot bot)
	{
		this.bot = bot;
		this.scanner = new Scanner(System.in);
	}

	public void run()
	{
		System.out.println(String.format("You (X) vs %s (O)", bot));
		System.out.println("\t<undo> to undo");
		System.out.println("\t<x>,<y> to play at those coordinates");
		System.out.println("\t<xs>;<ys> to play at those coordinates within the current macro");
		System.out.println("\t<time> <int> to change the time setting (currently " + time + "ms)");

		while (true)
		{
			System.out.println("Current board: ");
			System.out.println(curr);
			String input = scanner.nextLine();

			if (input.equals("undo"))
			{
				if (history.size() == 0)
					System.err.println("no history");

				curr = history.get(history.size() - 1);
			}
			else if (input.matches("time \\d+"))
			{
				String[] arr = input.split(" ");
				time = Integer.parseInt(arr[1]);
			}
			else if (input.matches("\\d(,|;)\\d"))
			{
				boolean small = input.contains(";");
				String[] arr = input.replace(";", ",").split(",");

				Coord coord = coordFromInput(arr, small);
				if (coord == null)
					continue;

				if (curr.availableMoves().contains(coord))
				{
					System.out.println("move on " + coord);
					history.add(curr.copy());
					curr.play(coord, PLAYER);

					if (curr.isDone())
					{
						System.out.println("Game won by You (X)");
						return;
					}

					Coord botMove = Util.moveBotWithTimeOut(bot, curr.copy(), time);
					System.out.println("bot moves " + botMove);
					curr.play(botMove, ENEMY);

					if (curr.isDone())
					{
						System.out.println("Game won by " + bot + " (O)");
						return;
					}
				}
				else
				{
					if (curr.isDone())
						System.out.println("game done, won by " + toSymbol(curr.wonBy()));
					else
						System.out.println("invalid move, choose one of " + curr.availableMoves());
				}
			}
			else
			{
				System.out.println("invalid command");
			}
		}
	}

	private Coord coordFromInput(String[] arr, boolean small)
	{
		if (small)
		{
			int xm = -1;
			int ym = -1;

			for (Coord c : curr.availableMoves())
			{
				if (xm == -1)
				{
					xm = c.xm();
					ym = c.ym();
				}
				else
				{
					if (xm != c.xm() || ym != c.ym())
					{
						System.out.println("You can't use small coords here, there are multiple " +
								"playable macros");
						return null;
					}
				}
			}

			return Coord.coord(xm, ym, Integer.parseInt(arr[0]), Integer.parseInt(arr[1]));
		}

		return Coord.coord(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]));
	}

	private static String toSymbol(byte player)
	{
		if (player == PLAYER)
			return "X";
		else if (player == ENEMY)
			return "O";
		else
			return "?";
	}
}
