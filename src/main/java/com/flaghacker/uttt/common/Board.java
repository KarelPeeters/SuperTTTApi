package com.flaghacker.uttt.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Board
{
	public static final byte NEUTRAL = 0;
	public static final byte PLAYER = 1;
	public static final byte ENEMY = - 1;
	public static final byte FULL = 64;        //todo add checks for this

	private byte[][][][] tiles;
	private byte[][] macroTiles;
	private boolean[][] nextMacros;
	private byte wonBy = NEUTRAL;
	private List<Coord> freeTiles = new ArrayList<>();
	private byte nextPlayer = PLAYER;

	public Board(Board other)
	{
		this.tiles = new byte[3][3][3][3];
		for (int xm = 0; xm < 3; xm++)
			for (int ym = 0; ym < 3; ym++)
				for (int xs = 0; xs < 3; xs++)
					System.arraycopy(other.tiles[xm][ym][xs], 0, this.tiles[xm][ym][xs], 0, 3);

		this.macroTiles = new byte[3][3];
		this.nextMacros = new boolean[3][3];
		for (int xm = 0; xm < 3; xm++)
		{
			System.arraycopy(other.macroTiles[xm], 0, this.macroTiles[xm], 0, 3);
			System.arraycopy(other.nextMacros[xm], 0, this.nextMacros[xm], 0, 3);
		}

		this.wonBy = other.wonBy;
		this.freeTiles = new ArrayList<>(other.freeTiles);
		this.nextPlayer = other.nextPlayer;
	}

	public Board(byte[][][][] tiles, byte[][] macroTiles, boolean[][] nextMacros)
	{
		this.tiles = tiles;
		//todo compute based on tiles
		this.macroTiles = macroTiles;

		freeTiles.addAll(
				Coord.list()
						.stream()
						.filter(coord -> tile(coord) == NEUTRAL)
						.collect(Collectors.toList())
		);

		this.nextMacros = nextMacros;
	}

	public Board()
	{
		this(new byte[3][3][3][3], new byte[3][3], null);

		this.nextMacros = new boolean[3][3];
		for (int xm = 0; xm < 3; xm++)
			for (int ym = 0; ym < 3; ym++)
				nextMacros[xm][ym] = true;
	}

	public byte[][] tiles(int xm, int ym)
	{
		return tiles[xm][ym];
	}

	public byte macro(int xm, int ym)
	{
		return macroTiles[xm][ym];
	}

	public byte tile(Coord coord)
	{
		return tiles[coord.xm()][coord.ym()][coord.xs()][coord.ys()];
	}

	public byte tile(int x, int y)
	{
		return tiles[x / 3][y / 3][x % 3][y % 3];
	}

	public boolean play(Coord coord, byte player)
	{
		if (! availableMoves().contains(coord))
			throw new IllegalArgumentException(coord + " is not available, choose one of: " + availableMoves());

		if (player == PLAYER)
			nextPlayer = ENEMY;
		else if (player == ENEMY)
			nextPlayer = PLAYER;
		else
			throw new IllegalArgumentException("player should be one of PLAYER, ENEMY; was " + player);

		tiles[coord.xm()][coord.ym()][coord.xs()][coord.ys()] = player;
		freeTiles.remove(coord);

		boolean free = macro(coord.xs(), coord.ys()) != NEUTRAL;
		for (int xm = 0; xm < 3; xm++)
			for (int ym = 0; ym < 3; ym++)
				nextMacros[xm][ym] = free || (coord.xs() == xm && coord.ys() == ym);

		isWon(coord);
		return macro(coord.xm(), coord.ym()) != NEUTRAL;
	}

	public byte wonBy()
	{
		return wonBy;
	}

	public List<Coord> freeTiles()
	{
		return freeTiles;
	}

	public List<Coord> availableMoves()
	{
		return Collections.unmodifiableList(
				freeTiles.stream()
						.filter(coord -> nextMacros[coord.xm()][coord.ym()])
						.collect(Collectors.toList())
		);
	}

	//checks whether the board was won by the tile placed at absolute (x,y)
	private boolean isWon(Coord coord)
	{
		byte player = tile(coord);

		if (wonGrid(coord.xs(), coord.ys(), tiles(coord.xm(), coord.ym())))
		{
			macroTiles[coord.xm()][coord.ym()] = player;
			if (wonGrid(coord.xm(), coord.ym(), macroTiles))
			{
				wonBy = player;
				return true;
			}
		}

		return false;
	}

	public boolean isDone()
	{
		return wonBy() != NEUTRAL || freeTiles.isEmpty();
	}


	private boolean wonGrid(int x, int y, byte[][] grid)
	{
		byte player = grid[x][y];

		//center
		if (x == 1 && y == 1)
		{
			for (int i = 0; i < 2; i++)
				if (check(i, 0, grid, player) && check(2 - i, 2, grid, player))
					return true;
			return check(2, 1, grid, player) && check(0, 1, grid, player);
		}

		//corners
		if (x % 2 == 0 && y % 2 == 0)
			return (check(2 - x, y, grid, player) && check(1, y, grid, player))
					|| (check(x, 2 - y, grid, player) && check(x, 1, grid, player))
					|| (check(1, 1, grid, player) && check(2 - x, 2 - y, grid, player));

		//horizontal sides
		if (x % 2 == 0)
			return (check(x, 0, grid, player) && check(x, 2, grid, player))
					|| (check(1, 1, grid, player) && check(2 - x, 2 - y, grid, player));

		//vertical sides
		if (y % 2 == 0)
			return (check(0, y, grid, player) && check(2, y, grid, player))
					|| (check(1, 1, grid, player) && check(1, 2 - y, grid, player));

		throw new AssertionError();
	}

	public byte nextPlayer()
	{
		return nextPlayer;
	}

	private boolean check(int x, int y, byte[][] grid, byte player)
	{
		return grid[x][y] == player;
	}

	public Board copy()
	{
		return new Board(this);
	}

	public boolean isMacroEmpty(int xm, int ym)
	{
		for (int xs = 0; xs <= 2; xs++)
			for (int ys = 0; ys <= 2; ys++)
				if (tile(Coord.coord(xm, ym, xs, ys)) != NEUTRAL)
					return false;

		return true;
	}

	public boolean singleMacro()
	{
		int count = 0;
		for (int xm = 0; xm < 3; xm++)
		{
			for (int ym = 0; ym < 3; ym++)
			{
				if (nextMacros[xm][ym])
					count++;
				if (count > 1)
					return false;
			}
		}

		return true;
	}

	public boolean macroFull(int xm, int ym)
	{
		int count = 0;
		for (int x = 3 * xm; x < 3 * xm + 3; x++)
			for (int y = 3 * ym; y < 3 * ym + 3; y++)
				if (tile(x, y) != NEUTRAL)
					count++;

		return count == 9;
	}

	public Coord currMacro()
	{
		if (! singleMacro())
			return null;

		for (int xm = 0; xm < 3; xm++)
			for (int ym = 0; ym < 3; ym++)
				if (nextMacros[xm][ym])
					return Coord.coord(xm, 0, ym, 0);

		throw new RuntimeException();
	}

	public List<Coord> getAvailableCorners(List<Coord> moves)
	{
		return moves.stream()
				.filter(coord -> coord.xs() % 2 == 0 && coord.ys() % 2 == 0)
				.collect(Collectors.toList());
	}

	public List<Coord> getAvailableSides(List<Coord> moves)
	{
		return moves.stream()
				.filter(coord -> (coord.xs() + coord.ys() == 1 || coord.xs() + coord.ys() == 3))
				.collect(Collectors.toList());
	}

	@Override
	public String toString()
	{
		List<Coord> coordList = Coord.list();

		List<String> strings = coordList.stream()
				.map(cell -> String.join("", tile(cell) == PLAYER ? "X" : (tile(cell) == ENEMY ? "O" : " ")))
				.collect(Collectors.toList());

		String line = removeEnd(repeat(repeat("-", 3) + "+", 3), "+");

		String result = "";
		for (int y = 0; y < 9; y++)
		{
			for (int x = 0; x < 9; x++)
			{
				result += strings.get(9 * y + x);

				if (x == 2 || x == 5)
					result += "|";
			}

			if (y == 2 || y == 5)
				result += "\n" + line;
			result += "\n";
		}

		return result;
	}

	private static String repeat(String str, int count)
	{
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < count; i++)
			builder.append(str);
		return builder.toString();
	}

	private static String removeEnd(String str, String end)
	{
		if (str.endsWith(end)) {
			return str.substring(0, str.length() - end.length());
		}
		return str;
	}
}
