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
import java.util.*
import java.util.zip.GZIPInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream




@RunWith(Parameterized::class)
class BoardPlayTest(private val play: PlayTrough) {
	@Test
	fun testPlayTrough() {
		play.check()
	}

	class PlayTrough {
		private val moves = mutableListOf<Byte>()
		private val expected = mutableListOf<JSONObject>()

		constructor(json: JSONArray) {
			for (i in 0 until json.length()) {
				val obj = json.getJSONObject(i)

				if (i > 0) moves.add(obj.getInt("move").toByte())
				expected.add(obj.getJSONObject("expected"))
			}

			if (expected.size != moves.size + 1) throw IllegalArgumentException()
		}

		constructor(boards: List<Board>) {
			for (i in boards.indices) {
				val board = boards[i]
				if (i > 0) moves.add(board.lastMove!!)
				expected.add(boardToJSON(board))
			}
		}

		fun check() {
			val board = Board()

			for (i in expected.indices) {
				checkMatch(board, expected[i])
				if (i < moves.size) board.play(moves[i])
			}
		}

		fun toJSON(): JSONArray {
			val json = JSONArray()

			for (i in expected.indices) {
				val obj = JSONObject()
				obj.put("expected", expected[i])
				if (i > 0) obj.put("move", moves[i - 1])
				json.put(obj)
			}

			return json
		}
	}

	companion object {
		val PLAYTROUGHS_LOCATION = "playtroughs.json"

		@JvmStatic
		@Parameterized.Parameters
		fun loadPlayTroughs(): List<PlayTrough> {
			try {
				val resource = BoardPlayTest::class.java.getResource(PLAYTROUGHS_LOCATION)
				val json = JSONArray(decompress(IOUtils.toByteArray(resource)))

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

			IOUtils.write(compress(arr.toString()), out)
		}

		@Throws(IOException::class)
		fun compress(data: String): ByteArray {
			val bos = ByteArrayOutputStream(data.length)
			val gzip = GZIPOutputStream(bos)
			gzip.write(data.toByteArray(StandardCharsets.UTF_8))
			gzip.close()
			val compressed = bos.toByteArray()
			bos.close()
			return compressed
		}

		@Throws(IOException::class)
		fun decompress(compressed: ByteArray): String {
			val bis = ByteArrayInputStream(compressed)
			val gis = GZIPInputStream(bis)
			val bytes = IOUtils.toByteArray(gis)
			return String(bytes, StandardCharsets.UTF_8)
		}
	}
}
