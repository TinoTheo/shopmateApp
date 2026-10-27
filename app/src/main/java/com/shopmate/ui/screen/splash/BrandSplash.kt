package com.shopmate.ui.screen.splash

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.shopmate.BuildConfig
import com.shopmate.R
import com.shopmate.ui.theme.Inter
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.DeviceLayout

/**
 * Brand splash, "Ink over" (design_handoff_owner_dashboard › ShopMate Logo Motion). It starts on
 * exactly the frame the system splash shows — the white M, centred — then inks the M in Checkout
 * Neon along its own stroke, moves it into its slot in the stacked lockup, settles "Shop" and
 * "ate", and brings the tagline in last. Plays once in 1.55s and holds. **No spinner, no
 * progress bar.** Reduced motion (animations off): a 200ms crossfade from the M to the lockup.
 *
 * The art is the brand owner's, used as supplied: four layers split from the stacked lockup
 * (drawable-nodpi/splash_*) and the tagline vector. The centreline below is a mask only — it is
 * never drawn, so the M is never redrawn by hand.
 */
@Composable
fun BrandSplash(deviceLayout: DeviceLayout, storeName: String) {
    val context = LocalContext.current
    val reduced = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    // One clock per launch: the root shows the splash in more than one state, and switching
    // between them must not replay it. Driven by frames, not a tween, so it still runs (as the
    // crossfade) when the system has animations off.
    var elapsed by remember { mutableLongStateOf(sinceLaunch()) }
    LaunchedEffect(Unit) {
        val end = if (reduced) REDUCED_MS else TOTAL_MS
        while (elapsed < end) withFrameNanos { elapsed = sinceLaunch() }
    }

    val white = ImageBitmap.imageResource(R.drawable.splash_m_white)
    val neon = ImageBitmap.imageResource(R.drawable.splash_m_neon)
    val shop = ImageBitmap.imageResource(R.drawable.splash_shop)
    val ate = ImageBitmap.imageResource(R.drawable.splash_ate)
    val tagline = rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_splash_tagline))
    val centreline = remember { PathParser().parsePathString(CENTRELINE).toPath() }
    val measure = remember { PathMeasure().apply { setPath(centreline, false) } }
    val trimmed = remember { Path() }
    val density = LocalDensity.current
    val description = stringResource(R.string.app_name)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(TradeGreen),
    ) {
        val lockupWidth = when (deviceLayout) {
            DeviceLayout.COMPACT -> min(maxWidth * 0.72f, 300.dp)
            DeviceLayout.EXPANDED -> min(maxWidth * 0.5f, 460.dp)
        }
        val lockupPx = with(density) { lockupWidth.toPx() }
        val splashMPx = with(density) { SPLASH_M_HEIGHT.toPx() }
        val risePx = with(density) { RISE.toPx() }

        Canvas(Modifier.fillMaxSize().semantics { contentDescription = description }) {
            val t = elapsed.toFloat()
            val k = lockupPx / UNITS_WIDE
            val origin = Offset((size.width - lockupPx) / 2f, (size.height - LOCKUP_UNITS_TALL * k) / 2f)
            val f = if (reduced) Frame.reduced(t) else Frame.at(t)

            // "Shop", "ate" and the tagline, in lockup units.
            translate(origin.x, origin.y) {
                scale(k, k, pivot = Offset.Zero) {
                    layer(shop, f.shop, risePx * f.shopRise / k)
                    layer(ate, f.ate, risePx * f.ateRise / k)
                    translate(TAGLINE_X, TAGLINE_Y) {
                        with(tagline) { draw(Size(TAGLINE_W, TAGLINE_H), alpha = f.tagline) }
                    }
                }
            }

            // The M: from the splash's centre and size to its slot in the lockup.
            val startScale = splashMPx / M_UNITS_TALL
            val sc = startScale + (k - startScale) * f.move
            val cx = size.width / 2f + (origin.x + M_CENTRE.x * k - size.width / 2f) * f.move
            val cy = size.height / 2f + (origin.y + M_CENTRE.y * k - size.height / 2f) * f.move
            translate(cx, cy) {
                scale(sc, sc, pivot = Offset.Zero) {
                    translate(-M_CENTRE.x, -M_CENTRE.y) {
                        if (f.white > 0f) layer(white, f.white, 0f)
                        if (f.neon > 0f) {
                            if (f.trace >= 1f) {
                                layer(neon, f.neon, 0f)
                            } else {
                                // Neon shown only under the trimmed centreline: the pen's path so far.
                                drawIntoCanvas { it.saveLayer(Rect(-100f, 0f, 700f, 1000f), Paint()) }
                                layer(neon, f.neon, 0f)
                                trimmed.reset()
                                measure.getSegment(0f, measure.length * f.trace, trimmed, true)
                                drawPath(
                                    trimmed,
                                    Color.White,
                                    style = Stroke(width = MASK_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round),
                                    blendMode = BlendMode.DstIn,
                                )
                                drawIntoCanvas { it.restore() }
                            }
                        }
                    }
                }
            }
        }

        if (deviceLayout == DeviceLayout.EXPANDED) {
            // 1i: store and version at the foot of the tablet splash.
            Text(
                text = listOfNotNull(storeName.ifBlank { null }, "v${BuildConfig.VERSION_NAME}")
                    .joinToString(" · "),
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    fontFeatureSettings = "tnum",
                    color = ShopWhite.copy(alpha = 0.6f),
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 36.dp),
            )
        }
    }
}

