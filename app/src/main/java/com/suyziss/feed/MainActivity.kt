package com.suyziss.feed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private enum class Tab(val label: String) { Home("Home"), Explore("Explore"), Create("Create"), Inbox("Inbox"), Profile("Profile") }
private data class Post(val user: String, val caption: String, val category: String, val colors: List<Color>, val likes: String, val comments: String)
private val samplePosts = listOf(
    Post("suyziss", "A quiet moment in the city. 夜", "JAPAN / NIGHT", listOf(Color(0xFF11151C), Color(0xFF394653), Color(0xFF101115)), "12.8K", "284"),
    Post("narei.kaoru", "Somewhere between the lights.", "CITY / LIFE", listOf(Color(0xFF201A2D), Color(0xFF67516F), Color(0xFF17141D)), "8,421", "109"),
    Post("akivass", "Less noise. More focus.", "MINIMAL", listOf(Color(0xFF152B2C), Color(0xFF456A67), Color(0xFF101A1B)), "5,092", "76"),
    Post("kelzuma", "The world after midnight.", "ATMOSPHERE", listOf(Color(0xFF30241F), Color(0xFF765343), Color(0xFF171311)), "3,810", "51")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { SuyzissApp() }
    }
}

@Composable
private fun SuyzissApp() {
    var dark by rememberSaveable { mutableStateOf(true) }
    val colors = if (dark) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colors) {
        var selected by rememberSaveable { mutableStateOf(Tab.Home) }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        val icon = when (tab) {
                            Tab.Home -> Icons.Default.Home
                            Tab.Explore -> Icons.Default.Search
                            Tab.Create -> Icons.Default.AddBox
                            Tab.Inbox -> Icons.Default.Inbox
                            Tab.Profile -> Icons.Default.Person
                        }
                        NavigationBarItem(
                            selected = selected == tab,
                            onClick = { selected = tab },
                            icon = { Icon(icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selected) {
                    Tab.Home -> FeedScreen()
                    Tab.Explore -> ExploreScreen()
                    Tab.Create -> SimpleScreen("Create publication", "Video and photo publishing will be implemented next.")
                    Tab.Inbox -> SimpleScreen("Messages", "Your conversations will appear here.")
                    Tab.Profile -> ProfileScreen(dark = dark, onThemeChange = { dark = it })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedScreen() {
    val pager = rememberPagerState(pageCount = { samplePosts.size })
    val liked = remember { mutableStateMapOf<Int, Boolean>() }
    VerticalPager(state = pager, modifier = Modifier.fillMaxSize()) { index ->
        val post = samplePosts[index]
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(post.colors))) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(post.category, color = Color.White.copy(alpha = .62f), fontSize = 10.sp, letterSpacing = 3.sp)
                Text("suyziss.", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp)
                Text(post.caption, color = Color.White.copy(alpha = .9f), fontSize = 15.sp)
                Icon(Icons.Default.PlayArrow, contentDescription = "Video placeholder", tint = Color.White.copy(alpha = .7f), modifier = Modifier.size(34.dp))
            }
            Text("FOR YOU     FOLLOWING", color = Color.White.copy(alpha = .85f), fontSize = 11.sp, letterSpacing = 1.5.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp))
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                IconButton(onClick = { liked[index] = !(liked[index] ?: false) }) {
                    Icon(if (liked[index] == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Like", tint = if (liked[index] == true) Color(0xFFFF526B) else Color.White)
                }
                Text(post.likes, color = Color.White, fontSize = 11.sp)
                IconButton(onClick = {}) { Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments", tint = Color.White) }
                Text(post.comments, color = Color.White, fontSize = 11.sp)
                IconButton(onClick = {}) { Icon(Icons.Default.Send, contentDescription = "Share", tint = Color.White) }
                IconButton(onClick = {}) { Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.White) }
            }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 76.dp, bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("@${post.user}", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(post.caption, color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ExploreScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    val topics = listOf("Night city", "Design", "Japan", "Photography", "Minimalism", "Music", "Cars", "Travel")
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Explore", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search SUYZISS FEED") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
        topics.filter { it.contains(query, ignoreCase = true) }.forEach { topic ->
            ListItem(headlineContent = { Text(topic) }, leadingContent = { Icon(Icons.Default.Tag, null) })
        }
    }
}

@Composable
private fun ProfileScreen(dark: Boolean, onThemeChange: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        Text("@suyziss", style = MaterialTheme.typography.titleLarge)
        Text("Building a quieter corner of the internet.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { onThemeChange(true) }) { Icon(Icons.Default.DarkMode, null); Text(" Dark") }
            OutlinedButton(onClick = { onThemeChange(false) }) { Icon(Icons.Default.LightMode, null); Text(" Light") }
        }
        TextButton(onClick = { onThemeChange(!dark) }) { Text("Switch theme") }
    }
}

@Composable
private fun SimpleScreen(title: String, subtitle: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
