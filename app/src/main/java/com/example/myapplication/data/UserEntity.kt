package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val fullName: String,
    val email: String,
    val password: String // In a production app you'd hash this, but for local offline use, plaintext or simple storage works for a prototype!
)