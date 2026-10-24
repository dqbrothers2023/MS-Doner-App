package com.example.myapplication.ui.theme

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.DonorEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedBloodDonorDashboard(
    donors: List<DonorEntity>,
    onSaveDonor: (name: String, bloodType: String, location: String, date: String) -> Unit,
    onDeleteDonor: (DonorEntity) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var lastDonationDate by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val bloodGroups = listOf("All", "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

    val filteredDonors = if (selectedFilter == "All") {
        donors
    } else {
        donors.filter { it.bloodType.equals(selectedFilter, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "MS doner Dashboard", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006D77))
            ) {
                Text("Logout")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input Form Card
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "Register New Donor", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Donor Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = bloodType,
                    onValueChange = { bloodType = it },
                    label = { Text("Blood Type (e.g., A+, O-)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (City)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = lastDonationDate,
                    onValueChange = { lastDonationDate = it },
                    label = { Text("Last Donation Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank() && bloodType.isNotBlank() && location.isNotBlank()) {
                            onSaveDonor(name, bloodType, location, lastDonationDate)
                            name = ""
                            bloodType = ""
                            location = ""
                            lastDonationDate = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006D77))
                ) {
                    Text("Save Donor Locally")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Blood Group Filter Chips
        Text(text = "Filter by Blood Group:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            bloodGroups.take(5).forEach { group ->
                FilterChip(
                    selected = selectedFilter == group,
                    onClick = { selectedFilter = group },
                    label = { Text(group, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Registered Donors (${filteredDonors.size}):",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Donors List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredDonors) { donor ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F1F2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Name: ${donor.fullName}", fontWeight = FontWeight.Bold)
                            Text(
                                text = "Type: ${donor.bloodType}",
                                color = Color(0xFFB00020),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(text = "Location: ${donor.location}")
                        Text(text = "Last Donation: ${donor.lastDonationDate}")
                        Text(text = "Eligible: ${if (donor.isEligible) "Yes" else "No"}", fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action Buttons Row (Delete)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { onDeleteDonor(donor) }) {
                                Text("Delete", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}