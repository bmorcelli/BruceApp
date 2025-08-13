package bruce.app

import android.content.Intent

actual fun launchNavigator() {
    val context = MainActivity.instance
    val intent = Intent(context, NavigatorActivity::class.java)
    context.startActivity(intent)
}
