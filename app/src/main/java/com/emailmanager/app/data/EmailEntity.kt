package com.emailmanager.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class EmailStatus(val label: String) {
    UNUSED("Unused"),
    IN_USE("In Use"),
    ARCHIVED("Archived");

    companion object {
        fun fromName(name: String?): EmailStatus {
            val n = name?.trim().orEmpty()
            return entries.firstOrNull { it.name.equals(n, true) || it.label.equals(n, true) } ?: UNUSED
        }
    }
}

@Entity(tableName = "emails", indices = [Index(value = ["email"], unique = true)])
data class EmailEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val label: String = "",
    val notes: String = "",
    val status: EmailStatus = EmailStatus.UNUSED,
    val createdAt: Long = System.currentTimeMillis()
)
