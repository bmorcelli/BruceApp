package bruce.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

fun parseDumpToBytes(text: String): ByteArray {
    val startIdx = text.indexOf("AA")
    val cleaned = if (startIdx >= 0) text.substring(startIdx) else text
    val until = cleaned.substringBefore("[End of Dump]", cleaned)
    val regex = Regex("\\b[0-9A-Fa-f]{2}\\b")
    val hexPairs = regex.findAll(until).map { it.value }.toList()
    return hexPairs.map { it.toInt(16).toByte() }.toByteArray()
}

fun renderTft(data: ByteArray): ImageBitmap {
    var width = 320
    var height = 240
    var image = ImageBitmap(width, height)
    var canvas = Canvas(image)
    var offset = 0

    fun color565(c: Int): Color {
        val r = ((c shr 11) and 0x1F) * 255 / 31
        val g = ((c shr 5) and 0x3F) * 255 / 63
        val b = (c and 0x1F) * 255 / 31
        return Color(r, g, b)
    }

    while (offset < data.size) {
        if (data[offset] != 0xAA.toByte()) break
        val size = data[offset + 1].toInt() and 0xFF
        val fn = data[offset + 2].toInt() and 0xFF
        var pos = offset + 3

        fun readInt16(): Int {
            val v = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
            pos += 2
            return v
        }

        fun readInt8(): Int {
            val v = data[pos].toInt() and 0xFF
            pos += 1
            return v
        }

        fun readString(rem: Int): String {
            val bytes = data.copyOfRange(pos, pos + rem)
            pos += rem
            return bytes.toString(Charsets.UTF_8)
        }

        when (fn) {
            99 -> {
                width = readInt16()
                height = readInt16()
                readInt16() // rotation
                image = ImageBitmap(width, height)
                canvas = Canvas(image)
            }
            0 -> {
                val fg = color565(readInt16())
                canvas.drawRect(Rect(0f, 0f, width.toFloat(), height.toFloat()), Paint().apply { color = fg })
            }
            1 -> {
                val x = readInt16().toFloat()
                val y = readInt16().toFloat()
                val w = readInt16().toFloat()
                val h = readInt16().toFloat()
                val fg = color565(readInt16())
                canvas.drawRect(Rect(x, y, x + w, y + h), Paint().apply {
                    color = fg
                    style = PaintingStyle.Stroke
                    strokeWidth = 1f
                })
            }
            2 -> {
                val x = readInt16().toFloat()
                val y = readInt16().toFloat()
                val w = readInt16().toFloat()
                val h = readInt16().toFloat()
                val fg = color565(readInt16())
                canvas.drawRect(Rect(x, y, x + w, y + h), Paint().apply { color = fg })
            }
            5 -> {
                val x = readInt16().toFloat()
                val y = readInt16().toFloat()
                val r = readInt16().toFloat()
                val fg = color565(readInt16())
                canvas.drawCircle(Offset(x, y), r, Paint().apply {
                    color = fg
                    style = PaintingStyle.Stroke
                    strokeWidth = 1f
                })
            }
            6 -> {
                val x = readInt16().toFloat()
                val y = readInt16().toFloat()
                val r = readInt16().toFloat()
                val fg = color565(readInt16())
                canvas.drawCircle(Offset(x, y), r, Paint().apply { color = fg })
            }
            11 -> {
                val x = readInt16().toFloat()
                val y = readInt16().toFloat()
                val x1 = readInt16().toFloat()
                val y1 = readInt16().toFloat()
                val fg = color565(readInt16())
                canvas.drawLine(Offset(x, y), Offset(x1, y1), Paint().apply { color = fg; strokeWidth = 1f })
            }
            14, 15, 16, 17 -> {
                // Text rendering not supported in this simplified renderer.
                // Consume remaining bytes for this command.
                readInt16(); readInt16(); readInt16(); readInt16(); readInt16()
                val remaining = size - (pos - offset)
                if (remaining > 0) { readString(remaining) }
            }
            20 -> {
                val x = readInt16().toFloat()
                val y = readInt16().toFloat()
                val h = readInt16().toFloat()
                val fg = color565(readInt16())
                canvas.drawLine(Offset(x, y), Offset(x, y + h), Paint().apply { color = fg })
            }
            21 -> {
                val x = readInt16().toFloat()
                val y = readInt16().toFloat()
                val w = readInt16().toFloat()
                val fg = color565(readInt16())
                canvas.drawLine(Offset(x, y), Offset(x + w, y), Paint().apply { color = fg })
            }
        }
        offset += size
    }
    return image
}

@Composable
fun NavigatorDialog(
    image: ImageBitmap?,
    onNavigate: (String) -> Unit,
    onReload: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(width = ((image?.width ?: 320)).dp, height = ((image?.height ?: 240)).dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    image?.let {
                        Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row { NavButton("Pg↑", "nextpage", onNavigate); Spacer(Modifier.width(4.dp)); NavButton("▲", "up", onNavigate); Spacer(Modifier.width(4.dp)); NavButton("M", "menu", onNavigate) }
                    Spacer(Modifier.height(4.dp))
                    Row { NavButton("◀", "prev", onNavigate); Spacer(Modifier.width(4.dp)); NavButton("OK", "sel", onNavigate); Spacer(Modifier.width(4.dp)); NavButton("▶", "next", onNavigate) }
                    Spacer(Modifier.height(4.dp))
                    Row { NavButton("Pg↓", "prevpage", onNavigate); Spacer(Modifier.width(4.dp)); NavButton("▼", "down", onNavigate); Spacer(Modifier.width(4.dp)); NavButton("⟲", "esc", onNavigate) }
                }
            }
        },
        confirmButton = {
            Button(onClick = onReload) { Text("Recarregar") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
private fun NavButton(text: String, dir: String, onNavigate: (String) -> Unit) {
    Button(onClick = { onNavigate(dir) }, modifier = Modifier.size(48.dp)) {
        Text(text)
    }
}

