package com.flaghacker.sttt.common

interface Bot {
	fun move(board: Board, timer: Timer): Byte?
}
