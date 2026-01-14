package com.example.silentsaver.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vip_contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNumber: String
)