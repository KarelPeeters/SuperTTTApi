package com.flaghacker.sttt.bots;

import com.flaghacker.sttt.common.*;

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

		return negaMax(board, depth+1, NEGATIVE_INFINITY, POSITIVE_INFINITY, 1).move;
	}

	private ValuedMove negaMax(Board board, int depth, double a, double b, int player)
	{
		if (depth == 0 || board.isDone())
			return new ValuedMove(board.getLastMove(), player * value(board));

		List<Board> children = children(board);

		double bestValue = NEGATIVE_INFINITY;
		Coord bestMove = null;

		for (Board child : children)
		{
			double value = -negaMax(child, depth - 1, -b, -a, -player).value;

			if (value > bestValue || bestMove == null)
			{
				bestValue = value;
				bestMove = child.getLastMove();
			}
			a = max(a, value);
			if (a >= b)
				break;
		}

		return new ValuedMove(bestMove, bestValue);
	}

	private List<Board> children(Board board)
	{
		List<Coord> moves = board.availableMoves();
		List<Board> children = new ArrayList<>(moves.size());

		for (Coord move : moves)
		{
			Board child = board.copy();
			child.play(move);
			children.add(child);
		}

		return children;
	}

	private static class ValuedMove
	{
		public final Coord move;
		public final double value;

		public ValuedMove(Coord move, double value)
		{
			this.move = move;
			this.value = value;
		}
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
