package com.example.vga.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A barely-visible dot-grid, drawn behind screen content. Purely decorative —
 * spacing is wide and opacity is low enough that it never competes with
 * foreground text or cards.
 */
@Composable
fun DotGridBackground(
    modifier: Modifier = Modifier,
    dotColor: Color = Color(0xFF2D3142).copy(alpha = 0.035f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val spacing = 26.dp.toPx()
        val radius = 1.1.dp.toPx()

        var y = spacing / 2
        while (y < size.height) {
            var x = spacing / 2
            while (x < size.width) {
                drawCircle(
                    color = dotColor,
                    radius = radius,
                    center = Offset(x, y)
                )
                x += spacing
            }
            y += spacing
        }
    }
}
