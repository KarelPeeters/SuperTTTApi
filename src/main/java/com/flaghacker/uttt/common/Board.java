package com.flaghacker.uttt.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Board
{
	public static final byte EMPTY = 0;
	public static final byte PLAYER = 1;
	public static final byte ENEMY = - 1;
	public static final byte FULL = 64;        //todo add checks for this

	private byte[][][][] tiles;
	private byte[][] macroTiles;
	private boolean[][] nextMacros;
	private byte wonBy = EMPTY;
	private List<Coord> freeTiles = new ArrayList<>();

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
	}

	public Board(byte[][][][] tiles, byte[][] macroTiles, boolean[][] nextMacros)
	{
		this.tiles = tiles;
		//todo compute based on tiles
		this.macroTiles = macroTiles;

		freeTiles.addAll(
				Coord.list()
						.stream()
						.filter(coord -> tile(coord) == EMPTY)
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
		if (player == EMPTY)
			return false;

		if (! availableMoves().contains(coord))
			throw new IllegalArgumentException(coord + " is not available, choose one of: " + availableMoves());

		tiles[coord.xm()][coord.ys()][coord.xs()][coord.ys()] = player;
		freeTiles.remove(coord);

		for (int xm = 0; xm < 3; xm++)
			for (int ym = 0; ym < 3; ym++)
				nextMacros[xm][ym] = (coord.xs() == xm && coord.ys() == xm);

		return isWon(coord);
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

	private boolean isDone()
	{
		return wonBy() != EMPTY;
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

	private boolean check(int x, int y, byte[][] grid, byte player)
	{
		return grid[x][y] == player;
	}

	public Board copy()
	{
		return new Board(this);
	}
}
