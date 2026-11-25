package com.hefimer.app.ui.theme

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.intellij.lang.annotations.Language

/**
 * Apple Glass Optical Engine (from dev-hkm/apple-glass skill).
 * - Strict Monochrome polarity in both OLED Dark & Apple Light modes (Emerald only for Secret R2).
 * - Tier 1: Zero-dependency AGSL RuntimeShader Liquid Glass Lens (API 33+).
 * - Tier 3: Luminous Frosted Surface with diagonal Fresnel specular catchlight & overhead room bounce sheen (API 26+).
 * - Accessibility Firewall: rememberReduceTransparency() guarantees >= 4.5:1 legibility when High Contrast is enabled.
 */
enum class AppleGlassVariant {
    /** Floating navigation chrome, docks, banners, bottom sheets */
    Elevated,
    /** Standard cards, composer containers, toolstrips */
    Regular,
    /** Subtle floating HUD pills & compact overlays */
    Clear,
}

@Composable
fun rememberReduceTransparency(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Secure.getInt(context.contentResolver, "high_text_contrast_enabled", 0) == 1
        } catch (_: Exception) {
            false
        }
    }
}

@Language("AGSL")
private const val LIQUID_GLASS_LENS_AGSL = """
    uniform shader content;
    uniform float2 size;
    uniform float cornerRadius;
    uniform float bezelWidth;
    uniform float refractionStrength;
    uniform float dispersion;
    uniform float isLight;

    float sdRoundedBox(float2 p, float2 b, float r) {
        float2 q = abs(p) - b + r;
        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    }

    half4 main(float2 fragCoord) {
        float2 halfSize = size * 0.5;
        float2 p = fragCoord - halfSize;
        float maxR = min(cornerRadius, min(halfSize.x, halfSize.y));
        float d = sdRoundedBox(p, halfSize, maxR);

        if (d > 0.0) {
            return content.eval(fragCoord);
        }

        float eps = 1.0;
        float2 grad = float2(
            sdRoundedBox(p + float2(eps, 0.0), halfSize, maxR) - sdRoundedBox(p - float2(eps, 0.0), halfSize, maxR),
            sdRoundedBox(p + float2(0.0, eps), halfSize, maxR) - sdRoundedBox(p - float2(0.0, eps), halfSize, maxR)
        );
        float gradLen = length(grad);
        float2 normal2D = gradLen > 0.0001 ? grad / gradLen : float2(0.0, 0.0);

        float bezel = smoothstep(-bezelWidth, 0.0, d);
        float lensProfile = pow(bezel, 0.85);
        float2 offset = normal2D * (refractionStrength * bezel * lensProfile);

        half r = content.eval(clamp(fragCoord - offset * (1.0 - dispersion), float2(0.0), size)).r;
        half g = content.eval(clamp(fragCoord - offset, float2(0.0), size)).g;
        half b = content.eval(clamp(fragCoord - offset * (1.0 + dispersion), float2(0.0), size)).b;
        half4 refracted = half4(r, g, b, content.eval(fragCoord).a);

        float3 surfaceN = normalize(float3(normal2D * bezel, max(0.28, 1.0 - bezel)));
        float3 lightDir = normalize(float3(-0.55, -0.75, 0.55));
        float spec = pow(max(dot(surfaceN, lightDir), 0.0), 12.0) * bezel;
        float rimGlow = pow(bezel, 2.0) * (isLight > 0.5 ? 0.36 : 0.22);

        half3 glassTint = isLight > 0.5 ? half3(0.98, 0.98, 0.99) : half3(0.14, 0.14, 0.16);
        float tintMix = isLight > 0.5 ? 0.16 : 0.14;

        half3 lit = mix(refracted.rgb, glassTint, tintMix * bezel);
        lit += half3(1.0) * half(spec * 0.28 + rimGlow);

        return half4(lit, refracted.a);
    }
"""

/**
 * Applies API 33+ AGSL curved bevel refraction & Fresnel specular catchlight over a UI surface,
 * degrading cleanly to a no-op on API < 33 without crash.
 */
fun Modifier.appleLiquidRefraction(
    cornerRadius: Dp = 24.dp,
    bezelWidth: Dp = 10.dp,
    refractionPx: Float = 6f,
    dispersion: Float = 0.0f,
    isLightMode: Boolean = HefimerColors.isLightMode,
): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@composed this

    val density = LocalDensity.current
    val widthPx = remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    val heightPx = remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    val shader = remember { RuntimeShader(LIQUID_GLASS_LENS_AGSL) }

    this
        .onSizeChanged {
            widthPx.floatValue = it.width.toFloat().coerceAtLeast(1f)
            heightPx.floatValue = it.height.toFloat().coerceAtLeast(1f)
        }
        .graphicsLayer {
            shader.setFloatUniform("size", widthPx.floatValue, heightPx.floatValue)
            shader.setFloatUniform("cornerRadius", with(density) { cornerRadius.toPx() })
            shader.setFloatUniform("bezelWidth", with(density) { bezelWidth.toPx() })
            shader.setFloatUniform("refractionStrength", refractionPx)
            shader.setFloatUniform("dispersion", dispersion)
            shader.setFloatUniform("isLight", if (isLightMode) 1f else 0f)
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
            clip = true
        }
}

