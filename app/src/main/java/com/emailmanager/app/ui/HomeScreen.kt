package com.emailmanager.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emailmanager.app.EmailViewModel
import com.emailmanager.app.SortOrder
import com.emailmanager.app.data.EmailEntity
import com.emailmanager.app.data.EmailStatus
import com.emailmanager.app.util.LaunchResult
import com.emailmanager.app.util.Launchers

/** Number of fixed items above the email cards in the LazyColumn (used for scrolling). */
private const val HEADER_ITEMS = 6

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: EmailViewModel, snackbar: SnackbarHostState, onOpenImport: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val all by vm.allEmails.collectAsStateWithLifecycle()
    val visible by vm.visible.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val filter by vm.statusFilter.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<EmailEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<EmailEntity?>(null) }
    var showDeleteAll by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    var navTick by remember { mutableIntStateOf(0) }

    // After Next / Previous, bring the newly selected card into view if it is off-screen.
    LaunchedEffect(navTick) {
        if (navTick > 0) {
            val id = vm.selectedId.value
            val index = vm.visible.value.indexOfFirst { it.id == id }
            if (index >= 0) {
                val target = index + HEADER_ITEMS
                if (listState.layoutInfo.visibleItemsInfo.none { it.index == target }) {
                    listState.animateScrollToItem(target)
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportTo(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importFrom(uri) }

    fun copyEntry(entry: EmailEntity) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("Email", entry.email))
        vm.onCopied(entry)
    }

    val current = visible.firstOrNull { it.id == selectedId }
    val position = visible.indexOfFirst { it.id == selectedId } + 1
    val imeVisible = WindowInsets.isImeVisible

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Email Manager", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Export backup file") },
                                onClick = { menuOpen = false; exportLauncher.launch("email-manager-backup.json") }
                            )
                            DropdownMenuItem(
                                text = { Text("Import backup file") },
                                onClick = { menuOpen = false; importLauncher.launch(arrayOf("*/*")) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete all data", color = MaterialTheme.colorScheme.error) },
                                onClick = { menuOpen = false; showDeleteAll = true }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!imeVisible) {
                NavigatorBar(
                    current = current,
                    position = position,
                    total = visible.size,
                    onPrevious = { vm.previous(); navTick++ },
                    onNext = { vm.next(); navTick++ },
                    onCopy = { current?.let { copyEntry(it) } }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Total saved
            item {
                StatsCard(
                    total = all.size,
                    unused = all.count { it.status == EmailStatus.UNUSED },
                    inUse = all.count { it.status == EmailStatus.IN_USE },
                    archived = all.count { it.status == EmailStatus.ARCHIVED }
                )
            }
            // 2. Quick access
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickButton("Gmail", Icons.Filled.Email, Modifier.weight(1f)) {
                        if (Launchers.openGmail(context) == LaunchResult.FAILED) {
                            vm.showMessage("Could not open Gmail")
                        }
                    }
                    QuickButton("Google Maps", Icons.Filled.Place, Modifier.weight(1f)) {
                        when (Launchers.openMaps(context)) {
                            LaunchResult.APP -> Unit
                            LaunchResult.BROWSER -> vm.showMessage("Google Maps app not found. Opened in browser.")
                            LaunchResult.FAILED -> vm.showMessage("No app or browser available to open Google Maps")
                        }
                    }
                    QuickButton("Google Account", Icons.Filled.AccountCircle, Modifier.weight(1f)) {
                        if (Launchers.openAccounts(context) == LaunchResult.FAILED) {
                            vm.showMessage("No browser available to open Google Account")
                        }
                    }
                }
            }
            // 3. Add / Import
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showAdd = true },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add Email")
                    }
                    OutlinedButton(
                        onClick = onOpenImport,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) { Text("Import list") }
                }
            }
            // 4. Search
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search email, label or notes") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { vm.setQuery("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
            }
            // 5. Status filter
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(selected = filter == null, onClick = { vm.setStatusFilter(null) }, label = { Text("All") })
                    EmailStatus.entries.forEach { s ->
                        FilterChip(
                            selected = filter == s,
                            onClick = { vm.setStatusFilter(if (filter == s) null else s) },
                            label = { Text(s.label) }
                        )
                    }
                }
            }
            // 6. Count + sort
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${visible.size} shown",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box {
                        TextButton(onClick = { sortMenuOpen = true }) { Text("Sort: ${sort.label}") }
                        DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            SortOrder.entries.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.label) },
                                    onClick = { vm.setSort(s); sortMenuOpen = false }
                                )
                            }
                        }
                    }
                }
            }

            // Email cards
            items(visible, key = { it.id }) { entry ->
                EmailCard(
                    entry = entry,
                    selected = entry.id == selectedId,
                    onSelect = { vm.select(entry.id) },
                    onCopy = { copyEntry(entry) },
                    onEdit = { editTarget = entry },
                    onDelete = { deleteTarget = entry }
                )
            }

            // Empty states
            if (loaded && visible.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = if (all.isEmpty())
                                "No emails saved yet.\nTap \"Add Email\" or \"Import list\" to get started."
                            else
                                "No emails match your search or filter.",
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // ---- dialogs ----------------------------------------------------------------------------

    if (showAdd || editTarget != null) {
        EmailEditDialog(
            initial = editTarget,
            isDuplicate = vm::isDuplicate,
            onDismiss = { showAdd = false; editTarget = null },
            onSave = { email, label, notes, status ->
                vm.save(editTarget, email, label, notes, status)
                showAdd = false
                editTarget = null
            }
        )
    }
    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            email = target.email,
            onConfirm = { vm.delete(target); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
    if (showDeleteAll) {
        ConfirmDeleteAllDialog(
            count = all.size,
            onConfirm = { vm.deleteAll(); showDeleteAll = false },
            onDismiss = { showDeleteAll = false }
        )
    }
}

// ---- pieces ---------------------------------------------------------------------------------

@Composable
private fun StatsCard(total: Int, unused: Int, inUse: Int, archived: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Total saved email IDs", style = MaterialTheme.typography.labelLarge)
                Text(
                    "$total",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Unused: $unused", style = MaterialTheme.typography.bodyMedium)
                Text("In Use: $inUse", style = MaterialTheme.typography.bodyMedium)
                Text("Archived: $archived", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun QuickButton(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(68.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmailCard(
    entry: EmailEntity,
    selected: Boolean,
    onSelect: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (selected) cs.primaryContainer else cs.surface),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) cs.primary else cs.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    entry.email,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                StatusBadge(entry.status)
            }
            if (entry.label.isNotBlank()) {
                Text(entry.label, style = MaterialTheme.typography.bodyMedium, color = cs.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (entry.notes.isNotBlank()) {
                Text(entry.notes, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onCopy, modifier = Modifier.weight(1f).height(48.dp)) { Text("Copy Email") }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit ${entry.email}") }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${entry.email}", tint = cs.error)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: EmailStatus) {
    val (bg, fg) = when (status) {
        EmailStatus.UNUSED -> Color(0xFFE3EEFF) to Color(0xFF0B3C8C)
        EmailStatus.IN_USE -> Color(0xFFDDF3DF) to Color(0xFF1B5E20)
        EmailStatus.ARCHIVED -> Color(0xFFE6E8EC) to Color(0xFF4A5565)
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            status.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = fg
        )
    }
}

/** Always visible at the bottom: big Prev / Copy / Next buttons reachable with one thumb. */
@Composable
private fun NavigatorBar(
    current: EmailEntity?,
    position: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCopy: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp, shadowElevation = 10.dp) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column {
                Text(
                    if (current != null) "Current  \u2022  $position of $total" else "Current email",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    current?.email ?: "None selected. Tap an entry or press Next.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (current != null && current.label.isNotBlank()) {
                    Text(
                        current.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f).height(56.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Prev", maxLines = 1)
                }
                Button(
                    onClick = onCopy,
                    enabled = current != null,
                    modifier = Modifier.weight(1.5f).height(56.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) { Text("Copy Email", maxLines = 1) }
                OutlinedButton(
                    onClick = onNext,
                    modifier = Modifier.weight(1f).height(56.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Next", maxLines = 1)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
