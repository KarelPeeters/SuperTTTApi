package com.flaghacker.sttt.games;

import com.flaghacker.sttt.common.*;

import java.io.InputStream;
import java.util.Scanner;

import static com.flaghacker.sttt.common.Player.ENEMY;
import static com.flaghacker.sttt.common.Player.NEUTRAL;
import static com.flaghacker.sttt.common.Player.PLAYER;

public class AIGame
{
	private final Scanner scan;
	private final Bot bot;

	private Player[][] tmpTiles;
	private Player[] tmpMacro;
	private boolean[] tmpNextMacro;

	private int botId = 0;
	private int timePerMove;
	private int timeLeft;

	public AIGame(Bot bot, InputStream in)
	{
		this.bot = bot;
		scan = new Scanner(in);
	}

	public void run()
	{
		while (scan.hasNextLine())
		{
			String line = scan.nextLine();

			if (line.length() == 0) continue;

			String[] parts = line.split(" ");
			switch (parts[0])
			{
				case "settings":
					switch (parts[1])
					{
						case "your_botid":
							botId = Integer.parseInt(parts[2]);
							break;
						case "time_per_move":
							timePerMove = Integer.parseInt(parts[2]);
							System.err.println("timePerMove: " + timePerMove);
							break;
					}
					break;
				case "update":
					if (parts[1].equals("game"))
						parseGameData(parts[2], parts[3]);
					break;
				case "action":
					if (parts[1].equals("move"))
					{
						timeLeft = Integer.parseInt(parts[2]);
						moveBot();
					}
					break;
				default:
					System.out.println("unknown command");
					break;
			}
		}
	}

	private int timeForNextMove()
	{
		int time = timePerMove + timeLeft / (81 - moveNr);
		System.err.println("time allocated for next move: " + time);
		return time;
	}

	private void moveBot()
	{
		long delay = (long) (0.9 * timeForNextMove());

		//TODO fix this
		Board board = new Board(tmpTiles, tmpMacro, tmpNextMacro);
		Coord move = Util.moveBotWithTimeOut(bot, board, delay);

		System.out.println("place_move " + move.x() + " " + move.y());
	}

	public Board getBoard()
	{
		//TODO fix this
		return new Board(tmpTiles, tmpMacro, tmpNextMacro);
	}

	private int roundNr;
	private int moveNr;

	public void parseGameData(String key, String value)
	{
		switch (key)
		{
			case "round":
				roundNr = Integer.parseInt(value);
				break;
			case "move":
				moveNr = Integer.parseInt(value);
				break;
			case "field":
				parseFromString(value);
				break;
			case "macroboard":
				parseMacroBoardFromString(value);
				break;
		}
	}

	public void parseFromString(String s)
	{
		this.tmpTiles = new Player[9][9];

		System.err.println("Move " + moveNr);
		s = s.replace(";", ",");
		String[] r = s.split(",");
		int counter = 0;
		for (int o = 0; o < 81; o++)
		{
			tmpTiles[o % 9][o / 9] = toPlayer(Integer.parseInt(r[counter]));
			counter++;
		}
	}

	public void parseMacroBoardFromString(String s)
	{
		this.tmpNextMacro = new boolean[9];
		this.tmpMacro = new Player[9];

		String[] r = s.split(",");
		int counter = 0;
		for (int om = 0; om < 9; om++)
		{
			int val = Integer.parseInt(r[counter]);
			tmpNextMacro[om] = val == - 1;
			tmpMacro[om] = toPlayer(val);
			counter++;
		}
	}

	private Player toPlayer(int i)
	{
		if (i == 0 || i == - 1)
			return NEUTRAL;
		if (i == botId)
			return PLAYER;
		if (i == 3 - botId)
			return ENEMY;

		throw new IllegalArgumentException("i can't be " + i);
	}

	private int fromPlayer(Player b)
	{
		if (b == NEUTRAL)
			return 0;
		if (b == PLAYER)
			return botId;
		if (b == ENEMY)
			return 3 - botId;

		throw new IllegalArgumentException("b can't be " + b);
	}

	public int getRoundNr()
	{
		return roundNr;
	}

	public int getMoveNr()
	{
		return moveNr;
	}
}
