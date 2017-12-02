package com.flaghacker.sttt.common

interface KotlinBot {
    fun move(board: KotlinBoard, timer: Timer): Byte?
}
