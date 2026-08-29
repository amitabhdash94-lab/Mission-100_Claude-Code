package com.neurofit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neurofit.app.ui.theme.MonoFamily
import com.neurofit.app.ui.theme.NeuroColors
import com.neurofit.app.ui.theme.NeuroFitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // NEUROFIT is dark only. Without an explicit style enableEdgeToEdge follows the
        // system light/dark setting, so on a phone in light mode the status bar and
        // gesture handle would be drawn dark on our near black background and vanish.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContent {
            NeuroFitTheme {
                BootScreen()
            }
        }
    }
}

/**
 * Phase 1 placeholder. Its only job is to prove that the CI produced APK
 * installs and launches on the device. Replaced by the real navigation shell
 * in Phase 6.
 */
@Composable
fun BootScreen(modifier: Modifier = Modifier) {
    val panelDescription = stringResource(R.string.cd_boot_panel)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeuroColors.BackgroundVoid)
    ) {
        ScanlineOverlay(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp)
                .semantics { contentDescription = panelDescription },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PulseRule()

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.boot_status),
                style = MaterialTheme.typography.displayLarge,
                color = NeuroColors.NeonCyan,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.boot_subtitle),
                style = MaterialTheme.typography.titleLarge,
                color = NeuroColors.NeonMagenta,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            PulseRule()

            Spacer(modifier = Modifier.height(36.dp))

            TerminalRow(
                label = stringResource(R.string.boot_build_label),
                value = "v${BuildConfig.VERSION_NAME} / ${BuildConfig.VERSION_CODE}",
                accent = NeuroColors.AcidGreen
            )
            TerminalRow(
                label = stringResource(R.string.boot_pipeline_label),
                value = stringResource(R.string.boot_pipeline_value),
                accent = NeuroColors.Amber
            )
            TerminalRow(
                label = stringResource(R.string.boot_data_label),
                value = stringResource(R.string.boot_data_value),
                accent = NeuroColors.NeonCyan
            )
        }
    }
}

/** Key value readout in mono type with a leading marker. Precursor to TerminalRow in Phase 2. */
@Composable
private fun TerminalRow(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.boot_row_marker),
            style = MaterialTheme.typography.labelMedium,
            color = accent
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = NeuroColors.TextSecondary,
            modifier = Modifier.width(88.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = MonoFamily,
            color = NeuroColors.TextPrimary
        )
    }
}

/** Slow breathing horizontal rule. Frame independent, driven by the animation clock. */
@Composable
private fun PulseRule(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        drawLine(
            color = NeuroColors.NeonCyan.copy(alpha = alpha),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height
        )
    }
}

/** Static scanlines at low alpha. Becomes a toggleable overlay in Phase 2. */
@Composable
private fun ScanlineOverlay(modifier: Modifier = Modifier) {
    // graphicsLayer gives this Canvas its own retained render node. Without it the
    // sibling PulseRule animation invalidates the whole view every frame and these
    // several hundred drawLine calls are re-recorded forever, for a static overlay.
    Canvas(modifier = modifier.graphicsLayer()) {
        val spacing = 4f
        val lineColor = NeuroColors.TextPrimary.copy(alpha = 0.03f)
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += spacing
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF04060B)
@Composable
private fun BootScreenPreview() {
    NeuroFitTheme {
        BootScreen()
    }
}
