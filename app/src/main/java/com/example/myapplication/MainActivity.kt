package com.example.myapplication

import android.Manifest
import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.DonorEntity
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.FileWriter
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    val currentUser by authViewModel.currentUser.collectAsState()

                    if (currentUser == null) {
                        LoginRegisterScreen(
                            authViewModel = authViewModel,
                            onLoginSuccess = {}
                        )
                    } else {
                        MainAppNavigator(
                            userName = currentUser?.fullName ?: "User",
                            onLogout = { authViewModel.logout() }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// MAIN NAVIGATION WITH RED BOTTOM BAR (3 TABS)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigator(
    userName: String,
    onLogout: () -> Unit
) {
    var currentTab by remember { mutableStateOf("home") } // "home", "directory", "register", "profile"
    var donorToEdit by remember { mutableStateOf<DonorEntity?>(null) }
    var currentUserName by remember { mutableStateOf(userName) }

    Scaffold(
        bottomBar = {
            Surface(
                color = Color(0xFFB00020),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 36.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavIcon(
                        icon = Icons.Default.Home,
                        label = "Home",
                        isSelected = currentTab == "home",
                        onClick = { currentTab = "home" }
                    )
                    BottomNavIcon(
                        icon = Icons.Default.List,
                        label = "Donate",
                        isSelected = currentTab == "directory",
                        onClick = { currentTab = "directory" }
                    )
                    BottomNavIcon(
                        icon = Icons.Default.Person,
                        label = "Profile",
                        isSelected = currentTab == "profile",
                        onClick = { currentTab = "profile" }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                "home" -> DashboardHomeScreen(
                    userName = currentUserName,
                    onNavigateToDirectory = { currentTab = "directory" },
                    onNavigateToRegister = {
                        donorToEdit = null
                        currentTab = "register"
                    }
                )
                "directory" -> DonorDirectoryScreen(
                    onEditDonor = { donor ->
                        donorToEdit = donor
                        currentTab = "register"
                    },
                    onBackToHome = { currentTab = "home" }
                )
                "register" -> RegisterDonorScreen(
                    donorToEdit = donorToEdit,
                    onNavigateBack = {
                        donorToEdit = null
                        currentTab = "home"
                    }
                )
                "profile" -> ProfileScreen(
                    userName = currentUserName,
                    onUpdateName = { newName -> currentUserName = newName },
                    onLogout = onLogout,
                    onBackToHome = { currentTab = "home" }
                )
            }
        }
    }
}

@Composable
fun BottomNavIcon(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (isSelected) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier
                .height(40.dp)
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = Color(0xFFB00020), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, color = Color(0xFFB00020), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    } else {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White.copy(alpha = 0.8f))
        }
    }
}

