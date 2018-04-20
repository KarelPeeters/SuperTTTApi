/*
package com.flaghacker.sttt.common

import boardToJSON
import checkMatch
import org.apache.commons.io.IOUtils
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.io.IOException
import java.io.OutputStream
import java.net.URL
import java.util.ArrayList

@RunWith(Parameterized::class)
class BoardPlayTest(private val play: PlayTrough) {

	@Test
	fun testPlayTrough() {
		play.check()
	}

	class PlayTrough {
		private val moves = ArrayList<Byte>()
		private val expected = ArrayList<JSONObject>()

		constructor(json: JSONArray) {
			for (i in 0 until json.length()) {
				val obj = json.getJSONObject(i)

				if (i > 0)
					moves.add(Coord.coord(obj.getInt("move")))
				expected.add(obj.getJSONObject("expected"))
			}

			if (expected.size != moves.size + 1)
				throw IllegalArgumentException()
		}

		constructor(boards: List<Board>) {
			for (i in boards.indices) {
				val board = boards[i]
				if (i > 0)
					moves.add(board.lastMove?:-1)
				expected.add(boardToJSON(board))
			}
		}

		fun check() {
			val board = Board()

			for (i in expected.indices) {
				checkMatch(board, expected[i])

				if (i < moves.size)
					board.play(moves[i])
			}
		}

		fun toJSON(): JSONArray {
			val json = JSONArray()

			for (i in expected.indices) {
				val obj = JSONObject()
				obj.put("expected", expected[i])
				if (i > 0)
					obj.put("move", moves[i - 1].o())
				json.put(obj)
			}

			return json
		}

		fun getMoves(): List<Byte> {
			return moves
		}
	}

	companion object {
		val PLAYTROUGHS_LOCATION = "playtroughs.json"

		@Parameterized.Parameters
		fun loadPlayTroughs(): List<PlayTrough> {
			try {
				val resource = BoardPlayTest::class.java.getResource(PLAYTROUGHS_LOCATION)
				val json = JSONArray(IOUtils.toString(resource))

				val playTroughs = ArrayList<PlayTrough>()
				for (i in 0 until json.length())
					playTroughs.add(PlayTrough(json.getJSONArray(i)))

				return playTroughs
			} catch (e: IOException) {
				throw RuntimeException(e)
			}

		}

		@Throws(IOException::class)
		fun savePlayTroughs(playTroughs: List<PlayTrough>, out: OutputStream) {
			val arr = JSONArray()
			for (playTrough in playTroughs)
				arr.put(playTrough.toJSON())

			IOUtils.write(arr.toString(0), out)
		}
	}
}
*/
