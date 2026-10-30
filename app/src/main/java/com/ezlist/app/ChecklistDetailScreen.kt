package com.ezlist.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistDetailScreen(
    checklist: DemoChecklist,
    onTaskToggle: (taskId: Int) -> Unit,
    onAddTask: (String) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onEditTask: (Int, String) -> Unit,
    onBack: () -> Unit
) {
    val tasks = checklist.tasks
    val completed = tasks.count { it.completed }
    val total = tasks.size
    val progress = if (total == 0) 0f else completed.toFloat() / total.toFloat()

    var pendingTask by remember { mutableStateOf<DemoTask?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var editingTaskId by remember { mutableStateOf<Int?>(null) }
    var deleteConfirmTaskId by remember { mutableStateOf<Int?>(null) }

    val currentStep = checklist.tasks.firstOrNull { !it.completed }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = { Text(text = checklist.name) }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = checklist.mode,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$completed / $total completed",
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No tasks yet", fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showAddTaskDialog = true }) {
                            Text("Add a task")
                        }
                    }
                }
            } else {
                LazyColumn {
                    itemsIndexed(checklist.tasks) { index, task ->
                        TaskRow(
                            index = index + 1,
                            task = task,
                            onTap = {
                                if (checklist.mode == "Sequential") {
                                    when {
                                        task.completed             -> onTaskToggle(task.id)
                                        task.id == currentStep?.id -> onTaskToggle(task.id)
                                        else                       -> pendingTask = task
                                    }
                                } else {
                                    onTaskToggle(task.id)
                                }
                            },
                            onEdit = { editingTaskId = task.id },
                            onDelete = { deleteConfirmTaskId = task.id }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = { showAddTaskDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ Add Task")
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    if (pendingTask != null) {
        OutOfSequenceDialog(
            onGoBack   = { pendingTask = null },
            onContinue = { onTaskToggle(pendingTask!!.id); pendingTask = null }
        )
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onAdd = { taskName ->
                onAddTask(taskName)
                showAddTaskDialog = false
            }
        )
    }

    editingTaskId?.let { taskId ->
        val task = tasks.find { it.id == taskId }
        if (task != null) {
            EditTaskDialog(
                initialName = task.name,
                onDismiss = { editingTaskId = null },
                onSave = { taskName ->
                    onEditTask(taskId, taskName)
                    editingTaskId = null
                }
            )
        }
    }

    deleteConfirmTaskId?.let { taskId ->
        val task = tasks.find { it.id == taskId }
        if (task != null) {
            DeleteConfirmationDialog(
                title = "Delete Task?",
                message = "Delete '${task.name}'?",
                onDismiss = { deleteConfirmTaskId = null },
                onConfirm = {
                    onDeleteTask(taskId)
                    deleteConfirmTaskId = null
                }
            )
        }
    }
}

@Composable
fun TaskRow(
    index: Int,
    task: DemoTask,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "$index.",
                modifier = Modifier.width(28.dp),
                fontSize = 14.sp
            )
            Text(
                text = if (task.completed) "✓" else "○",
                modifier = Modifier.width(24.dp),
                fontSize = 16.sp,
                color = if (task.completed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = task.name,
                fontSize = 15.sp,
                textDecoration = if (task.completed) TextDecoration.LineThrough
                                 else TextDecoration.None,
                color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.size(32.dp)
        ) {
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun OutOfSequenceDialog(
    onGoBack: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onGoBack,
        title   = { Text("Out of Sequence") },
        text    = {
            Text(
                "This task is not the next step in the sequence. " +
                "There are earlier tasks that haven't been completed yet. " +
                "Do you want to continue anyway?"
            )
        },
        confirmButton = { TextButton(onClick = onContinue) { Text("Continue Anyway") } },
        dismissButton = { TextButton(onClick = onGoBack)   { Text("Go Back") } }
    )
}
