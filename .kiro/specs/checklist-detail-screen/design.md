# Design Document: Checklist Detail Screen

## Overview

This feature extends the EzList Android app by splitting the monolithic `MainActivity.kt` into five purpose-specific files and adding a Checklist Detail Screen. The Detail Screen shows all tasks for a selected checklist, supports task toggling in Flexible mode, enforces sequential ordering (with an out-of-sequence warning dialog) in Sequential mode, and derives all progress values from live task state rather than hardcoded integers. Navigation is handled by Jetpack Navigation Compose. All state is held in memory using Compose `remember`; there is no database, ViewModel, or persistence layer.

---

## Architecture

### File Structure

All files live in `com.ezlist.app` (package root: `app/src/main/java/com/ezlist/app/`).

```
com.ezlist.app/
├── MainActivity.kt            # Entry point + theme wrapper only
├── DemoData.kt                # Data classes + demo list only
├── AppNavigation.kt           # NavHost + route definitions only
├── HomeScreen.kt              # EzListHome + ChecklistCard composables
└── ChecklistDetailScreen.kt   # ChecklistDetailScreen + TaskRow composables
```

### Role of Each File

| File | Sole responsibility |
|---|---|
| `MainActivity.kt` | Hosts `ComponentActivity`, calls `setContent`, wraps in `MaterialTheme`, calls `AppNavigation` composable |
| `DemoData.kt` | Defines `DemoTask`, `DemoChecklist`, and the top-level `demoData` list |
| `AppNavigation.kt` | Creates the `NavController`, holds the single mutable `checklists` state, defines the `NavHost` with two routes |
| `HomeScreen.kt` | Renders the home screen list and each checklist card |
| `ChecklistDetailScreen.kt` | Renders the detail screen, task list rows, and the out-of-sequence dialog |

No file may contain declarations belonging to another file's responsibility. There are no ViewModels, no LiveData, no StateFlow, no Room, and no Hilt.

### Architecture Diagram

```
MainActivity.kt
    └── AppNavigation.kt  (owns checklists state + NavHost)
            ├── HomeScreen.kt     (EzListHome + ChecklistCard)
            └── ChecklistDetailScreen.kt  (detail screen + TaskRow + dialog)

DemoData.kt  (imported by AppNavigation, HomeScreen, ChecklistDetailScreen)
```

### State Management

Mutable state lives exclusively in `AppNavigation.kt`:

```kotlin
@Composable
fun AppNavigation(darkMode: Boolean, onToggleTheme: () -> Unit) {
    var checklists by remember { mutableStateOf(demoData) }
    val navController = rememberNavController()
    // ...
}
```

`demoData` (immutable `val` in `DemoData.kt`) seeds the initial state. From that point, `checklists` is the live copy that all composables read.

State flows **downward** as read-only parameters and mutations flow **upward** via a single lambda callback:

```kotlin
onTaskToggle: (checklistId: Int, taskId: Int) -> Unit
```

`AppNavigation.kt` implements this lambda using `copy()` to produce a new immutable list:

```kotlin
val onTaskToggle: (Int, Int) -> Unit = { checklistId, taskId ->
    checklists = checklists.map { checklist ->
        if (checklist.id == checklistId) {
            checklist.copy(
                tasks = checklist.tasks.map { task ->
                    if (task.id == taskId) task.copy(completed = !task.completed)
                    else task
                }
            )
        } else checklist
    }
}
```

Assigning a new list triggers recomposition of both the Home Screen cards and the open Detail Screen atomically. No ViewModel is needed; the state is re-seeded from `demoData` on configuration change, which is acceptable because all data is demo-only.

### Navigation

Two routes are defined in a single `NavHost` inside `AppNavigation.kt`:

| Route | Screen | Argument |
|---|---|---|
| `"home"` | `EzListHome` | none |
| `"detail/{checklistId}"` | `ChecklistDetailScreen` | `checklistId: Int` |

The `checklistId` integer is embedded in the route path and declared with `NavType.IntType`. If the argument is absent or resolves to no matching checklist, `popBackStack()` is called inside a `LaunchedEffect` before any Detail Screen content is rendered. Back navigation (arrow button and system back) both call `popBackStack()`.

### Dependency Change

Add to `gradle/libs.versions.toml`:
```toml
# [versions]
navigationCompose = "2.9.0"

# [libraries]
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
```

Add to `app/build.gradle.kts` dependencies block:
```kotlin
implementation(libs.androidx.navigation.compose)
```

