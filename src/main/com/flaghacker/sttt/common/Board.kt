@file:Suppress("NOTHING_TO_INLINE")

package com.flaghacker.sttt.common

import java.io.Serializable
import java.util.*

typealias Coord = Byte

fun toCoord(x: Int, y: Int) = (((x / 3) + (y / 3) * 3) * 9 + ((x % 3) + (y % 3) * 3)).toByte()
fun Int.toPair() = toByte().toPair()
fun Coord.toPair(): Pair<Int, Int> {
	val om = this / 9
	val os = this % 9
	return Pair((om % 3) * 3 + (os % 3), (om / 3) * 3 + (os / 3))
}

const val FULL_GRID = 0b111111111

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
	 * Each element represents a single grid
	 * first 9 for player,
	 * next 9 for enemy
	 * last 2 main for player and enemy
	 *
	 * player: p=0, enemy: p=1
	 *
	 * macro: grids[9*p + om]
	 * main: grids[p + 18]
	 *
	 * tile: (grid >> os) & 1
	 */
	private var grids: IntArray

	private var macroMask: Int
	private var doneMacroMask: Int

	var lastMove: Coord?; private set
	var nextPlayer: Player; private set
	var wonBy: Player?; private set

	private var _availableMoves: ByteArray?

	val availableMoves get() = availableMoves()
	val isDone get() = wonBy != null

	/** Constructs an empty [Board]. */
	constructor() {
		grids = IntArray(20)
		macroMask = 0b111111111
		doneMacroMask = 0
		_availableMoves = null
		nextPlayer = Player.PLAYER
		lastMove = null
		wonBy = null
	}

	/** Returns a copy of the current board. */
	fun copy() = Board(this)

	private constructor(board: Board) {
		grids = board.grids.copyOf()
		macroMask = board.macroMask
		doneMacroMask = board.doneMacroMask
		lastMove = board.lastMove
		nextPlayer = board.nextPlayer
		wonBy = board.wonBy
		_availableMoves = board._availableMoves
	}

	/**
	 * Constructs a Board using a 2 dimensional Array of players, the next player and the lastMove.
	 * @param board 2 dimensional array containing who owns each tile. The format is `board[x][y]`
	 * @param nextPlayer the next player
	 * @param lastMove the [Coord] of the last move played on the board, null if the board is still empty
	 * */
	constructor(board: Array<Array<Player>>, nextPlayer: Player, lastMove: Coord?) {
		if (board.size != 9 && board.all { it.size != 9 })
			throw IllegalArgumentException("Wrong board dimensions $board")
		if (nextPlayer == Player.NEUTRAL)
			throw IllegalArgumentException("nextPlayer can't be $nextPlayer")
		if (lastMove != null && lastMove !in 0 until 81)
			throw IllegalArgumentException("lastMove must be null or in range, was $lastMove")

		this.doneMacroMask = 0
		this._availableMoves = null
		this.wonBy = null

		this.grids = IntArray(20)
		for (i in 0 until 81) {
			val owner = board[i.toPair().first][i.toPair().second]
			if (owner != Player.NEUTRAL) {
				val p = owner.ordinal
				val om = i / 9
				val os = i % 9

				this.nextPlayer = owner
				setTileAndUpdate(p, om, os)
			}
		}

		this.lastMove = lastMove
		this.nextPlayer = nextPlayer
		this.macroMask = if (lastMove == null) doneMacroMask.inv() else calcMacroMask(lastMove % 9)
	}

	/**
	 * Returns which Player owns the requested macro.
	 * @param macroIndex the index of the macro (0-8)
	 */
	fun macro(macroIndex: Byte): Player = when {
		grids[18].getBit(macroIndex.toInt()) -> Player.PLAYER
		grids[19].getBit(macroIndex.toInt()) -> Player.ENEMY
		else -> Player.NEUTRAL
	}

	/**
	 * Returns which Player owns the requested tile.
	 * @param index the index of the tile (0-80)
	 */
	fun tile(index: Coord): Player = when {
		grids[index / 9].getBit(index % 9) -> Player.PLAYER
		grids[9 + index / 9].getBit(index % 9) -> Player.ENEMY
		else -> Player.NEUTRAL
	}

	/**
	 * Returns a copy of the Board with the [Player]s swapped.
	 * @return A copy of the original [Board] with the [Player]s swapped
	 */
	fun flip() = copy().apply {
		nextPlayer = nextPlayer.otherWithNeutral()
		wonBy = wonBy?.otherWithNeutral()
		grids = IntArray(20) { 0 }.apply {
			for (i in 0 until 9) this[i] = grids[i + 9]
			for (i in 9 until 18) this[i] = grids[i - 9]
			this[18] = grids[19]
			this[19] = grids[18]
		}
	}

	/**
	 * Returns the available [Coord]s. The coords are cached so the available moves
	 * will only be calculated on the first call.
	 * @return a [ByteArray] containing the available [Coord]s.
	 */
	private fun availableMoves(): ByteArray {
		val availableMoves = _availableMoves ?: if (isDone)
			ByteArray(0)
		else {
			var size = 0
			val out = ByteArray(81)
			for (om in 0 until 9) {
				if (macroMask.getBit(om)) {
					val grid = grids[om] or grids[9 + om]
					for (os in 0 until 9) {
						if (!grid.getBit(os)) out[size++] = (9 * om + os).toByte()
					}
				}
			}
			Arrays.copyOf(out, size)
		}

		_availableMoves = availableMoves
		return availableMoves
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
				val grid = grids[om] or grids[9 + om]
				for (os in 0 until 9) {
					if (!grid.getBit(os)) out[size++] = (9 * om + os).toByte().let(map)
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
				val grid = grids[om] or grids[9 + om]
				for (os in 0 until 9) {
					if (!grid.getBit(os)) count++
				}
			}
		}

		if (count == 0)
			throw IllegalStateException("No moves available")

		val chosen = random.nextInt(count)
		var curr = 0

		for (om in 0 until 9) {
			if (macroMask.getBit(om)) {
				val grid = grids[om] or grids[9 + om]
				for (os in 0 until 9) {
					if (!grid.getBit(os) && curr++ == chosen)
						return (9 * om + os).toByte()
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
		if (isDone) throw IllegalStateException("isDone")

		val om = index / 9
		val os = index % 9
		val p = nextPlayer.ordinal

		//If the move is not available throw exception
		if ((grids[om] or grids[9 + om]).getBit(os) || !macroMask.getBit(om))
			throw IllegalStateException("Position $index not playable")

		//Actually do the move
		val macroWin = setTileAndUpdate(p, om, os)

		//Prepare the board for the next player
		_availableMoves = null
		lastMove = index
		nextPlayer = nextPlayer.other()

		return macroWin
	}

	private fun setTileAndUpdate(p: Int, om: Int, os: Int): Boolean {
		//Write move to board & check for macro win
		val newGrid = grids[9 * p + om] or (1 shl os)
		grids[9 * p + om] = newGrid

		//Check if the current player won
		val macroWin = wonGrid[newGrid]
		if (macroWin) {
			val newMacroGrid = grids[18 + p] or (1 shl om)
			grids[18 + p] = newMacroGrid
			if (wonGrid[newMacroGrid])
				wonBy = nextPlayer
		}

		//Mark the macro as done if won or full
		val totalGrid = newGrid or grids[9 * (1 - p) + om]
		if (macroWin || totalGrid == FULL_GRID)
			doneMacroMask = doneMacroMask or (1 shl om)

		macroMask = calcMacroMask(os)
		return macroWin
	}

	private fun calcMacroMask(os: Int) =
			if (doneMacroMask.getBit(os)) (FULL_GRID and doneMacroMask.inv())
			else (1 shl os)

	override fun toString() = toString(false)

	fun toString(showMoves: Boolean) = (0 until 81).joinToString("") {
		val coord = toCoord(it % 9, it / 9)
		when {
			(it == 0 || it == 80) -> ""
			(it % 27 == 0) -> "\n---+---+---\n"
			(it % 9 == 0) -> "\n"
			(it % 3 == 0 || it % 6 == 0) -> "|"
			else -> ""
		} + when {
			tile(coord) == Player.PLAYER -> "X"
			tile(coord) == Player.ENEMY -> "O"
			showMoves && coord in availableMoves -> "."
			else -> " "
		}
	}

	override fun hashCode(): Int {
		var result = grids.contentHashCode()
		result = 31 * result + (lastMove ?: 0)
		result = 31 * result + nextPlayer.hashCode()
		return result
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as Board

		if (!grids.contentEquals(other.grids)) return false
		if (nextPlayer != other.nextPlayer) return false
		if (lastMove != other.lastMove) return false

		return true
	}
}

private inline fun Int.getBit(index: Int) = ((this shr index) and 1) != 0
private inline fun Int.isMaskSet(mask: Int) = this and mask == mask
