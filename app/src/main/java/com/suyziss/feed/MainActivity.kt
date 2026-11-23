package com.suyziss.feed

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private enum class Tab { Home, Friends, Create, Inbox, Profile }
private enum class AppTheme { System, Light, Dark }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { SuyzissApp() }
    }
}

@Composable
private fun SuyzissApp() {
    var themeName by rememberSaveable { mutableStateOf(AppTheme.System.name) }
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Home.name) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var privateAccount by rememberSaveable { mutableStateOf(false) }
    val profileContext = LocalContext.current
    val profilePrefs = remember { profileContext.getSharedPreferences("suyziss_profile", android.content.Context.MODE_PRIVATE) }
    var profileName by rememberSaveable { mutableStateOf(profilePrefs.getString("name", "SUYZISS") ?: "SUYZISS") }
    var profileUsername by rememberSaveable { mutableStateOf(profilePrefs.getString("username", "suyziss") ?: "suyziss") }
    var profileBio by rememberSaveable { mutableStateOf(profilePrefs.getString("bio", "") ?: "") }
    var profileAvatar by rememberSaveable { mutableStateOf(profilePrefs.getString("avatar", "") ?: "") }
    LaunchedEffect(profileName, profileUsername, profileBio, profileAvatar) {
        profilePrefs.edit().putString("name", profileName).putString("username", profileUsername).putString("bio", profileBio).putString("avatar", profileAvatar).apply()
    }

    val appTheme = AppTheme.valueOf(themeName)
    val dark = when (appTheme) {
        AppTheme.System -> isSystemInDarkTheme()
        AppTheme.Light -> false
        AppTheme.Dark -> true
    }
    val scheme = if (dark) darkColorScheme(
        background = Color(0xFF09090B),
        surface = Color(0xFF141417),
        primary = Color(0xFFFFFFFF)
    ) else lightColorScheme()

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!settingsOpen) {
                    BottomNavigationBar(
                        selected = Tab.valueOf(selectedTab),
                        onSelect = {
                            selectedTab = it.name
                            settingsOpen = false
                        }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (settingsOpen) {
                    SettingsScreen(
                        privateAccount = privateAccount,
                        onPrivateAccountChange = { privateAccount = it },
                        theme = appTheme,
                        onThemeChange = { themeName = it.name },
                        onBack = { settingsOpen = false }
                    )
                } else {
                    when (Tab.valueOf(selectedTab)) {
                        Tab.Home -> EmptyFeedScreen()
                        Tab.Friends -> FriendsScreen()
                        Tab.Create -> CreateScreen()
                        Tab.Inbox -> InboxScreen()
                        Tab.Profile -> ProfileScreen(
                            privateAccount = privateAccount,
                            displayName = profileName,
                            username = profileUsername,
                            bio = profileBio,
                            avatarUri = profileAvatar,
                            onProfileChange = { n, u, b, a -> profileName = n; profileUsername = u; profileBio = b; profileAvatar = a },
                            onSettings = { settingsOpen = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF050506),
            contentColor = Color.White,
            shape = RoundedCornerShape(40.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomNavItem(
                    selected = selected == Tab.Home,
                    onClick = { onSelect(Tab.Home) },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = "Главная", modifier = Modifier.size(27.dp)) }
                )
                BottomNavItem(
                    selected = selected == Tab.Friends,
                    onClick = { onSelect(Tab.Friends) },
                    icon = { Icon(Icons.Outlined.Group, contentDescription = "Друзья", modifier = Modifier.size(29.dp)) }
                )
                Box(
                    modifier = Modifier.size(width = 62.dp, height = 48.dp).clickable { onSelect(Tab.Create) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.offset(x = (-3).dp).size(48.dp, 38.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF00D5E8)))
                    Box(Modifier.offset(x = 3.dp).size(48.dp, 38.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFFF315B)))
                    Surface(
                        color = Color.White,
                        contentColor = Color.Black,
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.size(48.dp, 38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Создать", modifier = Modifier.size(30.dp))
                        }
                    }
                }
                BottomNavItem(
                    selected = selected == Tab.Inbox,
                    onClick = { onSelect(Tab.Inbox) },
                    icon = {
                        Box {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Сообщения", modifier = Modifier.size(28.dp))
                            if (selected != Tab.Inbox) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-3).dp)
                                        .size(8.dp).clip(CircleShape).background(Color(0xFF168BFF))
                                )
                            }
                        }
                    }
                )
                BottomNavItem(
                    selected = selected == Tab.Profile,
                    onClick = { onSelect(Tab.Profile) },
                    icon = { Icon(Icons.Outlined.PersonOutline, contentDescription = "Профиль", modifier = Modifier.size(29.dp)) }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(width = 60.dp, height = 54.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(if (selected) Color(0xFF303034) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) { icon() }
    }
}

