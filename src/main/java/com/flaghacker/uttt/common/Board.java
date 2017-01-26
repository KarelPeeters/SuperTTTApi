package com.flaghacker.uttt.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Board
{
	public static final byte NEUTRAL = 0;
	public static final byte PLAYER = 1;
	public static final byte ENEMY = - 1;

	private byte[][] tiles;
	private byte[] macroTiles;
	private boolean[] nextMacros;
	private byte wonBy = NEUTRAL;
	private List<Coord> freeTiles = new ArrayList<>();
	private byte nextPlayer = PLAYER;
	private Coord lastMove;

	public Board(Board other)
	{
		this.tiles = new byte[9][9];
		for (int om = 0; om < 9; om++)
			System.arraycopy(other.tiles[om], 0, tiles[om], 0, 9);

		this.macroTiles = new byte[9];
		this.nextMacros = new boolean[9];
		System.arraycopy(other.macroTiles, 0, this.macroTiles, 0, 9);
		System.arraycopy(other.nextMacros, 0, this.nextMacros, 0, 9);

		this.wonBy = other.wonBy;
		this.freeTiles = new ArrayList<>(other.freeTiles);
		this.nextPlayer = other.nextPlayer;
	}

	public Board(byte[][] tiles, byte[] macroTiles, boolean[] nextMacros)
	{
		this.tiles = tiles;
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
		this(new byte[9][9], new byte[9], null);

		this.nextMacros = new boolean[9];
		for (int om = 0; om < 9; om++)
			nextMacros[om] = true;
	}

	public byte[] tiles(int xm, int ym)
	{
		return tiles[xm + 3 * ym];
	}

	public byte macro(int xm, int ym)
	{
		return macroTiles[xm + 3 * ym];
	}

	public byte tile(Coord coord)
	{
		return tiles[coord.om()][coord.os()];
	}

	public byte tile(int x, int y)
	{
		return tile(Coord.coord(x, y));
	}

	public boolean play(Coord coord, byte player)
	{
		if (! availableMoves().contains(coord))
			throw new IllegalArgumentException(coord + " is not available, choose one of: " + availableMoves());

		if (! (nextPlayer == player))
			throw new IllegalArgumentException(nextPlayer + " must play instead of " + player);

		lastMove = coord;
		nextPlayer = other(player);

		tiles[coord.om()][coord.os()] = player;

		freeTiles.remove(coord);
		if (macroFull(coord.xm(), coord.ym()))
			freeTiles.removeAll(Coord.macro(coord.xm(), coord.ym()));

		isWon(coord);

		boolean free = (macro(coord.xs(), coord.ys()) != NEUTRAL) || macroFull(coord.xs(), coord.ys());
		for (int om = 0; om < 9; om++)
			nextMacros[om] = ((free || (coord.os() == om)) && ! macroFull(om)
					&& macro(om) == NEUTRAL);

		return macro(coord.xm(), coord.ym()) != NEUTRAL;
	}

	public byte macro(int om)
	{
		return macro(om % 3, om / 3);
	}

	private boolean macroFull(int om)
	{
		return macroFull(om % 3, om / 3);
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
						.filter(coord -> nextMacros[coord.om()])
						.collect(Collectors.toList())
		);
	}

	//checks whether the board was won by the tile placed at absolute (x,y)
	private boolean isWon(Coord coord)
	{
		byte player = tile(coord);

		if (wonGrid(coord.os(), tiles(coord.xm(), coord.ym())))
		{
			macroTiles[coord.om()] = player;
			if (wonGrid(coord.om(), macroTiles))
			{
				wonBy = player;
				return true;
			}
		}

		return false;
	}

	public boolean isDone()
	{
		return wonBy != NEUTRAL || availableMoves().size() == 0;
	}


	private boolean wonGrid(int o, byte[] grid)
	{
		byte player = grid[o];
		int x = o % 3;
		int y = o / 3;

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

	public List<Coord> getEnemyTiles(int xm, int ym)
	{
		return Coord.macro(xm, ym).stream()
				.filter(coord -> tile(coord) == ENEMY)
				.collect(Collectors.toList());
	}

	public byte nextPlayer()
	{
		return nextPlayer;
	}

	private boolean check(int x, int y, byte[] grid, byte player)
	{
		return grid[x + 3 * y] == player;
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
		for (int om = 0; om < 9; om++)
		{
			if (nextMacros[om])
				count++;
			if (count > 1)
				return false;
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

	public static byte other(byte player)
	{
		if (player == PLAYER)
			return ENEMY;
		else if (player == ENEMY)
			return PLAYER;
		else
			throw new IllegalArgumentException("player should be one of PLAYER, ENEMY; was " + player);
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
		if (str.endsWith(end))
		{
			return str.substring(0, str.length() - end.length());
		}
		return str;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Board board = (Board) o;
		return wonBy == board.wonBy &&
				nextPlayer == board.nextPlayer &&
				Arrays.deepEquals(tiles, board.tiles) &&
				Arrays.equals(nextMacros, board.nextMacros);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(Arrays.deepHashCode(tiles), Arrays.hashCode(nextMacros), wonBy, nextPlayer);
	}

	public Coord getLastMove()
	{
		return lastMove;
	}

	public void setNextPlayer(byte nextPlayer)
	{
		this.nextPlayer = nextPlayer;
	}

	public List<Coord> getPlayerTiles(int xm, int ym)
	{
		return Coord.macro(xm, ym).stream()
				.filter(coord -> tile(coord) == PLAYER)
				.collect(Collectors.toList());
	}

	public void setNextMacro(int xm, int ym)
	{
		for (int _om = 0; _om < 9; _om++)
			this.nextMacros[_om] = false;

		this.nextMacros[xm + 3 * ym] = true;
	}

	public void enableAllMacros()
	{
		for (int _om = 0; _om < 9; _om++)
			this.nextMacros[_om] = true;
	}

	public boolean macroFinishesGame(int xm, int ym, byte player)
	{
		int om = xm + 3 * ym;

		byte tmpPlayer = macroTiles[om];
		macroTiles[om] = player;

		boolean won = wonGrid(om, macroTiles);
		macroTiles[om] = tmpPlayer;

		return won;
	}

	public void setMacro(int xm, int ym, byte player)
	{
		macroTiles[xm + 3 * ym] = player;
	}

	public boolean winnableMacro(int xm, int ym, byte player)
	{
		Board test = copy();
		test.setNextMacro(xm, ym);
		test.setNextPlayer(player);

		for (Coord move : test.availableMoves())
		{
			test.play(move, player);
			test.setNextMacro(xm, ym);
			test.setNextPlayer(player);
		}

		return macro(xm, ym) == player;
	}
}
