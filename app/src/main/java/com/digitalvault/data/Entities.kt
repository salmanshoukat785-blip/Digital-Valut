package com.digitalvault.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Hassas maloomat (title, username, email, password, url, notes) sirf `payload` mein hain,
 * jo AES-256-GCM se encrypt hota hai. Baqi columns sirf filter/sort aur reminder ke liye hain
 * aur koi password ya naam zahir nahi karte.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slug: String?,          // default categories ki pehchan (nil = custom)
    val name: String,
    val icon: String,
    val color: Long,            // ARGB
    val sortOrder: Int
)

@Entity(tableName = "accounts", indices = [Index("categoryId")])
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val lastUsedAt: Long? = null,
    val expiryAt: Long,
    val payload: ByteArray
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val payload: ByteArray
)

data class CategoryCount(val categoryId: Long, val total: Int)
