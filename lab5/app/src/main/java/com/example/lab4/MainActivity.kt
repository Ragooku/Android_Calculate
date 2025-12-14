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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.example.lab4.ui.theme.Lab4Theme
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab4Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainMenuScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// управление состоянием игры
enum class GameState {
    MENU, PLAYING, GAME_OVER, LEADERBOARD, USERNAME_INPUT
}

// движения змейки
enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

// сама игра
data class SnakeGame(
    val snake: List<Offset>,
    val food: Offset,
    val direction: Direction,
    val score: Int,
    val level: Int,
    val gameOver: Boolean
)

// Game ViewModel
class GameViewModel(private val repository: GameRepository) : ViewModel() {
    private val _gameRecords = MutableStateFlow<List<GameRecord>>(emptyList())
    val gameRecords: StateFlow<List<GameRecord>> = _gameRecords.asStateFlow()

    private val _topPlayers = MutableStateFlow<List<GameRecord>>(emptyList())
    val topPlayers: StateFlow<List<GameRecord>> = _topPlayers.asStateFlow()

    init {
        loadAllRecords()
        loadTopPlayers()
    }

    fun saveGameResult(playerName: String, score: Int, level: Int, timeSeconds: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.addGameResult(playerName, score, level, timeSeconds)
            // После сохранения обновляем данные
            loadTopPlayers()
            loadAllRecords()
        }
    }

    private fun loadAllRecords() {
        CoroutineScope(Dispatchers.IO).launch {
            repository.getAllRecords().collect { records ->
                _gameRecords.value = records
            }
        }
    }

    private fun loadTopPlayers() {
        CoroutineScope(Dispatchers.IO).launch {
            val records = repository.getTop10Records()
            _topPlayers.value = records
        }
    }

    // Добавьте метод для принудительного обновления
    fun refreshData() {
        loadTopPlayers()
        loadAllRecords()
    }
}


// управление рекордами
// управление рекордами
class LeaderboardManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("game_leaderboard", Context.MODE_PRIVATE)
    private val currentUserPrefs: SharedPreferences = context.getSharedPreferences("current_user", Context.MODE_PRIVATE)
    private val gameRepository = GameRepository(context)

    //получение текущего имени
    fun getCurrentUserName(): String {
        return currentUserPrefs.getString("user_name", "") ?: ""
    }

    //сохранение имени
    fun setCurrentUserName(userName: String) {
        currentUserPrefs.edit().putString("user_name", userName).apply()
    }

    //после игры обновляем
    fun updateLeaderboard(score: Int, level: Int, timeSeconds: Int) {
        val userName = getCurrentUserName()
        if (userName.isEmpty()) return

        // Сохраняем в Room
        CoroutineScope(Dispatchers.IO).launch {
            gameRepository.addGameResult(
                playerName = userName,
                score = score,
                level = level,
                timeSeconds = timeSeconds
            )
            // Не нужно обновлять здесь, GameViewModel сам обновит данные
        }
    }

    //загрузка всех игроков
    suspend fun loadAllPlayers(): List<GameRecord> {
        return gameRepository.getTop10Records()
    }

    //топ 10
    suspend fun getTopPlayers(limit: Int = 10): List<GameRecord> {
        return gameRepository.getTop10Records()
    }

    //очистка таблицы
    suspend fun clearLeaderboard() {
        gameRepository.clearAll()
    }
}

