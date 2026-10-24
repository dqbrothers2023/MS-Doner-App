package com.example.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getDatabase(application).userDao()

    // Holds the currently logged-in user, or null if logged out
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

    // Error or success messages for UI
    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage

    fun registerUser(fullName: String, email: String, password: String, onResult: (Boolean) -> Unit) {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            _authMessage.value = "All fields are required"
            onResult(false)
            return
        }

        viewModelScope.launch {
            val existing = userDao.getUserByEmail(email)
            if (existing != null) {
                _authMessage.value = "Email already registered"
                onResult(false)
            } else {
                val newUser = UserEntity(fullName = fullName, email = email, password = password)
                userDao.registerUser(newUser)
                _authMessage.value = "Registration successful! Please log in."
                onResult(true)
            }
        }
    }

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _authMessage.value = "Please fill in all fields"
            onResult(false)
            return
        }

        viewModelScope.launch {
            val user = userDao.login(email, password)
            if (user != null) {
                _currentUser.value = user
                _authMessage.value = null
                onResult(true)
            } else {
                _authMessage.value = "Invalid email or password"
                onResult(false)
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _authMessage.value = "Logged out successfully"
    }
}