package common

import com.flaghacker.sttt.common.Board
import com.flaghacker.sttt.common.Player
import com.flaghacker.sttt.common.toCoord
import java.io.Serializable
import java.util.*
import java.util.random.RandomGenerator

//internal const val GRID_MASK = 0b111111111
//internal const val GRID_BITS = 9

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

private val EXPAND_MASK = IntArray(8) { mask ->
	var expanded = 0
	if (((mask shr 2) and 1) == 1) expanded += GRID_MASK shl 18
	if (((mask shr 1) and 1) == 1) expanded += GRID_MASK shl 9
	if (((mask shr 0) and 1) == 1) expanded += GRID_MASK
	expanded
}

private val CALC_COL_TILE = IntArray(27) { dividend ->
	val col = dividend / 9
	val tile = dividend % 9
	(col shl 4) + tile
}

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
		if (WIN_CASES.any { winCase -> (grid and winCase) == winCase })
			res = res or (1 shl i)
	}
	res
}

typealias Coord2 = Byte 		// MMMM SSSS

class BoardUnsafe2 : Serializable {
	internal var rowX: IntArray // each 3x9 player tiles
	internal var rowO: IntArray // each 3x9 enemy tiles
	internal var playableMacroMask: Int // 9 bits indicating if macros are playable


    //internal var grids: IntArray
    private var mainGrid: Int
	//internal var openMacroMask: Int
	var nextPlayX : Boolean; private set
	var lastMove : Coord2; private set // default -1

	@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    val isDone inline get() = playableMacroMask == 0
	val availableMoves get() = availableMoves<Byte> { it }

	/** Constructs an empty [BoardUnsafe2]. */
    constructor() {
		rowX = IntArray(3)
		rowO = IntArray(3)
		playableMacroMask = GRID_MASK
		mainGrid = 0
		nextPlayX = true
        lastMove = -1
    }

    /** Returns a copy of the current board. */
    fun copy() = BoardUnsafe2(this)

    /** Copy constructor */
    private constructor(board: BoardUnsafe2) {
		rowX = board.rowX.copyOf()
		rowO = board.rowO.copyOf()
		playableMacroMask = board.playableMacroMask
        mainGrid = board.mainGrid
		nextPlayX = board.nextPlayX
        lastMove = board.lastMove
    }

	fun loadInstance(board: BoardUnsafe2) {
		for (row in 0..<3) {
			rowX[row] = board.rowX[row]
			rowO[row] = board.rowO[row]
		}
		playableMacroMask = board.playableMacroMask
		mainGrid = board.mainGrid
		nextPlayX = board.nextPlayX
		lastMove = board.lastMove
	}

	// copy from a safe board
	public constructor(board: Board) {
		rowX = IntArray(3)
		rowO = IntArray(3)
		for (om in 0..<9) {
			val row = om / 3
			val col = om % 3
			val xMacro = board.grids[om] and GRID_MASK
			val oMacro = board.grids[om] shr GRID_BITS
			rowX[row] = rowX[row] or (xMacro shl (col * GRID_BITS))
			rowO[row] = rowO[row] or (oMacro shl (col * GRID_BITS))
		}

		playableMacroMask = board.openMacroMask
		mainGrid = board.mainGrid
		nextPlayX = board.nextPlayer == Player.PLAYER

		// Convert safe moves to unsafe move format
		lastMove = if (board.lastMove == null) -1 else {
			val om = (board.lastMove!!.toInt() and 0xFF) / 9
			val row = om / 3
			val col = om % 3
			val os = (board.lastMove!!.toInt() and 0xFF) % 9
			((row shl 6) or (col shl 4) or os).toByte()
		}
	}


