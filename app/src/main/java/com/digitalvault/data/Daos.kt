package com.digitalvault.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT * FROM categories WHERE slug = :slug LIMIT 1")
    suspend fun bySlug(slug: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun get(id: Long): CategoryEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM categories")
    suspend fun maxOrder(): Int

    @Insert suspend fun insert(c: CategoryEntity): Long
    @Insert suspend fun insertAll(list: List<CategoryEntity>)
    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun getAll(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun get(id: Long): AccountEntity?

    @Query("SELECT categoryId, COUNT(*) AS total FROM accounts GROUP BY categoryId")
    fun observeCounts(): Flow<List<CategoryCount>>

    /** Bagair decrypt kiye: kitne accounts ki expiry `before` tak aa chuki hai. */
    @Query("SELECT COUNT(*) FROM accounts WHERE expiryAt <= :before")
    suspend fun countExpiring(before: Long): Int

    @Insert suspend fun insert(a: AccountEntity): Long
    @Update suspend fun update(a: AccountEntity)
    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("UPDATE accounts SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)
    @Query("UPDATE accounts SET lastUsedAt = :time WHERE id = :id")
    suspend fun setLastUsed(id: Long, time: Long)
    @Query("UPDATE accounts SET categoryId = :to WHERE categoryId = :from")
    suspend fun moveCategory(from: Long, to: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: Long): NoteEntity?

    @Insert suspend fun insert(n: NoteEntity): Long
    @Update suspend fun update(n: NoteEntity)
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}
