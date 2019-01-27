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

private const val FULL_GRID = 0b111111111

private val LINE_MASKS = intArrayOf(
		0b000_000_111,
		0b000_111_000,
		0b111_000_000,
		0b001_001_001,
		0b010_010_010,
		0b100_100_100,
		0b100_010_001,
		0b001_010_100
)

/**
 * Whether `grid` is won:
 * `(WIN_GRID[grid / 32] >> (grid % 32)) & 1 != 0`
 */
private val WIN_GRID = IntArray(16) {
	var res = 0
	for (i in 0 until 32) {
		val grid = it * 32 + i
		if (LINE_MASKS.any { line -> grid.isMaskSet(line) })
			res = res or (1 shl i)
	}
	res
}

class Board : Serializable {
	/**
	 * Each element represents two grid grids (one for player and one for enemy)
	 * first 9 macro: grids[om]
	 * last 1 main: grids[9]
	 *
	 *
	 * First 9 bits contain the player data, second 9 bits contain the enemy data
	 * Player grid: grid[om] and FULL_GRID
	 * Enemy grid: grid[om] shr 9
	 *
	 * Format: 0bEEEEEEEEEPPPPPPPPP with P=player E=enemy
	 * Tile: (grid >> os + 9*p) & 1 (p=0 for player p=1 for enemy)
	 */
	private var grids: IntArray
	private var mainGrid: Int

	/** playable macros for the next move, each set macro is guaranteed to have at least one free tile */
	private var macroMask: Int
	/** macros that can be played in , ie they aren't full or won */
	private var openMacroMask: Int

	var lastMove: Coord?; private set
	var nextPlayer: Player; private set
	/** null: no one has won, NEUTRAL: tie */
	var wonBy: Player?; private set

	val availableMoves get() = availableMoves()
	val isDone get() = wonBy != null

	/** Constructs an empty [Board]. */
	constructor() {
		grids = IntArray(9)
		mainGrid = 0
		macroMask = FULL_GRID
		openMacroMask = FULL_GRID
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

		this.openMacroMask = FULL_GRID
		this.wonBy = null

		this.grids = IntArray(10)
		this.mainGrid = 0
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
		this.macroMask = if (lastMove == null) openMacroMask else calcMacroMask(lastMove % 9)
	}

	/** Returns a copy of the current board. */
	fun copy() = Board(this)

	/** Copy constructor */
	private constructor(board: Board) {
		grids = board.grids.copyOf()
		mainGrid = board.mainGrid
		macroMask = board.macroMask
		openMacroMask = board.openMacroMask
		lastMove = board.lastMove
		nextPlayer = board.nextPlayer
		wonBy = board.wonBy
	}

	/**
	 * Returns a copy of the Board with the [Player]s swapped, including win
	 * @return A copy of the original [Board] with the [Player]s swapped
	 */
	fun flip(): Board {
		val cpy = copy()
		cpy.nextPlayer = nextPlayer.otherWithNeutral()
		cpy.wonBy = wonBy?.otherWithNeutral()
		for (i in 0 until 9)
			cpy.grids[i] = (grids[i] shr 9) or ((grids[i] and FULL_GRID) shl 9)
		cpy.mainGrid = (mainGrid shr 9) or ((mainGrid and FULL_GRID) shl 9)
		return cpy
	}

	/**
	 * Returns which Player owns the requested macro.
	 * @param macroIndex the index of the macro (0-8)
	 */
	fun macro(macroIndex: Byte): Player = when {
		mainGrid.hasBit(macroIndex.toInt()) -> Player.PLAYER
		mainGrid.hasBit(macroIndex.toInt() + 9) -> Player.ENEMY
		else -> Player.NEUTRAL
	}

	/**
	 * Returns which Player owns the requested tile.
	 * @param index the index of the tile (0-80)
	 */
	fun tile(index: Coord): Player = when {
		grids[index / 9].hasBit(index % 9) -> Player.PLAYER
		grids[index / 9].hasBit(index % 9 + 9) -> Player.ENEMY
		else -> Player.NEUTRAL
	}

