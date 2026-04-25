package com.ayko.trackitt.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CookieIcon(
    fillColor: Color,
    modifier: Modifier = Modifier,
    strokeColor: Color? = null,
    strokeWidth: Dp = 2.dp,
    size: Dp = 56.dp,
    content: @Composable () -> Unit = {},
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size),
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val sx = w / 56f
            val sy = h / 56f

            val path = Path().apply {
                moveTo(28f * sx, 1f * sy)
                quadraticBezierTo(35f * sx, 8f * sy, 45f * sx, 7f * sy)
                quadraticBezierTo(46f * sx, 17f * sy, 55f * sx, 23f * sy)
                quadraticBezierTo(49f * sx, 31f * sy, 51f * sx, 42f * sy)
                quadraticBezierTo(41f * sx, 44f * sy, 37f * sx, 53f * sy)
                quadraticBezierTo(28f * sx, 49f * sy, 19f * sx, 53f * sy)
                quadraticBezierTo(15f * sx, 44f * sy, 5f * sx, 42f * sy)
                quadraticBezierTo(7f * sx, 31f * sy, 1f * sx, 23f * sy)
                quadraticBezierTo(10f * sx, 17f * sy, 11f * sx, 7f * sy)
                quadraticBezierTo(21f * sx, 8f * sy, 28f * sx, 1f * sy)
                close()
            }

            drawPath(path, fillColor, style = Fill)
            if (strokeColor != null) {
                drawPath(path, strokeColor, style = Stroke(width = strokeWidth.toPx()))
            }
        }
        content()
    }
}
