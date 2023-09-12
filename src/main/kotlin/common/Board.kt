package common

import java.io.Serializable
import java.util.*

fun toCoord(x: Int, y: Int) = ((((x / 3) + (y / 3) * 3) shl 4) + ((x % 3) + (y % 3) * 3)).toByte()

internal const val GRID_MASK = 0b111111111
internal const val GRID_BITS = 9
private val WIN_GRIDS = intArrayOf(
    0b000_000_111,
    0b000_111_000,
    0b111_000_000,
    0b001_001_001,
    0b010_010_010,
    0b100_100_100,
    0b100_010_001,
    0b001_010_100
)

/** Lookup table containing whether a 9b tile grid is won
 * 2**9 possible combinations => 16 (=512/32) integers
 *
 * Usage: `(WIN_GRID[grid/32] >> (grid%32)) & 1 == 1`
 * **/
private val WIN_GRID_LUT = IntArray(16) {
    var res = 0
    for (i in 0 until 32) {
        val grid = it * 32 + i
        if (WIN_GRIDS.any { winGrid -> (grid and winGrid) == winGrid })
            res = res or (1 shl i)
    }
    res
}

typealias Coord = Byte // 4 top bits contain macro, 4 bottom bits contain tile

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
class Board : Serializable {
    @Transient private var random: Xoroshiro

    // Unexposed variables
    internal var grids: IntArray    // per macro, taken tiles per player (2 x 9b) x 9 macros
    private var mainGrid: Int        // for game, won macros per player (2 x 9b)
    internal var openMacroMask: Int // available macros 9b
    internal var movesPlayed = 0

    // Exposed variables
    var nextPlayX: Boolean; internal set
    var lastMove: Coord; internal set // default -1
    var tied: Boolean; internal set

    // Exposed derived variables
    val isDone inline get() = openMacroMask == 0
    val availableMoves inline get() = getAvailableMoves<Byte> { it }
    val wonBy: Player get() = if (!isDone || tied) Player.NEUTRAL else if (nextPlayX) Player.PLAYER else Player.ENEMY

    /** Constructs an empty [Board]. */
    constructor() {
        this.random = Xoroshiro()
        this.grids = IntArray(9)
        this.mainGrid = 0
        this.openMacroMask = GRID_MASK
        this.nextPlayX = true
        this.lastMove = -1
        this.tied = false
    }

    /** Returns a copy of the current board. */
    fun copy() = Board(this)

    private constructor(board: Board) {
        this.grids = board.grids.copyOf()
        this.mainGrid = board.mainGrid
        this.openMacroMask = board.openMacroMask
        this.nextPlayX = board.nextPlayX
        this.lastMove = board.lastMove
        this.random = board.random
        this.tied = board.tied
    }

    /**
     * Constructs a Board with a given state. Macro and main wins are calculated automatically.
     * @param board 81 Char String containing the board. This string can be generated with toCompactString().
     * */
    constructor(board: String) {
        val coordMap = (0..<81).map { it.toCoordsNew() }

        // Input checking
        if (board.length != 81 || board.uppercase()
                .any { !Player.legalChar(it) } || board.count { it.isLowerCase() } > 1
        ) throw IllegalArgumentException("Illegal board string; $board")

        // Extract last move + input checking
        val lastMoveIdx = board.indexOfFirst { it.isLowerCase() } // returns -1 if not found
        var lastMove = (-1).toByte()
        if (lastMoveIdx != -1){
            lastMove = coordMap[lastMoveIdx]
            if (lastMoveIdx !in 0 until 81 || board[lastMoveIdx] == Player.NEUTRAL.char) {
                throw IllegalArgumentException("Illegal lastMoveIdx ($lastMoveIdx) for board; $board")
            }
        } else if (board.any { it != Player.NEUTRAL.char }){
            throw IllegalArgumentException("No lastMove for a non-empty board; $board")
        }

        // Set the default values
        this.random = Xoroshiro()
        this.openMacroMask = GRID_MASK
        this.grids = IntArray(9)
        this.mainGrid = 0
        this.nextPlayX = true
        this.tied = false

        // Create coord list
        val xMoves = board.uppercase().mapIndexed { idx, c -> if (c == Player.PLAYER.char) coordMap[idx] else null }
            .filterNotNull()
        val oMoves = board.uppercase().mapIndexed { idx, c -> if (c == Player.ENEMY.char) coordMap[idx] else null }
            .filterNotNull()
        if ((xMoves.size - oMoves.size) !in 0..1)
            throw IllegalArgumentException("X should have 0-1 more moves than O; actual=${xMoves.size - oMoves.size}")

        // Play all the moves
        val moves = xMoves.zip(oMoves) { x, o -> listOf(x, o) }.flatten()
            .toMutableList() + if (xMoves.size > oMoves.size) xMoves.drop(oMoves.size) else oMoves.drop(xMoves.size)
        for (move in moves) {
            play(move)
        }

        this.lastMove = lastMove
        if (lastMoveIdx != -1 && !isDone){ // done boards could have a randomized winner
            if(this.nextPlayX != Player.fromChar(board[lastMoveIdx].uppercaseChar()).other().bool())
                throw IllegalArgumentException("The last move belongs to the wrong player")
        }
    }

