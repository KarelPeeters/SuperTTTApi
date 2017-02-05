package com.flaghacker.uttt.common;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Board
{
	public static final byte NEUTRAL = 0;
	public static final byte PLAYER = 2;
	public static final byte ENEMY = 3;

	private static final int TILE_START = 0;
	private static final int MACROTILE_START = 9 * 9;
	private static final int NEXTMACRO_START = 9 * 9 + 9;


	private BitSet data;
	private byte wonBy = NEUTRAL;
	private List<Coord> freeTiles = new ArrayList<>();
	private byte nextPlayer = PLAYER;
	private Coord lastMove;

	public Board(Board other)
	{
		this.data = (BitSet) other.data.clone();

		this.wonBy = other.wonBy;
		this.freeTiles = new ArrayList<>(other.freeTiles);
		this.nextPlayer = other.nextPlayer;
	}

	public Board(byte[][] tiles, byte[] macroTiles, boolean[] nextMacros)
	{
		this.data = new BitSet(2 * (9 * 9 + 9) + 9);

		for (Coord coord : Coord.list())
			setTile(coord, tiles[coord.x()][coord.y()]);

		for (int xm = 0; xm < 3; xm++)
		{
			for (int ym = 0; ym < 3; ym++)
			{
				setMacro(xm + 3 * ym, macroTiles[xm + 3 * ym]);
				setNextMacro(xm + 3 * ym, nextMacros == null || nextMacros[xm + 3 * ym]);
			}
		}


		for (Coord coord : Coord.list())
			if (tile(coord) == NEUTRAL)
				freeTiles.add(coord);
	}

	public Board()
	{
		this(new byte[9][9], new byte[9], null);
	}

	//low level data methods
	private byte readPlayer(int i)
	{
		boolean occupied = data.get(i);
		boolean enemy = data.get(i + 1);

		return occupied ? (enemy ? ENEMY : PLAYER) : NEUTRAL;
	}

	private void writePlayer(int i, byte player)
	{
		data.set(i, player == NEUTRAL);
		data.set(i, player == PLAYER);    //doesn't matter what's set when player==NEUTRAL
	}

	public byte tile(Coord coord)
	{
		return readPlayer(TILE_START + coord.i());
	}

	private void setTile(Coord coord, byte player)
	{
		writePlayer(TILE_START + 9 * coord.om() + coord.os(), player);
	}

	public byte macro(int om)
	{
		return readPlayer(MACROTILE_START + om);
	}

	private void setMacro(int om, byte player)
	{
		writePlayer(MACROTILE_START + om, player);
	}

	public boolean nextMacro(int om)
	{
		return data.get(NEXTMACRO_START + om);
	}

	private void setNextMacro(int om, boolean value)
	{
		data.set(NEXTMACRO_START + om, value);
	}

	//utility wrappers
	public byte tile(int x, int y)
	{
		return tile(Coord.coord(x, y));
	}

	public byte macro(int xm, int ym)
	{
		return macro(xm + 3 * ym);
	}

	//other methods
	public boolean play(Coord coord, byte player)
	{
		if (!availableMoves().contains(coord))
			throw new IllegalArgumentException(coord + " is not available, choose one of: " + availableMoves());

		if (!(nextPlayer == player))
			throw new IllegalArgumentException(nextPlayer + " must play instead of " + player);

		lastMove = coord;
		nextPlayer = other(player);

		setTile(coord, player);

		freeTiles.remove(coord);
		if (macroFull(coord.xm(), coord.ym()))
			freeTiles.removeAll(Coord.macro(coord.xm(), coord.ym()));

		isWon(coord);

		boolean free = (macro(coord.os()) != NEUTRAL) || macroFull(coord.xs(), coord.ys());
		for (int om = 0; om < 9; om++)
			setNextMacro(om, ((free || (coord.os() == om)) && !macroFull(om)
					&& macro(om) == NEUTRAL));

		return macro(coord.xm(), coord.ym()) != NEUTRAL;
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
		List<Coord> result = new ArrayList<>();

		for (Coord coord : freeTiles)
			if (nextMacro(coord.om()))
				result.add(coord);

		return Collections.unmodifiableList(result);
	}

	//checks whether the board was won by the tile placed at absolute (x,y)
	private boolean isWon(Coord coord)
	{
		byte player = tile(coord);

		if (wonGrid(coord.os(), TILE_START + coord.i()))
		{
			setMacro(coord.om(), player);
			if (wonGrid(coord.om(), MACROTILE_START))
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

	private boolean wonGrid(int playedIndex, int dataOffset)
	{
		byte player = readPlayer(dataOffset + playedIndex);
		int x = playedIndex % 3;
		int y = playedIndex / 3;

		//center
		if (x == 1 && y == 1)
		{
			for (int i = 0; i < 2; i++)
				if (check(i, 0, dataOffset, player) && check(2 - i, 2, dataOffset, player))
					return true;
			return check(2, 1, dataOffset, player) && check(0, 1, dataOffset, player);
		}

		//corners
		if (x % 2 == 0 && y % 2 == 0)
			return (check(2 - x, y, dataOffset, player) && check(1, y, dataOffset, player))
					|| (check(x, 2 - y, dataOffset, player) && check(x, 1, dataOffset, player))
					|| (check(1, 1, dataOffset, player) && check(2 - x, 2 - y, dataOffset, player));

		//horizontal sides
		if (x % 2 == 0)
			return (check(x, 0, dataOffset, player) && check(x, 2, dataOffset, player))
					|| (check(1, 1, dataOffset, player) && check(2 - x, 2 - y, dataOffset, player));

		//vertical sides
		if (y % 2 == 0)
			return (check(0, y, dataOffset, player) && check(2, y, dataOffset, player))
					|| (check(1, 1, dataOffset, player) && check(1, 2 - y, dataOffset, player));

		throw new AssertionError();
	}

	public List<Coord> getEnemyTiles(int xm, int ym)
	{
		List<Coord> result = new ArrayList<>();

		for (Coord coord : Coord.macro(xm, ym))
			if (tile(coord) == ENEMY)
				result.add(coord);

		return Collections.unmodifiableList(result);
	}

	public byte nextPlayer()
	{
		return nextPlayer;
	}

	private boolean check(int x, int y, int dataOffset, byte player)
	{
		return readPlayer(dataOffset + (x + 3 * y)) == player;
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
			if (nextMacro(om))
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
		List<Coord> result = new ArrayList<>();

		for (Coord coord : moves)
		{
			if (coord.xs() % 2 == 0 && coord.ys() % 2 == 0)
				result.add(coord);
		}

		return Collections.unmodifiableList(result);
	}

	public List<Coord> getAvailableSides(List<Coord> moves)
	{
		List<Coord> result = new ArrayList<>();

		for (Coord coord : moves)
		{
			if (coord.xs() + coord.ys() == 1 || coord.xs() + coord.ys() == 3)
				result.add(coord);
		}

		return Collections.unmodifiableList(result);
	}

	@Override
	public String toString()
	{
		List<Coord> coordList = Coord.list();

		List<String> strings = new ArrayList<>();
		for (Coord coord : coordList)
			strings.add(tile(coord) == PLAYER ? "X" : (tile(coord) == ENEMY ? "O" : " "));

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
				Objects.equals(data, board.data) &&
				Objects.equals(lastMove, board.lastMove);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(data, wonBy, nextPlayer, lastMove);
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
		List<Coord> result = new ArrayList<>();

		for (Coord coord : Coord.macro(xm, ym))
			if (tile(coord) == PLAYER)
				result.add(coord);

		return Collections.unmodifiableList(result);
	}

	public void disableAllMacrosExcept(int xm, int ym)
	{
		for (int _om = 0; _om < 9; _om++)
			setNextMacro(_om, false);

		setNextMacro(xm + 3 * ym, true);
	}

	public void enableAllMacros()
	{
		for (int _om = 0; _om < 9; _om++)
			this.setNextMacro(_om, true);
	}

	public boolean macroFinishesGame(int xm, int ym, byte player)
	{
		int om = xm + 3 * ym;

		byte tmpPlayer = macro(om);
		setMacro(om, player);

		boolean won = wonGrid(om, MACROTILE_START);
		setMacro(om, tmpPlayer);

		return won;
	}

	public boolean winnableMacro(int xm, int ym, byte player)
	{
		Board test = copy();
		test.disableAllMacrosExcept(xm, ym);
		test.setNextPlayer(player);

		for (Coord move : test.availableMoves())
		{
			test.play(move, player);
			test.disableAllMacrosExcept(xm, ym);
			test.setNextPlayer(player);
		}

		return macro(xm, ym) == player;
	}
}
