package com.philkes.notallyx.misc.appstore

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

@Composable
fun DeviceFrame(
    title: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    deviceFrameColor: Color = Color(0xFF1E1E1E),
    statusBarColor: Color = MaterialTheme.colorScheme.surface,
    statusBarContentColor: Color = MaterialTheme.colorScheme.onSurface,
    screenContent: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    backgroundColor,
                                    backgroundColor.copy(alpha = 0.75f),
                                )
                        )
                )
                .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        val deviceFrameRounding = 48.dp
        val contentRounding = deviceFrameRounding - 10.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 1. Marketing Header Text (Fixed height so title line count does not alter phone case
            // dimensions)
            Box(
                modifier = Modifier.fillMaxWidth().height(112.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = contentColorFor(backgroundColor),
                    lineHeight = 36.sp,
                    maxLines = 3,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. Hardware Device Frame & Case with Buttons
            val phoneWidth = 400.dp
            val phoneHeight = 820.dp

            Box(modifier = Modifier.width(phoneWidth).height(phoneHeight)) {
                // Main Phone Body
                val buttonsRounding = 12.dp
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .shadow(
                                24.dp,
                                RoundedCornerShape(deviceFrameRounding),
                                spotColor = Color.DarkGray,
                            )
                            .background(deviceFrameColor, RoundedCornerShape(deviceFrameRounding))
                            .padding(10.dp)
                            .clip(RoundedCornerShape(contentRounding))
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(statusBarColor)) {

                        // A. App Screen Content
                        Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp)) {
                            screenContent()
                        }

                        // B. Status Bar
                        CompositionLocalProvider(LocalContentColor provides statusBarContentColor) {
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .height(36.dp)
                                        .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "9:41",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusBarContentColor,
                                )

                                StatusBar(contentColor = statusBarContentColor)
                            }
                        }

                        // C. Punch-hole Camera
                        Box(
                            modifier =
                                Modifier.align(Alignment.TopCenter)
                                    .padding(top = 10.dp)
                                    .size(22.dp)
                                    .background(Color.Black, CircleShape)
                        )
                    }
                }

                // --- Hardware Buttons on the Right Side ---

                // Power Button (Upper Right)
                Box(
                    modifier =
                        Modifier.align(Alignment.TopEnd)
                            .padding(top = 180.dp)
                            .offset(x = 4.dp)
                            .width(4.dp)
                            .height(55.dp)
                            .background(
                                color = deviceFrameColor,
                                shape =
                                    RoundedCornerShape(
                                        topEnd = buttonsRounding,
                                        bottomEnd = buttonsRounding,
                                    ),
                            )
                )

                // Volume Buttons (Below Power Button)
                Column(
                    modifier =
                        Modifier.align(Alignment.TopEnd).padding(top = 255.dp).offset(x = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Volume Up
                    Box(
                        modifier =
                            Modifier.width(4.dp)
                                .height(45.dp)
                                .background(
                                    color = deviceFrameColor,
                                    shape =
                                        RoundedCornerShape(
                                            topEnd = buttonsRounding,
                                            bottomEnd = buttonsRounding,
                                        ),
                                )
                    )
                    // Volume Down
                    Box(
                        modifier =
                            Modifier.width(4.dp)
                                .height(45.dp)
                                .background(
                                    color = deviceFrameColor,
                                    shape =
                                        RoundedCornerShape(
                                            topEnd = buttonsRounding,
                                            bottomEnd = buttonsRounding,
                                        ),
                                )
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBar(
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Solid Wi-Fi Fan Icon
        Canvas(modifier = Modifier.size(13.dp, 12.dp)) {
            val w = size.width
            val h = size.height

            val path =
                Path().apply {
                    moveTo(w / 2f, h)
                    lineTo(w * 0.05f, h * 0.25f)
                    quadraticTo(w / 2f, -h * 0.1f, w * 0.95f, h * 0.25f)
                }
            drawPath(path = path, color = contentColor)
        }

        // 2. Mobile Signal Triangle Icon
        Canvas(modifier = Modifier.size(12.dp)) {
            val path =
                Path().apply {
                    moveTo(0f, size.height)
                    lineTo(size.width, size.height)
                    lineTo(size.width, 0f)
                    close()
                }
            drawPath(path = path, color = contentColor)
        }

        // 3. Battery Icon
        Canvas(modifier = Modifier.size(18.dp, 10.dp)) {
            val bodyWidth = size.width - 3.dp.toPx()

            drawRoundRect(
                color = contentColor,
                size = Size(bodyWidth, size.height),
                cornerRadius = CornerRadius(2.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx()),
            )
            drawRect(
                color = contentColor,
                topLeft = Offset(bodyWidth, size.height / 2 - 2.dp.toPx()),
                size = Size(2.dp.toPx(), 4.dp.toPx()),
            )
            drawRoundRect(
                color = contentColor,
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                size =
                    Size(
                        (bodyWidth - 4.dp.toPx()) * 0.75f,
                        size.height - 4.dp.toPx(),
                    ),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
        }
    }
}

@Preview
@Composable
fun DeviceFramePreview() {
    NotallyXTheme { DeviceFrame("Test") {} }
}
