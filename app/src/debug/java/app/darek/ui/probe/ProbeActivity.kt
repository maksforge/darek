package app.darek.ui.probe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.darek.R
import app.darek.theme.AppTheme
import app.darek.theme.Spacing
import app.darek.ui.common.DetailScaffold

/** The recogniser probe, opened from Settings' debug section (phase 1, section 1). */
class ProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AppTheme {
                ProbeScreen(onBack = ::finish)
            }
        }
    }
}

@Composable
private fun ProbeScreen(
    onBack: () -> Unit,
    viewModel: ProbeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val ask =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.start()
        }
    val idle = state.phase == ProbePhase.IDLE

    val view = LocalView.current
    DisposableEffect(idle) {
        view.keepScreenOn = !idle
        onDispose { view.keepScreenOn = false }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.stop() }

    DetailScaffold(title = stringResource(R.string.debug_probe), onBack = onBack) { modifier ->
        Column(
            modifier = modifier.fillMaxSize().padding(horizontal = Spacing.base),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
            ) {
                Button(onClick = {
                    when {
                        !idle -> viewModel.stop()
                        context.hasMicrophone() -> viewModel.start()
                        else -> ask.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }) { Text(stringResource(if (idle) R.string.debug_probe_start else R.string.debug_probe_stop)) }
                Switch(checked = state.free, enabled = idle, onCheckedChange = viewModel::setFree)
                Text(stringResource(R.string.debug_probe_free))
            }
            Text(
                when (state.phase) {
                    ProbePhase.IDLE -> state.error.orEmpty()
                    ProbePhase.PREPARING -> stringResource(R.string.debug_probe_preparing)
                    ProbePhase.LISTENING -> stringResource(R.string.debug_probe_partial, state.partial)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.hair)) {
                items(state.finals) { line ->
                    Text(
                        "${line.command?.name ?: "—"}  ${line.tokens}",
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (line.command !=
                                null
                            ) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                    )
                }
            }
        }
    }
}

private fun android.content.Context.hasMicrophone(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
