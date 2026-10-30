package com.ezlist.app

import kotlinx.serialization.Serializable

@Serializable
data class DemoTask(
    val id: Int,
    val name: String,
    val completed: Boolean
)

@Serializable
data class DemoChecklist(
    val id: Int,
    val name: String,
    val description: String,
    val mode: String,
    val tasks: List<DemoTask>
)

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
