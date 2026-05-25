package com.example.armenianletterstrainer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

val Context.dataStore by preferencesDataStore(name = "armenian_trainer")

data class ArmenianLetter(
    val symbol: String,
    val answer: String
)

data class LetterStat(
    var correct: Int = 0,
    var mistakes: Int = 0
)

enum class Screen {
    TRAINER,
    STATS,
    USERS,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ArmenianTrainerApp()
            }
        }
    }
}

@Composable
fun ArmenianTrainerApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val letters = remember {
        listOf(
            ArmenianLetter("ա", "а"),
            ArmenianLetter("բ", "б"),
            ArmenianLetter("գ", "г"),
            ArmenianLetter("դ", "д"),
            ArmenianLetter("ե", "е"),
            ArmenianLetter("զ", "з"),
            ArmenianLetter("է", "э"),
            ArmenianLetter("ը", "ы"),
            ArmenianLetter("թ", "т"),
            ArmenianLetter("ժ", "ж"),
            ArmenianLetter("ի", "и"),
            ArmenianLetter("լ", "л"),
            ArmenianLetter("խ", "х"),
            ArmenianLetter("ծ", "ц"),
            ArmenianLetter("կ", "к"),
            ArmenianLetter("հ", "h"),
            ArmenianLetter("ձ", "дз"),
            ArmenianLetter("ղ", "гх"),
            ArmenianLetter("ճ", "ч"),
            ArmenianLetter("մ", "м"),
            ArmenianLetter("յ", "й"),
            ArmenianLetter("ն", "н"),
            ArmenianLetter("շ", "ш"),
            ArmenianLetter("ո", "о"),
            ArmenianLetter("չ", "ч"),
            ArmenianLetter("պ", "п"),
            ArmenianLetter("ջ", "дж"),
            ArmenianLetter("ռ", "рр"),
            ArmenianLetter("ս", "с"),
            ArmenianLetter("վ", "в"),
            ArmenianLetter("տ", "т"),
            ArmenianLetter("ր", "р"),
            ArmenianLetter("ց", "ц"),
            ArmenianLetter("ւ", "в/у"),
            ArmenianLetter("փ", "п"),
            ArmenianLetter("ք", "к"),
            ArmenianLetter("օ", "о"),
            ArmenianLetter("ֆ", "ф"),
            ArmenianLetter("և", "ев")
        )
    }

    var screen by remember { mutableStateOf(Screen.TRAINER) }
    var users by remember { mutableStateOf(listOf("Основной")) }
    var currentUser by remember { mutableStateOf("Основной") }

    val enabledLetters = remember {
        mutableStateMapOf<String, Boolean>()
    }

    val stats = remember {
        mutableStateMapOf<String, LetterStat>()
    }

    var currentLetter by remember {
        mutableStateOf(letters.random())
    }

    var message by remember {
        mutableStateOf("Выбери звук")
    }

    var messageColor by remember {
        mutableStateOf(Color.DarkGray)
    }

    LaunchedEffect(Unit) {
        users = loadUsers(context)
        currentUser = loadCurrentUser(context, users)
    }

    LaunchedEffect(currentUser) {
        val loadedStats = loadStats(context, currentUser, letters)
        stats.clear()
        stats.putAll(loadedStats)

        val loadedEnabled = loadEnabledLetters(context, currentUser, letters)
        enabledLetters.clear()
        enabledLetters.putAll(loadedEnabled)

        currentLetter = getWeightedRandomLetter(getActiveLetters(letters, enabledLetters), stats)

        message = "Пользователь: $currentUser"
        messageColor = Color.DarkGray
    }

    val activeLetters = getActiveLetters(letters, enabledLetters)

    fun saveEnabled() {
        scope.launch {
            saveEnabledLetters(context, currentUser, enabledLetters)
        }
    }

    fun nextLetter() {
        currentLetter = getWeightedRandomLetter(activeLetters, stats)
    }

    fun checkAnswer(answer: String) {
        val oldStat = stats[currentLetter.symbol] ?: LetterStat()
        val newStat = oldStat.copy()

        if (answer == currentLetter.answer) {
            newStat.correct++
            message = "Правильно"
            messageColor = Color(0xFF2E7D32)
        } else {
            newStat.mistakes++
            message = "Ошибка: ${currentLetter.symbol} = ${currentLetter.answer}"
            messageColor = Color(0xFFC62828)
        }

        stats[currentLetter.symbol] = newStat

        scope.launch {
            saveStats(context, currentUser, stats)
        }

        nextLetter()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF4F1EA)
    ) {
        when (screen) {
            Screen.TRAINER -> TrainerScreen(
                currentUser = currentUser,
                currentLetter = currentLetter,
                answers = getAnswerOptions(currentLetter, activeLetters),
                message = message,
                messageColor = messageColor,
                totalCorrect = stats.values.sumOf { it.correct },
                totalMistakes = stats.values.sumOf { it.mistakes },
                activeLettersCount = activeLetters.size,
                onAnswer = { checkAnswer(it) },
                onShowStats = { screen = Screen.STATS },
                onShowUsers = { screen = Screen.USERS },
                onShowSettings = { screen = Screen.SETTINGS }
            )

            Screen.STATS -> StatsScreen(
                currentUser = currentUser,
                stats = stats,
                letters = letters,
                onBack = { screen = Screen.TRAINER },
                onResetStats = {
                    letters.forEach { stats[it.symbol] = LetterStat() }
                    scope.launch { saveStats(context, currentUser, stats) }
                }
            )

            Screen.USERS -> UsersScreen(
                users = users,
                currentUser = currentUser,
                onBack = { screen = Screen.TRAINER },
                onSelectUser = { user ->
                    currentUser = user
                    scope.launch { saveCurrentUser(context, user) }
                },
                onCreateUser = { newUser ->
                    if (newUser.isNotBlank() && !users.contains(newUser)) {
                        users = users + newUser
                        currentUser = newUser

                        scope.launch {
                            saveUsers(context, users)
                            saveCurrentUser(context, newUser)
                        }
                    }
                }
            )

            Screen.SETTINGS -> SettingsScreen(
                currentUser = currentUser,
                letters = letters,
                enabledLetters = enabledLetters,
                onBack = { screen = Screen.TRAINER },
                onToggleLetter = { symbol, enabled ->
                    val enabledCount = enabledLetters.count { it.value }

                    if (!enabled && enabledCount <= 1) {
                        return@SettingsScreen
                    }

                    enabledLetters[symbol] = enabled
                    saveEnabled()
                    currentLetter = getWeightedRandomLetter(getActiveLetters(letters, enabledLetters), stats)
                },
                onEnableAll = {
                    letters.forEach { enabledLetters[it.symbol] = true }
                    saveEnabled()
                },
                onEnableSimilar = {
                    letters.forEach { enabledLetters[it.symbol] = false }

                    listOf("զ", "ժ", "ձ", "ջ", "ս", "ծ", "ճ", "չ", "ց").forEach {
                        enabledLetters[it] = true
                    }

                    saveEnabled()
                    currentLetter = getWeightedRandomLetter(getActiveLetters(letters, enabledLetters), stats)
                }
            )
        }
    }
}

