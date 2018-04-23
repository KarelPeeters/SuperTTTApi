import com.flaghacker.sttt.bots.RandomBot
import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.BoardPlayTest
import com.flaghacker.sttt.common.Timer
import org.apache.commons.io.IOUtils
import org.json.JSONArray
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

fun main(args: Array<String>) {

}

fun generateNewPlaythroughs(){
	val playTroughs = mutableListOf<BoardPlayTest.PlayTrough>()

	repeat(10000) {
		val board = Board()
		val boards = mutableListOf(board.copy())
		while (!board.isDone) {
			board.play(RandomBot().move(board, Timer(30))!!)
			boards.add(board.copy())
		}
		playTroughs.add(BoardPlayTest.PlayTrough(boards))
	}

	val out = FileOutputStream("${System.getProperty("user.dir")}\\${BoardPlayTest.PLAYTROUGHS_LOCATION}")
	BoardPlayTest.savePlayTroughs(playTroughs, out)
}