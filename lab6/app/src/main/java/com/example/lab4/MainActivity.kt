package com.example.lab4

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.random.Random
import com.example.lab4.ui.theme.Lab4Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab4Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainMenuScreen()
                }
            }
        }
    }
}

// Управление состоянием игры
enum class GameState {
    MENU, PLAYING, GAME_OVER, LEADERBOARD, USERNAME_INPUT
}

// Движения змейки
enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

// Игра
data class SnakeGame(
    val snake: List<Offset>,
    val food: Offset,
    val direction: Direction,
    val score: Int,
    val level: Int,
    val gameOver: Boolean
)

// Игрок
data class Player(
    val userName: String,
    val highScore: Int,
    val highestLevel: Int,
    val gamesPlayed: Int
)

// Управление рекордами
class LeaderboardManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("game_leaderboard", Context.MODE_PRIVATE)
    private val currentUserPrefs: SharedPreferences = context.getSharedPreferences("current_user", Context.MODE_PRIVATE)
    private val firebaseManager = FirebaseManager()

    private val scope = CoroutineScope(Dispatchers.IO)

    fun getCurrentUserName(): String {
        return currentUserPrefs.getString("user_name", "") ?: ""
    }

    fun setCurrentUserName(userName: String) {
        currentUserPrefs.edit().putString("user_name", userName).apply()

        // Сохраняем в Firebase при первом входе
        scope.launch {
            val existing = firebaseManager.getPlayerStats(userName)
            if (existing == null) {
                firebaseManager.savePlayerScore(
                    FirebaseManager.PlayerRecord(
                        userName = userName,
                        highScore = 0,
                        highestLevel = 1,
                        gamesPlayed = 0
                    )
                )
            }
        }
    }

    fun updateLeaderboard(score: Int, level: Int) {
        val userName = getCurrentUserName()
        if (userName.isEmpty()) return

        // Сохраняем локально
        val players = loadAllPlayers()
        val existingPlayer = players.find { it.userName == userName }

        val updatedPlayer = if (existingPlayer != null) {
            existingPlayer.copy(
                highScore = maxOf(existingPlayer.highScore, score),
                highestLevel = maxOf(existingPlayer.highestLevel, level),
                gamesPlayed = existingPlayer.gamesPlayed + 1
            )
        } else {
            Player(
                userName = userName,
                highScore = score,
                highestLevel = level,
                gamesPlayed = 1
            )
        }

        savePlayer(updatedPlayer)

        // Синхронизируем с Firebase
        scope.launch {
            firebaseManager.updatePlayerStats(userName, score, level)
        }
    }

    private fun savePlayer(player: Player) {
        with(prefs.edit()) {
            putInt("${player.userName}_highScore", player.highScore)
            putInt("${player.userName}_highestLevel", player.highestLevel)
            putInt("${player.userName}_gamesPlayed", player.gamesPlayed)
            apply()
        }
    }

    fun loadAllPlayers(): List<Player> {
        val allEntries = prefs.all
        val userNames = allEntries.keys
            .filter { it.endsWith("_highScore") }
            .map { it.removeSuffix("_highScore") }

        return userNames.map { userName ->
            Player(
                userName = userName,
                highScore = prefs.getInt("${userName}_highScore", 0),
                highestLevel = prefs.getInt("${userName}_highestLevel", 1),
                gamesPlayed = prefs.getInt("${userName}_gamesPlayed", 0)
            )
        }.sortedByDescending { it.highScore }
    }

    fun loadTopPlayersFromFirebase(
        limit: Int = 10,
        onSuccess: (List<Player>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        scope.launch {
            try {
                val firebasePlayers = firebaseManager.getTopPlayers(limit)

                // Конвертируем Firebase PlayerRecord в локальный Player
                val players = firebasePlayers.map { record ->
                    Player(
                        userName = record.userName,
                        highScore = record.highScore,
                        highestLevel = record.highestLevel,
                        gamesPlayed = record.gamesPlayed
                    )
                }

                // Обновляем UI в главном потоке
                CoroutineScope(Dispatchers.Main).launch {
                    onSuccess(players)
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    onError(e)
                }
            }
        }
    }

    fun getTopPlayers(limit: Int = 10): List<Player> {
        return loadAllPlayers().take(limit)
    }

    fun clearLeaderboard() {
        prefs.edit().clear().apply()
    }
}

