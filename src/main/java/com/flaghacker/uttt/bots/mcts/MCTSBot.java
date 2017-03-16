package com.flaghacker.uttt.bots.mcts;

import com.flaghacker.uttt.common.Board;
import com.flaghacker.uttt.common.Bot;
import com.flaghacker.uttt.common.Coord;
import com.flaghacker.uttt.common.Player;
import com.flaghacker.uttt.common.Timer;
import com.flaghacker.uttt.common.Util;

import java.util.List;
import java.util.Random;

import static com.flaghacker.uttt.common.Player.NEUTRAL;

public class MCTSBot implements Bot
{
	private static final long serialVersionUID = 6534256310842724239L;

	private Random rand = Util.loggedRandom();
	private Settings settings;

	private BoardInfo info;

	public MCTSBot(Settings settings)
	{
		this.settings = settings;
	}

	@Override
	public Coord move(Board board, Timer timer)
	{
		int iterations = 0;

		List<Coord> moves = board.availableMoves();
		if (moves.size() == 1)
			return moves.get(0);

		try
		{
			setup(board);

			simulateAndUpdate(board, 0);

			while (timer.running())
			{
				searchIteration();
				iterations++;
			}

			log(iterations);

			Coord move = info.selectBestMove();

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
		info.inc(board, 0, NEUTRAL, null);
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

			List<Coord> moves = board.availableMoves();
			Coord move = moves.get(rand.nextInt(moves.size()));
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
