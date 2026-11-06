package com.digitalvault.data

/** UI ke liye decrypt shuda shakal. */
data class Account(
    val id: Long = 0,
    val title: String,
    val username: String = "",
    val email: String = "",
    val password: String,
    val url: String = "",
    val notes: String = "",
    val categoryId: Long,
    val isFavorite: Boolean = false,
    val createdAt: Long = 0,
    val lastUsedAt: Long? = null,
    val expiryAt: Long = 0
)

data class SecureNote(
    val id: Long = 0,
    val title: String,
    val body: String,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

data class PasswordHealth(
    val weak: List<Long>,
    val reused: List<Long>,
    val expired: List<Long>,
    val expiringSoon: List<Long>   // agle 7 din
)
