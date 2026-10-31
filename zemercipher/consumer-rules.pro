# Consumer proguard rules for zemer-cipher library

# Keep JavaScript interface methods
-keepclassmembers class com.zemer.cipher.CipherWebView {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class com.zemer.cipher.potoken.PoTokenWebView {
    @android.webkit.JavascriptInterface <methods>;
}
