# Requirements Document

## Introduction

This feature adds a Checklist Detail Screen to the EzList Android app. When a user taps a checklist card on the Home Screen, they are taken to a dedicated screen that displays the checklist's tasks and allows them to mark tasks as complete or incomplete. The feature also introduces Sequential mode enforcement (an "out of sequence" warning dialog when a user attempts to complete a later task while earlier tasks remain incomplete) and Flexible mode (unrestricted task toggling). The current monolithic `MainActivity.kt` is split into purpose-specific files. All state is in-memory only — no persistence, no database, no backend.

---

## Glossary

- **App**: The EzList Android application.
- **Home_Screen**: The main screen listing all checklists as cards.
- **Detail_Screen**: The Checklist Detail Screen showing tasks for a single checklist.
- **ChecklistCard**: A card composable on the Home Screen representing one checklist.
- **DemoChecklist**: The in-memory data model for a checklist, containing an id, name, description, mode, and list of tasks.
- **DemoTask**: The in-memory data model for a single task, containing an id, name, and completed flag.
- **Task_List**: The ordered list of `DemoTask` items within a `DemoChecklist`.
- **Progress**: The ratio of completed tasks to total tasks in a checklist.
- **Sequential_Mode**: A checklist mode where tasks are meant to be completed in order.
- **Flexible_Mode**: A checklist mode where tasks may be completed in any order.
- **Current_Step**: The first incomplete task in a Sequential_Mode checklist.
- **Sequence_Violation**: An attempt to mark a task complete when it is not the Current_Step in a Sequential_Mode checklist.
- **Out_of_Sequence_Dialog**: A warning dialog shown on a Sequence_Violation.
- **NavController**: The Jetpack Navigation Compose controller managing screen transitions.
- **NavHost**: The Jetpack Navigation Compose host defining the app's navigation graph.
- **Demo_Data**: The hardcoded in-memory list of `DemoChecklist` objects used during this milestone.

---

## Requirements

### Requirement 1: Expanded In-Memory Data Model

**User Story:** As a developer, I want a richer in-memory data model, so that checklists carry their full task data and the app no longer relies on hardcoded progress counts.

#### Acceptance Criteria

1. THE App SHALL define a `DemoTask` data class with fields: `id: Int`, `name: String`, and `completed: Boolean`.
2. THE App SHALL define a `DemoChecklist` data class with fields: `id: Int`, `name: String`, `description: String`, `mode: String` (valid values: `"Sequential"` or `"Flexible"`), and `tasks: List<DemoTask>`.
3. THE App SHALL store the `DemoTask` and `DemoChecklist` definitions in a file named `DemoData.kt` within the `com.ezlist.app` package.
4. THE App SHALL declare a top-level `val demoData: List<DemoChecklist>` in `DemoData.kt` containing exactly the following three checklists with these tasks and initial completion states:
   - **"Morning Routine"** (Sequential, id=1): Wake up (✓), Drink water (✓), Take a shower (✗), Get dressed (✗), Leave home (✗)
   - **"University Assignment"** (Flexible, id=2): Find references (✓), Write introduction (✗), Create slides (✓), Proofread (✗)
   - **"Project Workflow"** (Sequential, id=3): Define scope (✓), Create wireframes (✓), Set up project (✗), Build login screen (✗), Build home screen (✗), Add navigation (✗), Write tests (✗), Prepare release (✗)
5. THE App SHALL NOT use Room, SQLite, Firebase, or any database library for this data.

---

### Requirement 2: Progress Derived from Task List

**User Story:** As a user, I want progress indicators to reflect actual task completion, so that I always see an accurate count and progress bar.

#### Acceptance Criteria

1. WHEN the Home_Screen renders a ChecklistCard for a given `DemoChecklist`, THE App SHALL calculate the completed count as the number of items in that checklist's `tasks` list where `completed == true`.
2. WHEN the Home_Screen renders a ChecklistCard for a given `DemoChecklist`, THE App SHALL calculate the total count as `tasks.size` for that checklist.
3. THE App SHALL NOT use hardcoded integer literals for completed or total counts in any composable in `HomeScreen.kt` or `ChecklistDetailScreen.kt`.
4. WHEN a task's `completed` state changes in the in-memory state held by `AppNavigation.kt`, THE Home_Screen SHALL reflect the updated Progress on the corresponding ChecklistCard within the same recomposition cycle — no navigation or screen reload is required.
5. IF a checklist's `tasks` list is empty, THEN THE App SHALL display a completed count of 0, a total of 0, and a progress bar with zero fill on both the Home_Screen and Detail_Screen.

