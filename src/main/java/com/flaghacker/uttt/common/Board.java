package com.flaghacker.uttt.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.flaghacker.uttt.common.Player.ENEMY;
import static com.flaghacker.uttt.common.Player.NEUTRAL;
import static com.flaghacker.uttt.common.Player.PLAYER;

public class Board implements Serializable
{
	private static final long serialVersionUID = -8352590958828628704L;

	private static final int TILE_START = 0;
	private static final int MACRO_TILE_START = TILE_START + 2 * 9 * 9;
	private static final int NEXT_MACRO_START = MACRO_TILE_START + 2 * 9;

	private BitSet data;
	private Player wonBy = NEUTRAL;
	private List<Coord> freeTiles = new ArrayList<>();
	private List<Coord> availableMoves;
	private Player nextPlayer = PLAYER;
	private Coord lastMove;

	public Board(Board other)
	{
		this.data = (BitSet) other.data.clone();

		this.wonBy = other.wonBy;
		this.freeTiles = new ArrayList<>(other.freeTiles);
		this.availableMoves = other.availableMoves;
		this.nextPlayer = other.nextPlayer;
		this.lastMove = other.lastMove;
	}

	public Board(Player[][] tiles, Player[] macroTiles, boolean[] nextMacros)
	{
		this.data = new BitSet(2 * (9 * 9 + 9) + 9);

		for (Coord coord : Coord.list())
		{
			Player tile = tiles == null ? NEUTRAL : tiles[coord.x()][coord.y()];

			setTile(coord, tile);
			if (tile == NEUTRAL)
				freeTiles.add(coord);
		}

		for (int xm = 0; xm < 3; xm++)
		{
			for (int ym = 0; ym < 3; ym++)
			{
				setMacro(xm + 3 * ym, macroTiles == null ? NEUTRAL : macroTiles[xm + 3 * ym]);
				setNextMacro(xm + 3 * ym, nextMacros == null || nextMacros[xm + 3 * ym]);
			}
		}
	}

	public Board()
	{
		this(null, null, null);
	}

	//low level data methods
	private Player readPlayer(int i)
	{
		boolean occupied = data.get(i);
		boolean enemy = data.get(i + 1);

		return occupied ? (enemy ? ENEMY : PLAYER) : NEUTRAL;
	}

	private void writePlayer(int i, Player player)
	{
		data.set(i, player != NEUTRAL);
		data.set(i + 1, player == ENEMY);
	}

	public Player tile(Coord coord)
	{
		return readPlayer(TILE_START + 2 * (coord.os() + 9 * coord.om()));
	}

	private void setTile(Coord coord, Player player)
	{
		writePlayer(TILE_START + 2 * (9 * coord.om() + coord.os()), player);
	}

	public Player macro(int om)
	{
		return readPlayer(MACRO_TILE_START + 2 * om);
	}

	private void setMacro(int om, Player player)
	{
		writePlayer(MACRO_TILE_START + 2 * om, player);
	}

	public boolean nextMacro(int om)
	{
		return data.get(NEXT_MACRO_START + om);
	}

	public void setNextMacro(int om, boolean value)
	{
		data.set(NEXT_MACRO_START + om, value);
	}

	//utility wrappers
	public Player tile(int x, int y)
	{
		return tile(Coord.coord(x, y));
	}

	public Player macro(int xm, int ym)
	{
		return macro(xm + 3 * ym);
	}

	public void setLastMove(Coord lastMove)
	{
		this.lastMove = lastMove;
	}

	//other methods
	public boolean play(Coord coord)
	{
		if (!availableMoves().contains(coord))
			throw new IllegalArgumentException(coord + " is not available, choose one of: " + availableMoves());

		Player player = nextPlayer;

		lastMove = coord;
		nextPlayer = nextPlayer.other();
		availableMoves = null;

		setTile(coord, player);

		freeTiles.remove(coord);
		if (macroFull(coord.xm(), coord.ym()))
			freeTiles.removeAll(Coord.macro(coord.xm(), coord.ym()));

		isWon(coord);

		boolean free = (macro(coord.os()) != NEUTRAL) || macroFull(coord.xs(), coord.ys());
		for (int om = 0; om < 9; om++)
			setNextMacro(om, ((free || (coord.os() == om)) && !macroFull(om) && macro(om) == NEUTRAL));

		return macro(coord.xm(), coord.ym()) != NEUTRAL;
	}

	private boolean macroFull(int om)
	{
		return macroFull(om % 3, om / 3);
	}

	public Player wonBy()
	{
		return wonBy;
	}

	public List<Coord> freeTiles()
	{
		return freeTiles;
	}

	public List<Coord> availableMoves()
	{
		if (availableMoves == null)
		{
			List<Coord> result = new ArrayList<>();
			for (Coord coord : freeTiles)
			{
				if (nextMacro(coord.om()))
					result.add(coord);
			}

			availableMoves = Collections.unmodifiableList(result);
		}
		return availableMoves;
	}

	//checks whether the board was won by the tile placed at coord
	private boolean isWon(Coord coord)
	{
		Player player = tile(coord);

		if (wonGrid(coord.os(), TILE_START + 2 * 9 * coord.om()))
		{
			setMacro(coord.om(), player);
			if (wonGrid(coord.om(), MACRO_TILE_START))
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
		Player player = readPlayer(dataOffset + 2 * playedIndex);
		int x = playedIndex % 3;
		int y = playedIndex / 3;

		//center
		if (x == 1 && y == 1)
		{
			for (int i = 0; i <= 2; i++)
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

	public Player nextPlayer()
	{
		return nextPlayer;
	}

	private boolean check(int x, int y, int dataOffset, Player player)
	{
		return readPlayer(dataOffset + 2 * (x + 3 * y)) == player;
	}

	public Board copy()
	{
		return new Board(this);
	}

	public Board flip()
	{
		Board board = this.copy();

		for (Coord coord : Coord.list())
			board.setTile(coord, board.tile(coord).otherWithNeutral());
		for (int om = 0; om < 9; om++)
			board.setMacro(om, board.macro(om).otherWithNeutral());

		board.wonBy = board.wonBy.otherWithNeutral();
		board.nextPlayer = board.nextPlayer.otherWithNeutral();

		return board;
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

	public void setNextPlayer(Player nextPlayer)
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

	public boolean macroFinishesGame(int xm, int ym, Player player)
	{
		int om = xm + 3 * ym;

		Player tmpPlayer = macro(om);
		setMacro(om, player);

		boolean won = wonGrid(om, MACRO_TILE_START);
		setMacro(om, tmpPlayer);

		return won;
	}

	public boolean winnableMacro(int xm, int ym, Player player)
	{
		Board test = copy();
		test.disableAllMacrosExcept(xm, ym);
		test.setNextPlayer(player);

		for (Coord move : test.availableMoves())
		{
			test.play(move);
			test.disableAllMacrosExcept(xm, ym);
			test.setNextPlayer(player);
		}

		return macro(xm, ym) == player;
	}
}
