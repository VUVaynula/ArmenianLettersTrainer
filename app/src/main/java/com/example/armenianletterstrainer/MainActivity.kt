package com.example.armenianletterstrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily


data class Letter(
    val id: String,
    val symbol: String,
    val answer: String,
    val group: String,
    val cursive: Boolean = false
)

data class Profile(
    val id: Int,
    val name: String,
    val selectedLetterIds: Set<String>,
    val mode: LetterMode
)

enum class LetterMode {
    PRINTED,
    CURSIVE,
    BOTH
}

enum class Screen {
    TRAINING,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                App()
            }
        }
    }
}

val ArmenianHandwritingFont = FontFamily(
    Font(R.font.tumanian_handwriting)
)
@Composable
fun App() {
    val allLetters = remember { armenianLetters() }

    val profiles = remember {
        mutableStateListOf(
            Profile(
                id = 1,
                name = "Основной",
                selectedLetterIds = allLetters.map { it.id }.toSet(),
                mode = LetterMode.BOTH
            )
        )
    }

    var currentProfileId by remember { mutableIntStateOf(1) }
    var screen by remember { mutableStateOf(Screen.TRAINING) }

    val currentProfile = profiles.firstOrNull { it.id == currentProfileId } ?: profiles.first()

    fun updateCurrentProfile(updated: Profile) {
        val index = profiles.indexOfFirst { it.id == updated.id }
        if (index >= 0) profiles[index] = updated
    }

    when (screen) {
        Screen.TRAINING -> TrainingScreen(
            allLetters = allLetters,
            profile = currentProfile,
            onSettingsClick = { screen = Screen.SETTINGS }
        )

        Screen.SETTINGS -> SettingsScreen(
            allLetters = allLetters,
            profiles = profiles,
            currentProfileId = currentProfileId,
            onSelectProfile = { currentProfileId = it },
            onUpdateProfile = { updateCurrentProfile(it) },
            onBack = { screen = Screen.TRAINING }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    allLetters: List<Letter>,
    profile: Profile,
    onSettingsClick: () -> Unit
) {
    val trainingLetters = allLetters.filter { letter ->
        letter.id in profile.selectedLetterIds &&
                when (profile.mode) {
                    LetterMode.PRINTED -> !letter.cursive
                    LetterMode.CURSIVE -> letter.cursive
                    LetterMode.BOTH -> true
                }
    }

    var currentLetter by remember { mutableStateOf<Letter?>(null) }
    var answers by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var answered by remember { mutableStateOf(false) }
    var cardColor by remember { mutableStateOf(Color.White) }

    fun nextLetter() {
        if (trainingLetters.isEmpty()) {
            currentLetter = null
            answers = emptyList()
            return
        }

        val next = randomLetterWithoutRepeat(trainingLetters, currentLetter)
        currentLetter = next
        answers = buildAnswerOptions(next, allLetters)
        selectedAnswer = null
        answered = false
        cardColor = Color.White
    }

    LaunchedEffect(profile) {
        nextLetter()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Армянский алфавит") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F1EA))
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ԱԲԳ",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold
            )

            if (currentLetter == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Не выбраны буквы для тренировки")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onSettingsClick) {
                        Text("Открыть настройки")
                    }
                }
            } else {
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
                            text = currentLetter!!.symbol,
                            fontSize = 160.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (currentLetter!!.cursive) {
                                ArmenianHandwritingFont
                            } else {
                                FontFamily.Default
                            }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    answers.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { answer ->
                                val isCorrect = answered && answer == currentLetter!!.answer
                                val isWrong = answered && answer == selectedAnswer && answer != currentLetter!!.answer

                                val colors = when {
                                    isCorrect -> ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    isWrong -> ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                                    else -> ButtonDefaults.buttonColors()
                                }

                                Button(
                                    onClick = {
                                        if (!answered) {
                                            answered = true
                                            selectedAnswer = answer

                                            cardColor =
                                                if (answer == currentLetter!!.answer) {
                                                    Color.Green
                                                } else {
                                                    Color.Red
                                                }
                                        }
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

                    if (answered) {
                        Button(
                            onClick = { nextLetter() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Следующая", fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    allLetters: List<Letter>,
    profiles: MutableList<Profile>,
    currentProfileId: Int,
    onSelectProfile: (Int) -> Unit,
    onUpdateProfile: (Profile) -> Unit,
    onBack: () -> Unit
) {
    var newProfileName by remember { mutableStateOf("") }
    val currentProfile by remember(profiles, currentProfileId) {
        derivedStateOf {
            profiles.firstOrNull { it.id == currentProfileId }
                ?: profiles.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Профили", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            items(profiles, key = { it.id }) { profile ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectProfile(profile.id) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = profile.id == currentProfileId,
                        onClick = { onSelectProfile(profile.id) }
                    )

                    Text(profile.name, modifier = Modifier.weight(1f))

                    IconButton(
                        enabled = profiles.size > 1,
                        onClick = {
                            profiles.remove(profile)
                            if (profile.id == currentProfileId) {
                                onSelectProfile(profiles.first().id)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    label = { Text("Новый профиль") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        val name = newProfileName.trim()
                        if (name.isNotEmpty()) {
                            val newId = (profiles.maxOfOrNull { it.id } ?: 0) + 1
                            profiles.add(
                                Profile(
                                    id = newId,
                                    name = name,
                                    selectedLetterIds = allLetters.map { it.id }.toSet(),
                                    mode = LetterMode.BOTH
                                )
                            )
                            onSelectProfile(newId)
                            newProfileName = ""
                        }
                    }
                ) {
                    Text("Добавить профиль")
                }
            }

            item {
                Divider()
                Text("Тип букв", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            item {
                ModeRow("Печатные", currentProfile.mode == LetterMode.PRINTED) {
                    onUpdateProfile(currentProfile.copy(mode = LetterMode.PRINTED))
                }

                ModeRow("Рукописные", currentProfile.mode == LetterMode.CURSIVE) {
                    onUpdateProfile(currentProfile.copy(mode = LetterMode.CURSIVE))
                }

                ModeRow("Печатные и рукописные", currentProfile.mode == LetterMode.BOTH) {
                    onUpdateProfile(currentProfile.copy(mode = LetterMode.BOTH))
                }
            }

            item {
                Divider()
                Text("Буквы для тренировки", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            items(allLetters, key = { it.id }) { letter ->
                val checked = letter.id in currentProfile.selectedLetterIds

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newSet =
                                if (checked) currentProfile.selectedLetterIds - letter.id
                                else currentProfile.selectedLetterIds + letter.id

                            onUpdateProfile(currentProfile.copy(selectedLetterIds = newSet))
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            val newSet =
                                if (isChecked) currentProfile.selectedLetterIds + letter.id
                                else currentProfile.selectedLetterIds - letter.id

                            onUpdateProfile(currentProfile.copy(selectedLetterIds = newSet))
                        }
                    )

                    Text(
                        text = "${letter.symbol} — ${letter.answer}" +
                                if (letter.cursive) " рукописная" else " печатная",
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ModeRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text)
    }
}

fun randomLetterWithoutRepeat(
    letters: List<Letter>,
    previous: Letter?
): Letter {
    if (letters.size <= 1) return letters.first()

    var next: Letter
    do {
        next = letters.random()
    } while (next.id == previous?.id)

    return next
}

fun buildAnswerOptions(
    current: Letter,
    allLetters: List<Letter>
): List<String> {
    val sameGroupAnswers = allLetters
        .filter { it.group == current.group }
        .map { it.answer }
        .distinct()
        .toMutableList()

    if (current.answer !in sameGroupAnswers) {
        sameGroupAnswers.add(current.answer)
    }

    val otherAnswers = allLetters
        .map { it.answer }
        .distinct()
        .filter { it != current.answer && it !in sameGroupAnswers }
        .shuffled()

    val options = mutableListOf<String>()
    options.add(current.answer)

    options.addAll(
        sameGroupAnswers
            .filter { it != current.answer }
            .shuffled()
            .take(3)
    )

    if (options.size < 4) {
        options.addAll(otherAnswers.take(4 - options.size))
    }

    return options.distinct().shuffled()
}

fun armenianLetters(): List<Letter> {
    return listOf(
        Letter("a_upper", "Ա", "а", "a_i_v_r"),
        Letter("a_lower", "ա", "а", "a_i_v_r"),

        Letter("b_upper", "Բ", "б", "b_g_p_q"),
        Letter("b_lower", "բ", "б", "b_g_p_q"),

        Letter("g_upper", "Գ", "г", "b_g_p_q"),
        Letter("g_lower", "գ", "г", "b_g_p_q"),

        Letter("d_upper", "Դ", "д", "d_t_l"),
        Letter("d_lower", "դ", "д", "d_t_l"),

        Letter("e_upper", "Ե", "е", "e_eh"),
        Letter("e_lower", "ե", "е", "e_eh"),

        Letter("z_upper", "Զ", "з", "z_zh_dz_dzh"),
        Letter("z_lower", "զ", "з", "z_zh_dz_dzh"),

        Letter("eh_upper", "Է", "э", "e_eh"),
        Letter("eh_lower", "է", "э", "e_eh"),

        Letter("yt_upper", "Ը", "ы", "yt_to"),
        Letter("yt_lower", "ը", "ы", "yt_to"),

        Letter("to_upper", "Թ", "т", "yt_to"),
        Letter("to_lower", "թ", "т", "yt_to"),

        Letter("zh_upper", "Ժ", "ж", "z_zh_dz_dzh"),
        Letter("zh_lower", "ժ", "ж", "z_zh_dz_dzh"),

        Letter("i_upper", "Ի", "и", "a_i_v_r"),
        Letter("i_lower", "ի", "и", "a_i_v_r"),

        Letter("l_upper", "Լ", "л", "d_t_l"),
        Letter("l_lower", "լ", "л", "d_t_l"),

        Letter("kh_upper", "Խ", "х", "kh_ts_k"),
        Letter("kh_lower", "խ", "х", "kh_ts_k"),

        Letter("ts_upper", "Ծ", "ц", "kh_ts_k"),
        Letter("ts_lower", "ծ", "ц", "kh_ts_k"),

        Letter("k_upper", "Կ", "к", "kh_ts_k"),
        Letter("k_lower", "կ", "к", "kh_ts_k"),

        Letter("h_upper", "Հ", "h", "h_gh_ch"),
        Letter("h_lower", "հ", "h", "h_gh_ch"),

        Letter("dz_upper", "Ձ", "дз", "z_zh_dz_dzh"),
        Letter("dz_lower", "ձ", "дз", "z_zh_dz_dzh"),

        Letter("gh_upper", "Ղ", "гх", "h_gh_ch"),
        Letter("gh_lower", "ղ", "гх", "h_gh_ch"),

        Letter("ch_upper", "Ճ", "ч", "h_gh_ch"),
        Letter("ch_lower", "ճ", "ч", "h_gh_ch"),

        Letter("m_upper", "Մ", "м", "m_n_sh"),
        Letter("m_lower", "մ", "м", "m_n_sh"),

        Letter("y_upper", "Յ", "й", "y_n"),
        Letter("y_lower", "յ", "й", "y_n"),

        Letter("n_upper", "Ն", "н", "m_n_sh"),
        Letter("n_lower", "ն", "н", "m_n_sh"),

        Letter("sh_upper", "Շ", "ш", "m_n_sh"),
        Letter("sh_lower", "շ", "ш", "m_n_sh"),

        Letter("vo_upper", "Ո", "во", "vo_ch"),
        Letter("vo_lower", "ո", "во", "vo_ch"),

        Letter("cha_upper", "Չ", "ч", "vo_ch"),
        Letter("cha_lower", "չ", "ч", "vo_ch"),

        Letter("p_upper", "Պ", "п", "b_g_p_q"),
        Letter("p_lower", "պ", "п", "b_g_p_q"),

        Letter("j_upper", "Ջ", "дж", "z_zh_dz_dzh"),
        Letter("j_lower", "ջ", "дж", "z_zh_dz_dzh"),

        Letter("rr_upper", "Ռ", "р", "r_rr_s_v"),
        Letter("rr_lower", "ռ", "р", "r_rr_s_v"),

        Letter("s_upper", "Ս", "с", "r_rr_s_v"),
        Letter("s_lower", "ս", "с", "r_rr_s_v"),

        Letter("v_upper", "Վ", "в", "r_rr_s_v"),
        Letter("v_lower", "վ", "в", "r_rr_s_v"),

        Letter("t_upper", "Տ", "т", "d_t_l"),
        Letter("t_lower", "տ", "т", "d_t_l"),

        Letter("r_upper", "Ր", "р", "a_i_v_r"),
        Letter("r_lower", "ր", "р", "a_i_v_r"),

        Letter("c_upper", "Ց", "ц", "ts_c_q"),
        Letter("c_lower", "ց", "ц", "ts_c_q"),

        Letter("w_upper", "Ւ", "в", "w_p_q"),
        Letter("w_lower", "ւ", "в", "w_p_q"),

        Letter("ph_upper", "Փ", "п", "w_p_q"),
        Letter("ph_lower", "փ", "п", "w_p_q"),

        Letter("q_upper", "Ք", "к", "b_g_p_q"),
        Letter("q_lower", "ք", "к", "b_g_p_q"),

        Letter("o_upper", "Օ", "о", "o_f"),
        Letter("o_lower", "օ", "о", "o_f"),

        Letter("f_upper", "Ֆ", "ф", "o_f"),
        Letter("f_lower", "ֆ", "ф", "o_f")
    )
}
