package com.flaghacker.sttt.common;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class MultiBoardTest
{
	private static Random random = new Random(0);
	private Board board;

	public MultiBoardTest(Board board)
	{
		this.board = board;
	}

	@Parameterized.Parameters
	public static List<Board> data()
	{
		List<Board> list = new ArrayList<>();

		for (int i = 0; i < 10; i++)
		{
			Board board = new Board();
			BoardTest.randomBoard(random,i);
			list.add(board);
		}

		return list;
	}

/*	@Test
	public void testCopySeparate()
	{
		JSONObject origJSON = JSONBoardUtil.boardToJSON(board);
		Board copy = board.copy();

		play(copy, 15);
		assertNotEquals(board, copy);
		JSONBoardUtil.checkMatch(board, origJSON);
	}*/

	@Test
	public void testCopyEqualsHashcode()
	{
		Board copy = board.copy();

		assertTrue(copy.equals(board) && board.equals(copy));
		assertEquals(board.hashCode(), copy.hashCode());
	}
}
