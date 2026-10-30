# Implementation Plan: Checklist Detail Screen

## Overview

Split `MainActivity.kt` into five purpose-specific Kotlin files and add a Checklist Detail
Screen with Flexible and Sequential task-toggling modes. Navigation is handled by Jetpack
Navigation Compose. All state lives in-memory using Compose `remember`; there is no database,
ViewModel, or persistence layer.

The six tasks follow the safe creation order from the design so that every referenced symbol
exists before it is used — Gradle dependency first, then data model, then screens, then
navigation wiring, then the MainActivity refactor.

---

## Tasks

- [x] 1. Add Navigation Compose dependency
  - [x] 1.1 Add `navigationCompose = "2.9.0"` to the `[versions]` table in `gradle/libs.versions.toml`
    - Insert the new line after the existing `composeBom` entry
    - Do NOT change any existing version entry
    - _Requirements: 11.1, 11.2, 11.4_
  - [x] 1.2 Add the library entry to the `[libraries]` table in `gradle/libs.versions.toml`
    - Add `androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }`
    - Insert after the `androidx-compose-material3` entry
    - _Requirements: 11.2_
  - [x] 1.3 Add `implementation(libs.androidx.navigation.compose)` to the `dependencies` block in `app/build.gradle.kts`
    - Insert after the existing `implementation(libs.androidx.lifecycle.runtime.ktx)` line
    - Do NOT add a separate version string; resolution comes from the version catalog entry
    - _Requirements: 11.1, 11.3_

- [x] 2. Create `DemoData.kt`
  - [x] 2.1 Create the file `app/src/main/java/com/ezlist/app/DemoData.kt`
    - Package declaration: `package com.ezlist.app`
    - No imports from other new files; no composable functions; no navigation logic
    - _Requirements: 1.3, 10.2_
  - [x] 2.2 Define the `DemoTask` data class
    - Fields: `id: Int`, `name: String`, `completed: Boolean`
    - _Requirements: 1.1_
  - [x] 2.3 Define the `DemoChecklist` data class
    - Fields: `id: Int`, `name: String`, `description: String`, `mode: String`, `tasks: List<DemoTask>`
    - _Requirements: 1.2_
  - [x] 2.4 Declare `val demoData: List<DemoChecklist>` with the three required checklists
    - "Morning Routine" (Sequential, id=1): Wake up ✓, Drink water ✓, Take a shower ✗, Get dressed ✗, Leave home ✗
    - "University Assignment" (Flexible, id=2): Find references ✓, Write introduction ✗, Create slides ✓, Proofread ✗
    - "Project Workflow" (Sequential, id=3): Define scope ✓, Create wireframes ✓, Set up project ✗, Build login screen ✗, Build home screen ✗, Add navigation ✗, Write tests ✗, Prepare release ✗
    - Task `id` values are 1-based within each checklist
    - _Requirements: 1.4_

- [x] 3. Create `HomeScreen.kt`
  - [x] 3.1 Create the file `app/src/main/java/com/ezlist/app/HomeScreen.kt`
    - Package declaration: `package com.ezlist.app`
    - Copy the existing `EzListHome` composable body from `MainActivity.kt` into this file as a starting point
    - _Requirements: 10.4_
  - [x] 3.2 Update the `EzListHome` composable signature
    - Replace the `lists: List<DemoChecklist>` parameter with `checklists: List<DemoChecklist>`
    - Add `onChecklistClick: (checklistId: Int) -> Unit` parameter
    - Update all internal references from `lists` to `checklists`
    - _Requirements: 3.3, 9.1_
  - [x] 3.3 Update the `ChecklistCard` composable signature and body
    - Add `onClick: () -> Unit` parameter
    - Replace `Card(modifier = ..., shape = ..., colors = ...)` with the Material 3 clickable overload: `Card(onClick = onClick, modifier = ..., shape = ..., colors = ...)`
    - Remove the hardcoded `checklist.completed` and `checklist.total` fields — derive progress inline:
      ```kotlin
      val completed = checklist.tasks.count { it.completed }
      val total = checklist.tasks.size
      val progress = if (total == 0) 0f else completed.toFloat() / total.toFloat()
      ```
    - Update the `"${checklist.completed} / ${checklist.total}"` text to use the derived `completed` and `total` variables
    - _Requirements: 2.1, 2.2, 2.3, 9.1_
  - [x] 3.4 Wire `onChecklistClick` into the `LazyColumn` item call
    - Pass `onClick = { onChecklistClick(checklist.id) }` to each `ChecklistCard`
    - _Requirements: 3.3_

