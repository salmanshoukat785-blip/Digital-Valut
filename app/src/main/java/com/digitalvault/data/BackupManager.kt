package com.digitalvault.data

import android.util.Base64
import com.digitalvault.security.Crypto
import org.json.JSONArray
import org.json.JSONObject

/**
 * Encrypted backup: poora vault JSON bana kar AES-256-GCM se encrypt hota hai.
 * Key = PBKDF2(backup password, random salt). Backup ka password Master Password se alag ho sakta hai.
 */
object BackupManager {
    private const val MAGIC = "DIGITALVAULT-BACKUP"
    private const val ITER = 210_000

    fun encrypt(s: VaultSnapshot, password: CharArray): ByteArray {
        val plain = toJson(s).toString().toByteArray(Charsets.UTF_8)
        val salt = Crypto.randomBytes(16)
        val key = Crypto.pbkdf2(password, salt, ITER)
        try {
            val blob = Crypto.encrypt(key, plain)
            return JSONObject()
                .put("magic", MAGIC).put("v", 1).put("iter", ITER)
                .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .put("data", Base64.encodeToString(blob, Base64.NO_WRAP))
                .toString().toByteArray(Charsets.UTF_8)
        } finally {
            Crypto.wipe(key)
            Crypto.wipe(plain)
        }
    }

    /** Ghalat password ya kharab file par null. */
    fun decrypt(bytes: ByteArray, password: CharArray): VaultSnapshot? {
        try {
            val env = JSONObject(String(bytes, Charsets.UTF_8))
            if (env.optString("magic") != MAGIC) return null
            val iter = env.getInt("iter")
            if (iter !in 10_000..2_000_000) return null
            val salt = Base64.decode(env.getString("salt"), Base64.NO_WRAP)
            val key = Crypto.pbkdf2(password, salt, iter)
            val plain = try {
                Crypto.decrypt(key, Base64.decode(env.getString("data"), Base64.NO_WRAP))
            } finally {
                Crypto.wipe(key)
            }
            return fromJson(JSONObject(String(plain, Charsets.UTF_8)))
        } catch (e: Exception) {
            return null
        }
    }

    private fun toJson(s: VaultSnapshot): JSONObject {
        val cats = JSONArray()
        s.categories.forEach {
            cats.put(
                JSONObject().put("id", it.id).put("slug", it.slug ?: JSONObject.NULL)
                    .put("name", it.name).put("icon", it.icon).put("color", it.color)
                    .put("sortOrder", it.sortOrder)
            )
        }
        val accs = JSONArray()
        s.accounts.forEach {
            accs.put(
                JSONObject().put("categoryId", it.categoryId).put("title", it.title)
                    .put("username", it.username).put("email", it.email).put("password", it.password)
                    .put("url", it.url).put("notes", it.notes).put("isFavorite", it.isFavorite)
                    .put("createdAt", it.createdAt).put("lastUsedAt", it.lastUsedAt ?: -1L)
                    .put("expiryAt", it.expiryAt)
            )
        }
        val notes = JSONArray()
        s.notes.forEach {
            notes.put(
                JSONObject().put("title", it.title).put("body", it.body)
                    .put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)
            )
        }
        return JSONObject().put("categories", cats).put("accounts", accs).put("notes", notes)
    }

    private fun fromJson(j: JSONObject): VaultSnapshot {
        val cats = j.getJSONArray("categories").let { a ->
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                CategoryEntity(
                    id = o.getLong("id"),
                    slug = if (o.isNull("slug")) null else o.getString("slug"),
                    name = o.getString("name"), icon = o.getString("icon"),
                    color = o.getLong("color"), sortOrder = o.getInt("sortOrder")
                )
            }
        }
        val accs = j.getJSONArray("accounts").let { a ->
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                val used = o.optLong("lastUsedAt", -1L)
                Account(
                    title = o.getString("title"), username = o.optString("username"),
                    email = o.optString("email"), password = o.getString("password"),
                    url = o.optString("url"), notes = o.optString("notes"),
                    categoryId = o.optLong("categoryId"), isFavorite = o.optBoolean("isFavorite"),
                    createdAt = o.optLong("createdAt"), lastUsedAt = if (used > 0) used else null,
                    expiryAt = o.optLong("expiryAt")
                )
            }
        }
        val notes = j.getJSONArray("notes").let { a ->
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                SecureNote(
                    title = o.optString("title"), body = o.optString("body"),
                    createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt")
                )
            }
        }
        return VaultSnapshot(cats, accs, notes)
    }
}
