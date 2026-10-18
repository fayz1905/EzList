package com.ezlist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EzListApp()
        }
    }
}


data class DemoChecklist(
    val name: String,
    val mode: String,
    val completed: Int,
    val total: Int
)


@Composable
fun EzListApp() {

    var darkMode by rememberSaveable {
        mutableStateOf(true)
    }

    val demoLists = listOf(
        DemoChecklist(
            name = "Morning Routine",
            mode = "Sequential",
            completed = 3,
            total = 5
        ),
        DemoChecklist(
            name = "University Assignment",
            mode = "Flexible",
            completed = 4,
            total = 6
        ),
        DemoChecklist(
            name = "Project Workflow",
            mode = "Sequential",
            completed = 2,
            total = 8
        )
    )

    val colorScheme =
        if (darkMode) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }

    MaterialTheme(
        colorScheme = colorScheme
    ) {

        Surface(
            modifier = Modifier.fillMaxSize()
        ) {

            EzListHome(
                darkMode = darkMode,
                onToggleTheme = {
                    darkMode = !darkMode
                },
                lists = demoLists
            )
        }
    }
}


@Composable
fun EzListHome(
    darkMode: Boolean,
    onToggleTheme: () -> Unit,
    lists: List<DemoChecklist>
) {

    Scaffold(

        floatingActionButton = {

            FloatingActionButton(
                onClick = {
                    // Nanti digunakan untuk membuat checklist baru
                }
            ) {
                Text(
                    text = "+",
                    fontSize = 24.sp
                )
            }
        }

    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        text = "EzList",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "Simple lists, smarter order.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                TextButton(
                    onClick = onToggleTheme
                ) {

                    Text(
                        text = if (darkMode) "☀" else "☾",
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Text(
                text = "MY CHECKLISTS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {

                items(lists) { checklist ->

                    ChecklistCard(
                        checklist = checklist
                    )
                }
            }
        }
    }
}


@Composable
fun ChecklistCard(
    checklist: DemoChecklist
) {

    val progress =
        if (checklist.total == 0) {
            0f
        } else {
            checklist.completed.toFloat() / checklist.total.toFloat()
        }

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        )
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Text(
                    text = checklist.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = checklist.mode,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(50)
                    )
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        )
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "${checklist.completed} / ${checklist.total}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}