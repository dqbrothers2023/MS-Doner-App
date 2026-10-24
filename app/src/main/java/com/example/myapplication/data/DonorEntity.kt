package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "donors")
data class DonorEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val bloodType: String,
    val location: String,
    val phone: String,
    val lastDonationDate: String,
    val isEligible: Boolean = true,
    val age: Int = 18 // Added age field
)