Navigation Compose `2.9.0` is managed by Compose BOM `2026.02.01` — no version conflict. No other existing version entries are modified.

---

## Data Models

### DemoTask

```kotlin
data class DemoTask(
    val id: Int,
    val name: String,
    val completed: Boolean
)
```

| Field | Type | Notes |
|---|---|---|
| `id` | `Int` | Unique within a checklist's task list |
| `name` | `String` | Display label |
| `completed` | `Boolean` | Toggled by user interaction |

### DemoChecklist

```kotlin
data class DemoChecklist(
    val id: Int,
    val name: String,
    val description: String,
    val mode: String,          // "Sequential" or "Flexible"
    val tasks: List<DemoTask>
)
```

| Field | Type | Notes |
|---|---|---|
| `id` | `Int` | Used as the navigation route argument |
| `name` | `String` | Display title |
| `description` | `String` | Reserved for future use; empty for demo data |
| `mode` | `String` | `"Sequential"` or `"Flexible"` — controls toggle behaviour |
| `tasks` | `List<DemoTask>` | Ordered; order is significant in Sequential mode |

### demoData (top-level val in DemoData.kt)

```kotlin
val demoData: List<DemoChecklist> = listOf(
    DemoChecklist(
        id = 1, name = "Morning Routine", description = "", mode = "Sequential",
        tasks = listOf(
            DemoTask(id = 1, name = "Wake up",       completed = true),
            DemoTask(id = 2, name = "Drink water",   completed = true),
            DemoTask(id = 3, name = "Take a shower", completed = false),
            DemoTask(id = 4, name = "Get dressed",   completed = false),
            DemoTask(id = 5, name = "Leave home",    completed = false)
        )
    ),
    DemoChecklist(
        id = 2, name = "University Assignment", description = "", mode = "Flexible",
        tasks = listOf(
            DemoTask(id = 1, name = "Find references",    completed = true),
            DemoTask(id = 2, name = "Write introduction", completed = false),
            DemoTask(id = 3, name = "Create slides",      completed = true),
            DemoTask(id = 4, name = "Proofread",          completed = false)
        )
    ),
    DemoChecklist(
        id = 3, name = "Project Workflow", description = "", mode = "Sequential",
        tasks = listOf(
            DemoTask(id = 1, name = "Define scope",       completed = true),
            DemoTask(id = 2, name = "Create wireframes",  completed = true),
            DemoTask(id = 3, name = "Set up project",     completed = false),
            DemoTask(id = 4, name = "Build login screen", completed = false),
            DemoTask(id = 5, name = "Build home screen",  completed = false),
            DemoTask(id = 6, name = "Add navigation",     completed = false),
            DemoTask(id = 7, name = "Write tests",        completed = false),
            DemoTask(id = 8, name = "Prepare release",    completed = false)
        )
    )
)
```

---

## Components and Interfaces

### MainActivity.kt

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EzListApp() }
    }
}

