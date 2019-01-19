package com.flaghacker.sttt.common

class Timer(private val length: Long) {
	private var start: Long? = null
	var isInterrupted: Boolean = false
	val running get() = timeLeft() > 0

	fun started() = start != null
	fun start() = apply {
		if (start != null)
			throw IllegalStateException("this Timer has already been started")

		start = System.currentTimeMillis()
	}

	fun interrupt() {
		this.isInterrupted = true
	}

	fun timeLeft(): Long {
		val start = start ?: throw IllegalStateException("this Timer hasn't been started yet")

		val left = length - (System.currentTimeMillis() - start)
		return if (left > 0 && !isInterrupted) left else 0
	}
}
