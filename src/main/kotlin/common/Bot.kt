package common

import java.io.Serializable

interface Bot : Serializable {
	fun move(board: Board, percentDone: (Int) -> Unit): Coord
	fun cancel()
}
