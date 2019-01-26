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
@JvmField
val WIN_GRID = BooleanArray(512).apply {
	val lines = intArrayOf(
			0, 1, 2,
			3, 4, 5,
			6, 7, 8,
			0, 3, 6,
			1, 4, 7,
			2, 5, 8,
			0, 4, 8,
			2, 4, 6
	)

	for (grid in 0 until 512) {
		this[grid] = (0 until lines.size / 3).any { i ->
			grid.getBit(lines[3 * i]) and grid.getBit(lines[3 * i + 1]) and grid.getBit(lines[3 * i + 2]) != 0
		}
	}
}
/**
 * The index of the single bit set.
 * `BIT_INDEX[(1 << i) % 11] == i` for all `0 <= i < 9`
 */
@JvmField
val BIT_INDEX = intArrayOf(-1, 0, 1, 8, 2, 4, -1, 7, 3, 6, 5)

class Board : Serializable {
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
	/**
	 * Contains for each macro all of the occupied tiles
	 * `fullGrids[om] = grids[om] or grids[9 + om]`
	 */
	private var fullGrids: IntArray

	/** playable macros for the next move, each set macro is guaranteed to have at least one free tile */
	private var macroMask: Int
	/** macros that can't be played in because they are won or full */
	private var doneMacroMask: Int

	var lastMove: Coord?; private set
	var nextPlayer: Player; private set
	/** null: no one has won, NEUTRAL: tie */
	var wonBy: Player?; private set

	private var _availableMoves: ByteArray?

	val availableMoves get() = availableMoves()
	val isDone get() = wonBy != null

	/** Constructs an empty [Board]. */
	constructor() {
		grids = IntArray(20)
		fullGrids = IntArray(9)
		macroMask = 0b111111111
		doneMacroMask = 0
		_availableMoves = null
		nextPlayer = Player.PLAYER
		lastMove = null
		wonBy = null
	}

	/**
	 * Constructs a Board with a given state. Macro and main wins are calculated automatically.
	 * @param board 2 dimensional array containing who owns each tile. The format is `board[x][y]`
	 * @param nextPlayer the next player
	 * @param lastMove the last move played on the board, `null` means the next move is freeplay
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
		this.fullGrids = IntArray(9)
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

	/** Returns a copy of the current board. */
	fun copy() = Board(this)

	/** Copy constructor */
	private constructor(board: Board) {
		grids = board.grids.copyOf()
		fullGrids = board.fullGrids.copyOf()
		macroMask = board.macroMask
		doneMacroMask = board.doneMacroMask
		lastMove = board.lastMove
		nextPlayer = board.nextPlayer
		wonBy = board.wonBy
		_availableMoves = board._availableMoves
	}

	/**
	 * Returns a copy of the Board with the [Player]s swapped, including win
	 * @return A copy of the original [Board] with the [Player]s swapped
	 */
	fun flip(): Board {
		val cpy = copy()
		cpy.nextPlayer = nextPlayer.otherWithNeutral()
		cpy.wonBy = wonBy?.otherWithNeutral()

		for (i in 0 until 9) cpy.grids[i] = grids[i + 9]
		for (i in 9 until 18) cpy.grids[i] = grids[i - 9]
		cpy.grids[18] = grids[19]
		cpy.grids[19] = grids[18]
		return cpy
	}

	/**
	 * Returns which Player owns the requested macro.
	 * @param macroIndex the index of the macro (0-8)
	 */
	fun macro(macroIndex: Byte): Player = when {
		grids[18].hasBit(macroIndex.toInt()) -> Player.PLAYER
		grids[19].hasBit(macroIndex.toInt()) -> Player.ENEMY
		else -> Player.NEUTRAL
	}

