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
	internal var rowPlayable: IntArray // each 3x9 tiles indicating whether they are playable


    //internal var grids: IntArray
    private var mainGrid: Int
	//internal var openMacroMask: Int
	var nextPlayX : Boolean; private set
	var lastMove : Coord2; private set // default -1

	@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    val isDone inline get() = (rowPlayable[0] == 0) and (rowPlayable[1] == 0) and (rowPlayable[2] == 0)
	val availableMoves get() = availableMoves<Byte> { it }

	/** Constructs an empty [BoardUnsafe2]. */
    constructor() {
		rowX = IntArray(3)
		rowO = IntArray(3)
		rowPlayable = IntArray(3) { 0x7FFFFFF } // 27 enabled LSB enabled
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
		rowPlayable = board.rowPlayable.copyOf()
        mainGrid = board.mainGrid
		nextPlayX = board.nextPlayX
        lastMove = board.lastMove
    }

	fun loadInstance(board: BoardUnsafe2) {
		for (row in 0..<3) {
			rowX[row] = board.rowX[row]
			rowO[row] = board.rowO[row]
			rowPlayable[row] = board.rowPlayable[row]
		}
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

		rowPlayable = IntArray(3) { 0 } // 27 enabled LSB enabled
		board.openMacroMask.forEachBit { om ->
			val row = om / 3
			val col = om % 3
			rowPlayable[row] = rowPlayable[row] or (GRID_MASK shl (col * GRID_BITS))
		}

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
		// Board is already finished
		//if (openMacroMask == 0) TODO()

		// If empty board force AI to play center => avoid null check in loop
		if (lastMove == (-1).toByte()) TODO() // this should not occur on an empty board

		var finish = 0
		toploop@while (finish == 0){
			// Generate macro mask
			//val macroLastMoveRow = (lastMove.toInt() shr 6)and 0b11
			//val macroLastMoveCol =(lastMove.toInt() shr 4) and 0b11
			val tileLastMove = lastMove.toInt() and 0xF
			val tileRow = tileLastMove / 3 // this should probably be a lookup TODO
			val tileCol = tileLastMove % 3 // this should probably be a lookup TODO

			// tODO rename
			val colShift = tileCol * GRID_BITS
			val openRowMask = (rowX[tileRow] or rowO[tileRow]).inv() and rowPlayable[tileRow]
			var openMacroMask = (openRowMask shr colShift) and GRID_MASK

			if (openMacroMask != 0) {
				val count = Integer.bitCount(openMacroMask)
				val index = (random.nextInt() ushr 1) % count
				repeat(index) { openMacroMask = openMacroMask.removeLastSetBit() }
				val os = Integer.numberOfTrailingZeros(openMacroMask)
				finish = play(((tileRow shl 6) or (tileCol shl 4) or os).toByte())
			} else { // free-move
				val maskRow0 = (rowX[0] or rowO[0]).inv() and rowPlayable[0]
				val maskRow1 = (rowX[1] or rowO[1]).inv() and rowPlayable[1]
				val maskRow2 = (rowX[2] or rowO[2]).inv() and rowPlayable[2]

				// 1. Count moves
				val countCum0 = Integer.bitCount(maskRow0)
				val countCum1 = countCum0 + Integer.bitCount(maskRow1)
				val countCum2 = countCum1 + Integer.bitCount(maskRow2)

				// 2.Pick random move
				val sel = ((random.nextInt() ushr 1) % countCum2)

				// 3. Find the row for this move
				//var selRow = if(sel >= countCum0) 1 else 0
				//if (sel >= countCum1) selRow = 2
				//val lb = if(sel >= countCum1) 1 else 0
				//val selRow = la or (lb shl 1)
				val selRow: Int = when {
					sel < countCum0 -> 0
					sel < countCum1 -> 1
					else -> 2
				}

				// 4. Select the mask for this row
				var selMask = when (selRow) {
					0 -> maskRow0
					1 -> maskRow1
					else -> maskRow2
				}

				// 5. Calculate how many bits need to be removed from this mask
				val selRepeats = when (selRow) {
					0 -> sel
					1 -> sel - countCum0
					else -> sel - countCum1
				}

				// 6. Remove bits & play move
				repeat(selRepeats) { selMask = selMask.removeLastSetBit() }
				val indexInRow = Integer.numberOfTrailingZeros(selMask)
				val col = indexInRow / 9
				val os =  indexInRow % 9
				finish = play(((selRow shl 6) or (col shl 4) or os).toByte())
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
    inline fun <reified T> availableMoves(map: (Coord2) -> T): Array<T> {
		var macroMask = GRID_MASK // playable macros

		// output vars
		var size = 0
		val out = arrayOfNulls<T>(81)

		// create list
		var openMacroMask = 0
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
		}

		return Arrays.copyOf(out, size)
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

			rowPlayable[row] = rowPlayable[row] xor (GRID_MASK shl colShift)
			if (nextPlayX){
				mainGrid = mainGrid or omShifted
				mainGridPlayer = mainGrid and GRID_MASK
			} else {
				mainGrid = mainGrid or (omShifted shl GRID_BITS)
				mainGridPlayer = mainGrid shr GRID_BITS
			}

			// Check if the game is won
			if (mainGridPlayer.gridWon()){
				for (i in 0..<3) rowPlayable[i] = 0
				return if (nextPlayX) 1 /*WIN X*/ else 2 /*WIN O*/
			}
		} else if (gridBothPlayer == 0b111111111){
			rowPlayable[row] = rowPlayable[row] xor (GRID_MASK shl colShift)
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

