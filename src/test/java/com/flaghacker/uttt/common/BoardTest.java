package com.flaghacker.uttt.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
}
