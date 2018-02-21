package com.flaghacker.sttt.common;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Random;

import static com.flaghacker.sttt.common.Player.ENEMY;
import static com.flaghacker.sttt.common.Player.NEUTRAL;
import static com.flaghacker.sttt.common.Player.PLAYER;
import static org.junit.Assert.assertEquals;

@RunWith(Theories.class)
public class BoardTest {
	@Test
	public void testOther() {
		assertEquals(PLAYER, ENEMY.other());
		assertEquals(ENEMY, PLAYER.other());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOtherNeutralFails() {
		NEUTRAL.other();
	}

	@Test
	public void testPlayRemembered() {
		final Byte move = 40;//TODO coord(4, 4);
		Board board = new Board();

		Player player = board.nextPlayer();
		board.play(move);

		assertEquals(player, board.tile(move));
	}

	@Test
	public void testSerializedEquals() {
		Board board = randomBoard(new Random(0), 10);
		assertEquals(board, SerializationUtils.clone(board));
	}

	@Test
	public void testDoubleFlip() {
		Board board = randomBoard(new Random(0), 10);
		assertEquals(board, board.flip().flip());
	}

	@DataPoints
	public static Player[] players = {PLAYER, ENEMY};

/*	@Theory
	public void testManhattanWin(Player player)
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
	}*/

/*	@Theory
	public void testDiagonalWin(Player player)
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
	}*/

/*	@Theory
	public void testFullWin(Player player)
	{
		assertEquals(
				"macro top line",
				player, playedBoard(player, 0, 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0, 7, 0, 8, 0).wonBy());
	}*/

/*	public static Board playedBoard(Player player, int ... moves)
	{
		Board board = new Board();
		playMoves(board, player, moves);
		return board;
	}*/

/*	public static void playMoves(Board board, Player player, int ... moves)
	{
		if (moves.length % 2 != 0)
			throw new IllegalArgumentException();

		for (int i = 0; i < moves.length / 2; i++)
		{
			Coord move = Coord.coord(moves[2 * i], moves[2 * i + 1]);

			board.setNextPlayer(player);
			board.setNextMacro(move.om(), true);
			board.play(move);
		}
	}*/

	public static Board randomBoard(Random rand, int moves) {
		return playRandom(new Board(), rand, moves);
	}
	public static Board playRandom(Board startBoard, Random rand, int moveCount) {
		for (int i = 0; i < moveCount && !startBoard.isDone(); i++) {
			List<Byte> available = startBoard.availableMoves();
			startBoard.play(available.get(rand.nextInt(available.size())));
		}
		return startBoard;
	}
}