// Главное меню
// Главное меню
@Composable
fun MainMenuScreen() {
    val context = LocalContext.current
    val leaderboardManager = remember { LeaderboardManager(context) }
    val topPlayers = remember { mutableStateOf(emptyList<Player>()) }
    val isLoading = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val showFirebaseInfo = remember { mutableStateOf(false) }

    val isVisible = remember { mutableStateOf(false) }
    var gameState by remember { mutableStateOf(GameState.MENU) }
    var currentScore by remember { mutableStateOf(0) }
    var currentLevel by remember { mutableStateOf(1) }

    // Check if user needs to enter name
    LaunchedEffect(Unit) {
        isVisible.value = true
        if (leaderboardManager.getCurrentUserName().isEmpty()) {
            gameState = GameState.USERNAME_INPUT
        }
    }

    // При переходе на экран лидерборда загружаем данные
    LaunchedEffect(gameState) {
        if (gameState == GameState.LEADERBOARD) {
            isLoading.value = true
            errorMessage.value = null
            showFirebaseInfo.value = true

            leaderboardManager.loadTopPlayersFromFirebase(
                limit = 10,
                onSuccess = { players ->
                    // Обновляем состояние в основном потоке
                    topPlayers.value = players
                    isLoading.value = false

                    // Используем отдельный LaunchedEffect для отложенного скрытия
                    // Это делается в UI-части, не здесь
                },
                onError = { error ->
                    // Если не удалось загрузить из Firebase, используем локальные данные
                    topPlayers.value = leaderboardManager.getTopPlayers()
                    errorMessage.value = "Не удалось загрузить онлайн-таблицу лидеров. Используются локальные данные."
                    isLoading.value = false
                }
            )
        }
    }

    // Отслеживаем изменение showFirebaseInfo для автоскрытия сообщения
    LaunchedEffect(showFirebaseInfo.value) {
        if (showFirebaseInfo.value) {
            delay(3000)
            showFirebaseInfo.value = false
        }
    }

    when (gameState) {
        GameState.MENU -> {
            MainMenuContent(
                leaderboardManager = leaderboardManager,
                isVisible = isVisible.value,
                onPlayClick = {
                    gameState = GameState.PLAYING
                    currentScore = 0
                    currentLevel = 1
                },
                onLeaderboardClick = {
                    gameState = GameState.LEADERBOARD
                },
                onChangeNameClick = {
                    gameState = GameState.USERNAME_INPUT
                },
                onExitClick = {
                    if (context is ComponentActivity) {
                        context.finish()
                    }
                }
            )
        }
        GameState.PLAYING -> {
            SnakeGameScreen(
                onGameOver = { score, level ->
                    currentScore = score
                    currentLevel = level
                    leaderboardManager.updateLeaderboard(score, level)
                    gameState = GameState.GAME_OVER
                },
                onBackToMenu = {
                    gameState = GameState.MENU
                }
            )
        }
        GameState.GAME_OVER -> {
            GameOverScreen(
                score = currentScore,
                level = currentLevel,
                onRestart = {
                    gameState = GameState.PLAYING
                    currentScore = 0
                    currentLevel = 1
                },
                onMenu = {
                    gameState = GameState.MENU
                }
            )
        }
        GameState.LEADERBOARD -> {
            LeaderboardScreen(
                players = topPlayers.value,
                isLoading = isLoading.value,
                errorMessage = errorMessage.value,
                showFirebaseInfo = showFirebaseInfo.value,
                onBack = {
                    gameState = GameState.MENU
                },
                onRefresh = {
                    // При обновлении загружаем данные заново
                    isLoading.value = true
                    errorMessage.value = null
                    showFirebaseInfo.value = true

                    leaderboardManager.loadTopPlayersFromFirebase(
                        limit = 10,
                        onSuccess = { players ->
                            topPlayers.value = players
                            isLoading.value = false
                        },
                        onError = { error ->
                            topPlayers.value = leaderboardManager.getTopPlayers()
                            errorMessage.value = "Не удалось загрузить онлайн-таблицу лидеров. Используются локальные данные."
                            isLoading.value = false
                        }
                    )
                }
            )
        }
        GameState.USERNAME_INPUT -> {
            UsernameInputScreen(
                currentName = leaderboardManager.getCurrentUserName(),
                onNameSet = { newName ->
                    leaderboardManager.setCurrentUserName(newName)
                    gameState = GameState.MENU
                },
                onBack = {
                    if (leaderboardManager.getCurrentUserName().isNotEmpty()) {
                        gameState = GameState.MENU
                    }
                }
            )
        }
    }
}

