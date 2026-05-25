package com.example.armenianletterstrainer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

val Context.dataStore by preferencesDataStore(name = "trainer")

data class Letter(
    val symbol: String,
    val answer: String,
    val cursive: Boolean = false
)

data class Stat(
    var correct: Int = 0,
    var mistakes: Int = 0
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                TrainerScreen()
            }
        }
    }
}

@Composable
fun TrainerScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val letters = remember {
        listOf(
            Letter("զ", "з"),
            Letter("ժ", "ж"),
            Letter("ձ", "дз"),
            Letter("ջ", "дж"),

            Letter("Զ", "з"),
            Letter("Ժ", "ж"),
            Letter("Ձ", "дз"),
            Letter("Ջ", "дж"),

            Letter("զ", "з", true),
            Letter("ժ", "ж", true),
            Letter("ձ", "дз", true),
            Letter("ջ", "дж", true)
        )
    }

    val answers = listOf("з", "ж", "дз", "дж")

    val stats = remember {
        mutableStateMapOf<String, Stat>()
    }

    var currentLetter by remember {
        mutableStateOf(letters.random())
    }

    var selectedAnswer by remember {
        mutableStateOf<String?>(null)
    }

    var answered by remember {
        mutableStateOf(false)
    }

    var cardColor by remember {
        mutableStateOf(Color.White)
    }

    var userName by remember {
        mutableStateOf("Основной")
    }

    suspend fun saveStats() {

        val key = stringPreferencesKey("stats_$userName")

        val serialized = stats.entries.joinToString(";") {
            "${it.key}|${it.value.correct}|${it.value.mistakes}"
        }

        context.dataStore.edit {
            it[key] = serialized
        }
    }

    suspend fun loadStats() {

        val key = stringPreferencesKey("stats_$userName")

        val preferences = context.dataStore.data.first()

        val saved = preferences[key]

        stats.clear()

        if (!saved.isNullOrBlank()) {

            saved.split(";").forEach {

                val parts = it.split("|")

                if (parts.size == 3) {

                    stats[parts[0]] = Stat(
                        parts[1].toIntOrNull() ?: 0,
                        parts[2].toIntOrNull() ?: 0
                    )
                }
            }
        }
    }

    LaunchedEffect(userName) {
        loadStats()
    }

    fun nextLetter() {

        currentLetter = letters.random()

        answered = false

        selectedAnswer = null

        cardColor = Color.White
    }

    fun checkAnswer(answer: String) {

        if (answered) return

        answered = true

        selectedAnswer = answer

        val key = currentLetter.symbol +
                if (currentLetter.cursive) "_cursive" else "_print"

        val stat = stats[key] ?: Stat()

        if (answer == currentLetter.answer) {

            stat.correct++

            cardColor = Color(0xFFE8F5E9)

        } else {

            stat.mistakes++

            cardColor = Color(0xFFFFEBEE)
        }

        stats[key] = stat

        scope.launch {
            saveStats()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F1EA))
            .padding(20.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "ԱԲԳ",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Армянский алфавит",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = {
                        userName = "Основной"
                    }
                ) {
                    Text("Основной")
                }

                Button(
                    onClick = {
                        userName = "Ребёнок"
                    }
                ) {
                    Text("Ребёнок")
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),

            shape = RoundedCornerShape(30.dp),

            colors = CardDefaults.cardColors(
                containerColor = cardColor
            )
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),

                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = currentLetter.symbol,

                    fontSize = 160.sp,

                    fontWeight = FontWeight.Bold,

                    fontStyle =
                        if (currentLetter.cursive)
                            FontStyle.Italic
                        else
                            FontStyle.Normal
                )
            }
        }

        Column {

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                answers.chunked(2).forEach { row ->

                    Row(
                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        row.forEach { answer ->

                            val isCorrect =
                                answered &&
                                        answer == currentLetter.answer

                            val isWrong =
                                answered &&
                                        answer == selectedAnswer &&
                                        answer != currentLetter.answer

                            val colors = when {

                                isCorrect ->
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            Color(0xFF2E7D32)
                                    )

                                isWrong ->
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            Color(0xFFC62828)
                                    )

                                else ->
                                    ButtonDefaults.buttonColors()
                            }

                            Button(
                                onClick = {
                                    checkAnswer(answer)
                                },

                                enabled = !answered,

                                modifier = Modifier
                                    .weight(1f)
                                    .height(78.dp),

                                shape = RoundedCornerShape(20.dp),

                                colors = colors
                            ) {

                                Text(
                                    text = answer,

                                    fontSize = 26.sp,

                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (answered) {

                Button(
                    onClick = {
                        nextLetter()
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),

                    shape = RoundedCornerShape(20.dp)
                ) {

                    Text(
                        text = "Следующая",

                        fontSize = 24.sp,

                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        val totalCorrect = stats.values.sumOf { it.correct }

        val totalMistakes = stats.values.sumOf { it.mistakes }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color.White,
                    RoundedCornerShape(20.dp)
                )
                .padding(16.dp),

            horizontalArrangement =
                Arrangement.SpaceAround
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text("Верно")

                Text(
                    totalCorrect.toString(),

                    fontSize = 30.sp,

                    fontWeight = FontWeight.Bold,

                    color = Color(0xFF2E7D32)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text("Ошибки")

                Text(
                    totalMistakes.toString(),

                    fontSize = 30.sp,

                    fontWeight = FontWeight.Bold,

                    color = Color(0xFFC62828)
                )
            }
        }
    }
}