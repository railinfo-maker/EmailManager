package com.emailmanager.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emailmanager.app.EmailViewModel
import com.emailmanager.app.data.EmailStatus
import com.emailmanager.app.util.EmailParser

private const val PREVIEW_LIMIT = 50
private const val INVALID_LIMIT = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(vm: EmailViewModel, snackbar: SnackbarHostState, onDone: () -> Unit) {
    val all by vm.allEmails.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    var text by rememberSaveable { mutableStateOf("") }
    var statusName by rememberSaveable { mutableStateOf(EmailStatus.UNUSED.name) }
    val status = EmailStatus.valueOf(statusName)

    val existing = remember(all) { all.map { it.email }.toHashSet() }
    val parsed = remember(text) { EmailParser.parse(text) }
    val newOnes = remember(parsed, existing) { parsed.valid.filter { it !in existing } }
    val alreadySaved = parsed.valid.size - newOnes.size

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Import emails", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 10.dp) {
                Button(
                    onClick = {
                        vm.importEmails(newOnes, status)
                        onDone()
                    },
                    enabled = newOnes.isNotEmpty(),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(12.dp)
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        if (newOnes.isEmpty()) "Nothing to import"
                        else "Import ${newOnes.size} email${if (newOnes.size == 1) "" else "s"}"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Paste emails (one per line)") },
                minLines = 5,
                maxLines = 8
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        clipboard.getText()?.text?.let { pasted ->
                            text = if (text.isBlank()) pasted else text.trimEnd() + "\n" + pasted
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text("Paste clipboard") }
                OutlinedButton(
                    onClick = { text = "" },
                    enabled = text.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text("Clear") }
            }

            Text("Status for imported emails", style = MaterialTheme.typography.labelLarge)
            StatusChips(selected = status, onSelected = { statusName = it.name })

            // ---- preview ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (text.isBlank()) {
                        Text(
                            "Paste your emails above to see what will be imported.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("Ready to import: ${newOnes.size}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text("Already saved (skipped): $alreadySaved")
                        Text("Duplicates in pasted text (removed): ${parsed.duplicatesRemoved}")
                        Text(
                            "Invalid (skipped): ${parsed.invalid.size}",
                            color = if (parsed.invalid.isEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                        )
                        if (newOnes.isNotEmpty()) {
                            Text(
                                "To be imported" + if (newOnes.size > PREVIEW_LIMIT) " (first $PREVIEW_LIMIT)" else "",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            newOnes.take(PREVIEW_LIMIT).forEach {
                                Text(it, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (parsed.invalid.isNotEmpty()) {
                            Text(
                                "Invalid entries" + if (parsed.invalid.size > INVALID_LIMIT) " (first $INVALID_LIMIT)" else "",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            parsed.invalid.take(INVALID_LIMIT).forEach {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
