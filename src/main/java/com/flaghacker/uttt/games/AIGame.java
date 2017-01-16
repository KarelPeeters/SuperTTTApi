package com.flaghacker.uttt.games;

import com.flaghacker.uttt.common.Board;
import com.flaghacker.uttt.common.Bot;
import com.flaghacker.uttt.common.Coord;
import com.flaghacker.uttt.common.Util;

import java.util.Scanner;

import static com.flaghacker.uttt.common.Board.ENEMY;
import static com.flaghacker.uttt.common.Board.NEUTRAL;
import static com.flaghacker.uttt.common.Board.PLAYER;

public class AIGame
{
	private final Scanner scan = new Scanner(System.in);
	private final Bot bot;

	private byte[][][][] tmpTiles;
	private byte[][] tmpMacro;
	private boolean[][] tmpNextMacro;

	private int botId = 0;
	private int timePerMove;
	private int timeLeft;

	public AIGame(Bot bot)
	{
		this.bot = bot;
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
		int time = timePerMove + timeLeft/(81-moveNr);
		System.err.println("time allocated for next move: " + time);
		return time;
	}

	private void moveBot()
	{
		long delay = (long) (0.9 * timeForNextMove());

		Board board = new Board(tmpTiles, tmpMacro, tmpNextMacro);
		Coord move = Util.moveBotWithTimeOut(bot, board, delay);

		System.out.println("place_move " + move.x() + " " + move.y());
	}

	public Board getBoard()
	{
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
		this.tmpTiles = new byte[3][3][3][3];

		System.err.println("Move " + moveNr);
		s = s.replace(";", ",");
		String[] r = s.split(",");
		int counter = 0;
		for (int y = 0; y < 9; y++)
		{
			for (int x = 0; x < 9; x++)
			{
				tmpTiles[x / 3][y / 3][x % 3][y % 3] = toPlayer(Integer.parseInt(r[counter]));
				counter++;
			}
		}
	}

	public void parseMacroBoardFromString(String s)
	{
		this.tmpNextMacro = new boolean[3][3];
		this.tmpMacro = new byte[3][3];

		String[] r = s.split(",");
		int counter = 0;
		for (int ym = 0; ym < 3; ym++)
		{
			for (int xm = 0; xm < 3; xm++)
			{
				int val = Integer.parseInt(r[counter]);
				tmpNextMacro[xm][ym] = val == -1;
				tmpMacro[xm][ym] = toPlayer(val);
				counter++;
			}
		}
	}

	private byte toPlayer(int i)
	{
		if (i == 0 || i == -1)
			return NEUTRAL;
		if (i == botId)
			return PLAYER;
		if (i == 3-botId)
			return ENEMY;

		throw new IllegalArgumentException("i can't be " + i);
	}

	private int fromPlayer(byte b)
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