    /** Recycle the instance by loading the game state of another board **/
    fun loadInstance(board: Board) {
        for (om in 0..<9) grids[om] = board.grids[om]
        mainGrid = board.mainGrid
        openMacroMask = board.openMacroMask
        nextPlayX = board.nextPlayX
        lastMove = board.lastMove
        tied = board.tied
    }

    /** Play random moves until the board is finished, return winner
     *  Note: faster implementation, no array allocations **/
    fun randomPlayWinner(): Boolean {
        while (!isDone) {
            // Generate macro mask
            val tileLastMove = lastMove.toInt() and 0xF
            var macroMask = (1 shl tileLastMove) and openMacroMask
            if (macroMask == 0) macroMask = openMacroMask // free-move

            // Count available moves without allocating array
            var count = 9 * Integer.bitCount(macroMask)
            macroMask.forEachBit { count -= Integer.bitCount(grids[it]) }

            // Pick a random move without allocating array
            var rem = random.nextInt(count) + 1
            findMove@ while (macroMask != 0) {
                val om = Integer.numberOfTrailingZeros(macroMask)
                rem += Integer.bitCount(grids[om]) - 9

                // Check if chosen in range
                if (rem <= 0) {
                    // Fetch chosen tile OS
                    var openTileMask = ((grids[om] shr GRID_BITS) or (grids[om] and GRID_MASK)).inv()
                    repeat(-rem) { openTileMask = openTileMask.removeLastSetBit() }
                    val os = Integer.numberOfTrailingZeros(openTileMask)

                    // Play move and return
                    play(((om shl 4) + os).toByte())
                    break@findMove
                }

                macroMask = macroMask.removeLastSetBit()
            }
        }

        return nextPlayX
    }

    /** Execute the block of code for all the available moves
     * 	Note: this avoids needing to allocate an array **/
    inline fun forAvailableMoves(block: (coord: Coord) -> Unit) {
        // Create macro mask, also works for lastMove == -1
        val tileLastMove = lastMove.toInt() and 0xF
        var macroMask = (1 shl tileLastMove) and openMacroMask
        if (macroMask == 0) macroMask = openMacroMask // free-move

        // Iterate over all macros in the macroMask
        macroMask.forEachBit { om ->
            val osFree = ((grids[om] shr GRID_BITS) or grids[om]).inv() and GRID_MASK
            osFree.forEachBit { os ->
                block(((om shl 4) + os).toByte())
            }
        }
    }

    /** Returns an array of all the available moves **/
    inline fun <reified T> getAvailableMoves(map: (Coord) -> T): Array<T> {
        val out = arrayOfNulls<T>(81)
        var size = 0

        // Store all moves in an array, return array
        forAvailableMoves { out[size++] = it.let(map) }
        return Arrays.copyOf(out, size)
    }