@Composable
fun TrainerScreen(
    currentUser: String,
    currentLetter: ArmenianLetter,
    answers: List<String>,
    message: String,
    messageColor: Color,
    totalCorrect: Int,
    totalMistakes: Int,
    activeLettersCount: Int,
    onAnswer: (String) -> Unit,
    onShowStats: () -> Unit,
    onShowUsers: () -> Unit,
    onShowSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ԱԲԳ",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2933)
            )

            Text(
                text = "Тренажёр армянских букв",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2B2B2B)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$currentUser · букв включено: $activeLettersCount",
                fontSize = 15.sp,
                color = Color.Gray
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(245.dp),
            shape = RoundedCornerShape(34.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentLetter.symbol,
                    fontSize = 150.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2933)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                fontSize = 21.sp,
                fontWeight = FontWeight.Medium,
                color = messageColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                answers.chunked(2).forEach { rowAnswers ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowAnswers.forEach { answer ->
                            AnswerButton(
                                text = answer,
                                modifier = Modifier.weight(1f)
                            ) {
                                onAnswer(answer)
                            }
                        }

                        if (rowAnswers.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem("Верно", totalCorrect, Color(0xFF2E7D32))
                StatItem("Ошибки", totalMistakes, Color(0xFFC62828))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onShowStats,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Статистика")
                }

                Button(
                    onClick = onShowSettings,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Буквы")
                }

                Button(
                    onClick = onShowUsers,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Пользователи")
                }
            }
        }
    }
}

