package bruce.app

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity

class NavigatorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.loadDataWithBaseURL(null, navigatorHtml(), "text/html", "utf-8", null)
        setContentView(webView)
    }
}