@Composable
fun EzListApp() {
    var darkMode by rememberSaveable { mutableStateOf(true) }
    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()
    ) {
        AppNavigation(
            darkMode = darkMode,
            onToggleTheme = { darkMode = !darkMode }
        )
    }
}
```

Only `MainActivity` and `EzListApp` live here. No composable screens, data classes, or navigation routes.

### AppNavigation.kt

```kotlin
@Composable
fun AppNavigation(darkMode: Boolean, onToggleTheme: () -> Unit) {
    var checklists by remember { mutableStateOf(demoData) }
    val navController = rememberNavController()

    val onTaskToggle: (Int, Int) -> Unit = { checklistId, taskId ->
        checklists = checklists.map { checklist ->
            if (checklist.id == checklistId)
                checklist.copy(tasks = checklist.tasks.map { task ->
                    if (task.id == taskId) task.copy(completed = !task.completed) else task
                })
            else checklist
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            EzListHome(
                darkMode = darkMode,
                onToggleTheme = onToggleTheme,
                checklists = checklists,
                onChecklistClick = { id -> navController.navigate("detail/$id") }
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
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
```

### HomeScreen.kt

**EzListHome signature:**
```kotlin
@Composable
fun EzListHome(
    darkMode: Boolean,
    onToggleTheme: () -> Unit,
    checklists: List<DemoChecklist>,
    onChecklistClick: (checklistId: Int) -> Unit
)
```

**ChecklistCard signature:**
```kotlin
@Composable
fun ChecklistCard(
    checklist: DemoChecklist,
    onClick: () -> Unit
)
```

Inline progress inside `ChecklistCard`:
```kotlin
val completed = checklist.tasks.count { it.completed }
val total = checklist.tasks.size
val progress = if (total == 0) 0f else completed.toFloat() / total.toFloat()
```

The card becomes tappable using `Card(onClick = onClick, ...)` (Material 3 overload) which provides ripple feedback automatically.

### ChecklistDetailScreen.kt

**ChecklistDetailScreen signature:**
```kotlin
@Composable
fun ChecklistDetailScreen(
    checklist: DemoChecklist,
    onTaskToggle: (taskId: Int) -> Unit,
    onBack: () -> Unit
)
```

**TaskRow signature:**
```kotlin
@Composable
fun TaskRow(
    index: Int,       // 1-based display number
    task: DemoTask,
    onTap: () -> Unit
)
```

Inline progress inside `ChecklistDetailScreen`:
```kotlin
val tasks = checklist.tasks
val completed = tasks.count { it.completed }
val total = tasks.size
val progress = if (total == 0) 0f else completed.toFloat() / total.toFloat()
```

Strikethrough for completed task names:
```kotlin
Text(
    text = task.name,
    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
)
```

Completion indicator:
```kotlin
Text(text = if (task.completed) "✓" else "○")
```

Screen structure:
```
ChecklistDetailScreen (Scaffold)
├── TopAppBar  ← back arrow | checklist.name
└── Column (scrollable content)
    ├── mode label
    ├── "X / Y completed" text
    ├── progress bar
    └── LazyColumn
        └── TaskRow × N  (#index · ✓/○ · task.name · strikethrough)
[AlertDialog — Out of Sequence — shown conditionally]
```

### Sequential Mode Logic in ChecklistDetailScreen

Local state tracks which task triggered a sequence violation:

```kotlin
var pendingTask by remember { mutableStateOf<DemoTask?>(null) }
```

Current step is a derived value, recomputed on every recomposition:

```kotlin
val currentStep = checklist.tasks.firstOrNull { !it.completed }
```

Decision tree on task tap (constructed per-row inside `ChecklistDetailScreen` and passed as `onTap` to `TaskRow`):

```kotlin
onTap = {
    if (checklist.mode == "Sequential") {
        when {
            task.completed             -> onTaskToggle(task.id)  // uncheck, no dialog
            task.id == currentStep?.id -> onTaskToggle(task.id)  // check current step, no dialog
            else                       -> pendingTask = task     // show dialog
        }
    } else {
        onTaskToggle(task.id)  // Flexible: always toggle
    }
}
```

### Out-of-Sequence Dialog (OutOfSequenceDialog — inside ChecklistDetailScreen.kt)

```kotlin
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
```

Dialog is shown when `pendingTask != null`:

```kotlin
if (pendingTask != null) {
    OutOfSequenceDialog(
        onGoBack  = { pendingTask = null },
        onContinue = {
            onTaskToggle(pendingTask!!.id)
            pendingTask = null
        }
    )
}
```

Tapping "Go Back", using the back gesture, or tapping outside all map to `onDismissRequest` → `pendingTask = null`. No task is permanently locked.

---

## Error Handling

### Invalid Navigation Argument

If the `checklistId` argument is absent, non-integer, or does not match any entry in `demoData`, the Detail Screen route guard handles it:

```kotlin
if (checklist == null) {
    LaunchedEffect(Unit) { navController.popBackStack() }
    // No Detail Screen content is rendered
}
```

`LaunchedEffect` ensures `popBackStack()` runs as a side-effect after composition, not during it, satisfying Compose's rule against navigation calls during composition.

### Empty Task List

If `checklist.tasks` is empty:
- `completed = 0`, `total = 0`
- `progress = 0f` (guarded by `if (total == 0) 0f` branch)
- The task list renders nothing; no crash occurs

### Configuration Change

State held in `remember { mutableStateOf(demoData) }` does not survive configuration changes (rotation). The list is re-seeded from `demoData` on re-creation. This is acceptable for a demo-only, in-memory milestone. No `rememberSaveable` is needed for the checklist state because the demo data always produces the same initial state.

---

## Correctness Properties

The following invariants must hold at all times:

1. **Single NavHost** — Exactly one `NavHost` exists across all files. Verified by code review; no `NavHost` in `HomeScreen.kt`, `ChecklistDetailScreen.kt`, or `MainActivity.kt`.

2. **No hardcoded progress integers** — `completed` and `total` counts in `HomeScreen.kt` and `ChecklistDetailScreen.kt` are always derived from `checklist.tasks`. No integer literal `3`, `5`, `4`, `6`, `2`, or `8` appears as a progress value in those files.

3. **Sequential dialog only for forward violations** — The dialog fires only when `task.completed == false AND task.id != currentStep?.id`. It never fires when unchecking a completed task.

4. **"Continue Anyway" does not auto-complete earlier tasks** — Only `task.id` (the tapped task) is toggled. Earlier incomplete tasks are not touched.

5. **onTaskToggle is a pure toggle** — `task.copy(completed = !task.completed)`. Calling it twice on the same task returns to the original state.

6. **State ownership** — Only `AppNavigation.kt` holds a mutable `checklists` variable. `HomeScreen.kt` and `ChecklistDetailScreen.kt` receive read-only snapshots.

---

## Testing Strategy

This milestone targets manual on-device verification. There are no automated UI tests or unit tests required. The following scenarios should be verified on a physical device:

### Home Screen
- App launches with dark theme active
- Theme toggle switches light/dark and back
- Three checklist cards are shown with correct names, modes, and progress derived from task data
- Tapping a card navigates to its Detail Screen
- FAB "+" is visible and tapping it has no visible effect

### Detail Screen Display
- Name shows in bold ≥20sp at top
- Mode label ("Sequential" / "Flexible") is visible
- "X / Y completed" text matches initial task data
- Progress bar fill matches X/Y ratio
- All tasks shown numbered from 1
- Completed tasks show ✓ and strikethrough; incomplete tasks show ○

### Flexible Mode (University Assignment)
- Tap any incomplete task → it becomes ✓ with strikethrough
- Tap a completed task → it becomes ○ without strikethrough
- Progress bar and "X / Y" text update immediately
- No dialog ever appears

### Sequential Mode (Morning Routine)
- Tap "Take a shower" (current step) → completes immediately, no dialog
- Tap "Get dressed" (not current step) → Out-of-Sequence dialog appears
  - Tap "Go Back" → dialog closes, task remains ○
  - Re-open dialog, tap "Continue Anyway" → task becomes ✓, earlier tasks unchanged
- Tap a completed task ("Wake up") → unchecks it, no dialog
- Progress bar and "X / Y" update after each interaction

### Back Navigation
- Tap back arrow → returns to Home Screen
- Tap system back → returns to Home Screen
- From Home Screen, pressing back again does not return to Detail Screen

### Migration Correctness
- `MainActivity.kt` contains no composable screen functions or data classes
- `DemoData.kt` contains no composable functions
- `AppNavigation.kt` contains exactly one `NavHost`

---

## Migration Plan

### What to Remove from MainActivity.kt

| Declaration | Action |
|---|---|
| `data class DemoChecklist(name, mode, completed: Int, total: Int)` | Delete — replaced by richer class in `DemoData.kt` |
| `val demoLists = listOf(...)` inside `EzListApp` | Delete — replaced by `demoData` in `DemoData.kt` |
| `@Composable fun EzListHome(...)` | Move to `HomeScreen.kt` with updated signature |
| `@Composable fun ChecklistCard(...)` | Move to `HomeScreen.kt` with `onClick` param added |
| `@Preview fun EzListPreview()` | Move to `HomeScreen.kt` or delete |

`EzListApp` is kept but refactored to wrap `AppNavigation` instead of `EzListHome`.

### Safe Creation Order

1. Update `libs.versions.toml` and `app/build.gradle.kts` — add Navigation Compose dependency; Gradle sync.
2. Create `DemoData.kt` — defines `DemoTask`, `DemoChecklist`, and `demoData`; no imports from other new files.
3. Create `HomeScreen.kt` — imports `DemoChecklist` from `DemoData.kt`; updated composable signatures.
4. Create `ChecklistDetailScreen.kt` — imports `DemoTask`, `DemoChecklist` from `DemoData.kt`.
5. Create `AppNavigation.kt` — imports all of the above; defines `NavHost` and wires everything together.
6. Refactor `MainActivity.kt` last — remove old declarations, keep `MainActivity` + `EzListApp`, call `AppNavigation`.

This order ensures all referenced symbols exist before they are used, allowing the project to build at each step.