	/**
	 * Picks a random available move. Faster than calling [availableMoves]
	 * because this function doesn't allocate an array.
	 */
	fun randomPlayWinner(random: RandomGenerator): Boolean { // null => TIE, // True => X WON, // False => O WON
		var finish = 0
		toploop@while (finish == 0){
			// Generate macro mask
			val tileLastMove = lastMove.toInt() and 0xF

			// Create macro mask, deciding which macros are playable
			var macroMask = (1 shl tileLastMove) and playableMacroMask
			if (macroMask == 0) macroMask = playableMacroMask // free-move

			// Create a mask used to mask out unused macros
			val rowMask0 = EXPAND_MASK[(macroMask      ) and 0b111]
			val rowMask1 = EXPAND_MASK[(macroMask shr 3) and 0b111]
			val rowMask2 = EXPAND_MASK[(macroMask shr 6) and 0b111]

			// Actually mask out the rows
			val maskedRow0 = (rowX[0] or rowO[0]).inv() and rowMask0
			val maskedRow1 = (rowX[1] or rowO[1]).inv() and rowMask1
			val maskedRow2 = (rowX[2] or rowO[2]).inv() and rowMask2

			// Count the moves per row
			val countCum0 = Integer.bitCount(maskedRow0)
			val countCum1 = countCum0 + Integer.bitCount(maskedRow1)
			val countCum2 = countCum1 + Integer.bitCount(maskedRow2)

			// Pick random move
			val sel = random.fastRandBoundedInt(countCum2)

			// Find the row for this move
			val rem1 = sel - countCum0
			val rem2 = sel - countCum1
			val sel1 = (rem1 ushr 31) xor 1
			val sel2 = (rem2 ushr 31) xor 1
			var move = (sel1 + sel2) shl 6 // set the row

			// Decide bit index in row
			//var selRepeats = sel
			//if (sel1 == 1) selRepeats = rem1
			//if (sel2 == 1) selRepeats = rem2
			val selRepeats = ((-sel2) and rem2) or ((sel2-sel1) and rem1) or ((-sel1).inv() and sel)

			// Select mask
			//var selMask = maskedRow0
			//if (sel1 == 1) selMask = maskedRow1
			//if (sel2 == 1) selMask = maskedRow2
			var selMask = ((-sel2) and maskedRow2) or ((sel2-sel1) and maskedRow1) or ((-sel1).inv() and maskedRow0)

			// Remove bits
			repeat(selRepeats) { selMask = selMask.removeLastSetBit() }
			val indexInRow = Integer.numberOfTrailingZeros(selMask)
			move += CALC_COL_TILE[indexInRow]

			// Play move
			finish = play(move.toByte())
		}

		return when (finish) {
			1 -> true
			2 -> false
			else -> random.nextBoolean()
		}
	}

	// TODO check inline
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    inline fun <reified T> availableMoves(map: (Coord2) -> T): Array<T> {
		var macroMask = GRID_MASK // playable macros

		// output vars
		var size = 0
		val out = arrayOfNulls<T>(81)

		// create list
		// TODO
/*		var openMacroMask = 0
		if (lastMove != (-1).toByte()){
			val tileLastMove = lastMove.toInt() and 0xF
			val tileRow = tileLastMove / 3 // this should probably be a lookup TODO
			val tileCol = tileLastMove % 3 // this should probably be a lookup TODO
			val colShift = tileCol * GRID_BITS
			val openRowMask = (rowX[tileRow] or rowO[tileRow]).inv() and rowPlayable[tileRow]
			openMacroMask = (openRowMask shr colShift) and GRID_MASK
			if (openMacroMask != 0) openMacroMask.forEachBit { os ->
				out[size++] = map(((tileRow shl 6) or (tileCol shl 4) or os).toByte())
			}
		}

		if (openMacroMask == 0) { // free-move
			for (row in 0..<3) {
				val openRowMask = (rowX[row] or rowO[row]).inv() and rowPlayable[row]
				openRowMask.forEachBit { indexInRow ->
					val col = indexInRow / 9
					val os =  indexInRow % 9
					out[size++] = map(((row shl 6) or (col shl 4) or os).toByte())
				}
			}
		}*/

		return Arrays.copyOf(out, size)
    }

