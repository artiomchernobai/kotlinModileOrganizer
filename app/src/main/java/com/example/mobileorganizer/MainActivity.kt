package com.example.mobileorganizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mobileorganizer.ui.theme.MobileOrganizerTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileOrganizerTheme {
                MobileOrganizerApp()
            }
        }
    }
}

data class NoteItem(val id: Long, val date: String, val title: String, val description: String)
data class TaskItem(
    val id: Long,
    val title: String,
    val description: String,
    val priority: TaskPriority,
    val isDone: Boolean = false
)

enum class TaskPriority(val label: String, val tint: Color) {
    HIGH("Высокий", Color(0xFFD32F2F)),
    MEDIUM("Средний", Color(0xFFF57C00)),
    LOW("Низкий", Color(0xFF388E3C)),
}

enum class TaskSortMode(val label: String) {
    ALL("Все"),
    COMPLETED("Выполненные"),
    NOT_COMPLETED("Невыполненные"),
    HIGH("Высокий приоритет"),
    MEDIUM("Средний приоритет"),
    LOW("Низкий приоритет"),
}

@Composable
fun MobileOrganizerApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.CALENDAR) }
    val notes = remember { mutableStateListOf<NoteItem>() }
    val tasks = remember { mutableStateListOf<TaskItem>() }

    var noteDialogDate by remember { mutableStateOf<String?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.CALENDAR -> CalendarPage(
                    notes = notes,
                    onDayClick = { noteDialogDate = it },
                    modifier = Modifier.padding(innerPadding)
                )

                AppDestinations.NOTES -> NotesPage(
                    notes = notes,
                    onCreateClick = {
                        noteDialogDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            .format(Calendar.getInstance().time)
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                AppDestinations.TASKS -> TasksPage(
                    tasks = tasks,
                    onCreateClick = { showTaskDialog = true },
                    onToggleDone = { id ->
                        val i = tasks.indexOfFirst { it.id == id }
                        if (i != -1) tasks[i] = tasks[i].copy(isDone = !tasks[i].isDone)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    noteDialogDate?.let { selectedDate ->
        NoteDialog(
            date = selectedDate,
            onDismiss = { noteDialogDate = null },
            onSave = { title, description ->
                notes.add(
                    0,
                    NoteItem(
                        id = System.currentTimeMillis(),
                        date = selectedDate,
                        title = title,
                        description = description
                    )
                )
                noteDialogDate = null
            }
        )
    }

    if (showTaskDialog) {
        TaskDialog(
            onDismiss = { showTaskDialog = false },
            onSave = { title, description, priority ->
                tasks.add(
                    0,
                    TaskItem(
                        id = System.currentTimeMillis(),
                        title = title,
                        description = description,
                        priority = priority
                    )
                )
                showTaskDialog = false
            }
        )
    }
}

@Composable
fun CalendarPage(notes: List<NoteItem>, onDayClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val months = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    val weekDays = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")

    var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val firstDay = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }
    val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startOffset = firstDay.get(Calendar.DAY_OF_WEEK) - 1

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (showMonthPicker) selectedYear-- else {
                    if (selectedMonth == 0) {
                        selectedMonth = 11
                        selectedYear--
                    } else selectedMonth--
                }
            }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Назад")
            }

            Text(
                text = "${months[selectedMonth]} $selectedYear",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { showMonthPicker = !showMonthPicker }
            )

            IconButton(onClick = {
                if (showMonthPicker) selectedYear++ else {
                    if (selectedMonth == 11) {
                        selectedMonth = 0
                        selectedYear++
                    } else selectedMonth++
                }
            }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Вперед")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (showMonthPicker) {
            FlowRow(
                maxItemsInEachRow = 3,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                months.forEachIndexed { index, monthName ->
                    FilterChip(
                        selected = selectedMonth == index,
                        onClick = {
                            selectedMonth = index
                            showMonthPicker = false
                        },
                        label = { Text(monthName) }
                    )
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                weekDays.forEach { day ->
                    Text(day, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(8.dp))

            val totalCells = startOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                var dayCounter = 1
                repeat(rows) { rowIndex ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(7) { columnIndex ->
                            val position = rowIndex * 7 + columnIndex
                            if (position < startOffset || dayCounter > daysInMonth) {
                                Box(Modifier.size(42.dp))
                            } else {
                                val date = String.format(
                                    Locale.getDefault(),
                                    "%02d %s %d",
                                    dayCounter,
                                    months[selectedMonth],
                                    selectedYear
                                )
                                val hasNote = notes.any { it.date == date }
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        .background(
                                            if (hasNote) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { onDayClick(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(dayCounter.toString())
                                }
                                dayCounter++
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotesPage(notes: List<NoteItem>, onCreateClick: () -> Unit, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    val filtered = notes.filter {
        it.title.contains(query, true) ||
            it.description.contains(query, true) ||
            it.date.contains(query, true)
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Поиск заметок") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCreateClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.NoteAdd, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Создать заметку")
        }
        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Text("Пока заметок нет", style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 48.dp)) {
                items(filtered, key = { it.id }) { note ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(note.title, fontWeight = FontWeight.SemiBold)
                            Text(note.description, style = MaterialTheme.typography.bodyMedium)
                            Text(note.date, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TasksPage(
    tasks: List<TaskItem>,
    onCreateClick: () -> Unit,
    onToggleDone: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(TaskSortMode.ALL) }

    val filtered = tasks
        .filter { it.title.contains(query, true) || it.description.contains(query, true) }
        .filter {
            when (sortMode) {
                TaskSortMode.ALL -> true
                TaskSortMode.COMPLETED -> it.isDone
                TaskSortMode.NOT_COMPLETED -> !it.isDone
                TaskSortMode.HIGH -> it.priority == TaskPriority.HIGH
                TaskSortMode.MEDIUM -> it.priority == TaskPriority.MEDIUM
                TaskSortMode.LOW -> it.priority == TaskPriority.LOW
            }
        }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Поиск задач") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCreateClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.EditNote, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Создать задачу")
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskSortMode.entries.forEach {
                FilterChip(selected = sortMode == it, onClick = { sortMode = it }, label = { Text(it.label) })
            }
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 48.dp)) {
            items(filtered, key = { it.id }) { task ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = task.isDone, onCheckedChange = { onToggleDone(task.id) })
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(task.title, fontWeight = FontWeight.SemiBold)
                            Text(task.description, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(task.priority.label, color = task.priority.tint)
                    }
                }
            }
        }
    }
}

@Composable
fun NoteDialog(date: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая заметка") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = date, onValueChange = {}, readOnly = true, label = { Text("Дата") })
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") })
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onSave(title, description) }) { Text("Сохранить") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun TaskDialog(onDismiss: () -> Unit, onSave: (String, String, TaskPriority) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая задача") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") })
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach {
                        FilterChip(selected = priority == it, onClick = { priority = it }, label = { Text(it.label) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onSave(title, description, priority) }) {
                Text("Сохранить")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    CALENDAR("Календарь", Icons.Default.CalendarMonth),
    NOTES("Заметки", Icons.Default.NoteAdd),
    TASKS("Задачи", Icons.Default.CheckCircle),
}