// глав меню
@Composable
fun MainMenuScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val leaderboardManager = remember { LeaderboardManager(context) }
    val viewModel: GameViewModel = remember {
        GameViewModel(GameRepository(context))
    }
    val topPlayers by viewModel.topPlayers.collectAsStateWithLifecycle()

    val isVisible = remember { mutableStateOf(false) }
    var gameState by remember { mutableStateOf(GameState.MENU) }
    var currentScore by remember { mutableStateOf(0) }
    var currentLevel by remember { mutableStateOf(1) }
    var timeSeconds by remember { mutableStateOf(0) }

    // Обновляем данные при возвращении в меню
    LaunchedEffect(gameState) {
        if (gameState == GameState.MENU || gameState == GameState.LEADERBOARD) {
            // Обновляем данные при показе меню или таблицы рекордов
            viewModel.refreshData()
        }

        // Timer для игрового времени
        if (gameState == GameState.PLAYING) {
            while (gameState == GameState.PLAYING) {
                delay(1000L)
                timeSeconds++
            }
        } else {
            timeSeconds = 0
        }
    }


    // Check if user needs to enter name
    LaunchedEffect(Unit) {
        isVisible.value = true
        if (leaderboardManager.getCurrentUserName().isEmpty()) {
            gameState = GameState.USERNAME_INPUT
        }
    }

    when (gameState) {
        GameState.MENU -> {
            // Background Image
            Image(
                painter = painterResource(id = R.drawable.fone),
                contentDescription = "Menu Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            val titleAlpha = animateFloatAsState(
                targetValue = if (isVisible.value) 1f else 0f,
                animationSpec = tween(durationMillis = 800),
                label = "titleAlpha"
            ).value

            val buttonScale = animateFloatAsState(
                targetValue = if (isVisible.value) 1f else 0.8f,
                animationSpec = tween(durationMillis = 1000),
                label = "buttonScale"
            ).value

            Surface(
                modifier = modifier.fillMaxSize(),
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
                            text = "Snake Game",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        // Display current user name
                        Text(
                            text = "Игрок: ${leaderboardManager.getCurrentUserName()}",
                            fontSize = 18.sp,
                            color = Color.White,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // меню
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(buttonScale),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // играть кнопка
                        MenuButton(
                            text = "Играть",
                            icon = Icons.Default.PlayArrow,
                            onClick = {
                                gameState = GameState.PLAYING
                                currentScore = 0
                                currentLevel = 1
                                timeSeconds = 0
                            },
                            backgroundColor = MaterialTheme.colorScheme.primary,
                            textColor = MaterialTheme.colorScheme.onPrimary
                        )

                        // Топ игроков
                        MenuButton(
                            text = "Таблица рекордов",
                            icon = Icons.Default.Star,
                            onClick = {
                                gameState = GameState.LEADERBOARD
                            },
                            backgroundColor = MaterialTheme.colorScheme.secondary,
                            textColor = MaterialTheme.colorScheme.onSecondary
                        )

                        // Изменить имя
                        MenuButton(
                            text = "Сменить имя",
                            icon = Icons.Default.Person,
                            onClick = {
                                gameState = GameState.USERNAME_INPUT
                            },
                            backgroundColor = MaterialTheme.colorScheme.tertiary,
                            textColor = MaterialTheme.colorScheme.onTertiary
                        )

                        // выход
                        MenuButton(
                            text = "Выйти",
                            icon = Icons.Default.ExitToApp,
                            onClick = {
                                if (context is ComponentActivity) {
                                    context.finish()
                                }
                            },
                            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                            textColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
        GameState.PLAYING -> {
            SnakeGameScreen(
                onGameOver = { score, level ->
                    currentScore = score
                    currentLevel = level
                    // Сохраняем результат
                    leaderboardManager.updateLeaderboard(score, level, timeSeconds)
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
                timeSeconds = timeSeconds,
                onRestart = {
                    gameState = GameState.PLAYING
                    currentScore = 0
                    currentLevel = 1
                    timeSeconds = 0
                },
                onMenu = {
                    gameState = GameState.MENU
                    timeSeconds = 0
                }
            )
        }
        GameState.LEADERBOARD -> {
            LeaderboardScreen(
                records = topPlayers,
                onBack = {
                    gameState = GameState.MENU
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

// топ игроков меню
@Composable
fun LeaderboardScreen(
    records: List<GameRecord>,
    onBack: () -> Unit
) {
    // Background
    Image(
        painter = painterResource(id = R.drawable.fone),
        contentDescription = "Leaderboard Background",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Таблица рекордов",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (records.isEmpty()) {
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
                        contentDescription = "Нет данных",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Пока нет рекордов",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Сыграйте первую игру!",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Leaderboard list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                items(records) { record ->
                    LeaderboardItem(
                        record = record,
                        position = records.indexOf(record) + 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardItem(
    record: GameRecord,
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
                        contentDescription = "Медаль $position",
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

                Column {
                    Text(
                        text = record.playerName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Text(
                        text = record.date.toString().substring(0, 10),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // статистика
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${record.score} очков",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
                Text(
                    text = "Уровень: ${record.level}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${record.timeSeconds} сек",
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
        contentDescription = "Username Background",
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
                contentDescription = "Игрок",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Введите имя",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Это имя будет отображаться в таблице рекордов",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // ввод имени
        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Введите ваше имя", color = Color.White.copy(alpha = 0.7f)) },
            singleLine = true,
            label = { Text("Имя игрока", color = Color.White.copy(alpha = 0.7f)) }
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
                    text = "Сохранить и играть",
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
                        text = "Назад",
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
                // увеличиваем скорость с каждым уровнем
                newSpeed = (400L - (newLevel - 1) * 45L).coerceAtLeast(80L)
                gameSpeed.value = newSpeed

                // генерируем новую еду
                newFood = generateFood(gridSize, newSnake)
            } else {
                // удаляем хвост
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
        contentDescription = "Game Background",
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
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Счёт: ${game.score}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Уровень: ${game.level}",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // игровое поле
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

                    // яблоко изображение
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

        // кнопки управления
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // верхняя кнопка
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
                // левая кнопка
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

                // правая кнопка
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

            // нижняя кнопка
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
    timeSeconds: Int,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    // Background
    Image(
        painter = painterResource(id = R.drawable.fone),
        contentDescription = "Game Background",
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
            text = "Игра окончена!",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Red,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Финальный счёт: $score",
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Text(
                text = "Достигнутый уровень: $level",
                fontSize = 24.sp,
                color = Color.White
            )

            Text(
                text = "Время игры: ${timeSeconds} сек",
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(60.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MenuButton(
                text = "Играть снова",
                icon = Icons.Default.PlayArrow,
                onClick = onRestart,
                backgroundColor = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onPrimary
            )

            MenuButton(
                text = "Главное меню",
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

// отрисовка с изображениями
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
            timeSeconds = 120,
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
            records = listOf(
                GameRecord(playerName = "Игрок1", score = 150, level = 3, timeSeconds = 60),
                GameRecord(playerName = "Игрок2", score = 120, level = 2, timeSeconds = 45),
                GameRecord(playerName = "Игрок3", score = 90, level = 2, timeSeconds = 30)
            ),
            onBack = {}
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