	private inline fun playInline(index: Coord2): Int { // 0 => Not done, // 1 => X won, // 2 => O won // 3 => Tie
		val idx  = index.toInt()         // be careful of sign extension
		val row  = (idx shr 6) and 0b11	 // index[6:7]
		val col  = (idx shr 4) and 0b11	 // index[4:5]
		val tile = idx and 0b1111  		 // index[0:3]

		//val osShift = (1 shl tile)
		val macroGridPlayer : Int

		val colShift = 9 * col
		val move = 1 shl (tile + colShift)

		// Update and extract player local board
		lastMove = index
		if (nextPlayX){
			rowX[row]      = rowX[row] or move
			macroGridPlayer = (rowX[row] shr colShift) and GRID_MASK
		} else {
			rowO[row]      = rowO[row] or move
			macroGridPlayer = (rowO[row] shr colShift) and GRID_MASK
		}

		val gridBothPlayer = ((rowX[row] or rowO[row]) shr colShift) and GRID_MASK

		// Check if the macro is won
		val omShifted = (1 shl (3 * row + col))
		val macroWin = macroGridPlayer.gridWon()
		if (macroWin) {
			val mainGridPlayer : Int

			// Update and extract player global board

			playableMacroMask = playableMacroMask xor omShifted
			if (nextPlayX){
				mainGrid = mainGrid or omShifted
				mainGridPlayer = mainGrid and GRID_MASK
			} else {
				mainGrid = mainGrid or (omShifted shl GRID_BITS)
				mainGridPlayer = mainGrid shr GRID_BITS
			}

			// Check if the game is won
			if (mainGridPlayer.gridWon()){
				playableMacroMask = 0
				return if (nextPlayX) 1 /*WIN X*/ else 2 /*WIN O*/
			}
		} else if (gridBothPlayer == 0b111111111){
			playableMacroMask = playableMacroMask xor omShifted
		} else {
			nextPlayX = !nextPlayX
			return 0 /* NO MACRO FINISHED => GAME NOT FINISHED */
		}

		// Check if game is finished
		nextPlayX = !nextPlayX
		return if (isDone) 3 /*TIE*/ else 0 /*NOT FINISHED*/
	}

    fun play(index: Coord2): Int { // 0 => Not done, // 1 => X won, // 2 => O won // 3 => Tie
        val idx  = index.toInt()         // be careful of sign extension
		val row  = (idx shr 6) and 0b11	 // index[6:7]
        val col  = (idx shr 4) and 0b11	 // index[4:5]
        val tile = idx and 0b1111  		 // index[0:3]

		//val osShift = (1 shl tile)
		val macroGridPlayer : Int

		val colShift = 9 * col
		val move = 1 shl (tile + colShift)

		// Update and extract player local board
		lastMove = index
        if (nextPlayX){
			rowX[row]      = rowX[row] or move
			macroGridPlayer = (rowX[row] shr colShift) and GRID_MASK
        } else {
			rowO[row]      = rowO[row] or move
			macroGridPlayer = (rowO[row] shr colShift) and GRID_MASK
		}

		val gridBothPlayer = ((rowX[row] or rowO[row]) shr colShift) and GRID_MASK

		// Check if the macro is won
		val omShifted = (1 shl (3 * row + col))
		val macroWin = macroGridPlayer.gridWon()
		if (macroWin) {
			val mainGridPlayer : Int

			// Update and extract player global board

			playableMacroMask = playableMacroMask xor omShifted
			if (nextPlayX){
				mainGrid = mainGrid or omShifted
				mainGridPlayer = mainGrid and GRID_MASK
			} else {
				mainGrid = mainGrid or (omShifted shl GRID_BITS)
				mainGridPlayer = mainGrid shr GRID_BITS
			}

			// Check if the game is won
			if (mainGridPlayer.gridWon()){
				playableMacroMask = 0
				return if (nextPlayX) 1 /*WIN X*/ else 2 /*WIN O*/
			}
		} else if (gridBothPlayer == 0b111111111){
			playableMacroMask = playableMacroMask xor omShifted
		} else {
			nextPlayX = !nextPlayX
			return 0 /* NO MACRO FINISHED => GAME NOT FINISHED */
		}

		// Check if game is finished
		nextPlayX = !nextPlayX
		return if (isDone) 3 /*TIE*/ else 0 /*NOT FINISHED*/
    }

