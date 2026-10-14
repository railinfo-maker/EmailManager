package com.emailmanager.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emailmanager.app.data.EmailEntity
import com.emailmanager.app.data.EmailStatus
import com.emailmanager.app.util.EmailParser

/** Used for both "Add Email" (initial == null) and "Edit". */
@Composable
fun EmailEditDialog(
    initial: EmailEntity?,
    isDuplicate: (email: String, excludeId: Long?) -> Boolean,
    onDismiss: () -> Unit,
    onSave: (email: String, label: String, notes: String, status: EmailStatus) -> Unit
) {
    var email by rememberSaveable { mutableStateOf(initial?.email ?: "") }
    var label by rememberSaveable { mutableStateOf(initial?.label ?: "") }
    var notes by rememberSaveable { mutableStateOf(initial?.notes ?: "") }
    var statusName by rememberSaveable { mutableStateOf((initial?.status ?: EmailStatus.UNUSED).name) }
    val status = EmailStatus.valueOf(statusName)

    val normalized = email.trim().lowercase()
    val valid = EmailParser.isValid(normalized)
    val duplicate = valid && isDuplicate(normalized, initial?.id)
    val error = when {
        normalized.isEmpty() -> null
        !valid -> "Enter a valid email address"
        duplicate -> "This email is already saved"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Email" else "Edit Email") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email address") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = { if (error != null) Text(error) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label / nickname (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (never store passwords)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Status", style = MaterialTheme.typography.labelLarge)
                StatusChips(selected = status, onSelected = { statusName = it.name })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(normalized, label, notes, status) },
                enabled = valid && !duplicate
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun StatusChips(selected: EmailStatus, onSelected: (EmailStatus) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    ) {
        EmailStatus.entries.forEach { s ->
            FilterChip(
                selected = s == selected,
                onClick = { onSelected(s) },
                label = { Text(s.label, maxLines = 1) }
            )
        }
    }
}

@Composable
fun ConfirmDeleteDialog(email: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this email?") },
        text = { Text("$email will be removed from this phone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ConfirmDeleteAllDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ALL data?") },
        text = {
            Text(
                "This permanently deletes all $count saved emails, labels and notes from this phone. " +
                    "It cannot be undone.\n\nTip: use \"Export backup file\" first if you may need them again."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete everything", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
