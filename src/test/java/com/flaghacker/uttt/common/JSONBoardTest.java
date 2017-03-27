package com.flaghacker.uttt.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JSONBoardTest
{
	@Test
	public void testEmpty()
	{
		Board board = new Board();
		Board result = JSONBoard.fromJSON(JSONBoard.toJSON(board));

		assertEquals(board, result);
	}

	@Test
	public void testRandom()
	{
		Board board = BoardTest.randomBoard(Util.loggedRandom(), 10);
		Board result = JSONBoard.fromJSON(JSONBoard.toJSON(board));

		assertEquals(board, result);
	}
}