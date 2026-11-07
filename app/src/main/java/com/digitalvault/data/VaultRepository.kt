package com.digitalvault.data

import com.digitalvault.security.Crypto
import com.digitalvault.security.KeyVault
import com.digitalvault.security.PasswordStrength
import com.digitalvault.security.Strength
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/**
 * Data tak rasai ka wahid darwaza. Har cheez Data Key se encrypt/decrypt hoti hai.
 * Note: observe* flows tab chalayein jab vault unlocked ho. Lock par ye khali list dete hain.
 */
class VaultRepository(private val vault: KeyVault, db: VaultDatabase) {

    private val categoryDao = db.categories()
    private val accountDao = db.accounts()
    private val noteDao = db.notes()

    // ---------------- Categories ----------------

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    fun observeCategoryCounts(): Flow<List<CategoryCount>> = accountDao.observeCounts()

    suspend fun seedDefaultCategories() {
        if (categoryDao.count() > 0) return
        categoryDao.insertAll(
            listOf(
                CategoryEntity(slug = "social", name = "Social Media", icon = "group", color = 0xFF1E88E5, sortOrder = 1),
                CategoryEntity(slug = "email", name = "Email", icon = "mail", color = 0xFFE53935, sortOrder = 2),
                CategoryEntity(slug = "banking", name = "Banking", icon = "bank", color = 0xFF43A047, sortOrder = 3),
                CategoryEntity(slug = "websites", name = "Websites", icon = "language", color = 0xFF8E24AA, sortOrder = 4),
                CategoryEntity(slug = "work", name = "Work", icon = "work", color = 0xFFFB8C00, sortOrder = 5),
                CategoryEntity(slug = "personal", name = "Personal", icon = "person", color = 0xFF00897B, sortOrder = 6),
                CategoryEntity(slug = "wifi", name = "Wi-Fi", icon = "wifi", color = 0xFF3949AB, sortOrder = 7),
                CategoryEntity(slug = "other", name = "Other", icon = "more", color = 0xFF757575, sortOrder = 8)
            )
        )
    }

    suspend fun addCategory(name: String, icon: String = "folder", color: Long = 0xFF546E7A): Long =
        categoryDao.insert(
            CategoryEntity(slug = null, name = name.trim(), icon = icon, color = color,
                sortOrder = categoryDao.maxOrder() + 1)
        )

    suspend fun renameCategory(id: Long, name: String) = categoryDao.rename(id, name.trim())

    /** "Other" delete nahi hoti. Delete par us ke accounts "Other" mein chale jate hain. */
    suspend fun deleteCategory(id: Long): Boolean {
        val cat = categoryDao.get(id) ?: return false
        if (cat.slug == "other") return false
        val other = categoryDao.bySlug("other") ?: return false
        accountDao.moveCategory(id, other.id)
        categoryDao.delete(id)
        return true
    }

    // ---------------- Accounts ----------------

    fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAll().map { list ->
            try {
                val key = vault.requireKey()
                list.mapNotNull { runCatching { toAccount(it, key) }.getOrNull() }
            } catch (e: IllegalStateException) {
                emptyList()
            }
        }.flowOn(Dispatchers.Default)

    /**
     * Naya ya mojooda account save. expiryAt na dein to naye account ya badle hue password par
     * aaj se 90 din ka reminder lagta hai.
     */
    suspend fun saveAccount(a: Account, expiryAt: Long? = null): Long {
        val now = System.currentTimeMillis()
        val key = vault.requireKey()
        val existing = if (a.id != 0L) accountDao.get(a.id) else null
        val old = existing?.let { runCatching { toAccount(it, key) }.getOrNull() }
        val expiry = expiryAt ?: when {
            old == null || old.password != a.password -> now + DEFAULT_EXPIRY_MS
            else -> existing?.expiryAt ?: (now + DEFAULT_EXPIRY_MS)
        }
        val entity = AccountEntity(
            id = existing?.id ?: 0, categoryId = a.categoryId, isFavorite = a.isFavorite,
            createdAt = existing?.createdAt ?: now, lastUsedAt = existing?.lastUsedAt,
            expiryAt = expiry, payload = encode(a, key)
        )
        return if (existing == null) accountDao.insert(entity) else { accountDao.update(entity); entity.id }
    }

