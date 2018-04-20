/*
package com.flaghacker.sttt.common;

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

			List<PlayTrough> playTroughs = new ArrayList<>();
			for (int i = 0; i < json.length(); i++)
				playTroughs.add(new PlayTrough(json.getJSONArray(i)));

			return playTroughs;
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
		for (PlayTrough playTrough : playTroughs)
			arr.put(playTrough.toJSON());

		IOUtils.write(arr.toString(0), out);
	}

	public static class PlayTrough
	{
		private List<Byte> moves = new ArrayList<>();
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
				expected.add(JSONBoardUtil.boardToJSON(board));
			}
		}

		public void check()
		{
			Board board = new Board();

			for (int i = 0; i < expected.size(); i++)
			{
				JSONBoardUtil.checkMatch(board, expected.get(i));

				if (i < moves.size())
					board.play(moves.get(i));
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

		public List<Byte> getMoves()
		{
			return moves;
		}
	}
}
*/
