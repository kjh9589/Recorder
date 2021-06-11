package com.teamnoyes.recorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class SoundVisualizerView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onRequestCurrentAmplitude: (() -> Int)? = null

    // paint는 이런 모양으로 그려달라
    private val amplitudePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.purple_500)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = LINE_WIDTH
    }
    private var drawingWidth: Int = 0
    private var drawingHeight: Int = 0
    private var drawingAmplitudes: List<Int> = emptyList()
    private var isReplaying: Boolean = false
    private var replayingPosition = 0

    // 반복하여 자기자신을 호출하도록 Runnable 구현
    private val visualizeRepeatAction: Runnable = object : Runnable {
        override fun run() {
            if (!isReplaying) {
                // invoke 이름 없이 호출 가능
                val currentAmplitude = onRequestCurrentAmplitude?.invoke() ?: 0
                // 오른쪽에서 왼쪽으로 그리므로 마지막 aplitued가 첫번째로 그려져야한다.
                drawingAmplitudes = listOf(currentAmplitude) + drawingAmplitudes
            } else {
                replayingPosition++
            }

            // onDraw 재호출 뷰 갱신을 위한
            invalidate()

            // 자신을 20 milliSec 뒤에 호출하도록 함
            // 종료 전까지 계속 그림
            handler?.postDelayed(this, ACTION_INTERVAL)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        drawingWidth = w
        drawingHeight = h
    }

    // canvas는 무엇을 그릴 지를 결정한다.
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        // 뷰의 높이의 중앙
        val centerY = drawingHeight / 2f
        // 시작 영역 가로 길이의 오른쪽 끝
        var offsetX = drawingWidth.toFloat()

        drawingAmplitudes
            .let { amplitudes ->
                if (isReplaying) {
                    amplitudes.takeLast(replayingPosition)
                } else {
                    amplitudes
                }
            }
            .forEach { amplitude ->
                // 현재 그릴려는 진폭의 값 / 진폭 최대값 * 그릴려고 하는 뷰의 높이 * 0.8(100%라면 뷰가 꽉차기에 0.8 곱해줌)
                val lineLength = amplitude / MAX_AMPLITUDE * drawingHeight * 0.8F

                // x축에 어느 부분에 그릴 것인가
                offsetX -= LINE_SPACE
                // amplitude가 많을 때 view에서 다 못 그리고 왼쪽으로 벗어나는 것들은 그리지 않는다.
                if (offsetX < 0) return@forEach

                // 선 그리기
                // 시작점 x, y 끝나는 점 x, y 그릴 Paint
                canvas.drawLine(
                    offsetX,
                    centerY - lineLength / 2F,
                    offsetX,
                    centerY + lineLength / 2F,
                    amplitudePaint
                )
            }
    }

    fun startVisualizing(isReplaying: Boolean) {
        this.isReplaying = isReplaying
        handler?.post(visualizeRepeatAction)
    }

    fun stopVisualizing() {
        replayingPosition = 0
        handler?.removeCallbacks(visualizeRepeatAction)
    }

    fun clearVisualization() {
        drawingAmplitudes = emptyList()
        invalidate()
    }

    companion object {
        private const val LINE_WIDTH = 10F
        private const val LINE_SPACE = 15F
        private const val MAX_AMPLITUDE = Short.MAX_VALUE.toFloat()
        // 20 millisecond로 그리면 부드럽다
        private const val ACTION_INTERVAL = 20L
    }
}