    suspend fun deleteAccount(id: Long) = accountDao.delete(id)
    suspend fun setFavorite(id: Long, fav: Boolean) = accountDao.setFavorite(id, fav)
    /** Password ya username copy/dekhne par bulayein ("Recently Used" ke liye). */
    suspend fun markUsed(id: Long) = accountDao.setLastUsed(id, System.currentTimeMillis())

    // ---------------- Secure Notes ----------------

    fun observeNotes(): Flow<List<SecureNote>> =
        noteDao.observeAll().map { list ->
            try {
                val key = vault.requireKey()
                list.mapNotNull { runCatching { toNote(it, key) }.getOrNull() }
            } catch (e: IllegalStateException) {
                emptyList()
            }
        }.flowOn(Dispatchers.Default)

    suspend fun saveNote(n: SecureNote): Long {
        val now = System.currentTimeMillis()
        val key = vault.requireKey()
        val existing = if (n.id != 0L) noteDao.get(n.id) else null
        val payload = Crypto.encryptString(
            key, JSONObject().put("title", n.title).put("body", n.body).toString()
        )
        return if (existing == null) {
            noteDao.insert(NoteEntity(createdAt = now, updatedAt = now, payload = payload))
        } else {
            noteDao.update(existing.copy(updatedAt = now, payload = payload)); existing.id
        }
    }

    suspend fun deleteNote(id: Long) = noteDao.delete(id)

    // ---------------- Backup / import ----------------

    suspend fun categoryList(): List<CategoryEntity> = categoryDao.getAll()

    suspend fun snapshot(): VaultSnapshot {
        val key = vault.requireKey()
        return VaultSnapshot(
            categories = categoryDao.getAll(),
            accounts = accountDao.getAll().mapNotNull { runCatching { toAccount(it, key) }.getOrNull() },
            notes = noteDao.getAll().mapNotNull { runCatching { toNote(it, key) }.getOrNull() }
        )
    }

    /** Merge: jo cheez pehle se maujood ho (same naam, username, email, password) wo chhor di jati hai. */
    suspend fun importSnapshot(s: VaultSnapshot): ImportResult {
        val key = vault.requireKey()
        val now = System.currentTimeMillis()
        seedDefaultCategories()
        val cats = categoryDao.getAll().toMutableList()
        val idMap = HashMap<Long, Long>()
        for (c in s.categories) {
            val match = cats.firstOrNull {
                (c.slug != null && it.slug == c.slug) || it.name.equals(c.name, ignoreCase = true)
            }
            if (match != null) {
                idMap[c.id] = match.id
            } else {
                val newId = categoryDao.insert(c.copy(id = 0, slug = null))
                idMap[c.id] = newId
                cats.add(c.copy(id = newId, slug = null))
            }
        }
        val validIds = cats.map { it.id }.toSet()
        val otherId = cats.firstOrNull { it.slug == "other" }?.id ?: cats.firstOrNull()?.id ?: 0L

        fun sig(a: Account) =
            listOf(a.title.trim().lowercase(), a.username, a.email, a.password).joinToString("\u0000")
        val seen = accountDao.getAll()
            .mapNotNull { runCatching { toAccount(it, key) }.getOrNull() }
            .map { sig(it) }.toHashSet()
        var added = 0
        var skipped = 0
        for (a in s.accounts) {
            if (!seen.add(sig(a))) { skipped++; continue }
            val cid = idMap[a.categoryId] ?: if (a.categoryId in validIds) a.categoryId else otherId
            accountDao.insert(
                AccountEntity(
                    categoryId = cid, isFavorite = a.isFavorite,
                    createdAt = if (a.createdAt > 0) a.createdAt else now,
                    lastUsedAt = a.lastUsedAt,
                    expiryAt = if (a.expiryAt > 0) a.expiryAt else now + DEFAULT_EXPIRY_MS,
                    payload = encode(a, key)
                )
            )
            added++
        }

        fun nsig(n: SecureNote) = n.title.trim() + "\u0000" + n.body
        val seenNotes = noteDao.getAll()
            .mapNotNull { runCatching { toNote(it, key) }.getOrNull() }
            .map { nsig(it) }.toHashSet()
        var notesAdded = 0
        for (n in s.notes) {
            if (!seenNotes.add(nsig(n))) continue
            noteDao.insert(
                NoteEntity(
                    createdAt = if (n.createdAt > 0) n.createdAt else now,
                    updatedAt = if (n.updatedAt > 0) n.updatedAt else now,
                    payload = Crypto.encryptString(
                        key, JSONObject().put("title", n.title).put("body", n.body).toString()
                    )
                )
            )
            notesAdded++
        }
        return ImportResult(added, skipped, notesAdded)
    }

