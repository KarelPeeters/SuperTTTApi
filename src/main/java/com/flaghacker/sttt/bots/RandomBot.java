package com.flaghacker.sttt.bots;

import com.flaghacker.sttt.common.*;

import java.util.List;
import java.util.Random;

public class RandomBot implements Bot
{
	private static final long serialVersionUID = -4978779157732236475L;

	private final Random random;

	public RandomBot()
	{
		random = Util.loggedRandom();
	}

	public RandomBot(int seed)
	{
		random = new Random(seed);
	}

	@Override
	public Coord move(Board board, Timer timer)
	{
		List<Coord> moves = board.availableMoves();
		return moves.get(random.nextInt(moves.size()));
	}

	@Override
	public String toString()
	{
		return "RandomBot";
	}
}