@Composable
fun UsersScreen(
    users: List<String>,
    currentUser: String,
    onBack: () -> Unit,
    onSelectUser: (String) -> Unit,
    onCreateUser: (String) -> Unit
) {
    var newUserName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Button(onClick = onBack, shape = RoundedCornerShape(16.dp)) {
            Text("Назад")
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Пользователи",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = newUserName,
            onValueChange = { newUserName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Имя нового пользователя") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                onCreateUser(newUserName.trim())
                newUserName = ""
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Создать пользователя")
        }

        Spacer(modifier = Modifier.height(18.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(users) { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (user == currentUser) {
                            Text(
                                text = "активный",
                                color = Color.Gray
                            )
                        }
                    }

                    if (user == currentUser) {
                        Button(onClick = {}, enabled = false) {
                            Text("Выбран")
                        }
                    } else {
                        OutlinedButton(onClick = { onSelectUser(user) }) {
                            Text("Выбрать")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    currentUser: String,
    letters: List<ArmenianLetter>,
    enabledLetters: Map<String, Boolean>,
    onBack: () -> Unit,
    onToggleLetter: (String, Boolean) -> Unit,
    onEnableAll: () -> Unit,
    onEnableSimilar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Button(onClick = onBack, shape = RoundedCornerShape(16.dp)) {
            Text("Назад")
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Какие буквы тренировать",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Пользователь: $currentUser",
            fontSize = 15.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onEnableAll,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Все буквы")
            }

            OutlinedButton(
                onClick = onEnableSimilar,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Похожие")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(letters) { letter ->
                val checked = enabledLetters[letter.symbol] ?: true

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = letter.symbol,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(54.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Звук: ${letter.answer}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Switch(
                        checked = checked,
                        onCheckedChange = { enabled ->
                            onToggleLetter(letter.symbol, enabled)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatsScreen(
    currentUser: String,
    stats: Map<String, LetterStat>,
    letters: List<ArmenianLetter>,
    onBack: () -> Unit,
    onResetStats: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onBack, shape = RoundedCornerShape(16.dp)) {
                Text("Назад")
            }

            OutlinedButton(onClick = onResetStats, shape = RoundedCornerShape(16.dp)) {
                Text("Сбросить")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Статистика",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Пользователь: $currentUser",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(letters) { letter ->
                val stat = stats[letter.symbol] ?: LetterStat()
                LetterStatRow(letter, stat)
            }
        }
    }
}

@Composable
fun LetterStatRow(
    letter: ArmenianLetter,
    stat: LetterStat
) {
    val total = stat.correct + stat.mistakes
    val mistakePercent = if (total == 0) 0 else stat.mistakes * 100 / total

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = letter.symbol,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(56.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Звук: ${letter.answer}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Верно: ${stat.correct}, ошибки: ${stat.mistakes}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Text(
            text = "$mistakePercent%",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (mistakePercent > 30) Color(0xFFC62828) else Color(0xFF2E7D32)
        )
    }
}

@Composable
fun AnswerButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = text,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatItem(
    title: String,
    value: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 16.sp, color = Color.Gray)

        Text(
            text = value.toString(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

fun getActiveLetters(
    letters: List<ArmenianLetter>,
    enabledLetters: Map<String, Boolean>
): List<ArmenianLetter> {
    val active = letters.filter { enabledLetters[it.symbol] != false }
    return active.ifEmpty { letters.take(1) }
}

fun getWeightedRandomLetter(
    letters: List<ArmenianLetter>,
    stats: Map<String, LetterStat>
): ArmenianLetter {
    val weightedList = mutableListOf<ArmenianLetter>()

    letters.forEach { letter ->
        val stat = stats[letter.symbol]
        val mistakes = stat?.mistakes ?: 0
        val correct = stat?.correct ?: 0

        val weight = 1 + mistakes * 3 - correct / 5
        val safeWeight = weight.coerceAtLeast(1)

        repeat(safeWeight) {
            weightedList.add(letter)
        }
    }

    return weightedList[Random.nextInt(weightedList.size)]
}

fun getAnswerOptions(
    currentLetter: ArmenianLetter,
    activeLetters: List<ArmenianLetter>
): List<String> {
    val allAnswers = activeLetters.map { it.answer }.distinct()

    val wrongAnswers = allAnswers
        .filter { it != currentLetter.answer }
        .shuffled()
        .take(3)

    return (wrongAnswers + currentLetter.answer).shuffled()
}

suspend fun saveStats(
    context: Context,
    userName: String,
    stats: Map<String, LetterStat>
) {
    val key = stringPreferencesKey("stats_$userName")

    val serialized = stats.entries.joinToString(";") { entry ->
        val symbol = entry.key
        val stat = entry.value
        "$symbol|${stat.correct}|${stat.mistakes}"
    }

    context.dataStore.edit { preferences ->
        preferences[key] = serialized
    }
}

suspend fun loadStats(
    context: Context,
    userName: String,
    letters: List<ArmenianLetter>
): MutableMap<String, LetterStat> {
    val key = stringPreferencesKey("stats_$userName")
    val preferences = context.dataStore.data.first()
    val saved = preferences[key]

    val result = mutableMapOf<String, LetterStat>()

    letters.forEach { letter ->
        result[letter.symbol] = LetterStat()
    }

    if (!saved.isNullOrBlank()) {
        saved.split(";").forEach { item ->
            val parts = item.split("|")
            if (parts.size == 3) {
                result[parts[0]] = LetterStat(
                    correct = parts[1].toIntOrNull() ?: 0,
                    mistakes = parts[2].toIntOrNull() ?: 0
                )
            }
        }
    }

    return result
}

suspend fun saveUsers(context: Context, users: List<String>) {
    val key = stringPreferencesKey("users")

    context.dataStore.edit { preferences ->
        preferences[key] = users.joinToString("|")
    }
}

suspend fun loadUsers(context: Context): List<String> {
    val key = stringPreferencesKey("users")
    val preferences = context.dataStore.data.first()
    val saved = preferences[key]

    return if (saved.isNullOrBlank()) {
        listOf("Основной")
    } else {
        saved.split("|").filter { it.isNotBlank() }
    }
}

suspend fun saveCurrentUser(context: Context, user: String) {
    val key = stringPreferencesKey("current_user")

    context.dataStore.edit { preferences ->
        preferences[key] = user
    }
}

suspend fun loadCurrentUser(context: Context, users: List<String>): String {
    val key = stringPreferencesKey("current_user")
    val preferences = context.dataStore.data.first()
    val saved = preferences[key]

    return if (!saved.isNullOrBlank() && users.contains(saved)) {
        saved
    } else {
        users.first()
    }
}

suspend fun saveEnabledLetters(
    context: Context,
    userName: String,
    enabledLetters: Map<String, Boolean>
) {
    val key = stringPreferencesKey("enabled_$userName")

    val serialized = enabledLetters.entries.joinToString(";") { entry ->
        "${entry.key}|${entry.value}"
    }

    context.dataStore.edit { preferences ->
        preferences[key] = serialized
    }
}

suspend fun loadEnabledLetters(
    context: Context,
    userName: String,
    letters: List<ArmenianLetter>
): MutableMap<String, Boolean> {
    val key = stringPreferencesKey("enabled_$userName")
    val preferences = context.dataStore.data.first()
    val saved = preferences[key]

    val result = mutableMapOf<String, Boolean>()

    letters.forEach {
        result[it.symbol] = true
    }

    if (!saved.isNullOrBlank()) {
        saved.split(";").forEach { item ->
            val parts = item.split("|")
            if (parts.size == 2) {
                result[parts[0]] = parts[1].toBoolean()
            }
        }
    }

    return result
}