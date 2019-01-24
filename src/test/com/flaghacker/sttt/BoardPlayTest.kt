package com.flaghacker.sttt

import com.flaghacker.sttt.common.Board
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.junit.jupiter.api.Test
import java.io.InputStreamReader
import java.io.Writer
import java.lang.reflect.Type
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.text.Charsets.UTF_8

const val PLAYTHROUGHS_FILE = "playthroughs.json.gzip"

typealias Playthrough = List<State>

val PLAYTHROUGH_TYPE: Type = object : TypeToken<Playthrough>() {}.type

class BoardPlayTest {
	@Test
	fun testPlaythroughs() {
		var i = 0
		for (playthrough in loadPlaythroughs()) {
			println(i++)
			checkPlaythrough(playthrough)
		}
	}

	private fun checkPlaythrough(playthrough: Playthrough) {
		val board = Board()

		for (state in playthrough) {
			if (state.move != null)
				board.play(state.move.toByte())
			assertBoardMatches(state.expected, board)
		}
	}
}

private fun loadPlaythroughs(): Sequence<Playthrough> = sequence {
	val gson = Gson()

	val input = BoardPlayTest::class.java.getResourceAsStream(PLAYTHROUGHS_FILE)
	val gis = GZIPInputStream(input)

	JsonReader(InputStreamReader(gis, UTF_8)).use { reader ->
		reader.beginArray()
		while (reader.hasNext()) {
			val playthrough = gson.fromJson<Playthrough>(reader, PLAYTHROUGH_TYPE)
			yield(playthrough)
		}
		reader.endArray()
	}
}

@Suppress("unused")
object GeneratePlaythroughs {
	fun generatePlaythroughs(): Sequence<Playthrough> {
		val random = Random(0)
		return generateSequence {
			val board = Board()
			sequence {
				yield(State(null, board.toExpected()))
				while (!board.isDone) {
					val move = board.randomAvailableMove(random)
					board.play(move)
					yield(State(move, board.toExpected()))
				}
			}.toList()
		}
	}

	fun playthroughsToJSON(playthroughs: Sequence<Playthrough>, out: Writer) {
		val gson = Gson()
		val writer = JsonWriter(out)

		writer.beginArray()
		for (playthrough in playthroughs)
			gson.toJson(playthrough, PLAYTHROUGH_TYPE, writer)
		writer.endArray()

		writer.close()
	}
}