/**
 * Applies full Apple Glass optical pipeline (shadow depth + luminance-adaptive translucency +
 * overhead room bounce sheen + grazing diagonal Fresnel 1dp specular rim).
 * Strictly monochrome unless accented for Secret R2 Mode.
 */
fun Modifier.appleGlassSurface(
    shape: Shape = RoundedCornerShape(30.dp),
    variant: AppleGlassVariant = AppleGlassVariant.Regular,
    accentBorderTint: Color? = null,
    accentFillTint: Color? = null,
    elevation: Dp = when (variant) {
        AppleGlassVariant.Elevated -> 18.dp
        AppleGlassVariant.Regular -> 8.dp
        AppleGlassVariant.Clear -> 2.dp
    },
): Modifier = composed {
    val isLight = HefimerColors.isLightMode
    val reduceTransparency = rememberReduceTransparency()

    val fillAlpha = when {
        reduceTransparency -> 0.96f
        variant == AppleGlassVariant.Elevated -> if (isLight) 0.80f else 0.72f
        variant == AppleGlassVariant.Regular -> if (isLight) 0.72f else 0.56f
        else -> if (isLight) 0.44f else 0.08f
    }

    val baseFill = accentFillTint ?: if (isLight) {
        Color(0xFFFFFFFF).copy(alpha = fillAlpha)
    } else if (variant == AppleGlassVariant.Clear) {
        Color(0xFFFFFFFF).copy(alpha = fillAlpha)
    } else {
        Color(0xFF131316).copy(alpha = fillAlpha)
    }

    val topSheenAlpha = when (variant) {
        AppleGlassVariant.Elevated -> if (isLight) 0.65f else 0.14f
        AppleGlassVariant.Regular -> if (isLight) 0.52f else 0.105f
        AppleGlassVariant.Clear -> if (isLight) 0.36f else 0.075f
    }
    val bottomBounceAlpha = when (variant) {
        AppleGlassVariant.Elevated -> if (isLight) 0.18f else 0.045f
        AppleGlassVariant.Regular -> if (isLight) 0.12f else 0.032f
        AppleGlassVariant.Clear -> if (isLight) 0.06f else 0.020f
    }

    val innerSheen = remember(isLight, topSheenAlpha, bottomBounceAlpha) {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = topSheenAlpha),
            0.35f to Color.White.copy(alpha = topSheenAlpha * 0.28f),
            0.78f to Color.Transparent,
            1.0f to Color.White.copy(alpha = bottomBounceAlpha),
        )
    }

    val fresnelRim = remember(isLight, accentBorderTint, variant) {
        if (accentBorderTint != null) {
            Brush.linearGradient(
                0.0f to accentBorderTint.copy(alpha = 0.68f),
                0.36f to accentBorderTint.copy(alpha = 0.22f),
                0.70f to Color.Black.copy(alpha = if (isLight) 0.06f else 0.22f),
                1.0f to accentBorderTint.copy(alpha = 0.38f),
            )
        } else if (isLight) {
            val darkEdge = when (variant) {
                AppleGlassVariant.Elevated -> 0.12f
                AppleGlassVariant.Regular -> 0.10f
                AppleGlassVariant.Clear -> 0.08f
            }
            Brush.linearGradient(
                0.0f to Color.White.copy(alpha = 0.98f),
                0.30f to Color(0xFF0B0B0C).copy(alpha = darkEdge),
                0.70f to Color(0xFF0B0B0C).copy(alpha = darkEdge + 0.03f),
                1.0f to Color.White.copy(alpha = 0.78f),
            )
        } else {
            val topAlpha = when (variant) {
                AppleGlassVariant.Elevated -> 0.42f
                AppleGlassVariant.Regular -> 0.32f
                AppleGlassVariant.Clear -> 0.24f
            }
            val tailAlpha = when (variant) {
                AppleGlassVariant.Elevated -> 0.22f
                AppleGlassVariant.Regular -> 0.16f
                AppleGlassVariant.Clear -> 0.12f
            }
            Brush.linearGradient(
                0.0f to Color.White.copy(alpha = topAlpha),
                0.34f to Color.White.copy(alpha = 0.08f),
                0.70f to Color.Black.copy(alpha = 0.22f),
                1.0f to Color.White.copy(alpha = tailAlpha),
            )
        }
    }

    val ambientShadowAlpha = if (isLight) 0.08f else 0.46f
    val spotShadowAlpha = if (isLight) 0.12f else 0.62f

    val withShadow = if (elevation > 0.dp) {
        this.shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = ambientShadowAlpha),
            spotColor = Color.Black.copy(alpha = spotShadowAlpha),
        )
    } else {
        this
    }

    withShadow
        .clip(shape)
        .background(baseFill)
        .background(innerSheen)
        .border(width = 1.dp, brush = fresnelRim, shape = shape)
}

