@file:Suppress("NOTHING_TO_INLINE")

package com.flaghacker.sttt.common

import java.io.Serializable
import java.lang.IllegalStateException
import java.util.*

typealias Coord = Byte

fun toCoord(x: Int, y: Int) = (((x / 3) + (y / 3) * 3) * 9 + ((x % 3) + (y % 3) * 3)).toByte()
fun Int.toPair() = toByte().toPair()
fun Coord.toPair(): Pair<Int, Int> {
	val om = this / 9
	val os = this % 9
	return Pair((om % 3) * 3 + (os % 3), (om / 3) * 3 + (os / 3))
}

class Board : Serializable {
	companion object {
		val wonGrid: BooleanArray

		init {
			val max = 512
			val arr = listOf(
					0, 1, 2,
					3, 4, 5,
					6, 7, 8,
					0, 3, 6,
					1, 4, 7,
					2, 5, 8,
					0, 4, 8,
					2, 4, 6
			)

			val wonGrid = BooleanArray(max)

			for (grid in 0 until max) {
				wonGrid[grid] = arr.chunked(3) { (a, b, c) ->
					grid.getBit(a) && grid.getBit(b) && grid.getBit(c)
				}.any { it }
			}

			this.wonGrid = wonGrid
		}
	}

	/**
	Each element represents a row of macros (3 rows of 9 tiles)
	The first 3 Ints hold the macros for Player
	The next 3 Ints hold the macros for Enemy

	In each Int the bit representation is as follows:
	aaaaaaaaabbbbbbbbbcccccccccABC with: (least shifted -> most shifted)
	a/b/c: bit enabled if the player is the owner of the tile
	A/B/C: bit enabled if the player won the macro
	 */
	private var rows = IntArray(6) { 0 }
	private var macroMask = 0b111111111
	private var _availableMoves: ByteArray? = null

	var nextPlayer = Player.PLAYER; private set
	var lastMove: Coord? = null; private set
	var wonBy: Player? = null; private set

	val availableMoves get() = availableMoves()
	val isDone get() = wonBy != null

	/** Constructs an empty [Board]. */
	constructor()

	/** Returns a copy of the current board. */
	fun copy() = Board(this)

	private constructor(board: Board) {
		rows = board.rows.copyOf()
		wonBy = board.wonBy
		nextPlayer = board.nextPlayer
		macroMask = board.macroMask
		lastMove = board.lastMove
	}

	/**
	 * Constructs a Board using a 2 dimensional Array of players, a macroMask a lastMove.
	 * @param board 2 dimensional array containing who owns each tile. The format is `board[x][y]`
	 * @param nextPlayer the next player
	 * @param lastMove the [Coord] of the last move played on the board, null if the board is still empty
	 * */
	constructor(board: Array<Array<Player>>, nextPlayer: Player, lastMove: Coord?) {
		if (board.size != 9 && board.all { it.size != 9 } || nextPlayer == Player.NEUTRAL ||
				(lastMove != null && (lastMove < 0 || lastMove > 80)))
			throw IllegalArgumentException("Invalid arguments ($nextPlayer, $lastMove")

		for (i in 0 until 81) {
			val owner = board[i.toPair().first][i.toPair().second]
			if (owner != Player.NEUTRAL) {
				rows[i / 27 + owner.ordinal * 3] += 1 shl i % 27
				if (wonGrid((rows[i / 27 + owner.ordinal * 3] shr (((i / 9) % 3) * 9)) and 0b111111111, i % 9)) {
					rows[i / 27 + owner.ordinal * 3] = rows[i / 27 + owner.ordinal * 3] or (1 shl (27 + (i / 9) % 3))
					if (wonGrid(macroWinGrid(owner), i / 9)) wonBy = nextPlayer
				}
			}
		}

		this.lastMove = lastMove
		this.nextPlayer = nextPlayer
		this.macroMask = if (lastMove == null) 0b111111111 else {
			val winGrid = macroWinGrid(Player.PLAYER) or macroWinGrid(Player.ENEMY)
			val freeMove = winGrid.getBit(lastMove % 9) || macroFull(lastMove % 9)
			if (freeMove) (0b111111111 and winGrid.inv()) else (1 shl (lastMove % 9))
		}
	}

