package com.ezlist.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(darkMode: Boolean, onToggleTheme: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var checklists by remember { mutableStateOf(demoData.toList()) }
    var isInitialized by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    val persistenceManager = remember { PersistenceManager(context) }

    // Load persisted data on first launch
    LaunchedEffect(Unit) {
        scope.launch {
            val saved = persistenceManager.loadChecklists()
            if (saved != null) {
                checklists = saved
            } else {
                checklists = demoData.toList()
                persistenceManager.saveChecklists(checklists)
            }
            isInitialized = true
        }
    }

    val onTaskToggle: (Int, Int) -> Unit = { checklistId, taskId ->
        checklists = checklists.map { checklist ->
            if (checklist.id == checklistId)
                checklist.copy(
                    tasks = checklist.tasks.map { task ->
                        if (task.id == taskId) task.copy(completed = !task.completed)
                        else task
                    }
                )
            else checklist
        }
        scope.launch {
            persistenceManager.saveChecklists(checklists)
        }
    }

    val onAddChecklist: (String, String) -> Unit = { name, mode ->
        val newId = (checklists.maxByOrNull { it.id }?.id ?: 0) + 1
        val newChecklist = DemoChecklist(
            id = newId,
            name = name,
            description = "",
            mode = mode,
            tasks = emptyList()
        )
        checklists = checklists + newChecklist
        scope.launch {
            persistenceManager.saveChecklists(checklists)
        }
    }

    val onDeleteChecklist: (Int) -> Unit = { checklistId ->
        checklists = checklists.filter { it.id != checklistId }
        scope.launch {
            persistenceManager.saveChecklists(checklists)
        }
        if (navController.currentDestination?.route?.startsWith("detail/$checklistId") == true) {
            navController.popBackStack()
        }
    }

    val onEditChecklist: (Int, String, String) -> Unit = { checklistId, newName, newMode ->
        checklists = checklists.map { checklist ->
            if (checklist.id == checklistId)
                checklist.copy(name = newName, mode = newMode)
            else checklist
        }
        scope.launch {
            persistenceManager.saveChecklists(checklists)
        }
    }

    val onAddTask: (Int, String) -> Unit = { checklistId, taskName ->
        checklists = checklists.map { checklist ->
            if (checklist.id == checklistId) {
                val newTaskId = (checklist.tasks.maxByOrNull { it.id }?.id ?: 0) + 1
                val newTask = DemoTask(id = newTaskId, name = taskName, completed = false)
                checklist.copy(tasks = checklist.tasks + newTask)
            } else checklist
        }
        scope.launch {
            persistenceManager.saveChecklists(checklists)
        }
    }

    val onDeleteTask: (Int, Int) -> Unit = { checklistId, taskId ->
        checklists = checklists.map { checklist ->
            if (checklist.id == checklistId)
                checklist.copy(tasks = checklist.tasks.filter { it.id != taskId })
            else checklist
        }
        scope.launch {
            persistenceManager.saveChecklists(checklists)
        }
    }

    val onEditTask: (Int, Int, String) -> Unit = { checklistId, taskId, newName ->
        checklists = checklists.map { checklist ->
            if (checklist.id == checklistId)
                checklist.copy(
                    tasks = checklist.tasks.map { task ->
                        if (task.id == taskId) task.copy(name = newName)
                        else task
                    }
                )
            else checklist
        }
        scope.launch {
            persistenceManager.saveChecklists(checklists)
        }
    }

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            EzListHome(
                darkMode = darkMode,
                onToggleTheme = onToggleTheme,
                checklists = checklists,
                onChecklistClick = { id -> navController.navigate("detail/$id") },
                onAddChecklist = onAddChecklist,
                onDeleteChecklist = onDeleteChecklist,
                onEditChecklist = onEditChecklist,
                isInitialized = isInitialized
            )
        }

        composable(
            route = "detail/{checklistId}",
            arguments = listOf(navArgument("checklistId") { type = NavType.IntType })
        ) { backStackEntry ->
            val checklistId = backStackEntry.arguments?.getInt("checklistId")
            val checklist = checklists.find { it.id == checklistId }

            if (checklist == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                ChecklistDetailScreen(
                    checklist = checklist,
                    onTaskToggle = { taskId -> onTaskToggle(checklist.id, taskId) },
                    onAddTask = { taskName -> onAddTask(checklist.id, taskName) },
                    onDeleteTask = { taskId -> onDeleteTask(checklist.id, taskId) },
                    onEditTask = { taskId, taskName -> onEditTask(checklist.id, taskId, taskName) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
