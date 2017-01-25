package com.flaghacker.uttt.common;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JSONBoardUtil
{
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
		board.availableMoves().stream().map(Coord::o).forEach(moves::put);
		json.put("availableMoves", moves);

		JSONArray freeTiles = new JSONArray();
		board.freeTiles().stream().map(Coord::o).forEach(freeTiles::put);
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
		assertEquals(exp.getInt("nextPlayer"), board.nextPlayer());
		assertEquals(exp.getInt("wonBy"), board.wonBy());
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
			assertEquals(tiles.getInt(o), board.tile(Coord.coord(o)));

		JSONArray macros = exp.getJSONArray("macros");
		for (int om = 0; om < 9; om++)
			assertEquals(macros.getInt(om), board.macro(om));
	}

	private static List<Coord> arrToCoordList(JSONArray arr)
	{
		List<Coord> coords = new ArrayList<>();
		for (int i = 0; i < arr.length(); i++)
			coords.add(Coord.coord(arr.getInt(i)));
		return coords;
	}
}