	/**
	 * Returns which Player owns the requested macro.
	 * @param macroIndex the index of the macro (0-8)
	 */
	fun macro(macroIndex: Byte): Player = when {
		rows[macroIndex / 3].getBit(27 + macroIndex % 3) -> Player.PLAYER
		rows[3 + macroIndex / 3].getBit(27 + macroIndex % 3) -> Player.ENEMY
		else -> Player.NEUTRAL
	}

	/**
	 * Returns which Player owns the requested tile.
	 * @param index the index of the tile (0-80)
	 */
	fun tile(index: Coord): Player = when {
		rows[index / 27].getBit(index % 27) -> Player.PLAYER
		rows[3 + index / 27].getBit(index % 27) -> Player.ENEMY
		else -> Player.NEUTRAL
	}

	/**
	 * Returns a copy of the Board with the [Player]s swapped.
	 * @return A copy of the original [Board] with the [Player]s swapped
	 */
	fun flip() = copy().apply {
		nextPlayer = nextPlayer.otherWithNeutral()
		wonBy = wonBy?.otherWithNeutral()
		rows = IntArray(6) { 0 }.apply {
			for (i in 0..2) this[i] = rows[i + 3]
			for (i in 3..5) this[i] = rows[i - 3]
		}
	}

	/**
	 * Returns the available [Coord]s. The coords are cached so the available moves
	 * will only be calculated on the first call.
	 * @return a [ByteArray] containing the available [Coord]s.
	 */
	private fun availableMoves(): ByteArray {
		if (isDone) return ByteArray(0)

		if (_availableMoves == null) {
			var size = 0
			val out = ByteArray(81)
			for (om in 0 until 9) {
				if (macroMask.getBit(om)) {
					val row = rows[om / 3] or rows[om / 3 + 3]
					for (index in om * 9 until 9 + om * 9) {
						if (!row.getBit(index % 27)) out[size++] = index.toByte()
					}
				}
			}
			_availableMoves = Arrays.copyOf(out, size)
		}
		return _availableMoves!!
	}

	/**
	 * Get the available moves mapped to another type.
	 * Available moves are not cached when using this method.
	 * @param map the map applied to the available.
	 * @return An Array containing the [Coord]s mapped with the input map.
	 */
	@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
	inline fun <reified T> availableMoves(map: (Coord) -> T): Array<T> {
		if (isDone) return Array(0) { null!! }

		var size = 0
		val out = arrayOfNulls<T>(81)
		for (om in 0 until 9) {
			if (macroMask.getBit(om)) {
				val row = rows[om / 3] or rows[om / 3 + 3]
				for (index in om * 9 until 9 + om * 9) {
					if (!row.getBit(index % 27)) out[size++] = index.toByte().let(map)
				}
			}
		}
		return Arrays.copyOf(out, size)
	}

	fun randomAvailableMove(random: Random): Coord {
		if (isDone) throw IllegalStateException("isDone")

		var count = 0
		for (om in 0 until 9) {
			if (macroMask.getBit(om)) {
				val row = rows[om / 3] or rows[om / 3 + 3]
				for (index in om * 9 until 9 + om * 9) {
					if (!row.getBit(index % 27)) count++
				}
			}
		}

		val chosen = random.nextInt(count)
		var curr = 0

		for (om in 0 until 9) {
			if (macroMask.getBit(om)) {
				val row = rows[om / 3] or rows[om / 3 + 3]
				for (index in om * 9 until 9 + om * 9) {
					if (!row.getBit(index % 27) && curr++ == chosen)
						return index.toByte()
				}
			}
		}

		throw IllegalStateException()
	}