/** A layer of the lockup art: a 1200-unit-wide image, drawn at its natural size in lockup units. */
private fun DrawScope.layer(image: ImageBitmap, alpha: Float, riseUnits: Float) {
    if (alpha <= 0f) return
    drawImage(image, topLeft = Offset(0f, riseUnits), alpha = alpha.coerceIn(0f, 1f))
}

/** Where every layer is at [t] ms. Timings from the handoff's "Spec: A, Ink over". */
private class Frame(
    val trace: Float,
    val neon: Float,
    val white: Float,
    val move: Float,
    val shop: Float,
    /** Fraction of [RISE] still to travel. */
    val shopRise: Float,
    val ate: Float,
    val ateRise: Float,
    val tagline: Float,
) {
    companion object {
        fun at(t: Float): Frame {
            val trace = seg(t, 0f, 700f, Out)
            val shop = seg(t, 850f, 300f, Out)
            val ate = seg(t, 950f, 300f, Out)
            return Frame(
                trace = trace,
                neon = if (trace > 0f) 1f else 0f,
                white = if (t < 700f) 1f else 0f,
                move = seg(t, 550f, 500f, Std),
                shop = shop,
                shopRise = 1f - shop,
                ate = ate,
                ateRise = 1f - ate,
                tagline = seg(t, 1250f, 300f, Out),
            )
        }

        /** Animations off: the splash M fades out as the whole lockup fades in, in place. */
        fun reduced(t: Float): Frame {
            val p = seg(t, 0f, REDUCED_MS.toFloat(), null)
            return Frame(trace = 1f, neon = p, white = 0f, move = 1f, shop = p, shopRise = 0f, ate = p, ateRise = 0f, tagline = p)
        }

        private fun seg(t: Float, start: Float, duration: Float, easing: Easing?): Float {
            val p = ((t - start) / duration).coerceIn(0f, 1f)
            return easing?.transform(p) ?: p
        }
    }
}

private val Out = CubicBezierEasing(0f, 0f, 0.2f, 1f)
private val Std = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

private var launchedAt = 0L

/** Milliseconds since the first splash frame of this launch. */
private fun sinceLaunch(): Long {
    val now = SystemClock.uptimeMillis()
    if (launchedAt == 0L) launchedAt = now
    return now - launchedAt
}

private const val TOTAL_MS = 1_550L
private const val REDUCED_MS = 200L

/** "Shop" and "ate" settle up by this much as they fade in. */
private val RISE = 8.dp

/** Lockup units: 1200 × 945 (art 1200 × 823, tagline beneath). */
private const val UNITS_WIDE = 1200f
private const val LOCKUP_UNITS_TALL = 945f
private val M_CENTRE = Offset(215.5f, 544.5f)
private const val M_UNITS_TALL = 557f
private const val TAGLINE_X = 525f
private const val TAGLINE_Y = 868f
private const val TAGLINE_W = 620f
private const val TAGLINE_H = 73f
private const val MASK_STROKE = 86f

/** The M's centreline, in lockup units — a mask only, never drawn. */
private const val CENTRELINE =
    "M33 707 L33 390 A91 91 0 0 1 215 390 L215 615 A45.5 64 0 0 0 306 615 L306 563 A45.5 45 0 0 1 397 563 L397 790"

/**
 * The white M's height on the system splash: ic_splash_mark draws it at 40% of the splash
 * icon — measured 94dp on the tablet. Frame 0 starts the M at this size, centred, so the hand-off can't jump.
 */
private val SPLASH_M_HEIGHT = 94.dp
