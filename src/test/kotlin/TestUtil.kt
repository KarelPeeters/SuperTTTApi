import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Player
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

private const val PLAYER_JSON = 1
private const val ENEMY_JSON = -1
private const val NEUTRAL_JSON = 0

fun boardToJSON(board: Board): JSONObject {
	val availableMoves = JSONArray()
	val tiles = JSONArray()
	val macros = JSONArray()

	board.availableMoves.forEach { availableMoves.put(it) }
	(0 until 81).forEach { tiles.put(boardToJSONPlayer(board.tile(it.toByte()))) }
	(0 until 9).forEach { macros.put(boardToJSONPlayer(board.macro(it.toByte()))) }

	return JSONObject().apply {
		put("nextPlayer", boardToJSONPlayer(board.nextPlayer))
		put("wonBy", boardToJSONPlayer(board.wonBy))
		put("availableMoves", availableMoves)
		put("lastMove", board.lastMove)
		put("done", board.isDone)
		put("macros", macros)
		put("tiles", tiles)
	}
}

fun checkMatch(board: Board, exp: JSONObject) {
	assertEquals(jsonToBoardPlayer(exp.getInt("nextPlayer")), board.nextPlayer)
	assertEquals(jsonToBoardPlayer(exp.getInt("wonBy")), board.wonBy)
	assertEquals(exp.getBoolean("done"), board.isDone)

	if (exp.isNull("lastMove")) assertNull(board.lastMove)
	else assertEquals(exp.getInt("lastMove"), board.lastMove?.toInt())

	assertEquals(arrToCoordList(exp.getJSONArray("availableMoves")), board.availableMoves.toList())
	(0 until 81).forEach {
		assertEquals(jsonToBoardPlayer(exp.getJSONArray("tiles").getInt(it)), board.tile(it.toByte()))
	}
	(0 until 9).forEach {
		assertEquals(jsonToBoardPlayer(exp.getJSONArray("macros").getInt(it)), board.macro(it.toByte()))
	}
}

private fun arrToCoordList(arr: JSONArray) = (0 until arr.length()).map { arr.getInt(it).toByte() }

private fun jsonToBoardPlayer(jsonPlayer: Int) = when (jsonPlayer) {
	PLAYER_JSON -> Player.PLAYER
	ENEMY_JSON -> Player.ENEMY
	NEUTRAL_JSON -> Player.NEUTRAL
	else -> throw IllegalArgumentException("$jsonPlayer is not a valid JSON player")
}

private fun boardToJSONPlayer(boardPlayer: Player) = when (boardPlayer) {
	Player.PLAYER -> PLAYER_JSON
	Player.ENEMY -> ENEMY_JSON
	Player.NEUTRAL -> NEUTRAL_JSON
	else -> throw IllegalArgumentException("$boardPlayer is not a valid JSON player")
}