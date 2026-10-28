package com.brandonwallace.bastiontd.gl

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.Choreographer
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.brandonwallace.bastiontd.game.GameEngine
import kotlin.math.hypot

/**
 * OpenGL ES 3.0 SurfaceView with Choreographer VSync pacing and 3D orbit/pinch-zoom/raycast touch input.
 */
@SuppressLint("ViewConstructor")
class GameView(
    context: Context,
    val engine: GameEngine,
    private val onTapCallback: () -> Unit
) : GLSurfaceView(context), Choreographer.FrameCallback {

    val renderer = GameRenderer(engine)
    private var running = false
    private var skipFrameToggle = false

    private var prevX = 0f
    private var prevY = 0f
    private var downX = 0f
    private var downY = 0f
    private var isDragging = false
    private var multiTouchActive = false

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                multiTouchActive = true
                engine.zoomCamera(detector.scaleFactor)
                return true
            }
        }
    )

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                engine.resetCamera()
                onTapCallback()
                return true
            }
        }
    )

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 24, 0)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onResume() {
        super.onResume()
        if (!running) {
            running = true
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onPause() {
        if (running) {
            running = false
            Choreographer.getInstance().removeFrameCallback(this)
        }
        super.onPause()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        val batterySaver = engine.captureUiSnapshot().batterySaver
        if (batterySaver) {
            skipFrameToggle = !skipFrameToggle
            if (!skipFrameToggle) {
                requestRender()
            }
        } else {
            requestRender()
        }
        Choreographer.getInstance().postFrameCallback(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.x
                prevY = event.y
                downX = event.x
                downY = event.y
                isDragging = false
                multiTouchActive = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                multiTouchActive = true
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !multiTouchActive && !scaleDetector.isInProgress) {
                    val dx = event.x - prevX
                    val dy = event.y - prevY
                    val totalMove = hypot(event.x - downX, event.y - downY)
                    if (totalMove > 14f) {
                        isDragging = true
                    }
                    if (isDragging) {
                        engine.orbitCamera(-dx * 0.24f, dy * 0.16f)
                    }
                    prevX = event.x
                    prevY = event.y
                } else if (event.pointerCount >= 2) {
                    multiTouchActive = true
                    val cx = (event.getX(0) + event.getX(1)) * 0.5f
                    val cy = (event.getY(0) + event.getY(1)) * 0.5f
                    val dx = cx - prevX
                    val dy = cy - prevY
                    if (hypot(dx, dy) < 90f) {
                        engine.panCamera(-dx * 0.015f, -dy * 0.018f)
                    }
                    prevX = cx
                    prevY = cy
                }
            }

            MotionEvent.ACTION_UP -> {
                if (!isDragging && !multiTouchActive) {
                    renderer.handleTap(event.x, event.y, width, height)
                    onTapCallback()
                }
                multiTouchActive = false
                isDragging = false
            }
        }
        return true
    }
}
