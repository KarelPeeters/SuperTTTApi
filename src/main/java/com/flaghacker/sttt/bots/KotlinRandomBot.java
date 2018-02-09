package com.flaghacker.sttt.bots;

import com.flaghacker.sttt.common.KotlinBoard;
import com.flaghacker.sttt.common.KotlinBot;
import com.flaghacker.sttt.common.Timer;

import java.util.List;
import java.util.Random;

public class KotlinRandomBot implements KotlinBot
{
	private final Random random;

	public KotlinRandomBot()
	{
		random = new Random();
	}

	public KotlinRandomBot(int seed)
	{
		random = new Random(seed);
	}

	@Override
	public Byte move(KotlinBoard board, Timer timer)
	{
		List<Byte> moves = board.availableMoves();
		return moves.get(random.nextInt(moves.size()));
	}

	@Override
	public String toString()
	{
		return "RandomBot";
	}
}
