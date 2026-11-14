package app.crowdhub.core.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import app.crowdhub.BuildConfig

@Composable
fun SecureScreenEffect() {
    val context = LocalContext.current
    val enforceSecure = rememberShouldEnforceFlagSecure()
    DisposableEffect(context, enforceSecure) {
        if (!enforceSecure) {
            return@DisposableEffect onDispose {}
        }
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

private fun rememberShouldEnforceFlagSecure(): Boolean {
    return !BuildConfig.DEBUG && !isEmulatorDevice()
}

internal fun isEmulatorDevice(): Boolean {
    val fingerprint = Build.FINGERPRINT.lowercase()
    val model = Build.MODEL.lowercase()
    val hardware = Build.HARDWARE.lowercase()
    val product = Build.PRODUCT.lowercase()
    val manufacturer = Build.MANUFACTURER.lowercase()
    val brand = Build.BRAND.lowercase()
    val device = Build.DEVICE.lowercase()
    return fingerprint.startsWith("generic") ||
        fingerprint.startsWith("unknown") ||
        fingerprint.contains("emulator") ||
        fingerprint.contains("sdk_gphone") ||
        fingerprint.contains("vsoc") ||
        model.contains("google_sdk") ||
        model.contains("emulator") ||
        model.contains("android sdk built for") ||
        model.contains("cuttlefish") ||
        hardware.contains("goldfish") ||
        hardware.contains("ranchu") ||
        hardware.contains("cutf") ||
        product.contains("sdk") ||
        product.contains("emulator") ||
        product.contains("simulator") ||
        product.contains("aosp_cf") ||
        product.contains("vbox") ||
        device.contains("emu") ||
        device.contains("vsoc") ||
        manufacturer.contains("genymotion") ||
        (brand.startsWith("generic") && device.startsWith("generic"))
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
