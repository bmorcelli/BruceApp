package bruce.app

internal fun navigatorHtml(): String {
    val stream = NavigatorResource::class.java.classLoader.getResourceAsStream("navigator.html")
    return stream?.bufferedReader().use { it?.readText() } ?: ""
}

private object NavigatorResource