---

### Requirement 3: Navigation from Home Screen to Detail Screen

**User Story:** As a user, I want to tap a checklist card and open its detail screen, so that I can see and interact with the tasks.

#### Acceptance Criteria

1. THE App SHALL use Jetpack Navigation Compose (`androidx.navigation:navigation-compose`) as the sole navigation mechanism.
2. THE App SHALL define exactly one `NavHost` in a file named `AppNavigation.kt` within the `com.ezlist.app` package.
3. WHEN the user taps a `ChecklistCard` on the Home_Screen, THE App SHALL register the tap via an `onClick` lambda and THE NavController SHALL navigate to the Detail_Screen for that checklist.
4. WHEN the NavController navigates to the Detail_Screen, THE App SHALL pass the selected checklist's `id: Int` as a named navigation argument in the route path (e.g., `"detail/{checklistId}"`).
5. WHEN the Detail_Screen composable is entered, THE App SHALL retrieve the `checklistId` integer argument from the `NavBackStackEntry` and look up the matching `DemoChecklist` from `demoData`.
6. IF no checklist in `demoData` matches the received `checklistId`, or IF the `checklistId` argument is absent or non-integer, THEN THE NavController SHALL call `popBackStack()` before any Detail_Screen content is rendered, returning the user to the Home_Screen.

---

### Requirement 4: Back Navigation on Detail Screen

**User Story:** As a user, I want a back button on the detail screen, so that I can return to the Home Screen.

#### Acceptance Criteria

1. THE Detail_Screen SHALL display a back navigation control (arrow icon) in the TopAppBar navigation icon slot at the top-left of the screen.
2. WHEN the user taps the back navigation control, THE NavController SHALL call `popBackStack()`, removing the Detail_Screen entry from the back stack and returning to the Home_Screen.
3. WHEN the user triggers the Android system back action (hardware button or gesture) while on the Detail_Screen, THE NavController SHALL call `popBackStack()`, removing the Detail_Screen entry from the back stack and returning to the Home_Screen.
4. AFTER back navigation completes, THE back stack SHALL NOT contain the Detail_Screen entry, so that pressing back again from the Home_Screen does not return to the Detail_Screen.

---

### Requirement 5: Checklist Detail Screen Display

**User Story:** As a user, I want to see a checklist's full details on the detail screen, so that I can review all tasks and overall progress at a glance.

#### Acceptance Criteria

1. THE Detail_Screen SHALL display the checklist's `name` using a font size of at least 20sp and `fontWeight = FontWeight.Bold` at the top of the screen.
2. THE Detail_Screen SHALL display the checklist's `mode` as a text label showing either "Sequential" or "Flexible" in a clearly visible position below the name.
3. THE Detail_Screen SHALL display a text string in the format `"X / Y completed"` where X is `tasks.count { it.completed }` and Y is `tasks.size`, both computed at render time from the current task state.
4. THE Detail_Screen SHALL display a progress bar whose fill ratio equals `completedCount.toFloat() / totalCount.toFloat()`; IF `totalCount == 0`, THEN the fill ratio SHALL be `0f`.
5. THE Detail_Screen SHALL display the `Task_List` as a scrollable list where each row is prefixed with its 1-based position number.
6. IF a task's `completed` field is `true`, THEN THE Detail_Screen SHALL render that task row with a "✓" indicator.
7. IF a task's `completed` field is `false`, THEN THE Detail_Screen SHALL render that task row with a "○" indicator.
8. THE Detail_Screen SHALL render the name text of every completed task with a strikethrough decoration (`TextDecoration.LineThrough`) to visually distinguish it from incomplete tasks.

---

### Requirement 6: Task Toggle in Flexible Mode

**User Story:** As a user working on a Flexible checklist, I want to tap any task to toggle its completion state freely, so that I can complete tasks in any order that suits me.

