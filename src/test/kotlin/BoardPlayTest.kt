package com.flaghacker.sttt

import com.flaghacker.sttt.GeneratePlaythroughs.generatePlaythroughs
import com.flaghacker.sttt.GeneratePlaythroughs.playthroughsToJSON
import common.Board
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.junit.jupiter.api.Test
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.Writer
import java.lang.Exception
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
		val limit = System.getProperty("playthroughLimit")?.toIntOrNull() ?: Int.MAX_VALUE

		for (playthrough in loadPlaythroughs().take(limit))
			checkPlaythrough(playthrough)
	}

	private fun checkPlaythrough(playthrough: Playthrough) {
		val board = Board(randomizeTie = false)

		for (state in playthrough) {
			if (state.move != (-1).toByte()) {
				board.play(state.move)
			}
			assertBoardMatches(state.expected, board)
		}
	}
}

private fun loadPlaythroughs(): Sequence<Playthrough> = sequence {
	val gson = Gson()

	val input = javaClass.getResourceAsStream("/playthroughs.json.gz")

	if (input == null) {
		throw Exception("NULL")
	} else if (input.available() == 0) {
		throw Exception("ZERO")
	}
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
			val board = Board(randomizeTie = false)
			sequence {
				yield(State(-1, board.toExpected()))
				while (!board.isDone) {
					val moves = board.availableMoves
					val move = moves[random.nextInt(moves.size)]
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