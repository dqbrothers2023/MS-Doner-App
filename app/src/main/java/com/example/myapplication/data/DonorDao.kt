package com.example.myapplication.data

import androidx.room.*

@Dao
interface DonorDao {
    @Query("SELECT * FROM donors ORDER BY id DESC")
    suspend fun getAllDonors(): List<DonorEntity>

    @Query("SELECT COUNT(*) FROM donors")
    suspend fun getTotalDonorCount(): Int

    @Query("SELECT COUNT(*) FROM donors WHERE isEligible = 1")
    suspend fun getEligibleDonorCount(): Int

    @Query("SELECT COUNT(*) FROM donors WHERE bloodType = 'O+' OR bloodType = 'O-'")
    suspend fun getUniversalDonorCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonor(donor: DonorEntity)

    @Update
    suspend fun updateDonor(donor: DonorEntity)

    @Delete
    suspend fun deleteDonor(donor: DonorEntity)
}