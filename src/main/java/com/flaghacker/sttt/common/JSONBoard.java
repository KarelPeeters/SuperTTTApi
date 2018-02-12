/*
package com.flaghacker.sttt.common;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.flaghacker.sttt.common.Player.NEUTRAL;
import static com.flaghacker.sttt.common.PlayerKt.fromNiceString;

public class JSONBoard
{
	public static Board fromJSON(JSONObject json)
	{
		String tiles = json.getString("tiles");
		if (tiles.length() != 81)
			throw new IllegalArgumentException(tiles + " length must be 81");

		String nextMacros = json.getString("nextMacros");
		if (nextMacros.length() != 9)
			throw new IllegalArgumentException(nextMacros + " length must be 9");

		Player nextPlayer = fromNiceString(json.getString("nextPlayer"));
		//Byte lastMove = coordFromJSON(json.getJSONArray("lastMove"));

		Board board = new Board();

		for (int o = 0; o < 81; o++)
		{
			Player tile = fromNiceString(String.valueOf(tiles.charAt(o)));

			if (tile == NEUTRAL)
				continue;

			board.setNextPlayer(tile);
			board.enableAllMacros();

			board.play(Coord.coord(o));
		}

		for (int om = 0; om < 9; om++)
			board.setNextMacro(om, nextMacros.charAt(om) == '1');

		board.setNextPlayer(nextPlayer);
		board.setLastMove(lastMove);

		return board;
	}

	public static JSONObject toJSON(Board board)
	{
		JSONObject json = new JSONObject();

		String tiles = "";
		for (Coord coord : Coord.list())
			tiles += board.tile(coord).toNiceString();
		json.put("tiles", tiles);

		String nextMacros = "";
		for (int om = 0; om < 9; om++)
			nextMacros += board.nextMacro(om) ? "1" : "0";
		json.put("nextMacros", nextMacros);

		json.put("nextPlayer", board.nextPlayer().toNiceString());
		json.put("lastMove", coordToJSON(board.lastMove()));

		return json;
	}

	private static JSONArray coordToJSON(Coord coord)
	{
		JSONArray json = new JSONArray();

		if (coord != null)
		{
			json.put(coord.x());
			json.put(coord.y());
		}

		return json;
	}

	private static Coord coordFromJSON(JSONArray json)
	{
		if (json.length() == 0)
			return null;
		if (json.length() != 2)
			throw new IllegalArgumentException(json + " length must be 2 or 0");

		return Coord.coord(json.getInt(0), json.getInt(1));
	}
}

*/
