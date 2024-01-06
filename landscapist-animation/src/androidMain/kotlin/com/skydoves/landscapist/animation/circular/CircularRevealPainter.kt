/*
 * Designed and developed by 2020-2023 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.skydoves.landscapist.animation.circular

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.core.util.Pools

/**
 * CircularRevealPainter is a [Painter] which animates a clipping circle to reveal an image.
 * Reveal animations provide users visual continuity when we show an image.
 *
 * @param imageBitmap an image bitmap for loading for the content.
 * @param painter an image painter to draw an [ImageBitmap] into the provided canvas.
 */
internal actual class CircularRevealPainter actual constructor(
  actual val imageBitmap: ImageBitmap,
  actual val painter: Painter,
) : Painter() {

  actual var radius by mutableStateOf(0f, policy = neverEqualPolicy())

  override fun DrawScope.onDraw() {
    var dx = 0f
    var dy = 0f
    var scale: Float
    val shaderMatrix = Matrix()
    val shader = ImageShader(imageBitmap, TileMode.Clamp)
    val brush = ShaderBrush(shader)
    val paint = paintPool.acquire() ?: Paint()
    paint.asFrameworkPaint().apply {
      isAntiAlias = true
      isDither = true
      isFilterBitmap = true
    }

    drawIntoCanvas { canvas ->
      // cache the paint in the internal stack.
      canvas.saveLayer(size.toRect(), paint)

      val mDrawableRect = RectF(0f, 0f, size.width, size.height)
      val bitmapWidth: Int = imageBitmap.width
      val bitmapHeight: Int = imageBitmap.height

      if (bitmapWidth * mDrawableRect.height() > mDrawableRect.width() * bitmapHeight) {
        scale = mDrawableRect.height() / bitmapHeight.toFloat()
        dx = (mDrawableRect.width() - bitmapWidth * scale) * 0.5f
      } else {
        scale = mDrawableRect.width() / bitmapWidth.toFloat()
        dy = (mDrawableRect.height() - bitmapHeight * scale) * 0.5f
      }

      // resize the matrix to scale by sx and sy.
      shaderMatrix.setScale(scale, scale)

      // post translate the matrix with the specified translation.
      shaderMatrix.postTranslate(
        (dx + 0.5f) + mDrawableRect.left,
        (dy + 0.5f) + mDrawableRect.top,
      )
      // apply the scaled matrix to the shader.
      shader.setLocalMatrix(shaderMatrix)
      // calculate radius and draw an image bitmap as a circle.
      val calculatedRadius = size.width.coerceAtLeast(size.height) * radius
      drawCircle(brush, calculatedRadius, Offset(size.width / 2, size.height / 2))
      // restore canvas.
      canvas.restore()
      // resets the paint and release to the pool.
      paint.asFrameworkPaint().reset()
      paintPool.release(paint)
    }
  }

  /** return the dimension size of the [painter]'s intrinsic width and height. */
  override val intrinsicSize: Size get() = painter.intrinsicSize
}

/** paint pool which caching and reusing [Paint] instances. */
private val paintPool = Pools.SimplePool<Paint>(2)