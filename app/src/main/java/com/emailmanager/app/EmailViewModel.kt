package com.emailmanager.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.emailmanager.app.data.AppDatabase
import com.emailmanager.app.data.EmailEntity
import com.emailmanager.app.data.EmailStatus
import com.emailmanager.app.util.BackupManager
import com.emailmanager.app.util.EmailParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOrder(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    AZ("A to Z"),
    ZA("Z to A"),
    STATUS("By status")
}

class EmailViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).emailDao()
    private val prefs = app.getSharedPreferences("ui_state", Context.MODE_PRIVATE)

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** Every saved entry, straight from the local database. */
    val allEmails: StateFlow<List<EmailEntity>> = dao.observeAll()
        .onEach { _loaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _sort = MutableStateFlow(SortOrder.NEWEST)
    val sort: StateFlow<SortOrder> = _sort.asStateFlow()

    private val _statusFilter = MutableStateFlow<EmailStatus?>(null)
    val statusFilter: StateFlow<EmailStatus?> = _statusFilter.asStateFlow()

    /** Id of the entry used by Prev / Next / Copy. -1 means none. Remembered across restarts. */
    private val _selectedId = MutableStateFlow(prefs.getLong(KEY_SELECTED, -1L))
    val selectedId: StateFlow<Long> = _selectedId.asStateFlow()

    /** The list after search, status filter and sorting. Next / Previous walk this list. */
    val visible: StateFlow<List<EmailEntity>> =
        combine(allEmails, _query, _sort, _statusFilter) { list, q, sort, filter ->
            val needle = q.trim().lowercase()
            val filtered = list.filter { e ->
                (filter == null || e.status == filter) &&
                    (needle.isEmpty() ||
                        e.email.contains(needle) ||
                        e.label.lowercase().contains(needle) ||
                        e.notes.lowercase().contains(needle))
            }
            when (sort) {
                SortOrder.NEWEST -> filtered.sortedWith(compareByDescending<EmailEntity> { it.createdAt }.thenByDescending { it.id })
                SortOrder.OLDEST -> filtered.sortedWith(compareBy<EmailEntity> { it.createdAt }.thenBy { it.id })
                SortOrder.AZ -> filtered.sortedBy { it.email }
                SortOrder.ZA -> filtered.sortedByDescending { it.email }
                SortOrder.STATUS -> filtered.sortedWith(compareBy<EmailEntity> { it.status.ordinal }.thenBy { it.email })
            }
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun showMessage(text: String) {
        _messages.tryEmit(text)
    }

    // ---- search / sort / filter -------------------------------------------------------------

    fun setQuery(q: String) { _query.value = q }
    fun setSort(s: SortOrder) { _sort.value = s }
    fun setStatusFilter(s: EmailStatus?) { _statusFilter.value = s }

    // ---- selection and fast navigation ------------------------------------------------------

    fun select(id: Long) {
        _selectedId.value = id
        prefs.edit().putLong(KEY_SELECTED, id).apply()
    }

    fun next() {
        val list = visible.value
        if (list.isEmpty()) return
        val idx = list.indexOfFirst { it.id == _selectedId.value }
        select(list[(idx + 1) % list.size].id) // idx = -1 -> first item; wraps around at the end
    }

    fun previous() {
        val list = visible.value
        if (list.isEmpty()) return
        val idx = list.indexOfFirst { it.id == _selectedId.value }
        val target = if (idx < 0) list.lastIndex else (idx - 1 + list.size) % list.size
        select(list[target].id)
    }

    fun onCopied(entry: EmailEntity) {
        select(entry.id)
        showMessage("Copied: ${entry.email}")
    }

    // ---- create / edit / delete -------------------------------------------------------------

    fun isDuplicate(email: String, excludeId: Long?): Boolean {
        val n = email.trim().lowercase()
        return allEmails.value.any { it.email == n && it.id != excludeId }
    }

    fun save(existing: EmailEntity?, email: String, label: String, notes: String, status: EmailStatus) {
        val n = email.trim().lowercase()
        if (!EmailParser.isValid(n)) { showMessage("Invalid email address"); return }
        if (isDuplicate(n, existing?.id)) { showMessage("That email is already saved"); return }
        viewModelScope.launch {
            try {
                if (existing == null) {
                    val ids = dao.insertAll(listOf(EmailEntity(email = n, label = label.trim(), notes = notes.trim(), status = status)))
                    if (ids.firstOrNull()?.let { it > 0 } == true) select(ids.first())
                    showMessage("Email added")
                } else {
                    dao.update(existing.copy(email = n, label = label.trim(), notes = notes.trim(), status = status))
                    showMessage("Changes saved")
                }
            } catch (e: Exception) {
                showMessage("Could not save: ${e.message}")
            }
        }
    }

    fun delete(entry: EmailEntity) {
        viewModelScope.launch {
            dao.delete(entry)
            if (_selectedId.value == entry.id) select(-1L)
            showMessage("Deleted ${entry.email}")
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            dao.deleteAll()
            select(-1L)
            showMessage("All data deleted")
        }
    }

    // ---- bulk import from pasted text -------------------------------------------------------

    fun importEmails(emails: List<String>, status: EmailStatus) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // now - index keeps the first pasted line at the top of "Newest first".
            val items = emails.mapIndexed { i, e -> EmailEntity(email = e, status = status, createdAt = now - i) }
            val added = dao.insertAll(items).count { it != -1L }
            showMessage("Imported $added email${if (added == 1) "" else "s"}")
        }
    }

    // ---- backup file (user-selected via the system file picker) -----------------------------

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            try {
                val all = dao.getAll()
                withContext(Dispatchers.IO) {
                    val out = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                        ?: throw IllegalStateException("Cannot open the selected file")
                    out.use { it.write(BackupManager.toJson(all).toByteArray(Charsets.UTF_8)) }
                }
                showMessage("Exported ${all.size} entries")
            } catch (e: Exception) {
                showMessage("Export failed: ${e.message}")
            }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: throw IllegalStateException("Cannot open the selected file")
                }
                val parsed = BackupManager.parse(text)
                val items = parsed.items.map {
                    EmailEntity(email = it.email, label = it.label, notes = it.notes, status = it.status, createdAt = it.createdAt)
                }
                val added = dao.insertAll(items).count { it != -1L }
                val dupes = items.size - added
                showMessage(
                    "Imported $added new" +
                        (if (dupes > 0) ", $dupes already saved" else "") +
                        (if (parsed.skippedInvalid > 0) ", ${parsed.skippedInvalid} invalid skipped" else "")
                )
            } catch (e: Exception) {
                showMessage("Import failed: ${e.message}")
            }
        }
    }

    private companion object {
        const val KEY_SELECTED = "selected_id"
    }
}