#### Acceptance Criteria

1. WHILE the Detail_Screen displays a checklist whose `mode` is `"Flexible"`, THE App SHALL process a tap on any task row and toggle its `completed` state.
2. WHEN the user taps an incomplete task (`completed == false`) in Flexible_Mode, THE App SHALL set that task's `completed` field to `true`.
3. WHEN the user taps a completed task (`completed == true`) in Flexible_Mode, THE App SHALL set that task's `completed` field to `false`.
4. WHEN a task's `completed` state is toggled in Flexible_Mode, THE Detail_Screen SHALL update the progress bar, the "X / Y completed" text, and the task row indicator (✓ / ○) within 300 ms, without navigation or reload.
5. THE Detail_Screen SHALL NOT show the Out_of_Sequence_Dialog for any tap in Flexible_Mode.

---

### Requirement 7: Task Completion in Sequential Mode

**User Story:** As a user working on a Sequential checklist, I want to be guided through tasks in order, so that I complete prerequisites before moving on.

#### Acceptance Criteria

1. WHILE the Detail_Screen displays a checklist whose `mode` is `"Sequential"`, THE App SHALL define the Current_Step as `tasks.firstOrNull { !it.completed }`; IF all tasks are completed, THEN there is no Current_Step.
2. WHEN the user taps the Current_Step task, THE App SHALL set that task's `completed` field to `true` without showing any dialog.
3. WHEN the user taps a task that is not the Current_Step and that task's `completed` field is `false`, THE App SHALL show the Out_of_Sequence_Dialog.
4. WHEN the user taps a completed task in Sequential_Mode, THE App SHALL set that task's `completed` field to `false` without showing any dialog; THE App SHALL then re-evaluate the Current_Step based on the updated task list.
5. WHEN a task's `completed` state changes in Sequential_Mode, THE Detail_Screen SHALL update the progress bar and "X / Y completed" text within 100 ms without navigation or reload.

---

### Requirement 8: Out-of-Sequence Warning Dialog

**User Story:** As a user in Sequential mode, I want a clear warning when I try to skip ahead, so that I can decide whether to continue out of order or go back and follow the sequence.

#### Acceptance Criteria

1. WHEN a Sequence_Violation is detected, THE App SHALL display the Out_of_Sequence_Dialog with the title `"Out of Sequence"`.
2. THE Out_of_Sequence_Dialog SHALL include a body message that conveys all of the following: (a) the selected task is not the next task in the sequence, (b) there are earlier incomplete tasks, and (c) a question asking whether the user wants to continue anyway.
3. THE Out_of_Sequence_Dialog SHALL provide exactly two action buttons: `"Go Back"` and `"Continue Anyway"`.
4. WHEN the user taps `"Go Back"`, OR WHEN the user dismisses the dialog via the Android back gesture or by tapping outside it, THE App SHALL dismiss the Out_of_Sequence_Dialog and leave the tapped task's `completed` field unchanged (still `false`).
5. WHEN the user taps `"Continue Anyway"`, THE App SHALL dismiss the Out_of_Sequence_Dialog and set the tapped task's `completed` field to `true`.
6. THE Out_of_Sequence_Dialog SHALL be the only mechanism that presents a Sequence_Violation warning; THE App SHALL NOT permanently prevent the user from tapping or interacting with any task row.

---

### Requirement 9: Home Screen Preserved

**User Story:** As a user, I want the Home Screen to look and behave as it does today, so that the addition of the detail screen does not break my existing experience.

#### Acceptance Criteria

1. THE Home_Screen SHALL display each checklist card showing: the checklist name, the mode label ("Sequential" or "Flexible"), a progress bar, a "X / Y" completed count, and a percentage — all derived from the task list (not hardcoded).
2. WHEN the App launches cold, THE Home_Screen SHALL render with a dark color scheme (i.e., `darkColorScheme()` is active by default).
3. WHEN the user taps the theme toggle button, THE App SHALL switch to the opposite color scheme; tapping again SHALL switch back.
4. THE Home_Screen SHALL display a `FloatingActionButton` with a "+" label; tapping the FAB SHALL have no visible effect in this milestone (the onClick body is a no-op comment).

---

### Requirement 10: Code Structure — File Split