	override fun toString() = toString(true)
	fun toString(showAvailableMoves: Boolean) = (0 until 81).joinToString("") {
		// TODO update
		val coordsOld = toCoord((it % 9), it / 9) // stolen from normal Board
		val om = (coordsOld.toInt() and 0xFF) / 9
		val macroRow = om / 3
		val macroCol = om % 3
		val os = (coordsOld.toInt() and 0xFF) % 9
		val coordsNew = ((macroRow shl 6) or (macroCol shl 4) or os).toByte()

		when {
			(it == 0 || it == 80) -> ""
			(it % 27 == 0) -> "\n---+---+---\n"
			(it % 9 == 0) -> "\n"
			(it % 3 == 0 || it % 6 == 0) -> "|"
			else -> ""
		} + when {
			tileOldCoords(coordsOld) == Player.PLAYER -> "X"
			tileOldCoords(coordsOld) == Player.ENEMY -> "O"
			showAvailableMoves && coordsNew in availableMoves -> "."
			else -> " "
		}
	}

	fun tileOldCoords(index: Byte): Player = when {
		rowX[index / 27].hasBit(index % 27) -> Player.PLAYER
		rowO[index / 27].hasBit(index % 27) -> Player.ENEMY
		else -> Player.NEUTRAL
	}
}

private inline fun Int.hasBit(index: Int) = (this shr index) and 1 != 0
private inline fun Int.gridWon() = (WIN_GRID[this / 32] shr (this % 32)) and 1 != 0
private inline fun Int.removeLastSetBit() = this and (this - 1)

// Slower than doing:
// repeat(n) {value.removeLastSetBit()}
// val result = Integer.numberOfTrailingZeros(value)
internal inline fun Int.nthBitSet(n_orig: Int): Int{
	var value = toUInt()
	val pop2:UInt  = (value and 0x55555555u) + ((value shr  1) and 0x55555555u);
	val pop4  = (pop2  and 0x33333333u) + ((pop2  shr  2) and 0x33333333u);
	val pop8  = (pop4  and 0x0f0f0f0fu) + ((pop4  shr  4) and 0x0f0f0f0fu);
	val pop16 = (pop8  and 0x00ff00ffu) + ((pop8  shr  8) and 0x00ff00ffu);
	val pop32 = (pop16 and 0x000000ffu) + ((pop16 shr 16) and 0x000000ffu);
	var rank = 0u
	var n = n_orig.toUInt()

	if (n++ >= pop32)
		return 32;

	var temp = pop16 and 0xffu;
	/* if (n > temp) { n -= temp; rank += 16; } */
	rank += ((temp - n) and 256u) shr 4;
	n -= temp and ((temp - n) shr 8);

	temp = (pop8 shr rank.toInt()) and 0xffu;
	/* if (n > temp) { n -= temp; rank += 8; } */
	rank += ((temp - n) and 256u) shr 5;
	n -= temp and ((temp - n) shr 8);

	temp = (pop4 shr rank.toInt()) and 0x0fu;
	/* if (n > temp) { n -= temp; rank += 4; } */
	rank += ((temp - n) and 256u) shr 6;
	n -= temp and ((temp - n) shr 8);

	temp = (pop2 shr rank.toInt()) and 0x03u;
	/* if (n > temp) { n -= temp; rank += 2; } */
	rank += ((temp - n) and 256u) shr 7;
	n -= temp and ((temp - n) shr 8);

	temp = (value shr rank.toInt()) and 0x01u;
	/* if (n > temp) rank += 1; */
	rank += ((temp - n) and 256u) shr 8;

	return rank.toInt()
}
