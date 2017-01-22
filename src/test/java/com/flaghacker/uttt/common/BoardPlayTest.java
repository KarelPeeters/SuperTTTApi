package com.flaghacker.uttt.common;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(Parameterized.class)
public class BoardPlayTest
{
	private final PlayTrough play;

	public BoardPlayTest(PlayTrough play)
	{
		this.play = play;
	}

	public static final String PLAYTROUGHS_LOCATION = "playtroughs.json";

	@Parameterized.Parameters
	public static List<PlayTrough> loadPlayTroughs()
	{
		try
		{
			URL resource = BoardPlayTest.class.getResource(PLAYTROUGHS_LOCATION);
			JSONArray json = new JSONArray(IOUtils.toString(resource));

			return IntStream.iterate(0, i -> i++).limit(json.length())
					.mapToObj(json::getJSONArray)
					.map(PlayTrough::new)
					.collect(Collectors.toList());
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testPlayTrough()
	{
		play.check();
	}

	public static void savePlayTroughs(List<PlayTrough> playTroughs, OutputStream out) throws IOException
	{
		JSONArray arr = new JSONArray();
		playTroughs.stream()
				.map(PlayTrough::toJSON)
				.forEach(arr::put);
		IOUtils.write(arr.toString(0), out);
	}

	public static class PlayTrough
	{
		private List<Coord> moves = new ArrayList<>();
		private List<JSONObject> expected = new ArrayList<>();

		public PlayTrough(JSONArray json)
		{
			for (int i = 0; i < json.length(); i++)
			{
				JSONObject obj = json.getJSONObject(i);

				if (i > 0)
					moves.add(Coord.coord(obj.getInt("move")));
				expected.add(obj.getJSONObject("expected"));
			}

			if (expected.size() != moves.size() + 1)
				throw new IllegalArgumentException();
		}

		public PlayTrough(List<Board> boards)
		{
			for (int i = 0; i < boards.size(); i++)
			{
				Board board = boards.get(i);
				if (i > 0)
					moves.add(board.getLastMove());
				expected.add(boardToJSON(board));
			}
		}

		public void check()
		{
			Board board = new Board();

			for (int i = 0; i < expected.size(); i++)
			{
				checkMatch(board, expected.get(i));

				if (i < moves.size())
					board.play(moves.get(i), board.nextPlayer());
			}
		}

		public JSONArray toJSON()
		{
			JSONArray json = new JSONArray();

			for (int i = 0; i < expected.size(); i++)
			{
				JSONObject obj = new JSONObject();
				obj.put("expected", expected.get(i));
				if (i > 0)
					obj.put("move", moves.get(i - 1).o());
				json.put(obj);
			}

			return json;
		}

		private void checkMatch(Board board, JSONObject exp)
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

		private List<Coord> arrToCoordList(JSONArray arr)
		{
			List<Coord> coords = new ArrayList<>();
			for (int i = 0; i < arr.length(); i++)
				coords.add(Coord.coord(arr.getInt(i)));
			return coords;
		}

		private JSONObject boardToJSON(Board board)
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
	}
}