- [x] 4. Create `ChecklistDetailScreen.kt`
  - [x] 4.1 Create the file `app/src/main/java/com/ezlist/app/ChecklistDetailScreen.kt`
    - Package declaration: `package com.ezlist.app`
    - Imports: `DemoTask`, `DemoChecklist` from the same package (no explicit import needed within the same package)
    - _Requirements: 10.5_
  - [x] 4.2 Implement the `ChecklistDetailScreen` composable
    - Signature: `fun ChecklistDetailScreen(checklist: DemoChecklist, onTaskToggle: (taskId: Int) -> Unit, onBack: () -> Unit)`
    - Derive progress inline (no hardcoded integers):
      ```kotlin
      val tasks = checklist.tasks
      val completed = tasks.count { it.completed }
      val total = tasks.size
      val progress = if (total == 0) 0f else completed.toFloat() / total.toFloat()
      ```
    - Use a `Scaffold` with a `TopAppBar` containing: a back arrow `IconButton` that calls `onBack()` in the navigation icon slot, and the checklist `name` as the title
    - Below the `TopAppBar`, display: mode label, `"$completed / $total completed"` text, a progress bar matching the same visual style as `ChecklistCard`, and a `LazyColumn` of `TaskRow` composables
    - _Requirements: 4.1, 4.2, 5.1, 5.2, 5.3, 5.4, 5.5_
  - [x] 4.3 Implement the `TaskRow` composable
    - Signature: `fun TaskRow(index: Int, task: DemoTask, onTap: () -> Unit)`
    - Make the entire row tappable (wrap in a `Row` with `Modifier.clickable { onTap() }`)
    - Display: 1-based `index` number, `"✓"` if `task.completed` else `"○"`, and the task name
    - Apply `TextDecoration.LineThrough` to the task name text when `task.completed == true`
    - _Requirements: 5.5, 5.6, 5.7, 5.8_
  - [x] 4.4 Implement the `OutOfSequenceDialog` composable
    - Signature: `fun OutOfSequenceDialog(onGoBack: () -> Unit, onContinue: () -> Unit)`
    - Use `AlertDialog` with:
      - `title = { Text("Out of Sequence") }`
      - `text` body conveying: (a) selected task is not the next step, (b) earlier tasks remain incomplete, (c) question asking whether to continue
      - `confirmButton`: `TextButton(onClick = onContinue) { Text("Continue Anyway") }`
      - `dismissButton`: `TextButton(onClick = onGoBack) { Text("Go Back") }`
      - `onDismissRequest = onGoBack` (covers back-gesture and outside-tap dismissal)
    - _Requirements: 8.1, 8.2, 8.3, 8.4_
  - [x] 4.5 Wire the Sequential mode decision tree inside `ChecklistDetailScreen`
    - Declare `var pendingTask by remember { mutableStateOf<DemoTask?>(null) }` for dialog state
    - Derive `val currentStep = checklist.tasks.firstOrNull { !it.completed }` on every recomposition
    - Build the `onTap` lambda for each `TaskRow`:
      ```kotlin
      onTap = {
          if (checklist.mode == "Sequential") {
              when {
                  task.completed             -> onTaskToggle(task.id)   // uncheck, no dialog
                  task.id == currentStep?.id -> onTaskToggle(task.id)   // current step, no dialog
                  else                       -> pendingTask = task      // sequence violation
              }
          } else {
              onTaskToggle(task.id)  // Flexible: always toggle
          }
      }
      ```
    - Show `OutOfSequenceDialog` when `pendingTask != null`:
      - `onGoBack  = { pendingTask = null }`
      - `onContinue = { onTaskToggle(pendingTask!!.id); pendingTask = null }`
    - _Requirements: 6.1, 6.2, 6.3, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5, 8.4, 8.5, 8.6_

- [x] 5. Create `AppNavigation.kt`
  - [x] 5.1 Create the file `app/src/main/java/com/ezlist/app/AppNavigation.kt`
    - Package declaration: `package com.ezlist.app`
    - _Requirements: 3.2, 10.3_
  - [x] 5.2 Implement the `AppNavigation` composable with mutable state and the toggle lambda
    - Signature: `fun AppNavigation(darkMode: Boolean, onToggleTheme: () -> Unit)`
    - Declare `var checklists by remember { mutableStateOf(demoData) }` as the single source of truth for all checklist state
    - Declare `val navController = rememberNavController()`
    - Implement the `onTaskToggle: (Int, Int) -> Unit` lambda that uses `copy()` to produce a new immutable list:
      ```kotlin
      val onTaskToggle: (Int, Int) -> Unit = { checklistId, taskId ->
          checklists = checklists.map { checklist ->
              if (checklist.id == checklistId)
                  checklist.copy(tasks = checklist.tasks.map { task ->
                      if (task.id == taskId) task.copy(completed = !task.completed) else task
                  })
              else checklist
          }
      }
      ```
    - _Requirements: 2.4, 10.3, 10.7_
  - [x] 5.3 Define the `NavHost` with the `"home"` route
    - `NavHost(navController = navController, startDestination = "home")`
    - `"home"` composable block: call `EzListHome(darkMode, onToggleTheme, checklists, onChecklistClick = { id -> navController.navigate("detail/$id") })`
    - _Requirements: 3.1, 3.2, 3.3, 12.4, 12.5_
  - [x] 5.4 Define the `"detail/{checklistId}"` route with the invalid-id guard
    - Declare the argument: `arguments = listOf(navArgument("checklistId") { type = NavType.IntType })`
    - Retrieve `checklistId` from `backStackEntry.arguments?.getInt("checklistId")`
    - Look up `val checklist = checklists.find { it.id == checklistId }`
    - If `checklist == null`: call `LaunchedEffect(Unit) { navController.popBackStack() }` and render nothing else
    - If `checklist != null`: call `ChecklistDetailScreen(checklist, onTaskToggle = { taskId -> onTaskToggle(checklist.id, taskId) }, onBack = { navController.popBackStack() })`
    - _Requirements: 3.4, 3.5, 3.6, 4.2, 4.3, 4.4_

