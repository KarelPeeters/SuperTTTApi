package common

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Player
import com.flaghacker.sttt.common.toCoord
import java.io.Serializable
import java.util.*
import java.util.random.RandomGenerator

internal const val GRID_MASK = 0b111111111
internal const val GRID_BITS = 9

private val WIN_CASES = intArrayOf(
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
 * There are 2**9 = 512 possible boards per player, which are
 * either won or not won. This data is stored in 16 (512 / 32) ints
 *
 *  if bit `grid%32` of `WIN_GRID[grid/32]` is set the 9b grid is won:
 *
 * `(WIN_GRID[grid/32] >> (grid%32)) & 1 == 1`
 */
private val WIN_GRID = IntArray(16) {
	var res = 0
	for (i in 0 until 32) {
		val grid = it * 32 + i
		if (WIN_CASES.any { win_case -> (grid and win_case) == win_case })
			res = res or (1 shl i)
	}
	res
}

typealias Coord = Byte 		// MMMM SSSS

class BoardUnsafe : Serializable {
    internal var grids: IntArray
    private var mainGrid: Int
	internal var openMacroMask: Int
	var nextPlayX : Boolean; private set
	var lastMove : Coord; private set // default -1

    val isDone get() = openMacroMask == 0
	val availableMoves get() = availableMoves<Byte> { it }

	/** Constructs an empty [BoardUnsafe]. */
    constructor() {
        grids = IntArray(9)
        mainGrid = 0
        openMacroMask = GRID_MASK
		nextPlayX = true
        lastMove = -1
    }

    /** Returns a copy of the current board. */
    fun copy() = BoardUnsafe(this)

    /** Copy constructor */
    private constructor(board: BoardUnsafe) {
        grids = board.grids.copyOf()
        mainGrid = board.mainGrid
        openMacroMask = board.openMacroMask
		nextPlayX = board.nextPlayX
        lastMove = board.lastMove
    }

	fun loadInstance(board: BoardUnsafe) {
		for (om in 0..<9) grids[om] = board.grids[om]
		mainGrid = board.mainGrid
		openMacroMask = board.openMacroMask
		nextPlayX = board.nextPlayX
		lastMove = board.lastMove
	}

	// copy from a safe board
	public constructor(board: Board) {
		grids = board.grids.copyOf()
		mainGrid = board.mainGrid
		openMacroMask = board.openMacroMask
		nextPlayX = board.nextPlayer == Player.PLAYER

		// Convert safe moves to unsafe move format
		lastMove = if (board.lastMove == null) -1 else {
			val om = (board.lastMove!!.toInt() and 0xFF) / 9
			val os = (board.lastMove!!.toInt() and 0xFF) % 9
			((om shl 4) + os).toByte()
		}
	}


	/**
	 * Picks a random available move. Faster than calling [availableMoves]
	 * because this function doesn't allocate an array.
	 */
	fun randomPlayWinner(random: RandomGenerator): Boolean { // null => TIE, // True => X WON, // False => O WON
		var finish = 0
		while (finish == 0){
			// Generate macro mask
			val tileLastMove = lastMove.toInt() and 0xF
			var macroMask = (1 shl tileLastMove) and openMacroMask
			if (macroMask == 0) macroMask = openMacroMask // free-move

			// Count available moves without allocating array
			var count = 9 * Integer.bitCount(macroMask)
			macroMask.forEachBit { count -= Integer.bitCount(grids[it])}

			// Pick a random move without allocating array
			var rem = random.fastRandBoundedInt(count) + 1
			findMove@while (macroMask != 0) {
				val om = Integer.numberOfTrailingZeros(macroMask)
				rem += Integer.bitCount(grids[om]) - 9

				// Check if chosen in range
				if (rem <= 0) {
					// Fetch chosen tile OS
					var openTileMask = ((grids[om] shr GRID_BITS) or (grids[om] and GRID_MASK)).inv()
					repeat(-rem) { openTileMask = openTileMask.removeLastSetBit() }
					val os = Integer.numberOfTrailingZeros(openTileMask)

					// Play move and return
					finish = play(((om shl 4) + os).toByte())
					break@findMove
				}

				macroMask = macroMask.removeLastSetBit()
			}

		}

		return when (finish) {
			1 -> true
			2 -> false
			else -> random.nextBoolean()
		}
	}

	// TODO check inline
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    inline fun <reified T> availableMoves(map: (Coord) -> T): Array<T> {
		var macroMask = GRID_MASK // playable macros

		// output vars
		var size = 0
		val out = arrayOfNulls<T>(81)

		// If there exists a last move update the playable macro mask
		if (lastMove != (-1).toByte()){
			val tileLastMove = lastMove.toInt() and 0xF
			macroMask = (1 shl tileLastMove) and openMacroMask
			if (macroMask == 0) macroMask = openMacroMask // free-move
		}

		// Iterate over all macros in the macroMask
		macroMask.forEachBit { om ->
			val osFree = ((grids[om] shr GRID_BITS) or grids[om]).inv() and GRID_MASK
			osFree.forEachBit { os ->
				out[size++] = ((om shl 4) + os).toByte().let(map)
			}
		}

		return Arrays.copyOf(out, size)
    }

    fun play(index: Coord): Int { // 0 => Not done, // 1 => X won, // 2 => O won // 3 => Tie
        val idx = index.toInt() and 0xFF // remove sign extension
        val om = idx shr 4		 		 // top bits
        val os = idx and 0b1111  		 // lower bits

		val osShift = (1 shl os)
		val macroGridPlayer : Int

		// Update and extract player local board
		lastMove = index
        if (nextPlayX){
			grids[om] 		= grids[om] or osShift
			macroGridPlayer = grids[om] and GRID_MASK
        } else {
			grids[om] 		= grids[om] or (osShift shl GRID_BITS)
			macroGridPlayer = grids[om] shr GRID_BITS
		}

		// Check if the macro is won
		val omShift = (1 shl om)
		val macroWin = macroGridPlayer.gridWon()
		if (macroWin) {
			val mainGridPlayer : Int

			// Update and extract player global board
			openMacroMask = openMacroMask xor omShift
			if (nextPlayX){
				mainGrid = mainGrid or omShift
				mainGridPlayer = mainGrid and GRID_MASK
			} else {
				mainGrid = mainGrid or (omShift shl GRID_BITS)
				mainGridPlayer = mainGrid shr GRID_BITS
			}

			// Check if the game is won
			if (mainGridPlayer.gridWon()){
				openMacroMask = 0
				return if (nextPlayX) 1 /*WIN X*/ else 2 /*WIN O*/
			}
		} else if (Integer.bitCount(grids[om]) == 9){
			openMacroMask = openMacroMask xor omShift
		}

		nextPlayX = !nextPlayX
		return if (openMacroMask != 0) 0 /*NOT FINISHED*/ else 3 /*TIE*/
    }

	override fun toString() = toString(true)
	fun toString(showAvailableMoves: Boolean) = (0 until 81).joinToString("") {
		val coordOld = toCoord((it % 9), it / 9) // stolen from normal Board
		val om = (coordOld.toInt() and 0xFF) / 9
		val os = (coordOld.toInt() and 0xFF) % 9
		val coordNew = ((om shl 4) + os).toByte()

		when {
			(it == 0 || it == 80) -> ""
			(it % 27 == 0) -> "\n---+---+---\n"
			(it % 9 == 0) -> "\n"
			(it % 3 == 0 || it % 6 == 0) -> "|"
			else -> ""
		} + when {
			tile(coordOld) == Player.PLAYER -> "X"
			tile(coordOld) == Player.ENEMY -> "O"
			showAvailableMoves && coordNew in availableMoves -> "."
			else -> " "
		}
	}
	fun tile(index: Coord): Player = when {
		grids[index / 9].hasBit(index % 9) -> Player.PLAYER
		grids[index / 9].hasBit(index % 9 + 9) -> Player.ENEMY
		else -> Player.NEUTRAL
	}
}

private inline fun Int.hasBit(index: Int) = (this shr index) and 1 != 0
private inline fun Int.gridWon() = (WIN_GRID[this / 32] shr (this % 32)) and 1 != 0
private inline fun Int.removeLastSetBit() = this and (this - 1)

internal inline fun RandomGenerator.fastRandBoundedInt(bound: Int): Int {
	//return nextInt(bound) // Statistically superior random ;)
	//return (nextInt() ushr 1) % bound
	return ((nextInt().toUInt().toULong() * bound.toULong()) shr 32).toInt()
}

internal inline fun Int.forEachBit(block: (index: Int) -> Unit) {
	var x = this
	while (x != 0) {
		block(Integer.numberOfTrailingZeros(x))
		x = x and (x - 1)
	}
}
