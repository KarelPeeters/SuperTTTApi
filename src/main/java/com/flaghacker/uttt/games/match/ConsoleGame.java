package com.flaghacker.uttt.games.match;

import com.flaghacker.uttt.common.Board;
import com.flaghacker.uttt.common.Bot;
import com.flaghacker.uttt.common.Coord;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.flaghacker.uttt.common.Board.ENEMY;
import static com.flaghacker.uttt.common.Board.PLAYER;

public class ConsoleGame
{
	private Bot bot;
	private Scanner scanner;

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
			else if (input.matches("\\d,\\d"))
			{
				String[] arr = input.split(",");
				Coord coord = Coord.coord(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]));

				if (curr.availableMoves().contains(coord))
				{
					System.out.println("move on " + coord);
					history.add(curr.copy());
					curr.play(coord, PLAYER);

					Coord botMove = bot.move(curr.copy());
					System.out.println("bot moves " + botMove);
					curr.play(botMove, ENEMY);
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
