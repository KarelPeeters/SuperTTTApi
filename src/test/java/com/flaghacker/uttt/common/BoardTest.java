package com.flaghacker.uttt.common;

import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(Theories.class)
public class BoardTest
{
	@Test
	public void testOther()
	{
		assertEquals(Board.PLAYER, Board.other(Board.ENEMY));
		assertEquals(Board.ENEMY, Board.other(Board.PLAYER));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOtherNeutralFails()
	{
		Board.other(Board.NEUTRAL);
	}

	@Test
	public void testPlayRemembered()
	{
		final Coord move = Coord.coord(4, 4);
		Board board = new Board();

		byte player = board.nextPlayer();
		board.play(move, board.nextPlayer());

		assertEquals(player, board.tile(move));
	}

	@DataPoints
	public static byte[] players = {Board.PLAYER, Board.ENEMY};

	@Theory
	public void testManhattanWin(byte player)
	{
		for (int om = 0; om < 9; om++)
		{
			for (int i = 0; i < 3; i++)
			{
				assertEquals(
						String.format("horizontal %d in macro %d", i, om),
						player, playedBoard(player, 0, i, 1, i, 2, i).macro(0));
				assertEquals(
						String.format("vertical %d in macro %d", i, om),
						player, playedBoard(player, i, 0, i, 1, i, 2).macro(0));
			}
		}
	}

	@Theory
	public void testDiagonalWin(byte player)
	{
		for (int om = 0; om < 9; om++)
		{
			assertEquals(
					String.format("diagonal / in macro %d", om),
					player, playedBoard(player, 0, 0, 1, 1, 2, 2).macro(0));
			assertEquals(
					String.format("diagonal \\ in macro %d", om),
					player, playedBoard(player, 0, 2, 1, 1, 2, 0).macro(0));
		}
	}

	@Theory
	public void testFullWin(byte player)
	{
		assertEquals(
				"macro top line",
				player, playedBoard(player, 0, 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0, 7, 0, 8, 0).wonBy());
	}

	private Board playedBoard(byte player, int... moves)
	{
		Board board = new Board();
		playMoves(board, player, moves);
		return board;
	}

	private void playMoves(Board board, byte player, int... moves)
	{
		if (moves.length % 2 != 0)
			throw new IllegalArgumentException();

		for (int i = 0; i < moves.length / 2; i++)
		{
			Coord move = Coord.coord(moves[2 * i], moves[2 * i + 1]);

			board.setNextPlayer(player);
			board.setNextMacro(move.om(), true);
			board.play(move, player);
		}
	}
}
