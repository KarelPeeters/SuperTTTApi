package com.flaghacker.uttt.common;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JSONBoardUtil
{
	private static final byte PLAYER_JSON = 1;
	private static final byte ENEMY_JSON = -1;
	private static final byte NEUTRAL_JSON = 0;

	public static JSONObject boardToJSON(Board board)
	{
		JSONObject json = new JSONObject();

		//single values
		json.put("nextPlayer", board.nextPlayer());
		json.put("wonBy", board.wonBy());
		json.put("done", board.isDone());
		json.put("singleMacro", board.singleMacro());
		json.put("lastMove", board.getLastMove() == null ? null : board.getLastMove().o());

		//coord lists
		JSONArray moves = new JSONArray();
		for (Coord coord : board.availableMoves())
			moves.put(coord.o());
		json.put("availableMoves", moves);

		JSONArray freeTiles = new JSONArray();
		for (Coord coord : board.freeTiles())
			freeTiles.put(coord.o());
		json.put("freeTiles", freeTiles);

		//tiles
		JSONArray tiles = new JSONArray();
		for (int o = 0; o < 9 * 9; o++)
			tiles.put(board.tile(Coord.coord(o)));

		JSONArray macros = new JSONArray();
		for (int om = 0; om < 9; om++)
			macros.put(board.macro(om));

		json.put("tiles", tiles);
		json.put("macros", macros);

		return json;
	}

	public static void checkMatch(Board board, JSONObject exp)
	{
		//single values
		assertEquals(jsonToBoardPlayer(exp.getInt("nextPlayer")), board.nextPlayer());
		assertEquals(jsonToBoardPlayer(exp.getInt("wonBy")), board.wonBy());
		assertEquals(exp.getBoolean("done"), board.isDone());
		assertEquals(exp.get("singleMacro"), board.singleMacro());

		if (exp.isNull("lastMove"))
			assertNull(board.getLastMove());
		else
			assertEquals(Coord.coord(exp.getInt("lastMove")), board.getLastMove());

		//coord lists
		List<Coord> moves = board.availableMoves();
		List<Coord> expectedMoves = arrToCoordList(exp.getJSONArray("availableMoves"));
		assertEquals(expectedMoves, moves);
		List<Coord> free = board.freeTiles();
		List<Coord> expectedFree = arrToCoordList(exp.getJSONArray("freeTiles"));
		assertEquals(expectedFree, free);

		//tiles
		JSONArray tiles = exp.getJSONArray("tiles");
		for (int o = 0; o < 9 * 9; o++)
			assertEquals(jsonToBoardPlayer(tiles.getInt(o)), board.tile(Coord.coord(o)));

		JSONArray macros = exp.getJSONArray("macros");
		for (int om = 0; om < 9; om++)
			assertEquals(jsonToBoardPlayer(macros.getInt(om)), board.macro(om));
	}

	private static List<Coord> arrToCoordList(JSONArray arr)
	{
		List<Coord> coords = new ArrayList<>();
		for (int i = 0; i < arr.length(); i++)
			coords.add(Coord.coord(arr.getInt(i)));
		return coords;
	}

	private static byte jsonToBoardPlayer(int jsonPlayer)
	{
		switch (jsonPlayer)
		{
			case PLAYER_JSON:
				return Board.PLAYER;
			case ENEMY_JSON:
				return Board.ENEMY;
			case NEUTRAL_JSON:
				return Board.NEUTRAL;
			default:
				throw new IllegalArgumentException(jsonPlayer + " is not a valid JSON player");
		}
	}
}
