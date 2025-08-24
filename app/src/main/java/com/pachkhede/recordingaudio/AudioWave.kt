package com.pachkhede.recordingaudio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class AudioWave(context: Context, attrs: AttributeSet? = null): View(context, attrs){
    private var data = shortArrayOf()

    val paint : Paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 4f
        isAntiAlias = true
    }

    fun updateAudioData(data: ShortArray) {
        this.data = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) return

        val midY = height / 2f
        val scale = height / 2f / Short.MAX_VALUE

        var lastX = 0f
        var lastY = midY

        for (i in data.indices) {
            val x = i * (width.toFloat() / data.size)
            val y = midY - (data[i] * scale)
            canvas.drawLine(lastX, lastY, x, y, paint)
            lastX = x
            lastY = y
        }

    }



}