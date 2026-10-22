package com.shopmate

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.view.WindowCompat
import com.shopmate.ui.ShopMateRoot
import com.shopmate.ui.theme.ShopMateTheme
import com.shopmate.ui.util.toDeviceLayout
import com.shopmate.scan.HidScanSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var hidScanSource: HidScanSource

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Edge-to-edge: the Trade Green app frame (sidebar + status-bar strip) sits behind
        // the status bar, so force white status-bar icons; the white content sheet shows
        // through the bottom system bar, so use dark navigation-bar icons.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = true
        }
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            ShopMateTheme {
                ShopMateRoot(deviceLayout = windowSizeClass.widthSizeClass.toDeviceLayout())
            }
        }
    }

    /**
     * A keyboard-wedge scanner types the code and sends a terminator, exactly as a USB or
     * Bluetooth keyboard would, so its characters arrive here before any composable sees them.
     * Feeding them to [HidScanSource] is what turns those keystrokes back into one scanned code.
     *
     * Characters the scanner buffer consumes are swallowed so a scan never leaks into the
     * search field mid-sale. Anything else falls through to normal text input untouched.
     *
     * `RestrictedApi` is suppressed because it is a false positive here: this overrides the
     * framework's public `Activity.dispatchKeyEvent`, which `androidx.core.app.ComponentActivity`
     * re-declares with `@RestrictTo(LIBRARY_GROUP_PREFIX)`. Compose's `onPreviewKeyEvent` is not a
     * substitute — it only sees keys while something is focused, and a till waiting for a scan
     * often has nothing focused.
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val char = event.unicodeChar.toChar()
            val terminator = event.keyCode == KeyEvent.KEYCODE_ENTER
            if (terminator || char.isLetterOrDigit()) {
                val consumed = hidScanSource.onCharacter(if (terminator) '\n' else char)
                if (consumed) return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
