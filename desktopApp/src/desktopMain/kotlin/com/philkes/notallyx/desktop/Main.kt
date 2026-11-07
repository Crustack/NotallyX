package com.philkes.notallyx.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.desktop.di.desktopAppModules
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

fun main() = application {
    if (GlobalContext.getOrNull() == null) {
        startKoin { modules(desktopAppModules) }
    }
    val koin = GlobalContext.get()
    val database = koin.get<NotallyDatabase>()

    val windowState =
        rememberWindowState(
            size = DpSize(width = 800.dp, height = 650.dp),
            position = WindowPosition(Alignment.Center),
        )

    Window(
        onCloseRequest = {
            runBlocking { database.checkpoint() }
            if (GlobalContext.getOrNull() != null) {
                stopKoin()
            }
            exitApplication()
        },
        title = "NotallyX",
        state = windowState,
    ) {
        window.minimumSize = Dimension(400, 300)

        DisposableEffect(Unit) {
            val listener =
                object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent?) {
                        runBlocking { database.checkpoint() }
                    }
                }
            window.addWindowListener(listener)
            onDispose {
                window.removeWindowListener(listener)
                runBlocking { database.checkpoint() }
                if (GlobalContext.getOrNull() != null) {
                    stopKoin()
                }
            }
        }

        NotallyXTheme { DesktopAppNavHost(database = database) }
    }
}
