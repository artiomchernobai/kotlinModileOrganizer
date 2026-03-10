package com.example.mobileorganizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
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
    val status: TaskStatus
)

enum class TaskPriority(val label: String, val tint: Color) {
    HIGH("Высокий", Color(0xFFE53935)),
    MEDIUM("Обычный", Color(0xFF5A2D91)),
    LOW("Низкий", Color(0xFF64B5F6))
}

enum class TaskStatus(val label: String) {
    ACTIVE("Активная"),
    COMPLETED("Завершенная")
}

enum class TaskSortMode(val label: String) {
    ALL("Все"),
    ACTIVE("Активные"),
    COMPLETED("Завершенные"),
    HIGH("Высокий"),
    MEDIUM("Обычные"),
    LOW("Низкий")
}

@Composable
fun MobileOrganizerApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.CALENDAR) }
    val notes = remember { mutableStateListOf<NoteItem>() }
    val tasks = remember { mutableStateListOf<TaskItem>() }

    var noteDialogDate by remember { mutableStateOf<String?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }

    NavigationSuiteScaffold(
        layoutType = NavigationSuiteType.NavigationBar,
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold { innerPadding ->
            when (currentDestination) {
                AppDestinations.CALENDAR -> CalendarPage(
                    notes = notes,
                    onDayClick = { noteDialogDate = it },
                    modifier = Modifier.padding(innerPadding)
                )

                AppDestinations.NOTES -> NotesPage(
                    notes = notes,
                    onCreateClick = {
                        noteDialogDate = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
                            .format(Calendar.getInstance().time)
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                AppDestinations.TASKS -> TasksPage(
                    tasks = tasks,
                    onCreateClick = { showTaskDialog = true },
                    onToggleDone = { id ->
                        val i = tasks.indexOfFirst { it.id == id }
                        if (i != -1) {
                            val now = tasks[i]
                            tasks[i] = now.copy(
                                status = if (now.status == TaskStatus.ACTIVE) TaskStatus.COMPLETED else TaskStatus.ACTIVE
                            )
                        }
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
                notes.add(0, NoteItem(System.currentTimeMillis(), selectedDate, title, description))
                noteDialogDate = null
            }
        )
    }

    if (showTaskDialog) {
        TaskDialog(
            onDismiss = { showTaskDialog = false },
            onSave = { title, description, priority, status ->
                tasks.add(0, TaskItem(System.currentTimeMillis(), title, description, priority, status))
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
    val weekDays = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")

    var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val firstDay = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }
    val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
    val mondayOffset = (firstDay.get(Calendar.DAY_OF_WEEK) + 5) % 7

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = {
                if (showMonthPicker) selectedYear-- else if (selectedMonth == 0) {
                    selectedMonth = 11
                    selectedYear--
                } else selectedMonth--
            }) { Icon(Icons.Default.KeyboardArrowLeft, null) }

            Text(
                text = if (showMonthPicker) "$selectedYear" else "${months[selectedMonth]} $selectedYear",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showMonthPicker = !showMonthPicker }
            )

            IconButton(onClick = {
                if (showMonthPicker) selectedYear++ else if (selectedMonth == 11) {
                    selectedMonth = 0
                    selectedYear++
                } else selectedMonth++
            }) { Icon(Icons.Default.KeyboardArrowRight, null) }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Месяц", "Неделя", "Год").forEachIndexed { idx, title ->
                val selected = (idx == 0 && !showMonthPicker) || (idx == 2 && showMonthPicker)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        title,
                        color = if (selected) Color.White else Color(0xFF5F6368),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (showMonthPicker) {
            MonthGridPicker(year = selectedYear, onPick = {
                selectedMonth = it
                showMonthPicker = false
            })
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFE5E9F1), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        weekDays.forEach { day ->
                            Text(
                                day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                color = Color(0xFF90A4AE),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    val totalCells = 42
                    var day = 1
                    repeat(6) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(7) { col ->
                                val index = row * 7 + col
                                val inMonth = index >= mondayOffset && day <= daysInMonth
                                val dateText = if (inMonth) String.format(
                                    Locale("ru"),
                                    "%02d %s %d",
                                    day,
                                    months[selectedMonth],
                                    selectedYear
                                ) else ""
                                val hasNote = inMonth && notes.any { it.date == dateText }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(72.dp)
                                        .background(
                                            when {
                                                hasNote -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                inMonth -> Color(0xFFF7F8FA)
                                                else -> Color(0xFFDDE2EA)
                                            },
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = inMonth) { onDayClick(dateText) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (inMonth) day.toString() else "",
                                        color = if (hasNote) MaterialTheme.colorScheme.primary else Color(0xFF374151)
                                    )
                                }
                                if (inMonth) day++
                            }
                        }
                        if (day > daysInMonth && row > 3) return@repeat
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGridPicker(year: Int, onPick: (Int) -> Unit) {
    val months = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    val week = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(months.indices.toList()) { month ->
            val cal = Calendar.getInstance().apply { set(year, month, 1) }
            val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val offset = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(month) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(months[month], color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.KeyboardArrowRight, null, tint = Color(0xFF9CA3AF))
                    }
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = Color(0xFF90A4AE)) }
                    }
                    var d = 1
                    repeat(2) { r ->
                        Row(Modifier.fillMaxWidth()) {
                            repeat(7) { c ->
                                val idx = r * 7 + c
                                val show = idx >= offset && d <= days
                                Text(
                                    text = if (show) d.toString() else "",
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4B5563)
                                )
                                if (show) d++
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
        it.title.contains(query, true) || it.description.contains(query, true) || it.date.contains(query, true)
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFFF2F2F7)).padding(8.dp)) {
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FB))) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    label = { Text("Поиск заметок...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(onClick = onCreateClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Новая заметка")
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(filtered, key = { it.id }) { note ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFE))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(note.title, fontWeight = FontWeight.SemiBold)
                        Text(note.date, style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8))
                        Text(note.description, minLines = 3)
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
                TaskSortMode.ACTIVE -> it.status == TaskStatus.ACTIVE
                TaskSortMode.COMPLETED -> it.status == TaskStatus.COMPLETED
                TaskSortMode.HIGH -> it.priority == TaskPriority.HIGH
                TaskSortMode.MEDIUM -> it.priority == TaskPriority.MEDIUM
                TaskSortMode.LOW -> it.priority == TaskPriority.LOW
            }
        }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFFF2F2F7)).padding(8.dp)) {
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FB))) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        label = { Text("Поиск задач...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onCreateClick, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) { Text("+") }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskSortMode.entries.forEach {
                        FilterChip(selected = sortMode == it, onClick = { sortMode = it }, label = { Text(it.label) })
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(filtered, key = { it.id }) { task ->
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.status == TaskStatus.COMPLETED,
                            onCheckedChange = { onToggleDone(task.id) }
                        )
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(task.title, fontWeight = FontWeight.SemiBold)
                            Text(task.priority.label, color = task.priority.tint, style = MaterialTheme.typography.labelSmall)
                            Text(task.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))
                        }
                        Text(task.status.label, color = if (task.status == TaskStatus.COMPLETED) Color(0xFF16A34A) else Color(0xFF64748B))
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
        title = { Text("Новая заметка", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = date, onValueChange = {}, readOnly = true, label = { Text("Дата") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Заголовок") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Содержание") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    maxLines = 10
                )
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onSave(title, description) }) { Text("Сохранить") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(onDismiss: () -> Unit, onSave: (String, String, TaskPriority, TaskStatus) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var status by remember { mutableStateOf(TaskStatus.ACTIVE) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая задача", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8
                )

                ExposedDropdownMenuBox(expanded = priorityExpanded, onExpandedChange = { priorityExpanded = !priorityExpanded }) {
                    OutlinedTextField(
                        value = priority.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Приоритет") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                        TaskPriority.entries.forEach {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(it.label, color = it.tint) },
                                onClick = {
                                    priority = it
                                    priorityExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = !statusExpanded }) {
                    OutlinedTextField(
                        value = status.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Статус") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        TaskStatus.entries.forEach {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(it.label) },
                                onClick = {
                                    status = it
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onSave(title, description, priority, status) }) {
                Text("Сохранить")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    CALENDAR("Календарь", Icons.Default.Home),
    NOTES("Заметки", Icons.Default.Favorite),
    TASKS("Задачи", Icons.Default.AccountBox)
}