@Composable
private fun EmptyFeedScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("suyziss.", fontSize = 34.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp)
        Spacer(Modifier.height(12.dp))
        Text("Пока здесь пусто", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Публикации появятся здесь, когда авторы начнут делиться контентом.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun FriendsScreen() {
    EmptyState(
        title = "Друзья",
        message = "Здесь будут публикации людей, на которых ты подписан."
    )
}

@Composable
private fun CreateScreen() {
    EmptyState(
        title = "Создать публикацию",
        message = "Добавление фото и видео появится в следующем этапе разработки."
    )
}

@Composable
private fun InboxScreen() {
    EmptyState(
        title = "Входящие",
        message = "Когда появятся новые сообщения, они будут отображаться здесь."
    )
}

@Composable
private fun ProfileScreen(
    privateAccount: Boolean,
    displayName: String,
    username: String,
    bio: String,
    avatarUri: String,
    onProfileChange: (String, String, String, String) -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    var name by remember(displayName) { mutableStateOf(displayName) }
    var user by remember(username) { mutableStateOf(username) }
    var description by remember(bio) { mutableStateOf(bio) }
    var avatar by remember(avatarUri) { mutableStateOf(avatarUri) }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { }
            avatar = uri.toString()
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { editing = true }) { Icon(Icons.Outlined.Edit, "Редактировать профиль") }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Outlined.NotificationsNone, "Уведомления") }
            IconButton(onClick = {}) { Icon(Icons.Outlined.PersonAddAlt1, "Добавить друзей") }
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Menu, "Настройки") }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(displayName.ifBlank { username }, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    Text("@$username", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(34.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        ProfileStat("0", "Подписки")
                        ProfileStat("0", "Подписчики")
                        ProfileStat("0", "Лайки")
                    }
                }
                Box(Modifier.size(118.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    if (avatar.isNotBlank()) coil.compose.AsyncImage(model = avatar, contentDescription = "Аватар", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    else Icon(Icons.Outlined.CameraAlt, "Добавить аватар", tint = Color(0xFFFF315B), modifier = Modifier.size(52.dp))
                }
            }
            Spacer(Modifier.height(25.dp))
            Text(description.ifBlank { "Добавь описание профиля" }, fontSize = 18.sp, color = if (description.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
            if (privateAccount) Text("🔒 Закрытый аккаунт", modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(14.dp))
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(26.dp), modifier = Modifier.clickable { editing = true }) {
                Text("Редактировать профиль", Modifier.padding(horizontal = 22.dp, vertical = 12.dp), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(22.dp))
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                ProfileTabIcon(Icons.Outlined.GridOn, "Публикации", true)
                ProfileTabIcon(Icons.Outlined.Lock, "Приватные", false)
                ProfileTabIcon(Icons.Outlined.Repeat, "Репосты", false)
                ProfileTabIcon(Icons.Outlined.BookmarkBorder, "Избранное", false)
                ProfileTabIcon(Icons.Outlined.FavoriteBorder, "Понравилось", false)
            }
            Text("Публикаций пока нет", Modifier.align(Alignment.CenterHorizontally).padding(top = 22.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (editing) AlertDialog(
        onDismissRequest = { editing = false },
        title = { Text("Редактировать профиль") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(30) }, label = { Text("Имя") }, singleLine = true)
                OutlinedTextField(user, { v -> user = v.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(24) }, label = { Text("Username") }, prefix = { Text("@") }, singleLine = true)
                OutlinedTextField(description, { description = it.take(50) }, label = { Text("Описание") }, supportingText = { Text("${description.length}/50") }, minLines = 2, maxLines = 3)
                OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Изменить аватар") }
            }
        },
        confirmButton = { TextButton(enabled = user.isNotBlank(), onClick = { onProfileChange(name.trim(), user.trim(), description.trim(), avatar); editing = false }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = { editing = false }) { Text("Отмена") } }
    )
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column { Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold); Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun ProfileTabIcon(image: androidx.compose.ui.graphics.vector.ImageVector, description: String, selected: Boolean) {
    Icon(image, contentDescription = description, tint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(29.dp))
}
@Composable
private fun SettingsScreen(
    privateAccount: Boolean,
    onPrivateAccountChange: (Boolean) -> Unit,
    theme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("Назад") }
            Text("Настройки и конфиденциальность", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider()
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            SettingsSectionTitle("КОНФИДЕНЦИАЛЬНОСТЬ")
            ListItem(
                headlineContent = { Text("Закрытый аккаунт") },
                supportingContent = { Text("Только одобренные подписчики смогут видеть твои публикации.") },
                leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingContent = {
                    Switch(checked = privateAccount, onCheckedChange = onPrivateAccountChange)
                }
            )
            Spacer(Modifier.height(18.dp))
            SettingsSectionTitle("ОФОРМЛЕНИЕ")
            Text("Тема приложения", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp))
            AppTheme.entries.forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable { onThemeChange(option) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = theme == option, onClick = { onThemeChange(option) })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (option) {
                            AppTheme.System -> "Системная"
                            AppTheme.Light -> "Светлая"
                            AppTheme.Dark -> "Тёмная"
                        }
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            SettingsSectionTitle("ПОДДЕРЖКА")
            ListItem(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/suyziss"))
                    context.startActivity(intent)
                },
                headlineContent = { Text("Поддержка и обратная связь") },
                supportingContent = { Text("@suyziss") },
                leadingContent = { Icon(Icons.Default.SupportAgent, contentDescription = null) },
                trailingContent = { Text("↗", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "SUYZISS FEED · Настройки",
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.2.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
