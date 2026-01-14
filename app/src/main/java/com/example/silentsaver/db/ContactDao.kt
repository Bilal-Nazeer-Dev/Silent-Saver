package com.example.silentsaver.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM vip_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    // ADD THIS NEW FUNCTION for CallReceiver
    @Query("SELECT * FROM vip_contacts")
    suspend fun getAllContactsList(): List<ContactEntity>

    @Insert
    suspend fun insert(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM vip_contacts WHERE phoneNumber LIKE '%' || :number || '%')")
    suspend fun isNumberWhitelisted(number: String): Boolean
}