	/**
	 * Plays the given coord on the board.
	 * @param index the index of the coord to be played (0-80).
	 * @return Whether the move wins the macro being played in.
	 */
	fun play(index: Coord): Boolean {
		val row = index / 27                     //Row (0,1,2)
		val macroShift = (index / 9) % 3 * 9     //Shift to go to the right micro (9om)
		val moveShift = index % 9                //Shift required for index within matrix (os)
		val shift = moveShift + macroShift       //Total move offset in the row entry
		val pRow = nextPlayer.ordinal * 3 + row  //Index of the row entry in the rows array

		//If the move is not available throw exception
		if ((rows[row] or rows[row + 3]).getBit(shift) || !macroMask.getBit((index / 27) * 3 + (macroShift / 9)))
			throw RuntimeException("Position $index not available")
		else if (wonBy != null)
			throw RuntimeException("Can't play; game already over")

		//Write move to board & check for macro win
		rows[pRow] += (1 shl shift)
		val macroWin = wonGrid((rows[pRow] shr macroShift) and 0b111111111, moveShift)

		//Check if the current player won
		if (macroWin) {
			rows[pRow] += (1 shl (27 + macroShift / 9))
			if (wonGrid(macroWinGrid(nextPlayer), index / 9)) wonBy = nextPlayer
		}

		//Prepare the board for the next player
		val winGrid = macroWinGrid(Player.PLAYER) or macroWinGrid(Player.ENEMY)
		val freeMove = winGrid.getBit(moveShift) || macroFull(moveShift)
		_availableMoves = null
		macroMask = if (freeMove) (0b111111111 and winGrid.inv()) else (1 shl moveShift)
		lastMove = index
		nextPlayer = nextPlayer.other()

		var anyMove = false
		m@ for (om in 0 until 9) {
			val sumRow = rows[om / 3] or rows[3 + om / 3]
			if ((macroMask shr om) and 1 != 0)
				for (os in 0 until 9) {
					if (!sumRow.getBit(9 * (om % 3) + os)) {
						anyMove = true
						break@m
					}
				}
		}

		if (!anyMove)
			wonBy = Player.NEUTRAL

		return macroWin
	}

	private inline fun macroFull(om: Int) = (rows[om / 3] or rows[3 + om / 3]).shr((om % 3) * 9).isMaskSet(0b111111111)
	private inline fun macroWinGrid(player: Player) = (rows[0 + 3 * player.ordinal] shr 27)
			.or((rows[1 + 3 * player.ordinal] shr 27) shl 3)
			.or((rows[2 + 3 * player.ordinal] shr 27) shl 6)

	private fun wonGrid(grid: Int, index: Int) = Board.wonGrid[grid]

	override fun toString() = (0 until 81).map { it to toCoord(it % 9, it / 9) }.joinToString("") {
		when {
			(it.first == 0 || it.first == 80) -> ""
			(it.first % 27 == 0) -> "\n---+---+---\n"
			(it.first % 9 == 0) -> "\n"
			(it.first % 3 == 0 || it.first % 6 == 0) -> "|"
			else -> ""
		} + when {
			rows[it.second / 27].getBit(it.second % 27) -> "X"
			rows[(it.second / 27) + 3].getBit(it.second % 27) -> "O"
			else -> " "
		}
	}

	override fun hashCode() = 31 * (31 * Arrays.hashCode(rows) + nextPlayer.hashCode()) + (lastMove ?: -1)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as Board
		if (!Arrays.equals(rows, other.rows)) return false
		if (nextPlayer != other.nextPlayer) return false
		if (lastMove != other.lastMove) return false

		return true
	}
}

private inline fun Int.getBit(index: Int) = ((this shr index) and 1) != 0
private inline fun Int.isMaskSet(mask: Int) = this and mask == mask