    // ---------------- Encrypt / decrypt ----------------

    private fun encode(a: Account, key: ByteArray): ByteArray = Crypto.encryptString(
        key,
        JSONObject()
            .put("title", a.title).put("username", a.username).put("email", a.email)
            .put("password", a.password).put("url", a.url).put("notes", a.notes)
            .toString()
    )

    private fun toAccount(e: AccountEntity, key: ByteArray): Account {
        val j = JSONObject(Crypto.decryptString(key, e.payload))
        return Account(
            id = e.id, title = j.optString("title"), username = j.optString("username"),
            email = j.optString("email"), password = j.optString("password"),
            url = j.optString("url"), notes = j.optString("notes"),
            categoryId = e.categoryId, isFavorite = e.isFavorite, createdAt = e.createdAt,
            lastUsedAt = e.lastUsedAt, expiryAt = e.expiryAt
        )
    }

    private fun toNote(e: NoteEntity, key: ByteArray): SecureNote {
        val j = JSONObject(Crypto.decryptString(key, e.payload))
        return SecureNote(e.id, j.optString("title"), j.optString("body"), e.createdAt, e.updatedAt)
    }

    companion object {
        const val DEFAULT_EXPIRY_MS = 90L * 24 * 60 * 60 * 1000
        private const val SOON_MS = 7L * 24 * 60 * 60 * 1000

        /** Search: title, username, email aur URL mein. */
        fun search(list: List<Account>, query: String): List<Account> {
            val q = query.trim()
            if (q.isEmpty()) return list
            return list.filter {
                it.title.contains(q, true) || it.username.contains(q, true) ||
                    it.email.contains(q, true) || it.url.contains(q, true)
            }
        }

        fun recentlyAdded(list: List<Account>, n: Int = 5) = list.sortedByDescending { it.createdAt }.take(n)
        fun recentlyUsed(list: List<Account>, n: Int = 5) =
            list.filter { it.lastUsedAt != null }.sortedByDescending { it.lastUsedAt }.take(n)

        /** Kamzor, dohraye gaye aur expire hone wale passwords ki nishandehi. */
        fun health(list: List<Account>, now: Long = System.currentTimeMillis()): PasswordHealth {
            val reusedPasswords = list.groupBy { it.password }.filter { it.key.isNotEmpty() && it.value.size > 1 }.keys
            return PasswordHealth(
                weak = list.filter { PasswordStrength.evaluate(it.password) == Strength.WEAK }.map { it.id },
                reused = list.filter { it.password in reusedPasswords }.map { it.id },
                expired = list.filter { it.expiryAt <= now }.map { it.id },
                expiringSoon = list.filter { it.expiryAt in (now + 1)..(now + SOON_MS) }.map { it.id }
            )
        }
    }
}
