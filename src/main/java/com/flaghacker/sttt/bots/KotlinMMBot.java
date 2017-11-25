package com.flaghacker.sttt.bots;

import com.flaghacker.sttt.common.KotlinBoard;
import com.flaghacker.sttt.common.KotlinBot;
import com.flaghacker.sttt.common.KotlinPlayer;
import com.flaghacker.sttt.common.Timer;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Math.max;

@SuppressWarnings("Duplicates")
public class KotlinMMBot implements KotlinBot
{
	private final int depth;

	public KotlinMMBot(int depth)
	{
		this.depth = depth;
	}

	@Override
	public Byte move(KotlinBoard board, Timer timer)
	{
		if (board.nextPlayer() == KotlinPlayer.ENEMY)
			board = board.flip();

		return negaMax(board, depth+1, NEGATIVE_INFINITY, POSITIVE_INFINITY, 1).move;
	}

	private ValuedMove negaMax(KotlinBoard board, int depth, double a, double b, int player)
	{
		if (depth == 0 || board.isDone())
			return new ValuedMove(board.getLastMove(), player * value(board));

		List<KotlinBoard> children = children(board);

		double bestValue = NEGATIVE_INFINITY;
		Byte bestMove = null;

		for (KotlinBoard child : children)
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

	private List<KotlinBoard> children(KotlinBoard board)
	{
		List<Byte> moves = board.availableMoves();
		List<KotlinBoard> children = new ArrayList<>(moves.size());

		for (Byte move : moves)
		{
			KotlinBoard child = board.copy();
			child.play(move);
			children.add(child);
		}

		return children;
	}

	private static class ValuedMove
	{
		public final Byte move;
		public final double value;

		public ValuedMove(Byte move, double value)
		{
			this.move = move;
			this.value = value;
		}
	}

	private double value(KotlinBoard board)
	{
		if (board.isDone())
			return Double.POSITIVE_INFINITY * playerSign(board.wonBy());

		double value = 0;

		for (int i = 0; i < 80; i++)
			value += TILE_VALUE * tileFactor(i%9) * tileFactor(i/9) * playerSign(board.tile(i));

		for (int om = 0; om < 9; om++)
			value += MACRO_VALUE * tileFactor(om) * playerSign(board.macro(om));

		return value;
	}

	private static final double TILE_VALUE = 1;
	private static final double MACRO_VALUE = 10e9;

	private static final double CENTER_FACTOR = 4;
	private static final double CORNER_FACTOR = 3;
	private static final double EDGE_FACTOR = 1;

	private int playerSign(KotlinPlayer player)
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