@Composable
fun MainMenuContent(
    leaderboardManager: LeaderboardManager,
    isVisible: Boolean,
    onPlayClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onChangeNameClick: () -> Unit,
    onExitClick: () -> Unit
) {
    // Background Image
    Image(
        painter = painterResource(id = R.drawable.fone),
        contentDescription = stringResource(R.string.menu_background),
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )

    val titleAlpha = animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "titleAlpha"
    ).value

    val buttonScale = animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(durationMillis = 1000),
        label = "buttonScale"
    ).value

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Game Title with Animation
            Column(
                modifier = Modifier
                    .alpha(titleAlpha)
                    .padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.game_title),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Display current user name
                Text(
                    text = stringResource(R.string.player_label, leaderboardManager.getCurrentUserName()),
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Firebase status indicator
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Firebase",
                        tint = Color(0xFFFFCA28),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Firebase подключен",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Меню кнопок
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(buttonScale),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Играть кнопка
                MenuButton(
                    text = stringResource(R.string.play_button),
                    icon = Icons.Default.PlayArrow,
                    onClick = onPlayClick,
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    textColor = MaterialTheme.colorScheme.onPrimary
                )

                // Топ игроков (Firebase)
                MenuButton(
                    text = "🔥 ${stringResource(R.string.leaderboard_button)}",
                    icon = Icons.Default.Star,
                    onClick = onLeaderboardClick,
                    backgroundColor = Color(0xFFFF8A65),
                    textColor = Color.White
                )

                // Изменить имя
                MenuButton(
                    text = stringResource(R.string.change_name_button),
                    icon = Icons.Default.Person,
                    onClick = onChangeNameClick,
                    backgroundColor = MaterialTheme.colorScheme.tertiary,
                    textColor = MaterialTheme.colorScheme.onTertiary
                )

                // Выход
                MenuButton(
                    text = stringResource(R.string.exit_button),
                    icon = Icons.Default.ExitToApp,
                    onClick = onExitClick,
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                    textColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

// Топ игроков меню
@Composable
fun LeaderboardScreen(
    players: List<Player>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    showFirebaseInfo: Boolean = false,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    // Background
    Image(
        painter = painterResource(id = R.drawable.fone),
        contentDescription = stringResource(R.string.leaderboard_background),
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )

    // Firebase success animation
    val firebaseInfoAlpha by animateFloatAsState(
        targetValue = if (showFirebaseInfo) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "firebaseInfoAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with back button and refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back_icon),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Firebase",
                        tint = Color(0xFFFFCA28),
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.leaderboard_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh_icon),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Firebase success message
            if (showFirebaseInfo && firebaseInfoAlpha > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.8f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Firebase Connected",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Онлайн-таблица загружена!",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Показать ошибку, если есть
            errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF44336).copy(alpha = 0.8f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Ошибка подключения",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = message,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Info card about Firebase
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF8A65).copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Firebase Info",
                        tint = Color(0xFFFFCA28),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "🔥 Глобальная таблица лидеров",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Рейтинг игроков со всего мира в реальном времени",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && players.isEmpty()) {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Загрузка глобального рейтинга...",
                            fontSize = 18.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Подключение к Firebase",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else if (players.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = stringResource(R.string.no_data_icon),
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.leaderboard_empty_title),
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Сыграйте в игру, чтобы попасть в таблицу лидеров",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF8A65)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = Color.White,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Обновить",
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // Leaderboard list with header
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header stats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Топ ${players.size} игроков",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Глобальный рейтинг",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        if (players.isNotEmpty()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Лучший результат:",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "${players[0].highScore} очков",
                                    fontSize = 16.sp,
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Leaderboard list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(players) { player ->
                            LeaderboardItem(
                                player = player,
                                position = players.indexOf(player) + 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Refresh button at bottom
        if (!isLoading && players.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF8A65).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Обновить данные",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardItem(
    player: Player,
    position: Int,
    modifier: Modifier = Modifier
) {
    val medalColor = when (position) {
        1 -> Color(0xFFFFD700) // золото
        2 -> Color(0xFFC0C0C0) // серебро
        3 -> Color(0xFFCD7F32) // бронза
        else -> MaterialTheme.colorScheme.primary
    }
    val scoreColor = Color(0xFF4CAF50)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // имя и позиция
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // топ 3
                if (position <= 3) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = stringResource(R.string.medal_icon, position),
                        tint = medalColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Text(
                        text = "$position",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.width(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = player.userName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
            }

            // статистика
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${player.highScore}${stringResource(R.string.points_suffix)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
                Text(
                    text = stringResource(R.string.level_prefix, player.highestLevel),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${player.gamesPlayed}${stringResource(R.string.games_played_suffix)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun UsernameInputScreen(
    currentName: String,
    onNameSet: (String) -> Unit,
    onBack: () -> Unit
) {
    var userName by remember { mutableStateOf(currentName) }

    // фон игры
    Image(
        painter = painterResource(id = R.drawable.fone),
        contentDescription = stringResource(R.string.username_background),
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // заголовок
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = stringResource(R.string.player_icon),
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.username_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.username_subtitle),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // ввод имени - исправленная версия для Material3
        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = {
                Text(
                    stringResource(R.string.username_hint),
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            singleLine = true,
            label = {
                Text(
                    stringResource(R.string.username_label),
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // кнопки
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (userName.isNotBlank()) {
                        onNameSet(userName)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = userName.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.save_and_play),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (currentName.isNotEmpty()) {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.back_button),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SnakeGameScreen(
    onGameOver: (Int, Int) -> Unit,
    onBackToMenu: () -> Unit
) {
    // 12 клеток
    val gridSize = 12
    val gameSpeed = remember { mutableStateOf(400L) } // Start speed

    var game by remember {
        mutableStateOf(
            SnakeGame(
                // Начальная позиция змейки
                snake = listOf(Offset(3f, 3f)),
                food = Offset(6f, 6f),
                direction = Direction.RIGHT,
                score = 0,
                level = 1,
                gameOver = false
            )
        )
    }

    // подгрузка изображений
    val context = LocalContext.current
    val headBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.head)
    }.asImageBitmap()

    val appleBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.apple)
    }.asImageBitmap()

    // игровой цикл
    LaunchedEffect(game) {
        while (!game.gameOver) {
            delay(gameSpeed.value)

            val newSnake = mutableListOf<Offset>()
            val head = game.snake.first()

            // новая позиция головы
            val newHead = when (game.direction) {
                Direction.UP -> Offset(head.x, head.y - 1)
                Direction.DOWN -> Offset(head.x, head.y + 1)
                Direction.LEFT -> Offset(head.x - 1, head.y)
                Direction.RIGHT -> Offset(head.x + 1, head.y)
            }

            // проверка столкновения со стеной
            if (newHead.x < 0 || newHead.x >= gridSize ||
                newHead.y < 0 || newHead.y >= gridSize) {
                onGameOver(game.score, game.level)
                return@LaunchedEffect
            }

            // проверка столкновения с собой
            if (game.snake.contains(newHead)) {
                onGameOver(game.score, game.level)
                return@LaunchedEffect
            }

            newSnake.add(newHead)
            newSnake.addAll(game.snake)

            // скушать еду
            var newFood = game.food
            var newScore = game.score
            var newLevel = game.level
            var newSpeed = gameSpeed.value

            if (newHead == game.food) {
                newScore += 10
                // каждые 50 новый уровень
                newLevel = (newScore / 50) + 1
                // максимум 80 но каждый уровень новая скорость
                newSpeed = (400L - (newLevel - 1) * 45L).coerceAtLeast(80L)
                gameSpeed.value = newSpeed

                // если скушал то генерим
                newFood = generateFood(gridSize, newSnake)
            } else {
                // то оставляем предыдущий хвост
                newSnake.removeLast()
            }

            game = game.copy(
                snake = newSnake,
                food = newFood,
                score = newScore,
                level = newLevel
            )
        }
    }

    // задник
    Image(
        painter = painterResource(id = R.drawable.fone),
        contentDescription = stringResource(R.string.game_background),
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // меню со скором
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToMenu) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back_icon),
                    tint = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.score_label, game.score),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.level_label, game.level),
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // игра
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp) // Фиксированная высота
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Внутренний контейнер для квадратного поля
            Box(
                modifier = Modifier
                    .size(320.dp) // Фиксированный квадратный размер
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellWidth = size.width / gridSize
                    val cellHeight = size.height / gridSize

                    // DRAW GRID
                    val strokeColor = Color.White.copy(alpha = 0.15f)
                    val strokeWidth = 1f

                    // Вертикальные линии
                    for (i in 0..gridSize) {
                        drawLine(
                            color = strokeColor,
                            start = Offset(i * cellWidth, 0f),
                            end = Offset(i * cellWidth, size.height),
                            strokeWidth = strokeWidth
                        )
                    }

                    // Горизонтальные линии
                    for (i in 0..gridSize) {
                        drawLine(
                            color = strokeColor,
                            start = Offset(0f, i * cellHeight),
                            end = Offset(size.width, i * cellHeight),
                            strokeWidth = strokeWidth
                        )
                    }

                    // отрисовка змейки
                    game.snake.forEachIndexed { index, segment ->
                        val x = segment.x * cellWidth
                        val y = segment.y * cellHeight

                        if (index == 0) {
                            // Draw snake head with image
                            drawSnakeHeadWithImage(
                                x = x,
                                y = y,
                                cellWidth = cellWidth,
                                cellHeight = cellHeight,
                                direction = game.direction,
                                headBitmap = headBitmap
                            )
                        } else {
                            // тело змеи
                            drawRect(
                                color = Color.Green,
                                topLeft = Offset(x, y),
                                size = androidx.compose.ui.geometry.Size(cellWidth, cellHeight)
                            )
                        }
                    }

                    // яблоко изображ
                    val foodX = game.food.x * cellWidth
                    val foodY = game.food.y * cellHeight
                    drawAppleWithImage(
                        x = foodX,
                        y = foodY,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        appleBitmap = appleBitmap
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // батоны
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // верх бтв
            ControlButton(
                onClick = {
                    if (game.direction != Direction.DOWN) {
                        game = game.copy(direction = Direction.UP)
                    }
                },
                text = stringResource(R.string.up_button),
                modifier = Modifier.size(80.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // левый бтв
                ControlButton(
                    onClick = {
                        if (game.direction != Direction.RIGHT) {
                            game = game.copy(direction = Direction.LEFT)
                        }
                    },
                    text = stringResource(R.string.left_button),
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.width(80.dp))

                // правый бтв
                ControlButton(
                    onClick = {
                        if (game.direction != Direction.LEFT) {
                            game = game.copy(direction = Direction.RIGHT)
                        }
                    },
                    text = stringResource(R.string.right_button),
                    modifier = Modifier.size(80.dp)
                )
            }

            // низ бтв
            ControlButton(
                onClick = {
                    if (game.direction != Direction.UP) {
                        game = game.copy(direction = Direction.DOWN)
                    }
                },
                text = stringResource(R.string.down_button),
                modifier = Modifier.size(80.dp)
            )
        }
    }
}

@Composable
fun ControlButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun GameOverScreen(
    score: Int,
    level: Int,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    // Background
    Image(
        painter = painterResource(id = R.drawable.fone),
        contentDescription = stringResource(R.string.game_background),
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.game_over_title),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Red,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = stringResource(R.string.final_score, score),
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Text(
            text = stringResource(R.string.reached_level, level),
            fontSize = 24.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(60.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MenuButton(
                text = stringResource(R.string.play_again),
                icon = Icons.Default.PlayArrow,
                onClick = onRestart,
                backgroundColor = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onPrimary
            )

            MenuButton(
                text = stringResource(R.string.main_menu),
                icon = Icons.Default.ArrowBack,
                onClick = onMenu,
                backgroundColor = MaterialTheme.colorScheme.secondary,
                textColor = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}

// генерация еды
fun generateFood(gridSize: Int, snake: List<Offset>): Offset {
    var food: Offset
    do {
        food = Offset(
            Random.nextInt(0, gridSize).toFloat(),
            Random.nextInt(0, gridSize).toFloat()
        )
    } while (snake.contains(food))
    return food
}

// генерация с изображениями
fun DrawScope.drawSnakeHeadWithImage(
    x: Float,
    y: Float,
    cellWidth: Float,
    cellHeight: Float,
    direction: Direction,
    headBitmap: androidx.compose.ui.graphics.ImageBitmap
) {
    val rotation = when (direction) {
        Direction.UP -> 0f
        Direction.RIGHT -> 90f
        Direction.DOWN -> 180f
        Direction.LEFT -> 270f
    }

    withTransform({
        translate(left = x, top = y)
        rotate(rotation, pivot = Offset(cellWidth / 2, cellHeight / 2))
    }) {
        val scale = 0.9f

        drawImage(
            image = headBitmap,
            dstSize = IntSize((cellWidth * scale).toInt(), (cellHeight * scale).toInt()),
            dstOffset = IntOffset(
                ((cellWidth * (1 - scale) / 2)).toInt(),
                ((cellHeight * (1 - scale) / 2)).toInt()
            )
        )
    }
}

fun DrawScope.drawAppleWithImage(
    x: Float,
    y: Float,
    cellWidth: Float,
    cellHeight: Float,
    appleBitmap: androidx.compose.ui.graphics.ImageBitmap
) {
    val scale = 1.1f

    drawImage(
        image = appleBitmap,
        dstSize = IntSize((cellWidth * scale).toInt(), (cellHeight * scale).toInt()),
        dstOffset = IntOffset(
            (x - cellWidth * (scale - 1) / 2).toInt(),
            (y - cellHeight * (scale - 1) / 2).toInt()
        )
    )
}

@Composable
fun MenuButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainMenuPreview() {
    Lab4Theme {
        MainMenuScreen()
    }
}

@Preview
@Composable
fun SnakeGamePreview() {
    Lab4Theme {
        SnakeGameScreen(
            onGameOver = { _, _ -> },
            onBackToMenu = {}
        )
    }
}

@Preview
@Composable
fun GameOverPreview() {
    Lab4Theme {
        GameOverScreen(
            score = 150,
            level = 3,
            onRestart = {},
            onMenu = {}
        )
    }
}

@Preview
@Composable
fun LeaderboardPreview() {
    Lab4Theme {
        LeaderboardScreen(
            players = listOf(
                Player("Игрок1", 150, 3, 5),
                Player("Игрок2", 120, 2, 3),
                Player("Игрок3", 90, 2, 7)
            ),
            isLoading = false,
            errorMessage = null,
            showFirebaseInfo = false,
            onBack = {},
            onRefresh = {}
        )
    }
}

@Preview
@Composable
fun UsernameInputPreview() {
    Lab4Theme {
        UsernameInputScreen(
            currentName = "",
            onNameSet = {},
            onBack = {}
        )
    }
}