package me.rpgz.treetools.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import android.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

fun makeQrBitmap(
    text: String,
    size: Int = 512,
    margin: Int = 1,
    ecc: ErrorCorrectionLevel = ErrorCorrectionLevel.M
): Bitmap {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to margin,
        EncodeHintType.ERROR_CORRECTION to ecc
    )
    val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    val bmp = createBitmap(size, size)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bmp[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    return bmp
}

@Composable
fun QrImage(data: String, size: Int = 512) {
    val bmp = remember(data, size) { makeQrBitmap(data, size) }
    Image(
        bitmap = bmp.asImageBitmap(),
        contentDescription = "QR",
        modifier = Modifier.size(size.dp)
    )
}
