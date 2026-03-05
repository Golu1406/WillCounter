package com.example.myapplication

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.core.animateFloatAsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TimerApp() }
    }
}

@Composable
fun TimerApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("timerData", Context.MODE_PRIVATE) }

    var seconds by remember { mutableIntStateOf(0) }
    var days by remember { mutableIntStateOf(prefs.getInt("days", 0)) }
    var running by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(prefs.getStringSet("history", emptySet())!!.toList()) }

    // Restore elapsed time if app was closed
    LaunchedEffect(Unit) {
        val startTime = prefs.getLong("startTime", 0L)
        if (startTime > 0L) {
            val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            seconds = elapsed % 86400
            days += elapsed / 86400
            running = true
        }
    }

    // Save state whenever it changes
    LaunchedEffect(seconds, days, history, running) {
        prefs.edit().apply {
            putInt("days", days)
            putStringSet("history", history.toSet())
            if (running) putLong("startTime", System.currentTimeMillis() - seconds * 1000L)
            else putLong("startTime", 0L)
            apply()
        }
    }

    // Animate progress smoothly
    val targetProgress = (seconds.toFloat() / 86400f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = targetProgress)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF222327) // dark background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(200.dp)) // pushes circle down

                // Circle with progress + text inside
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable(
                            indication = null, // remove ripple/black overlay
                            interactionSource = remember { MutableInteractionSource() }
                        ) { running = !running }, // toggle start/stop
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.size(220.dp),
                        strokeWidth = 12.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.US, "%02d:%02d:%02d",
                                seconds / 3600, (seconds % 3600) / 60, seconds % 60),
                            fontSize = 28.sp,
                            color = Color.White
                        )
                        Text(
                            text = "$days Day${if (days != 1) "s" else ""}",
                            fontSize = 18.sp,
                            color = Color.White
                        )

                        // Show status text when stopped
                        if (!running) {
                            Text(
                                text = "Stopped",
                                fontSize = 16.sp,
                                color = Color.Red
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Main Start/Restart button with fixed width
                Button(
                    onClick = {
                        if (!running) {
                            running = true
                            prefs.edit().putLong("startTime", System.currentTimeMillis()).apply()
                        } else {
                            val record = "Session: %02d:%02d:%02d | Days: %d | %s".format(
                                seconds / 3600, (seconds % 3600) / 60, seconds % 60, days,
                                SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(Date())
                            )
                            history = listOf(record) + history
                            seconds = 0
                            prefs.edit().putLong("startTime", System.currentTimeMillis()).apply()
                        }
                    },
                    modifier = Modifier
                        .width(200.dp) // fixed width
                        .height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (running) "Restart" else "Start",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // History list centered
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(history) { item ->
                        Text(item, color = Color.White, fontSize = 16.sp)
                    }
                }
            }

            // 🗑️ Delete button at bottom-right corner with tomato color circle
            FloatingActionButton(
                onClick = { history = emptyList() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = Color(0xFFFF6347), // Tomato color
                shape = CircleShape
            ) {
                Text("🗑️", fontSize = 22.sp)
            }
        }
    }

    // Continuous timer loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (running) {
                seconds++
                if (seconds >= 86400) {
                    seconds = 0
                    days++
                }
            }
        }
    }
}