**User Story:** As a developer, I want the codebase split into focused files, so that each file has a single clear responsibility and the project is easy to navigate.

#### Acceptance Criteria

1. THE App SHALL contain a `MainActivity.kt` file in the `com.ezlist.app` package whose only top-level declarations are the `MainActivity` class (extending `ComponentActivity`), the `setContent` call, the `MaterialTheme` wrapper, and the call to the root navigation composable; it SHALL NOT contain any composable screen functions, data classes, or navigation routes.
2. THE App SHALL contain a `DemoData.kt` file in the `com.ezlist.app` package whose only top-level declarations are the `DemoTask` data class, the `DemoChecklist` data class, and the `demoData` list; it SHALL NOT contain any composable functions or navigation logic.
3. THE App SHALL contain an `AppNavigation.kt` file in the `com.ezlist.app` package whose only top-level declaration is a composable function that creates the `NavHost` and defines `composable { }` blocks binding route strings to screen composables; it SHALL NOT contain data class definitions or screen UI logic.
4. THE App SHALL contain a `HomeScreen.kt` file in the `com.ezlist.app` package whose only top-level declarations are the `EzListHome` composable and the `ChecklistCard` composable; it SHALL NOT contain data class definitions, navigation setup, or Detail_Screen UI.
5. THE App SHALL contain a `ChecklistDetailScreen.kt` file in the `com.ezlist.app` package whose only top-level declarations are the Detail_Screen composable and the `TaskRow` composable; it SHALL NOT contain data class definitions, navigation setup, or Home_Screen UI.
6. THE App SHALL NOT use `ViewModel`, `LiveData`, `StateFlow`, `Repository`, `Room`, `Hilt`, or any other Jetpack Architecture Component in this milestone.
7. THE App SHALL manage mutable state using `remember { mutableStateOf(...) }` or `mutableStateListOf`; all mutations SHALL be triggered exclusively through lambda callback parameters passed down the composable tree — no global mutable variables.

---

### Requirement 11: Navigation Compose Dependency

**User Story:** As a developer, I want Navigation Compose added as a dependency, so that screen-to-screen navigation compiles and runs correctly.

#### Acceptance Criteria

1. THE App SHALL declare `androidx.navigation:navigation-compose` as an `implementation` dependency in `app/build.gradle.kts`, referenced via the version catalog alias `libs.androidx.navigation.compose`.
2. THE App SHALL declare a `navigationCompose` version entry in `gradle/libs.versions.toml` set to `"2.9.0"`, and a library entry `androidx-navigation-compose` with `group = "androidx.navigation"`, `name = "navigation-compose"`, and `version.ref = "navigationCompose"`.
3. THE declared version `2.9.0` SHALL be the version managed by Compose BOM `2026.02.01`; no explicit `version.ref` override is needed in `app/build.gradle.kts` if the BOM is applied — the version catalog entry serves as the resolution anchor.
4. THE App SHALL NOT upgrade the existing `agp`, `kotlin`, `composeBom`, `activityCompose`, `lifecycleRuntimeKtx`, or `coreKtx` version entries in `gradle/libs.versions.toml`.

---

### Requirement 12: Compatibility and Build Constraints

**User Story:** As a developer, I want the app to build and run correctly within its existing constraints, so that no regressions are introduced.

#### Acceptance Criteria

1. THE App SHALL maintain `minSdk = 24`, `compileSdk { version = release(37) }`, and `targetSdk = 37` in `app/build.gradle.kts`.
2. THE App SHALL complete a cold start on an Android device running API 24 or higher, navigate from the Home_Screen to any Detail_Screen, interact with tasks, and navigate back — all without an uncaught exception, ANR, or crash.
3. THE App SHALL NOT declare `Room`, `SQLite`, `Firebase`, `authentication`, `cloud sync`, `WorkManager`, `AlarmManager`, or any backend-related library under any Gradle configuration (`implementation`, `api`, `compileOnly`, `kapt`, `ksp`, or `annotationProcessor`) in `app/build.gradle.kts`.
4. THE App SHALL NOT introduce more than one `NavHost` composable across all files.
5. THE App SHALL NOT implement nested navigation graphs (no `navigation { }` blocks within the single `NavHost`).