	/**
	 * Returns the available [Coord]s. The coords are cached so the available moves
	 * will only be calculated on the first call.
	 * @return a [ByteArray] containing the available [Coord]s.
	 */
	private fun availableMoves(): ByteArray {
		return if (isDone)
			ByteArray(0)
		else {
			var size = 0
			val out = ByteArray(81)
			macroMask.forEachBit { om ->
				for (os in 0 until 9) {
					if (!grids[om].hasBit(os) && !grids[om].hasBit(os + 9))
						out[size++] = (9 * om + os).toByte()
				}
			}
			out.copyOf(size)
		}
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
		macroMask.forEachBit { om ->
			for (os in 0 until 9) {
				if (!grids[om].hasBit(os) && !grids[om].hasBit(os + 9))
					out[size++] = (9 * om + os).toByte().let(map)
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

		var count = 9 * Integer.bitCount(macroMask)
		macroMask.forEachBit { om ->
			count -= Integer.bitCount(grids[om])

		}

		var left = random.nextInt(count) + 1

		macroMask.forEachBit { om ->
			left += Integer.bitCount(grids[om]) - 9

			if (left <= 0) {
				val grid = (grids[om] shr 9) or (grids[om] and FULL_GRID)
				val os = grid.inv().getNthSetIndex(-left)
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
		if (!macroMask.hasBit(om) || grids[om].hasBit(os) || grids[om].hasBit(os + 9))
			throw IllegalStateException("Position $index not playable")

		//Actually do the move
		val macroWin = setTileAndUpdate(p, om, os)

		//Prepare the board for the next player
		lastMove = index
		nextPlayer = nextPlayer.other()

		return macroWin
	}

	/**
	 * Update [grids], [wonBy], [openMacroMask] and [macroMask] when the given player plays on the given position.
	 * @return Whether the move wins the macro being played in.
	 */
	private fun setTileAndUpdate(p: Int, om: Int, os: Int): Boolean {
		//Write move to board & check for macro win
		val newGrid = grids[om] or (1 shl os + 9 * p)
		grids[om] = newGrid

		//Check if the current player won
		val macroWin = newGrid.getPlayer(p).winGrid()
		if (macroWin) {
			mainGrid = mainGrid or (1 shl (om + 9 * p))
			if (mainGrid.getPlayer(p).winGrid())
				wonBy = nextPlayer
		}

		//Mark the macro as done if won or full
		if (macroWin || Integer.bitCount(grids[om]) == 9) {
			openMacroMask = openMacroMask xor (1 shl om)
			if (openMacroMask == 0 && wonBy == null)
				wonBy = Player.NEUTRAL
		}
		macroMask = calcMacroMask(os)

		return macroWin
	}

	/**
	 * Calculates the new macro mask based on the previous move.
	 * * if the target macro is done, freeplay into all non-done macros
	 * * otherwise play in the target macro
	 */
	private fun calcMacroMask(os: Int) =
			if (openMacroMask.hasBit(os)) (1 shl os)
			else openMacroMask

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

private inline fun Int.hasBit(index: Int) = (this shr index) and 1 != 0
private inline fun Int.isMaskSet(mask: Int) = this and mask == mask
private inline fun Int.withoutLastBit() = this and (this - 1)

private inline fun Int.getNthSetIndex(n: Int): Int {
	var x = this
	repeat(n) { x = x.withoutLastBit() }
	return Integer.numberOfTrailingZeros(x)
}

private inline fun Int.getPlayer(p: Int) = if (p == 0) (this and FULL_GRID) else (this shr 9)
private inline fun Int.winGrid() = WIN_GRID[this / 32].hasBit(this % 32)

private inline fun Int.forEachBit(block: (index: Int) -> Unit) {
	var x = this
	while (x != 0) {
		block(Integer.numberOfTrailingZeros(x))
		x = x.withoutLastBit()
	}
}