- [x] 6. Refactor `MainActivity.kt`
  - [x] 6.1 Remove the old `DemoChecklist` data class from `MainActivity.kt`
    - Delete: `data class DemoChecklist(val name: String, val mode: String, val completed: Int, val total: Int)`
    - _Requirements: 10.1, 10.2_
  - [x] 6.2 Remove `demoLists`, `EzListHome`, `ChecklistCard`, and `EzListPreview` from `MainActivity.kt`
    - Delete the `val demoLists = listOf(...)` block inside `EzListApp`
    - Delete the `EzListHome` composable function entirely
    - Delete the `ChecklistCard` composable function entirely
    - Delete the `EzListPreview` preview function entirely
    - _Requirements: 10.1, 10.4_
  - [x] 6.3 Refactor `EzListApp` to call `AppNavigation`
    - Keep `var darkMode by rememberSaveable { mutableStateOf(true) }` inside `EzListApp`
    - Keep the `MaterialTheme(colorScheme = ...)` wrapper
    - Replace the `Surface { EzListHome(...) }` block with `AppNavigation(darkMode = darkMode, onToggleTheme = { darkMode = !darkMode })`
    - Remove the `Surface` wrapper (Scaffold inside `AppNavigation`/screens manages the background)
    - _Requirements: 9.2, 9.3, 10.1, 12.1_
  - [x] 6.4 Clean up unused imports in `MainActivity.kt`
    - Remove all imports that are no longer referenced after the deletions above (e.g., layout imports used only by the removed composables)
    - Keep: `Bundle`, `ComponentActivity`, `setContent`, `MaterialTheme`, `darkColorScheme`, `lightColorScheme`, `Composable`, `rememberSaveable`, `mutableStateOf`, `getValue`, `setValue`
    - _Requirements: 10.1, 12.1_

- [x] 7. Final checkpoint
  - Sync Gradle and verify the project builds without errors
  - Run the app on an emulator or device (API 24+) and confirm:
    - Home Screen displays three cards with correct names, modes, and live progress
    - Tapping a card navigates to the Detail Screen for that checklist
    - Detail Screen shows name, mode, "X / Y completed", progress bar, and numbered task list with ✓/○ indicators and strikethrough on completed tasks
    - Flexible mode: any task tap toggles freely; no dialog appears
    - Sequential mode: tapping the current step completes it; tapping a later incomplete step shows the Out-of-Sequence dialog; "Go Back" dismisses without change; "Continue Anyway" toggles the task
    - Back arrow and system back both return to the Home Screen without leaving the Detail Screen on the stack
  - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP. No tasks in this plan are marked optional because no correctness properties (property-based tests) are defined in the design — the design targets manual on-device verification only.
- The safe creation order (1 → 2 → 3 → 4 → 5 → 6) ensures all referenced symbols compile at each step. Do not reorder.
- Task 1 requires a Gradle sync before Tasks 2–6 will compile.
- Task 6 must come last; it removes declarations from `MainActivity.kt` that currently reference the old `DemoChecklist` shape. Tasks 3–5 must exist first so the new symbols are available.
- Progress counts in `HomeScreen.kt` and `ChecklistDetailScreen.kt` must never use integer literals for completed/total — always derive from `checklist.tasks`.
- Only `AppNavigation.kt` holds mutable `checklists` state; all other composables receive read-only snapshots and fire upward-flowing lambdas.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3"] },
    { "id": 2, "tasks": ["2.1"] },
    { "id": 3, "tasks": ["2.2", "2.3"] },
    { "id": 4, "tasks": ["2.4"] },
    { "id": 5, "tasks": ["3.1", "4.1"] },
    { "id": 6, "tasks": ["3.2", "4.2"] },
    { "id": 7, "tasks": ["3.3", "4.3", "4.4"] },
    { "id": 8, "tasks": ["3.4", "4.5"] },
    { "id": 9, "tasks": ["5.1"] },
    { "id": 10, "tasks": ["5.2"] },
    { "id": 11, "tasks": ["5.3", "5.4"] },
    { "id": 12, "tasks": ["6.1", "6.2"] },
    { "id": 13, "tasks": ["6.3"] },
    { "id": 14, "tasks": ["6.4"] }
  ]
}
```