@Composable
fun AppleGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(30.dp),
    variant: AppleGlassVariant = AppleGlassVariant.Regular,
    accentBorderTint: Color? = null,
    accentFillTint: Color? = null,
    elevation: Dp = when (variant) {
        AppleGlassVariant.Elevated -> 18.dp
        AppleGlassVariant.Regular -> 8.dp
        AppleGlassVariant.Clear -> 2.dp
    },
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.appleGlassSurface(
            shape = shape,
            variant = variant,
            accentBorderTint = accentBorderTint,
            accentFillTint = accentFillTint,
            elevation = elevation,
        ),
        content = content,
    )
}

/**
 * Ambient monochrome studio depth field behind scrollable content & floating glass chrome.
 * Ensures floating Apple Glass surfaces always refract & modulate subtle tonal depth
 * rather than floating over flat dead void (strict monochrome; emerald glow reserved for Secret R2).
 */
@Composable
fun AppleMonochromeBackdrop(
    isR2Mode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isLight = HefimerColors.isLightMode
    val baseColor = if (isLight) Color(0xFFE7E9EF) else Color(0xFF030305)
    val primaryGlowTarget = when {
        isR2Mode -> Color(0xFF10B981).copy(alpha = if (isLight) 0.16f else 0.13f)
        isLight -> Color(0xFFFFFFFF).copy(alpha = 0.96f)
        else -> Color.White.copy(alpha = 0.13f)
    }
    val secondaryGlowTarget = when {
        isR2Mode -> Color(0xFF059669).copy(alpha = if (isLight) 0.10f else 0.08f)
        isLight -> Color(0xFFC9CDD8).copy(alpha = 0.72f)
        else -> Color.White.copy(alpha = 0.075f)
    }
    val centerSweepTarget = when {
        isR2Mode -> Color(0xFF10B981).copy(alpha = if (isLight) 0.06f else 0.045f)
        isLight -> Color(0xFFFFFFFF).copy(alpha = 0.45f)
        else -> Color.White.copy(alpha = 0.04f)
    }
    val primaryGlow = animateColorAsState(
        targetValue = primaryGlowTarget,
        animationSpec = tween(AppMotion.Content, easing = AppMotion.AppleSilk),
        label = "backdrop_primary_glow",
    )
    val secondaryGlow = animateColorAsState(
        targetValue = secondaryGlowTarget,
        animationSpec = tween(AppMotion.Content, easing = AppMotion.AppleSilk),
        label = "backdrop_secondary_glow",
    )
    val centerSweep = animateColorAsState(
        targetValue = centerSweepTarget,
        animationSpec = tween(AppMotion.Content, easing = AppMotion.AppleSilk),
        label = "backdrop_center_sweep",
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor),
    ) {
        val w = size.width
        val h = size.height
        val radiusTop = maxOf(w, h) * 0.72f
        drawCircle(
            brush = Brush.radialGradient(
                0f to primaryGlow.value,
                0.50f to primaryGlow.value.copy(alpha = primaryGlow.value.alpha * 0.30f),
                1f to Color.Transparent,
                center = Offset(w * 0.18f, h * 0.10f),
                radius = radiusTop,
            ),
            radius = radiusTop,
            center = Offset(w * 0.18f, h * 0.10f),
        )
        val radiusMid = maxOf(w, h) * 0.56f
        drawCircle(
            brush = Brush.radialGradient(
                0f to centerSweep.value,
                0.55f to centerSweep.value.copy(alpha = centerSweep.value.alpha * 0.25f),
                1f to Color.Transparent,
                center = Offset(w * 0.52f, h * 0.46f),
                radius = radiusMid,
            ),
            radius = radiusMid,
            center = Offset(w * 0.52f, h * 0.46f),
        )
        val radiusBottom = maxOf(w, h) * 0.66f
        drawCircle(
            brush = Brush.radialGradient(
                0f to secondaryGlow.value,
                0.55f to secondaryGlow.value.copy(alpha = secondaryGlow.value.alpha * 0.25f),
                1f to Color.Transparent,
                center = Offset(w * 0.88f, h * 0.86f),
                radius = radiusBottom,
            ),
            radius = radiusBottom,
            center = Offset(w * 0.88f, h * 0.86f),
        )
    }
}
