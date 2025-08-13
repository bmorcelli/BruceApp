package bruce.app

import java.awt.Desktop
import java.nio.file.Files

actual fun launchNavigator() {
    val html = navigatorHtml()
    val tempFile = Files.createTempFile("navigator", ".html")
    Files.writeString(tempFile, html)
    Desktop.getDesktop().browse(tempFile.toUri())
}
