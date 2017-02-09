package com.flaghacker.uttt.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unchecked")
public class Coord
{
	private final int x;
	private final int y;

	private static final Coord[][] instances;
	private static final List<Coord>[][] macroLists;
	private static final List<Coord> coordList;

	static
	{
		List<Coord> tmpCoordList = new ArrayList<>(9 * 9);
		instances = new Coord[9][9];
		@SuppressWarnings("unchecked")
		List<Coord>[][] tmpMacroLists = (List<Coord>[][]) new List[3][3];

		for (int y = 0; y < 9; y++)
		{
			for (int x = 0; x < 9; x++)
			{
				Coord coord = new Coord(x, y);
				instances[x][y] = coord;
				tmpCoordList.add(coord);

				if (tmpMacroLists[x / 3][y / 3] == null)
					tmpMacroLists[x / 3][y / 3] = new ArrayList<>(9);
				tmpMacroLists[x / 3][y / 3].add(coord);
			}
		}

		macroLists = (List<Coord>[][]) new List[3][3];

		for (int xm = 0; xm < 3; xm++)
			for (int ym = 0; ym < 3; ym++)
				macroLists[xm][ym] = Collections.unmodifiableList(tmpMacroLists[xm][ym]);

		coordList = Collections.unmodifiableList(tmpCoordList);
	}

	private Coord(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	public static Coord coord(int x, int y)
	{
		return instances[x][y];
	}

	public static Coord coord(int xm, int ym, int xs, int ys)
	{
		assert (0 <= xm && xm < 3) && (0 <= ym && ym < 3) && (0 <= xs && xs < 3) && (0 <= ys && ys < 3);

		return coord(3 * xm + xs, 3 * ym + ys);
	}

	public static Coord coord(int o)
	{
		assert 0 <= o && o < 9 * 9;

		return coord(o % 9, o / 9);
	}

	public static List<Coord> list()
	{
		return coordList;
	}

	public static List<Coord> macro(int xm, int ym)
	{
		return macroLists[xm][ym];
	}

	public int x()
	{
		return x;
	}

	public int xm()
	{
		return x / 3;
	}

	public int xs()
	{
		return x % 3;
	}

	public int y()
	{
		return y;
	}

	public int ym()
	{
		return y / 3;
	}

	public int ys()
	{
		return y % 3;
	}

	public int o()
	{
		return x + 9 * y;
	}

	public int om()
	{
		return xm() + 3 * ym();
	}

	public int os()
	{
		return xs() + 3 * ys();
	}

	@Override
	public boolean equals(Object o)
	{
		return this == o;
	}

	@Override
	public int hashCode()
	{
		return o();
	}

	@Override
	public String toString()
	{
		return String.format("(%d,%d)", x, y);
	}
}
