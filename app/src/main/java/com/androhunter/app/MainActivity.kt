package com.androhunter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.androhunter.app.core.LanguageManager
import com.androhunter.app.core.Strings
import com.androhunter.app.navigation.AndroHunterNavGraph
import com.androhunter.app.navigation.Screen
import com.androhunter.app.ui.splash.SplashLanguageScreen
import com.androhunter.app.ui.theme.*

data class NavItem(val labelFn: () -> String, val icon: ImageVector, val screen: Screen)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LanguageManager.init(this)

        setContent {
            AndroHunterTheme {
                var showSplash by remember { mutableStateOf(LanguageManager.isFirstLaunch()) }
                // Recompose trigger for language change
                var langVersion by remember { mutableStateOf(0) }

                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        fadeIn(tween(600)) togetherWith fadeOut(tween(600))
                    },
                    label = "splash"
                ) { isSplash ->
                    if (isSplash) {
                        SplashLanguageScreen(onLanguageSelected = {
                            showSplash = false
                            langVersion++
                        })
                    } else {
                        key(langVersion) {
                            MainApp(onLanguageChange = { langVersion++ })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainApp(onLanguageChange: () -> Unit) {
    val s             = Strings.get()
    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentRoute  = navBackStack?.destination?.route

    val bottomItems = listOf(
        NavItem({ s.navApps },     Icons.Default.List,          Screen.AppList),
        NavItem({ s.navTerminal }, Icons.Default.Build,         Screen.Terminal),
        NavItem({ s.navAdb },      Icons.Default.Phone,         Screen.AdbManager),
        NavItem({ s.navMonitor },  Icons.Default.Notifications, Screen.BroadcastMon),
        NavItem({ s.navWeb },      Icons.Default.Search,        Screen.WebTester),
        NavItem({ s.navHijack },   Icons.Default.Warning,       Screen.TaskHijack),
        NavItem({ s.navAccess },   Icons.Default.Star,          Screen.AccessMon),
        NavItem({ s.navAbout },    Icons.Default.Info,          Screen.About),
    )

    Scaffold(
        containerColor = HunterBg,
        bottomBar = {
            NavigationBar(containerColor = HunterSurface, tonalElevation = 0.dp) {
                bottomItems.forEach { item ->
                    val selected = currentRoute?.contains(item.screen::class.simpleName ?: "") == true
                    NavigationBarItem(
                        selected = selected,
                        onClick  = {
                            navController.navigate(item.screen) {
                                popUpTo(Screen.AppList) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon  = { Icon(item.icon, contentDescription = item.labelFn(),
                            tint = if (selected) HunterGreen else HunterTextDim) },
                        label = { Text(item.labelFn(),
                            color = if (selected) HunterGreen else HunterTextDim,
                            style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = HunterCard)
                    )
                }
            }
        }
    ) { padding ->
        AndroHunterNavGraph(
            navController = navController,
            modifier      = Modifier.padding(padding)
        )
    }
}
