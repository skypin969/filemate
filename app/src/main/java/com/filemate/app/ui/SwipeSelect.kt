package com.filemate.app.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Menggeser satu baris ke kiri atau ke kanan memanggil [onSwipe] satu kali per geseran.
 * Dipakai untuk menandai berkas dengan geseran, seperti NP Manager.
 */
fun Modifier.swipeSelect(key: Any?, onSwipe: () -> Unit): Modifier = pointerInput(key) {
    val threshold = 36.dp.toPx()
    var total = 0f
    var fired = false
    detectHorizontalDragGestures(
        onDragStart = {
            total = 0f
            fired = false
        },
        onHorizontalDrag = { change, amount ->
            change.consume()
            total += amount
            if (!fired && abs(total) > threshold) {
                fired = true
                onSwipe()
            }
        },
    )
}
