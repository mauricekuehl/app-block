package com.mauricekuehl.appblock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mauricekuehl.appblock.data.InstalledAppsRepository
import com.mauricekuehl.appblock.data.ScheduleRepository
import com.mauricekuehl.appblock.ui.theme.AppBlockTheme

class BlockedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showBlockScreen(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showBlockScreen(intent)
    }

    private fun showBlockScreen(intent: Intent) {
        val blockedPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val appLabel = InstalledAppsRepository(this).labelFor(blockedPackage)
        val schedule = ScheduleRepository(this).load()

        setContent {
            AppBlockTheme {
                BlockedScreen(
                    appLabel = appLabel,
                    endMinute = schedule.endMinute,
                    onGoHome = ::goHome,
                    onChangeSchedule = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                )
            }
        }
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "blocked_package_name"
    }
}

@Composable
private fun BlockedScreen(
    appLabel: String,
    endMinute: Int,
    onGoHome: () -> Unit,
    onChangeSchedule: () -> Unit,
) {
    BackHandler(onBack = onGoHome)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = "$appLabel is taking a break",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Your block window ends at ${formatTime(endMinute)}.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))
            Button(onClick = onGoHome) {
                Text("Go to home screen")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onChangeSchedule) {
                Text("Change schedule")
            }
        }
    }
}

internal fun formatTime(minuteOfDay: Int): String =
    "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
