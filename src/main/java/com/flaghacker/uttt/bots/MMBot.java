package com.flaghacker.uttt.bots;

import com.flaghacker.uttt.common.Board;
import com.flaghacker.uttt.common.Bot;
import com.flaghacker.uttt.common.Coord;
import com.flaghacker.uttt.common.Player;
import com.flaghacker.uttt.common.Timer;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Math.max;

public class MMBot implements Bot
{
	private final int depth;

	public MMBot(int depth)
	{
		this.depth = depth;
	}

	@Override
	public Coord move(Board board, Timer timer)
	{
		if (board.nextPlayer() == Player.ENEMY)
			board = board.flip();

		double bestValue = 0;
		Coord bestMove = null;

		for (Coord move : board.availableMoves())
		{
			Board next = board.copy();
			next.play(move);

			double value = -negaMax(next, depth, NEGATIVE_INFINITY, POSITIVE_INFINITY, -1);

			if (bestMove == null || value > bestValue)
			{
				bestValue = value;
				bestMove = move;
			}
		}

		return bestMove;
	}

	private double negaMax(Board board, int depth, double a, double b, int player)
	{
		if (depth == 0 || board.isDone())
			return player * value(board);

		List<Board> children = children(board);

		double bestValue = NEGATIVE_INFINITY;

		for (Board child : children)
		{
			double value = -negaMax(child, depth - 1, -b, -a, -player);

			bestValue = max(bestValue, value);
			a = max(a, value);
			if (a >= b)
				break;
		}

		return bestValue;
	}

	private List<Board> children(Board board)
	{
		List<Coord> moves = board.availableMoves();
		List<Board> children = new ArrayList<>(moves.size());

		for (Coord move : moves)
		{
			Board next = board.copy();
			next.play(move);
			children.add(next);
		}

		return children;
	}

	private double value(Board board)
	{
		if (board.isDone())
			return Double.POSITIVE_INFINITY * playerSign(board.wonBy());

		double value = 0;

		for (Coord coord : Coord.list())
			value += TILE_VALUE * tileFactor(coord.os()) * tileFactor(coord.om()) * playerSign(board.tile(coord));

		for (int om = 0; om < 9; om++)
			value += MACRO_VALUE * tileFactor(om) * playerSign(board.macro(om));

		return value;
	}

	private static final double TILE_VALUE = 1;
	private static final double MACRO_VALUE = 10e9;

	private static final double CENTER_FACTOR = 4;
	private static final double CORNER_FACTOR = 3;
	private static final double EDGE_FACTOR = 1;

	private int playerSign(Player player)
	{
		switch (player)
		{
			case NEUTRAL:
				return 0;
			case PLAYER:
				return 1;
			case ENEMY:
				return -1;
		}

		throw new IllegalStateException();
	}

	private double tileFactor(int o)
	{
		int x = o % 3;
		int y = o / 3;

		if (x == 1 && y == 1)
			return CENTER_FACTOR;
		if (x == 1 || y == 1)
			return EDGE_FACTOR;
		return CORNER_FACTOR;
	}

	@Override
	public String toString()
	{
		return "MMBot";
	}
}
