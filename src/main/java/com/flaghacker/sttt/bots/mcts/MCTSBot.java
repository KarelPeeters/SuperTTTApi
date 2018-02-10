package com.flaghacker.sttt.bots.mcts;

import com.flaghacker.sttt.common.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public class MCTSBot implements Bot
{
	private static final long serialVersionUID = 6534256310842724239L;

	private Random rand = new Random();
	private Settings settings;

	private BoardInfo info;

	public MCTSBot(Settings settings)
	{
		this.settings = settings;
	}

	@Override
	public Byte move(@NotNull Board board, @NotNull Timer timer)
	{
		List<Byte> moves = board.availableMoves();
		if (moves.size() == 1)
			return moves.get(0);

		try
		{
			setup(board);

			simulateAndUpdate(board, 0);

			while (timer.running())
			{
				searchIteration();
			}

			Byte move = info.selectBestMove();

			if (moves.contains(move))
				return move;

			System.err.println(String.format("Illegal move: %s not in %s, playing randomly", move, moves));
			debug(board);
		}
		catch (Throwable e)
		{
			System.err.println("Encountered error, playing randomly");
			e.printStackTrace();

			debug(board);
		}

		return moves.get(rand.nextInt(moves.size()));
	}

	private void debug(Board board)
	{
		System.err.println("debug");
	}

	private void setup(Board board)
	{
		info = new BoardInfo(settings);
		info.inc(board, 0, Player.NEUTRAL, null);
	}

	private void searchIteration()
	{
		//System.out.println(info);

		BoardInfo.Info next = info.selectBest();
		simulateAndUpdate(next.board, next.depth);
	}

	private void simulateAndUpdate(Board board, int depth)
	{
		Player wonBy;
		Board start = board;
		Board firstChoice = null;

		while (true)
		{
			board = board.copy();

			List<Byte> moves = board.availableMoves();
			Byte move = moves.get(rand.nextInt(moves.size()));
			board.play(move);

			if (firstChoice == null)
				firstChoice = board;

			if (board.isDone())
			{
				wonBy = board.wonBy();
				break;
			}
		}

		update(start, firstChoice, wonBy, depth+1);
	}

	private void update(Board prev, Board board, Player wonBy, int depth)
	{
		info.incTotal();

		BoardInfo.Info nInfo = this.info.inc(board, depth, wonBy, prev == null ? null : info.getInfo(prev));
		this.info.incAllPrevious(nInfo, wonBy);
	}

	@Override
	public String toString()
	{
		return "MCTSBot";
	}

	private void log(Object object)
	{
		if (settings.log())
			System.err.println(String.valueOf(object));
	}
}