    /** Play a given coord on the board **/
    fun play(index: Coord): Boolean {
        val idx = index.toInt() and 0xFF  // remove sign extension
        val om = idx shr 4                  // top bits
        val os = idx and 0b1111          // lower bits

        val osShift = (1 shl os)
        val macroGridPlayer: Int

        // Update and extract player local board
        movesPlayed++
        lastMove = index
        if (nextPlayX) {
            grids[om] = grids[om] or osShift
            macroGridPlayer = grids[om] and GRID_MASK
        } else {
            grids[om] = grids[om] or (osShift shl GRID_BITS)
            macroGridPlayer = grids[om] shr GRID_BITS
        }

        // Check if the macro is won
        val omShift = (1 shl om)
        val macroWin = macroGridPlayer.gridWon()
        if (macroWin) {
            val mainGridPlayer: Int

            // Update and extract player global board
            openMacroMask = openMacroMask xor omShift
            if (nextPlayX) {
                mainGrid = mainGrid or omShift
                mainGridPlayer = mainGrid and GRID_MASK
            } else {
                mainGrid = mainGrid or (omShift shl GRID_BITS)
                mainGridPlayer = mainGrid shr GRID_BITS
            }

            // Check if the game is won
            if (mainGridPlayer.gridWon()) {
                openMacroMask = 0
                return true // early return to not touch nextPlayX
            }
        } else if (Integer.bitCount(grids[om]) == 9) {
            openMacroMask = openMacroMask xor omShift
        }

        if (openMacroMask == 0){
            tied = true
            nextPlayX = random.nextBoolean() // random hidden winner on tie
        } else nextPlayX = !nextPlayX
        return macroWin
    }

    /** Check owner of macro **/
    fun macro(macroIndex: Int): Player = when {
        mainGrid.hasBit(macroIndex) -> Player.PLAYER
        mainGrid.hasBit(macroIndex + 9) -> Player.ENEMY
        else -> Player.NEUTRAL
    }

    /** Check if macro is tied **/
    fun macroTied(macroIndex: Int) = (grids[macroIndex] and GRID_MASK or (grids[macroIndex] shr GRID_BITS)) == GRID_MASK

    /** Check if macro is part of win for player **/
    fun macroPartOfWin(macroIndex: Int): Boolean {
        if (!isDone || tied) return false
        val gridWinner = if (nextPlayX) (mainGrid and GRID_MASK) else (mainGrid shr GRID_BITS)
        val gridMinimal = WIN_GRIDS.filter { x -> ((gridWinner and x) == x) }.reduce { acc, winGrid -> acc or winGrid }
        return gridMinimal.hasBit(macroIndex)
    }

    /** Check owner of tile **/
    fun tile(index: Coord): Player {
        val om = (index.toInt() shr 4) and 0b1111      // top bits
        val os = index.toInt() and 0b1111          // lower bits
        return when {
            grids[om].hasBit(os) -> Player.PLAYER
            grids[om].hasBit(os + GRID_BITS) -> Player.ENEMY
            else -> Player.NEUTRAL
        }
    }

    fun Int.toCoordsNew() = ((this / 9 shl 4) or this % 9).toByte()
    fun toCompactString() = (0 until 81).joinToString("") {
        val tile = tile(it.toCoordsNew()).char.toString()
        if (it.toCoordsNew() == lastMove) tile.lowercase() else tile
    }

    override fun toString() = toString(true)
    fun toString(showAvailableMoves: Boolean) = (0 until 81).joinToString("") {
        val coord = toCoord((it % 9), it / 9) // stolen from normal Board
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
        result = 31 * result + mainGrid
        result = 31 * result + openMacroMask
        result = 31 * result + nextPlayX.hashCode()
        result = 31 * result + lastMove
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Board

        return if (!grids.contentEquals(other.grids)) false
        else if (mainGrid != other.mainGrid) false
        else if (openMacroMask != other.openMacroMask) false
        else if (nextPlayX != other.nextPlayX) false
        else if (lastMove != other.lastMove) false
        else true
    }
}

/** Check if bit at specified index is set **/
private inline fun Int.hasBit(index: Int) = (this shr index) and 1 != 0

/** Check in LUT if mask is won (input is a 9b tile mask) **/
internal inline fun Int.gridWon() = (WIN_GRID_LUT[this / 32] shr (this % 32)) and 1 != 0

/** Remove the last set bit of the mask **/
private inline fun Int.removeLastSetBit() = this and (this - 1)

/** Iterate over the set bits of a mask for each executing the given code block **/
internal inline fun Int.forEachBit(block: (index: Int) -> Unit) {
    var x = this
    while (x != 0) {
        block(Integer.numberOfTrailingZeros(x))
        x = x and (x - 1)
    }
}