	/**
	 * Returns which Player owns the requested tile.
	 * @param index the index of the tile (0-80)
	 */
	fun tile(index: Coord): Player = when {
		grids[index / 9].hasBit(index % 9) -> Player.PLAYER
		grids[9 + index / 9].hasBit(index % 9) -> Player.ENEMY
		else -> Player.NEUTRAL
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
				if (macroMask.hasBit(om)) {
					val grid = grids[om] or grids[9 + om]
					for (os in 0 until 9) {
						if (grid.getBit(os) == 0) out[size++] = (9 * om + os).toByte()
					}
				}
			}
			out.copyOf(size)
		}

		_availableMoves = availableMoves
		return availableMoves
	}

	/**
	 * Get the available moves mapped to another type.
	 * Available moves are not cached when using this method.
	 * @param map the map applied to the available.
	 * @return An [Array] containing the [Coord]s mapped with the input map.
	 */
	@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
	inline fun <reified T> availableMoves(map: (Coord) -> T): Array<T> {
		if (isDone) return Array(0) { null!! }

		var size = 0
		val out = arrayOfNulls<T>(81)
		for (om in 0 until 9) {
			if (macroMask.hasBit(om)) {
				val grid = grids[om] or grids[9 + om]
				for (os in 0 until 9) {
					if (grid.getBit(os) == 0) out[size++] = (9 * om + os).toByte().let(map)
				}
			}
		}
		return Arrays.copyOf(out, size)
	}

	/**
	 * Picks a random available move. Faster than calling [availableMoves]
	 * because this function doesn't allocate an array.
	 */
	fun randomAvailableMove(random: Random): Coord {
		if (isDone) throw IllegalStateException("isDone")

		var count = 0
		for (om in 0 until 9)
			count += ((macroMask shr om) and 1) * (9 - Integer.bitCount(fullGrids[om]))

		val chosen = random.nextInt(count)
		var curr = 0

		var leftMacroMask = macroMask
		while (leftMacroMask != 0) {
			val om = leftMacroMask.gridLastSetIndex()
			leftMacroMask = leftMacroMask.withoutLastBit()

			val grid = fullGrids[om]
			val c = 9 - Integer.bitCount(grid)

			if (curr + c <= chosen)
				curr += c
			else {
				val delta = chosen - curr
				val os = grid.inv().gridGetNthSetIndex(delta)
				return (9 * om + os).toByte()
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
		if ((grids[om] or grids[9 + om]).getBit(os) or (1 - macroMask.getBit(om)) != 0)
			throw IllegalStateException("Position $index not playable")

		//Actually do the move
		val macroWin = setTileAndUpdate(p, om, os)

		//Prepare the board for the next player
		_availableMoves = null
		lastMove = index
		nextPlayer = nextPlayer.other()

		return macroWin
	}

	/**
	 * Update [grids], [wonBy], [doneMacroMask] and [macroMask] when the given player plays on the given position.
	 * @return Whether the move wins the macro being played in.
	 */
	private fun setTileAndUpdate(p: Int, om: Int, os: Int): Boolean {
		//Write move to board & check for macro win
		val newGrid = grids[9 * p + om] or (1 shl os)
		grids[9 * p + om] = newGrid
		val newTotalGrid = fullGrids[om] or newGrid
		fullGrids[om] = newTotalGrid

		//Check if the current player won
		val macroWin = WIN_GRID[newGrid]
		if (macroWin) {
			val newMacroGrid = grids[18 + p] or (1 shl om)
			grids[18 + p] = newMacroGrid
			if (WIN_GRID[newMacroGrid])
				wonBy = nextPlayer
		}

		//Mark the macro as done if won or full
		if (macroWin || newTotalGrid == FULL_GRID)
			doneMacroMask = doneMacroMask or (1 shl om)

		macroMask = calcMacroMask(os)
		if (macroMask == 0)
			wonBy = Player.NEUTRAL

		return macroWin
	}

	/**
	 * Calculates the new macro mask based on the previous move.
	 * * if the target macro is done, freeplay into all non-done macros
	 * * otherwise play in the target macro
	 */
	private fun calcMacroMask(os: Int) =
			if (doneMacroMask.getBit(os) != 0) (FULL_GRID and doneMacroMask.inv())
			else (1 shl os)

	override fun toString() = toString(false)

	fun toString(showAvailableMoves: Boolean) = (0 until 81).joinToString("") {
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
			showAvailableMoves && coord in availableMoves -> "."
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

private inline fun Int.getBit(index: Int) = ((this shr index) and 1)
private inline fun Int.hasBit(index: Int) = getBit(index) != 0
private inline fun Int.isMaskSet(mask: Int) = this and mask == mask
private inline fun Int.withoutLastBit() = this and (this - 1)

private inline fun Int.gridGetNthSetIndex(n: Int): Int {
	var x = this
	for (i in 0 until n)
		x = x.withoutLastBit()
	return x.gridLastSetIndex()
}

private inline fun Int.gridLastSetIndex() = BIT_INDEX[(this and (-this)) % 11]