// ==========================================
// 1. DASHBOARD HOME SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHomeScreen(
    userName: String,
    onNavigateToDirectory: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val donorDao = remember { database.donorDao() }

    var totalMembers by remember { mutableStateOf(0) }
    var eligibleDonors by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        totalMembers = donorDao.getTotalDonorCount()
        eligibleDonors = donorDao.getEligibleDonorCount()
    }

    val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE8F1F2),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF1D2A44))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = userName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1D2A44))
                        Text(text = "Good Afternoon", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFDF7F4),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notification", tint = Color(0xFFB00020))
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Search Blood...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFB00020)) },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF7F4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Donate Blood", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1D2A44))
                        Text(text = "Save Life in Tamil Nadu", fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onNavigateToRegister,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Donate Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFB00020),
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Blood Group", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1D2A44))
                TextButton(onClick = onNavigateToDirectory) {
                    Text(text = "See All", color = Color(0xFFB00020), fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                bloodGroups.forEach { group ->
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFEBEE),
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { onNavigateToDirectory() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = group, color = Color(0xFFB00020), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Text(text = "Activity", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1D2A44))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActivityCard(
                    title = "Blood Donor",
                    subtitle = "$totalMembers Registered",
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToDirectory
                )
                ActivityCard(
                    title = "Register Member",
                    subtitle = "Add New Donor",
                    icon = Icons.Default.Add,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToRegister
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Our Contribution", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1D2A44))
                TextButton(onClick = onNavigateToDirectory) {
                    Text(text = "See All", color = Color(0xFFB00020), fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContributionBox("$totalMembers+", "Total Donors", Modifier.weight(1f))
                ContributionBox("$eligibleDonors", "Eligible Now", Modifier.weight(1f))
                ContributionBox("38", "TN Districts", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ActivityCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF7F4)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFEBEE),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color(0xFFB00020), modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1D2A44))
                Text(text = subtitle, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ContributionBox(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF7F4)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFB00020))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

// ==========================================
// 2. DONOR DIRECTORY SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorDirectoryScreen(
    onEditDonor: (DonorEntity) -> Unit,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val donorDao = remember { database.donorDao() }
    val scope = rememberCoroutineScope()

    var donors by remember { mutableStateOf<List<DonorEntity>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val bloodGroups = listOf("All", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

    fun loadDonors() {
        scope.launch { donors = donorDao.getAllDonors() }
    }

    LaunchedEffect(Unit) { loadDonors() }

    val filteredDonors = donors.filter { donor ->
        val matchesGroup = selectedFilter == "All" || donor.bloodType.equals(selectedFilter, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() || donor.location.contains(searchQuery, ignoreCase = true) || donor.fullName.contains(searchQuery, ignoreCase = true)
        matchesGroup && matchesSearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToHome) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1D2A44))
            }
            Text(text = "Donate Blood Directory", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1D2A44))
            Spacer(modifier = Modifier.width(40.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by City or Name...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFB00020)) },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            bloodGroups.forEach { group ->
                FilterChip(
                    selected = selectedFilter == group,
                    onClick = { selectedFilter = group },
                    label = { Text(group, fontSize = 9.sp) },
                    modifier = Modifier.height(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Blood Donors (${filteredDonors.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1D2A44))
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredDonors) { donor ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF7F4)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = Color(0xFFFFEBEE), modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFFB00020), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = donor.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1D2A44))
                                    Text(text = "📍 ${donor.location} • Age: ${donor.age}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFEBEE)
                            ) {
                                Text(
                                    text = donor.bloodType,
                                    color = Color(0xFFB00020),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "📞 Phone: ${donor.phone}", fontSize = 12.sp, color = Color(0xFF1D2A44))
                        Text(text = "📅 Last Donation: ${donor.lastDonationDate}", fontSize = 12.sp, color = Color(0xFF1D2A44))

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${donor.phone}"))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B9348)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Call", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        val url = "https://api.whatsapp.com/send?phone=${donor.phone}&text=Hi%20${donor.fullName},%20urgent%20blood%20needed%20for%20type%20${donor.bloodType}."
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("WhatsApp", fontSize = 11.sp)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { onEditDonor(donor) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Edit", fontSize = 11.sp)
                                }

                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            donorDao.deleteDonor(donor)
                                            loadDonors()
                                        }
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Delete", color = Color.Red, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. REGISTER / EDIT MEMBER SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterDonorScreen(
    donorToEdit: DonorEntity? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val donorDao = remember { database.donorDao() }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(donorToEdit?.fullName ?: "") }
    var bloodType by remember { mutableStateOf(donorToEdit?.bloodType ?: "A+") }
    var location by remember { mutableStateOf(donorToEdit?.location ?: "") }
    var phone by remember { mutableStateOf(donorToEdit?.phone ?: "") }
    var ageText by remember { mutableStateOf(donorToEdit?.age?.toString() ?: "") }
    var lastDonationDate by remember { mutableStateOf(donorToEdit?.lastDonationDate ?: "") }

    var dropdownExpanded by remember { mutableStateOf(false) }
    val bloodGroupOptions = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            lastDonationDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        contactUri?.let { uri ->
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                    val id = it.getString(idIndex)
                    val hasPhoneIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    val hasPhone = it.getString(hasPhoneIndex)

                    if (hasPhone == "1") {
                        val phoneCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(id),
                            null
                        )
                        phoneCursor?.use { pCursor ->
                            if (pCursor.moveToFirst()) {
                                val pIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                phone = pCursor.getString(pIndex).replace("\\s".toRegex(), "")
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1D2A44))
            }
            Text(
                text = if (donorToEdit == null) "Register New Member" else "Edit Member Details",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1D2A44)
            )
            Spacer(modifier = Modifier.width(40.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = bloodType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Blood Group") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    bloodGroupOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, fontWeight = FontWeight.Bold, color = Color(0xFFB00020)) },
                            onClick = {
                                bloodType = option
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = ageText,
                onValueChange = { ageText = it },
                label = { Text("Age (18-65)") },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Tamil Nadu City / District") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Button(
                onClick = { contactPickerLauncher.launch(null) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(imageVector = Icons.Default.Person, contentDescription = "Pick Contact")
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = lastDonationDate,
                onValueChange = { },
                readOnly = true,
                label = { Text("Last Donation Date") },
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Date",
                        modifier = Modifier.clickable { datePickerDialog.show() }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { datePickerDialog.show() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val age = ageText.toIntOrNull()

                if (age == null || age < 18 || age > 65) {
                    Toast.makeText(context, "Donor age must be between 18 and 65 years!", Toast.LENGTH_LONG).show()
                    return@Button
                }

                if (name.isNotBlank() && bloodType.isNotBlank() && phone.isNotBlank()) {
                    scope.launch {
                        val updatedDonor = DonorEntity(
                            id = donorToEdit?.id ?: 0,
                            fullName = name,
                            bloodType = bloodType,
                            location = location,
                            phone = phone,
                            lastDonationDate = if (lastDonationDate.isBlank()) "N/A" else lastDonationDate,
                            isEligible = true,
                            age = age
                        )

                        if (donorToEdit == null) {
                            donorDao.insertDonor(updatedDonor)
                            NotificationHelper.showNotification(
                                context = context,
                                title = "Member Registered Successfully!",
                                message = "$name ($bloodType) has been added to local records."
                            )
                        } else {
                            donorDao.updateDonor(updatedDonor)
                            Toast.makeText(context, "Member details updated successfully!", Toast.LENGTH_SHORT).show()
                        }

                        onNavigateBack()
                    }
                } else {
                    Toast.makeText(context, "Please fill in all required fields!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
        ) {
            Text(
                text = if (donorToEdit == null) "Save Member Record" else "Update Member Details",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==========================================
// 4. MNC-STANDARD PROFILE SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userName: String,
    onUpdateName: (String) -> Unit,
    onLogout: () -> Unit,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var wantToDonate by remember { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(userName) }

    // FILE PICKER FOR RESTORING BACKUP CSV
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(fileUri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val database = AppDatabase.getDatabase(context)
                    val dao = database.donorDao()

                    // Skip header line
                    reader.readLine()
                    var line: String? = reader.readLine()
                    var count = 0

                    while (line != null) {
                        val tokens = line.split(",")
                        if (tokens.size >= 8) {
                            val donor = DonorEntity(
                                id = 0, // Auto-generate ID to avoid conflict
                                fullName = tokens[1],
                                bloodType = tokens[2],
                                location = tokens[3],
                                phone = tokens[4],
                                age = tokens[5].toIntOrNull() ?: 20,
                                lastDonationDate = tokens[6],
                                isEligible = tokens[7].toBoolean()
                            )
                            dao.insertDonor(donor)
                            count++
                        }
                        line = reader.readLine()
                    }
                    reader.close()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Successfully restored $count records!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // EDIT PROFILE DIALOG
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profile Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedName.isNotBlank()) {
                            onUpdateName(editedName)
                            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // TOP APP BAR
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToHome) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1D2A44))
                }
                Text(text = "Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1D2A44))
                Spacer(modifier = Modifier.width(40.dp))
            }
        }

        // USER AVATAR & NAME
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFB00020),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = userName, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1D2A44))
                Text(text = "Last Donation: October, 2026", fontSize = 12.sp, color = Color.Gray)
            }
        }

        // METRICS COUNTERS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileMetricBox("Donations", "05", Modifier.weight(1f))
                ProfileMetricBox("Requests", "03", Modifier.weight(1f))
                ProfileMetricBox("Life Saved", "06", Modifier.weight(1f))
            }
        }

        // I WANT TO DONATE TOGGLE ROW
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF7F4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFFFFEBEE), modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFB00020), modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "I Want To Donate", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1D2A44))
                    }
                    Switch(
                        checked = wantToDonate,
                        onCheckedChange = { wantToDonate = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFB00020))
                    )
                }
            }
        }

        // SETTINGS & BACKUP / STORAGE ROWS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ProfileMenuRow(title = "Edit Profile", icon = Icons.Default.Edit) {
                    editedName = userName
                    showEditDialog = true
                }

                // LOCAL DATA BACKUP & EXPORT (CSV)
                ProfileMenuRow(title = "Backup & Export Data (CSV)", icon = Icons.Default.Refresh) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val database = AppDatabase.getDatabase(context)
                            val donors = database.donorDao().getAllDonors()

                            val csvFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "MS_Doner_Backup.csv")
                            val writer = FileWriter(csvFile)
                            writer.append("ID,FullName,BloodType,Location,Phone,Age,LastDonationDate,IsEligible\n")
                            for (donor in donors) {
                                writer.append("${donor.id},${donor.fullName},${donor.bloodType},${donor.location},${donor.phone},${donor.age},${donor.lastDonationDate},${donor.isEligible}\n")
                            }
                            writer.flush()
                            writer.close()

                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Backup saved to Documents folder!", Toast.LENGTH_LONG).show()

                                val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Donor Backup"))
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                // RESTORE DATA FROM CSV (Using Icons.Default.Check)
                ProfileMenuRow(title = "Restore Data from CSV", icon = Icons.Default.Check) {
                    restoreFileLauncher.launch(arrayOf("text/csv", "application/comma-separated-values", "*/*"))
                }

                // GOOGLE DRIVE CLOUD BACKUP
                ProfileMenuRow(title = "Google Drive Cloud Backup", icon = Icons.Default.Info) {
                    val driveIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://drive.google.com/")
                    }
                    try {
                        context.startActivity(driveIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Opening Google Drive...", Toast.LENGTH_SHORT).show()
                    }
                }

                ProfileMenuRow(title = "Blood Request", icon = Icons.Default.Favorite) {
                    Toast.makeText(context, "Blood Request clicked", Toast.LENGTH_SHORT).show()
                }

                ProfileMenuRow(title = "Settings", icon = Icons.Default.Settings) {
                    Toast.makeText(context, "Settings clicked", Toast.LENGTH_SHORT).show()
                }

                // INVITE FRIENDS
                ProfileMenuRow(title = "Invite Friends", icon = Icons.Default.Share) {
                    val shareText = "Join MS Doner App - Save Lives in Tamil Nadu! Download and register here: https://play.google.com/store/apps/details?id=com.example.myapplication"

                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Invite Link", shareText)
                    clipboard.setPrimaryClip(clip)

                    Toast.makeText(context, "Invite link copied to clipboard!", Toast.LENGTH_SHORT).show()

                    try {
                        val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(shareText)}")
                        }
                        context.startActivity(whatsappIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "WhatsApp not installed. Link copied!", Toast.LENGTH_LONG).show()
                    }
                }

                ProfileMenuRow(title = "Privacy Policy", icon = Icons.Default.Lock) {
                    Toast.makeText(context, "Privacy Policy clicked", Toast.LENGTH_SHORT).show()
                }
                ProfileMenuRow(title = "Rate Us", icon = Icons.Default.Star) {
                    Toast.makeText(context, "Rate Us clicked", Toast.LENGTH_SHORT).show()
                }
                ProfileMenuRow(title = "About Us", icon = Icons.Default.Info) {
                    Toast.makeText(context, "About Us clicked", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // SIGN OUT BUTTON
        item {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFB00020))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Sign Out", color = Color(0xFFB00020), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ProfileMetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF7F4)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1D2A44))
        }
    }
}

@Composable
fun ProfileMenuRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF7F4)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color(0xFFFFEBEE), modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFFB00020), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF1D2A44))
            }
            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}