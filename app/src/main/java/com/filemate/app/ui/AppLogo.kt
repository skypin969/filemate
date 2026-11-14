package com.filemate.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filemate.app.R

/**
 * Ikon Filemate di menu samping dan layar pembuka.
 * Warna awal memakai gambar hitam putih bawaan. Warna tema lain memakai kotak putih
 * dengan folder berwarna tema, sama seperti ikon peluncur.
 */
@Composable
fun FilemateLogo(
    size: Dp,
    corner: Dp,
    elevation: Dp = 0.dp,
    description: String? = null,
) {
    val shape = RoundedCornerShape(corner)
    val sizedModifier = Modifier
        .size(size)
        .shadow(elevation, shape)
        .clip(shape)
    if (ThemeColors.primary == ThemeColors.DEFAULT) {
        Image(
            painter = painterResource(R.drawable.logo_filemate),
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = sizedModifier,
        )
    } else {
        val folder = Color(ThemeColors.primary)
        val logo = remember(folder) { themedLogo(folder) }
        Image(
            painter = rememberVectorPainter(logo),
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = sizedModifier,
        )
    }
}

private const val TILE_PATH = "M0,0h108v108h-108z"
private const val FOLDER_PATH =
    "M30,38a4,4 0,0 1,4 -4h13l5,5h22a4,4 0,0 1,4 4v27a4,4 0,0 1,-4 4h-40a4,4 0,0 1,-4 -4z"
private const val GLYPH_PATH =
    "M40.3,57.4C41.1,55.5 43.3,55.4 44,57.3L47.3,66.6L53.5,48.6Q54.1,46.9 55.9,46.9H67.7M57,57H65"

/** Ikon vektor untuk warna tema [folder]: kotak putih, folder berwarna tema, huruf putih. */
private fun themedLogo(folder: Color): ImageVector =
    ImageVector.Builder(
        name = "FilemateTema",
        defaultWidth = 108.dp,
        defaultHeight = 108.dp,
        viewportWidth = 108f,
        viewportHeight = 108f,
    )
        .addPath(pathData = addPathNodes(TILE_PATH), fill = SolidColor(Color.White))
        .addPath(pathData = addPathNodes(FOLDER_PATH), fill = SolidColor(folder))
        .addPath(
            pathData = addPathNodes(GLYPH_PATH),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 3f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
