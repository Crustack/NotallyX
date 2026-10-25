package com.philkes.notallyx.misc.appstore.screenshots

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.misc.appstore.AppStorePreview
import com.philkes.notallyx.misc.appstore.DeviceFrame

/** 6. Display notes as widgets */
@AppStorePreview
@PreviewTest
@Composable
fun `06_Widgets`() {
    NotallyXTheme {
        DeviceFrame(
            title = stringResource(R.string.store_headline_widgets),
            statusBarColor = Color(0xFF0F0F19),
            statusBarContentColor = Color.White,
            screenContent = { WidgetHomeScreenMockup() },
        )
    }
}

/** Home screen mockup displaying the widget for screenshot 5 */
@Composable
private fun WidgetHomeScreenMockup() {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Color(0xFF0F0F19))
                .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            // Clock & Date at top
            Text(
                text = "09:41",
                fontSize = 44.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
            Text(
                text = "Thursday, Oct 24 • 21°C",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
            )

            // NotallyX Checklist Widget
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(24.dp))
                        .background(Color(0xFFF5E2DC), RoundedCornerShape(24.dp))
                        .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Dinner Bucketlist",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F1B16),
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.make_list_foreground),
                            contentDescription = null,
                            tint = Color(0xFF1F1B16).copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val items =
                        listOf(
                            "🌯 Burrito" to false,
                            "🥪 Philly Cheesesteak" to false,
                            "🥟 Pierogi" to false,
                            "🍜 Ramen" to true,
                        )

                    items.forEach { (text, checked) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null,
                                colors =
                                    CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF1F1B16),
                                        checkmarkColor = Color(0xFFF5E2DC),
                                        uncheckedColor = Color(0xFF1F1B16).copy(alpha = 0.6f),
                                    ),
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = text,
                                fontSize = 15.sp,
                                color =
                                    if (checked) Color(0xFF1F1B16).copy(alpha = 0.4f)
                                    else Color(0xFF1F1B16),
                                style =
                                    if (checked)
                                        TextStyle(textDecoration = TextDecoration.LineThrough)
                                    else TextStyle.Default,
                            )
                        }
                    }
                }
            }
        }
    }
}
