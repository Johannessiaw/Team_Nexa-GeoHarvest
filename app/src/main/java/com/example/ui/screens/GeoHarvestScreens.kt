@file:OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
package com.example.ui.screens

import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import com.example.data.*
import coil.compose.AsyncImage
import com.example.ui.viewmodel.GeoHarvestViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.android.gms.location.LocationServices
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun GeoHarvestMainApp(viewModel: GeoHarvestViewModel) {
    val userAuthenticated by viewModel.userAuthenticated.collectAsStateWithLifecycle()
    val authenticatedUserType by viewModel.authenticatedUserType.collectAsStateWithLifecycle()
    val authenticatedUserIdentifier by viewModel.authenticatedUserIdentifier.collectAsStateWithLifecycle()

    if (!userAuthenticated) {
        OnboardingSignInScreen(viewModel = viewModel)
        return
    }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val showSyncBanner by viewModel.showSyncBanner.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val listNotifications by viewModel.notificationList.collectAsStateWithLifecycle()
    var showNotificationDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Accompanist Location Permissions State
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        viewModel.setLocationPermissionGranted(permissionsState.allPermissionsGranted)
    }

    // Fused Location fetching
    val locationPermissionGranted by viewModel.locationPermissionGranted.collectAsStateWithLifecycle()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: android.location.Location? ->
                    if (loc != null) {
                        viewModel.updateLocation(loc.latitude, loc.longitude)
                    } else {
                        viewModel.updateLocation(7.5833, -1.9333)
                    }
                }
            } catch (e: SecurityException) {
                viewModel.updateLocation(7.5833, -1.9333)
            }
        }
    }

    // Chatbot sheet state
    var showChatSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Grass,
                            contentDescription = "Logo",
                            tint = Color(0xFF064E3B),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "GeoHarvest",
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                color = Color(0xFF064E3B),
                                fontSize = 19.sp,
                                letterSpacing = 0.5.sp
                            )
                            val userTypeText = when (authenticatedUserType) {
                                "GhanaCard" -> "Ghana Card: ${authenticatedUserIdentifier?.take(12)}..."
                                "Phone" -> "Phone: $authenticatedUserIdentifier"
                                "Google" -> "Google User"
                                else -> "Guest Access"
                            }
                            Text(
                                text = userTypeText,
                                fontSize = 13.sp,
                                color = Color(0xFF475569),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    // RED Notification Bell with Dynamic Badged Count
                    IconButton(
                        onClick = { 
                            showNotificationDialog = true
                        },
                        modifier = Modifier.testTag("notification_bell")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = Color.Red,
                                        contentColor = Color.White
                                    ) {
                                        Text(unreadCount.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = if (unreadCount > 0) Color.Red else Color(0xFF475569),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Log out / Sign out Button
                    IconButton(
                        onClick = { viewModel.signOut() },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = Color(0xFF475569))
                    }

                    // Prepopulated Reset Button (Demo Mode)
                    IconButton(
                        onClick = {
                            viewModel.forceResetDemo()
                            Toast.makeText(context, "Demo preloaded and reset successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("reset_demo_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Demo Data", tint = MaterialTheme.colorScheme.secondary)
                    }

                    // Online/Offline simulation switcher
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(
                                color = if (isOnline) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.toggleOnlineMode() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                            contentDescription = "Sync Connection",
                            tint = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOnline) "Online" else "Offline",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ) {
                NavigationBarItem(
                    selected = currentTab == "home",
                    onClick = { viewModel.setTab("home") },
                    icon = { Icon(if (currentTab == "home") Icons.Filled.Home else Icons.Outlined.Home, "Home") },
                    label = { Text("Home", fontSize = 14.sp) },
                    modifier = Modifier.testTag("tab_home")
                )
                NavigationBarItem(
                    selected = currentTab == "profile",
                    onClick = { viewModel.setTab("profile") },
                    icon = { Icon(if (currentTab == "profile") Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle, "Profile") },
                    label = { Text("Profile", fontSize = 14.sp) },
                    modifier = Modifier.testTag("tab_profile")
                )
                NavigationBarItem(
                    selected = currentTab == "marketplace",
                    onClick = { viewModel.setTab("marketplace") },
                    icon = { Icon(if (currentTab == "marketplace") Icons.Filled.Storefront else Icons.Outlined.Storefront, "Market") },
                    label = { Text("Market", fontSize = 14.sp) },
                    modifier = Modifier.testTag("tab_marketplace")
                )
                NavigationBarItem(
                    selected = currentTab == "live_map",
                    onClick = { viewModel.setTab("live_map") },
                    icon = { Icon(if (currentTab == "live_map") Icons.Filled.Map else Icons.Outlined.Map, "Map") },
                    label = { Text("Map", fontSize = 14.sp) },
                    modifier = Modifier.testTag("tab_live_map")
                )
                NavigationBarItem(
                    selected = currentTab == "activity",
                    onClick = { viewModel.setTab("activity") },
                    icon = { Icon(if (currentTab == "activity") Icons.Filled.TrendingUp else Icons.Outlined.TrendingUp, "Activity") },
                    label = { Text("Activity", fontSize = 14.sp) },
                    modifier = Modifier.testTag("tab_activity")
                )
            }
        },
        floatingActionButton = {
            // Elegant "Small Box" AI Advisor floating widget
            Card(
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .clickable { showChatSheet = true }
                    .testTag("floating_ai_assistant_btn")
                    .shadow(8.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15803D)), // vibrant green
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, Color(0xFFBBF7D0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "AI Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "AI Advisor",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFBBF7D0), CircleShape)
                            .size(8.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main views switcher
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "tab_navigation"
            ) { tab ->
                when (tab) {
                    "home" -> HomeScreen(viewModel)
                    "profile" -> ProfileScreen(viewModel)
                    "marketplace" -> MarketplaceScreen(viewModel)
                    "live_map" -> LiveMapScreen(viewModel)
                    "activity" -> ActivityScreen(viewModel)
                }
            }

            // Syncing loading indicator or completed banner
            AnimatedVisibility(
                visible = isSyncing,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .padding(16.dp)
                        .shadow(4.dp, RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Syncing offline listings with Accra hub...", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            AnimatedVisibility(
                visible = showSyncBanner,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .padding(16.dp)
                        .shadow(4.dp, RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("All listings synchronized successfully! ✅", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }

    // Floating Sheet for Multi-Turn AI Chatbot Assistant
    if (showChatSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChatSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            ChatSheetContent(viewModel) {
                showChatSheet = false
            }
        }
    }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { 
                showNotificationDialog = false
                viewModel.clearNotifications()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GeoHarvest Alerts", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (listNotifications.isEmpty()) {
                        Text("No notifications available.", color = Color(0xFF475569), fontSize = 17.sp)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 280.dp)
                        ) {
                            items(listNotifications) { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                            Text(item.timestamp, fontSize = 13.sp, color = Color(0xFF475569))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(item.message, fontSize = 14.sp, color = Color(0xFF475569))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showNotificationDialog = false
                        viewModel.clearNotifications()
                    }
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (listNotifications.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.clearNotifications()
                            viewModel.notificationList.value = emptyList()
                        }
                    ) {
                        Text("Clear All", color = Color.Red)
                    }
                }
            }
        )
    }

    val selectedProfileNameForDialog by viewModel.selectedProfileNameForDialog.collectAsStateWithLifecycle()
    if (selectedProfileNameForDialog != null) {
        UserProfileDialog(viewModel = viewModel)
    }

    // WhatsApp / Telegram Style Chat Overlay
    WhatsAppStyleChatOverlay(viewModel = viewModel)
}

@Composable
fun UserProfileDialog(viewModel: GeoHarvestViewModel) {
    val selectedName by viewModel.selectedProfileNameForDialog.collectAsStateWithLifecycle()
    val selectedRole by viewModel.selectedProfileRoleForDialog.collectAsStateWithLifecycle()
    val feedbackList by viewModel.transactionFeedback.collectAsStateWithLifecycle()
    val listings by viewModel.produceListings.collectAsStateWithLifecycle()
    val transporters by viewModel.transporters.collectAsStateWithLifecycle()
    val buyers by viewModel.buyers.collectAsStateWithLifecycle()
    val currentUserName by viewModel.authenticatedUserName.collectAsStateWithLifecycle()

    val name = selectedName ?: return
    val role = selectedRole ?: "Farmer"

    // Filter feedback for this target
    val filteredFeedback = feedbackList.filter { it.targetName.equals(name, ignoreCase = true) }

    // Resolve details dynamically based on role
    var rating = 4.8
    var completedCount = 12
    var trustScore = 95
    var phone = "+233 24 555 1234"
    var secondaryInfo = "Bono East Registered Member"

    when (role) {
        "Farmer" -> {
            val list = listings.find { it.farmerName.equals(name, ignoreCase = true) }
            if (list != null) {
                rating = list.farmerRating
                completedCount = list.farmerCompletedCount
                trustScore = list.trustScore
                phone = list.farmerPhone
                secondaryInfo = "Regional Produce Supplier"
            } else if (name.equals("Kofi Mensah", ignoreCase = true) || name.equals(currentUserName, ignoreCase = true)) {
                rating = 4.9
                completedCount = 24
                trustScore = 98
                phone = "+233 20 555 9999"
                secondaryInfo = "Bono East Co-op Gold Member"
            }
        }
        "Transporter" -> {
            val tr = transporters.find { it.name.equals(name, ignoreCase = true) }
            if (tr != null) {
                rating = tr.rating
                completedCount = tr.completedDeliveries
                trustScore = tr.trustScore
                phone = tr.phone
                secondaryInfo = "Verified Logistics Provider: ${tr.vehicle}"
            }
        }
        "Buyer" -> {
            val b = buyers.find { it.name.equals(name, ignoreCase = true) }
            if (b != null) {
                rating = b.rating
                completedCount = b.completedOrders
                trustScore = b.trustScore
                phone = b.phone
                secondaryInfo = "Vegetable Merchant (${b.memberSince})"
            }
        }
    }

    // Overwrite rating with average feedback rating if feedback exists to make it perfectly consistent!
    if (filteredFeedback.isNotEmpty()) {
        val avg = filteredFeedback.map { it.rating }.average()
        rating = Math.round(avg * 10.0) / 10.0
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = { viewModel.dismissUserProfileDialog() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("user_profile_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Avatar & Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = when (role) {
                                    "Farmer" -> Color(0xFFE8F5E9)
                                    "Transporter" -> Color(0xFFE3F2FD)
                                    else -> Color(0xFFFFF3E0)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (role) {
                                "Farmer" -> Icons.Default.Agriculture
                                "Transporter" -> Icons.Default.LocalShipping
                                else -> Icons.Default.Storefront
                            },
                            contentDescription = "Avatar Type",
                            tint = when (role) {
                                "Farmer" -> Color(0xFF2E7D32)
                                "Transporter" -> Color(0xFF1565C0)
                                else -> Color(0xFFEF6C00)
                            },
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Ghana Card Verified",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "$role • $secondaryInfo",
                            fontSize = 15.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Section (Row)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Rating", fontSize = 13.sp, color = Color(0xFF475569))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("$rating", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (role) {
                                "Farmer" -> "Sales"
                                "Transporter" -> "Deliveries"
                                else -> "Orders"
                            },
                            fontSize = 13.sp,
                            color = Color(0xFF475569)
                        )
                        Text("$completedCount", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Trust Score", fontSize = 13.sp, color = Color(0xFF475569))
                        Text("$trustScore%", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contact Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Phone", tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(phone, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // WhatsApp Style Chat Action button inside UserProfileDialog
                Button(
                    onClick = {
                        viewModel.dismissUserProfileDialog()
                        val greeting = when(role) {
                            "Transporter" -> "Akwaaba! Alhaji here. I am available for immediate transit with my vehicle. Where is the loading point, and what crops are we carrying?"
                            else -> "Hi! I am $name. Are you ready to supply fresh vegetables today?"
                        }
                        viewModel.startWhatsAppChat(name, role, "", greeting)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.Chat, null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chat with $name", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Past Feedback List
                Text(
                    text = "Transaction Feedback History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                if (filteredFeedback.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No feedback records found for this profile. Be the first to leave a review!",
                            fontSize = 14.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        filteredFeedback.forEach { fb ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(fb.senderName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        // Badge
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = if (fb.serviceType == "Marketplace") Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = fb.serviceType,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (fb.serviceType == "Marketplace") Color(0xFF2E7D32) else Color(0xFF1565C0)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        repeat(5) { starIndex ->
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (starIndex < fb.rating) Color(0xFFFBBF24) else Color(0xFFCBD5E1),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(fb.timestamp)),
                                            fontSize = 12.sp,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(fb.feedbackText, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Rate This User Form
                Text(
                    text = "Submit Rating & Feedback",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                // State for Form
                var selectedService by remember { mutableStateOf("Marketplace") }
                var selectedStars by remember { mutableStateOf(5) }
                var feedbackText by remember { mutableStateOf("") }
                var isError by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Service:", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedService = "Marketplace" }) {
                        RadioButton(selected = selectedService == "Marketplace", onClick = { selectedService = "Marketplace" })
                        Text("Marketplace", fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedService = "Logistics" }) {
                        RadioButton(selected = selectedService == "Logistics", onClick = { selectedService = "Logistics" })
                        Text("Logistics", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stars rating selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Stars:", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 8.dp))
                    repeat(5) { index ->
                        val starRating = index + 1
                        IconButton(
                            onClick = { selectedStars = starRating },
                            modifier = Modifier
                                .size(36.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$starRating Stars",
                                tint = if (starRating <= selectedStars) Color(0xFFFBBF24) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Comment Text Field
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = {
                        feedbackText = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Your Review / Feedback Comment", fontSize = 14.sp) },
                    placeholder = { Text("Describe your experience with this transaction...", fontSize = 14.sp) },
                    isError = isError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feedback_text_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (isError) {
                    Text("Please enter a feedback comment.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.dismissUserProfileDialog() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (feedbackText.isBlank()) {
                                isError = true
                            } else {
                                viewModel.addTransactionFeedback(
                                    targetName = name,
                                    senderName = currentUserName,
                                    serviceType = selectedService,
                                    rating = selectedStars,
                                    text = feedbackText
                                )
                                // Auto dismiss after submit
                                viewModel.dismissUserProfileDialog()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_feedback_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}

// ---------------------- 1. HOME / DASHBOARD ----------------------
@Composable
fun HomeScreen(viewModel: GeoHarvestViewModel) {
    val listings by viewModel.produceListings.collectAsStateWithLifecycle()
    val buyers by viewModel.buyers.collectAsStateWithLifecycle()
    val marketPrices by viewModel.marketPrices.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // 1. Connectivity Status Ribbon (6x1 Grid Row)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
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
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOnline) "Offline Mode Ready" else "Disconnected from Hub",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }
                    Text(
                        text = "Synced 2m ago",
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 2. Ghana Welcome Card & Trust Score Summary (Bento Core Banner)
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)), // Deep Emerald
                border = BorderStroke(1.dp, Color(0xFF022C22))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Welcome, Kofi Mensah", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified Status",
                                    tint = Color(0xFFFFF59D), // Soft Yellow
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verified Bono East Vegetable Farmer", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Bono East Pilot", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Trust Score", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                            Text("95% (Gold)", fontSize = 19.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFF59D))
                        }
                        Column {
                            Text("Completed Orders", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                            Text("15 Deliveries", fontSize = 19.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Column {
                            Text("Cancel Rate", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                            Text("2.0% Low", fontSize = 19.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }

        // ================= AI MATCHMAKER & RECOMMENDATION ENGINE =================
        item {
            var selectedPersona by remember { mutableStateOf("Buyer") } // Buyer or Farmer
            var selectedLocation by remember { mutableStateOf("Kumasi") } // Kumasi, Techiman, Accra, Tamale

            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFD1FAE5)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), // light green tint
                modifier = Modifier.fillMaxWidth().testTag("ai_matchmaker_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.SmartToy,
                                contentDescription = "AI Matchmaker",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI Smart Match Recommendation",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = Color(0xFF064E3B)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF059669), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("ML Engine", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Text(
                        "GeoHarvest automatically models current location metrics and ledger transactions to suggest optimal agrobusiness matches in real-time.",
                        fontSize = 14.sp,
                        color = Color(0xFF047857)
                    )

                    // Persona Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Buyer", "Farmer").forEach { persona ->
                            val active = selectedPersona == persona
                            Card(
                                onClick = { selectedPersona = persona },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (active) Color(0xFF059669) else Color.White
                                ),
                                border = BorderStroke(1.dp, if (active) Color(0xFF047857) else Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (persona == "Buyer") "I'm a Buyer" else "I'm a Farmer",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (active) Color.White else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }

                    // Location Selector Rows
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Kumasi", "Techiman", "Accra", "Tamale").forEach { loc ->
                            val active = selectedLocation == loc
                            FilterChip(
                                selected = active,
                                onClick = { selectedLocation = loc },
                                label = { Text(loc, fontSize = 14.sp) }
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFD1FAE5))

                    // Recommendations Output Block
                    Text(
                        "RECOMMENDED AGREEMENTS:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857),
                        letterSpacing = 0.5.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (selectedPersona == "Buyer") {
                            // Buyer recommendations:
                            // Tomatoes within 20 km, Cheapest seller, Highest-rated farmer, Closest transporter, Fastest delivery
                            val distText = if (selectedLocation == "Kumasi") "Tomatoes in Offinso (18 km)" else "Tomatoes in Tuobodom (12 km)"
                            val cheapestText = if (selectedLocation == "Accra") "Yaw Mensah (Accra Rural) - GHS 140/crate" else "Beatrice Ansah (Techiman) - GHS 120/crate"
                            val ratedText = "Kofi Mensah (Techiman) • ⭐ 4.9 (18 Deals)"
                            val transporterText = "Alhaji Ibrahim (Kia Ceres) • 2 km away"
                            val deliveryText = "Fastest: Kwame Baffour (Motorcycle) • 12 min ETA"

                            RecommendationItemRow(icon = Icons.Default.Verified, title = "Local Stock", value = distText, desc = "Based on crop maturity indices within 20km radius")
                            RecommendationItemRow(icon = Icons.Default.TrendingDown, title = "Cheapest Seller", value = cheapestText, desc = "Lowest negotiated bulk price on Agri-Ledger")
                            RecommendationItemRow(icon = Icons.Default.Star, title = "Highest-Rated Farmer", value = ratedText, desc = "Based on historic rating feedback and trust index")
                            RecommendationItemRow(icon = Icons.Default.LocalShipping, title = "Closest Transporter", value = transporterText, desc = "Instant request available for quick loading")
                            RecommendationItemRow(icon = Icons.Default.Speed, title = "Fastest Delivery", value = deliveryText, desc = "Optimized routing through best transit corridors")
                        } else {
                            // Farmer recommendations:
                            // Buyers needing tomatoes, Nearby transport, Nearby storage facility
                            val buyerText = "Accra Agro Wholesalers Ltd • Needs 200 Crates Tomatoes"
                            val transportText = "Kwame Baffour (Light Pickup) • 1.5 km away"
                            val storageText = "Bono East Central Grain Silo • 2.5 km away (Cold Storage Active)"

                            RecommendationItemRow(icon = Icons.Default.Storefront, title = "Active Demand Matching", value = buyerText, desc = "Verified buyers looking for immediate tomato shipments")
                            RecommendationItemRow(icon = Icons.Default.LocalShipping, title = "Nearby Logistical Support", value = transportText, desc = "Affordable carrier ready to load on-farm")
                            RecommendationItemRow(icon = Icons.Default.Warehouse, title = "Post-Harvest Cold Storage", value = storageText, desc = "Store produce safely to counter immediate price dips")
                        }
                    }
                }
            }
        }

        // ================= AGRI-INTELLIGENCE & REAL-TIME NEWS =================
        item {
            AgriNewsAndPracticesSection(viewModel)
        }

        // ================= PRICE TREND & INTELLIGENCE CANVAS =================
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().testTag("price_trend_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = "Price Trend",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Tomato Price Trend (GHS/Bag)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFDCFCE7), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("UPWARD 📈", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF15803D))
                        }
                    }

                    Text(
                        "Historical prices registered on Agri-Ledger over the last 30 days. High demand expected in Kumasi and Accra hubs next week.",
                        fontSize = 14.sp,
                        color = Color(0xFF475569)
                    )

                    // Canvas Graphic Price Trend Chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            PriceTrendChart()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf("Wk 1", "Wk 2", "Wk 3", "Wk 4", "Yesterday", "Today").forEach { label ->
                                    Text(label, fontSize = 12.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Price Breakdown Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PriceMetricBox(title = "Today's Price", price = "₵ 180", trend = "+2.8%", color = Color(0xFF15803D), modifier = Modifier.weight(1f))
                        PriceMetricBox(title = "Yesterday", price = "₵ 175", trend = "+1.4%", color = Color(0xFF15803D), modifier = Modifier.weight(1f))
                        PriceMetricBox(title = "Last Week", price = "₵ 150", trend = "-0.5%", color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
                        PriceMetricBox(title = "Monthly Trend", price = "₵ 135 Avg", trend = "+25% Up", color = Color(0xFF15803D), modifier = Modifier.weight(1f))
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Market Advisory: Prices are at a monthly high. We recommend selling tomatoes today to lock in high GHS yields before regional rain harvests begin.",
                                fontSize = 13.sp,
                                color = Color(0xFF1E40AF),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 3. Market Pricing (4x2 Grid Row) & Quick Post Button (2x2) Side-by-Side Bento Grid
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Market Pricing Card (col-span 4)
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    modifier = Modifier
                        .weight(3.5f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "LIVE PRICES",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCBD5E1),
                                letterSpacing = 1.sp
                            )
                            Text(
                                "₵ GHS/BAG",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                letterSpacing = 0.5.sp
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            marketPrices.take(2).forEach { price ->
                                val isUp = price.priceTrend == "UP"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val emoji = when (price.vegetableType) {
                                            "Tomatoes" -> "🍅"
                                            "Okra" -> "🥬"
                                            "Garden Eggs" -> "🥚"
                                            "Peppers" -> "🫑"
                                            else -> "🌱"
                                        }
                                        Text(
                                            "$emoji ${price.vegetableType}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "GHS ${price.recommendedPrice.toInt()}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF064E3B)
                                        )
                                        Text(
                                            text = if (isUp) "+2.4%" else "-0.8%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUp) Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Post Button Card (col-span 2)
                Card(
                    onClick = { viewModel.setTab("marketplace") },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF059669)), // bg-emerald-600
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight(),
                    border = BorderStroke(1.dp, Color(0xFF047857))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add listing",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "POST",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // 4. Logistics Tracker Card (6x2 Grid Row) - Dark slate theme tracker
        item {
            val activeOrder = orders.firstOrNull { it.status == "In Transit" } ?: orders.firstOrNull()
            if (activeOrder != null) {
                Card(
                    onClick = { viewModel.setTab("logistics") },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // bg-slate-900
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFF10B981), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = "Transit icon",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Delivery #${1000 + activeOrder.id}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        "${activeOrder.quantity.toInt()} Bags of ${activeOrder.vegetableType}",
                                        fontSize = 14.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "IN TRANSIT",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Custom progress indicators
                        val progress = activeOrder.trackingProgress
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (step in 1..4) {
                                val isStepDone = progress >= (step * 2)
                                Box(
                                    modifier = Modifier
                                        .height(6.dp)
                                        .weight(1f)
                                        .background(
                                            color = if (isStepDone) Color(0xFF10B981) else Color(0xFF334155),
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "ETA: 4.2 km away",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399)
                            )
                            Text(
                                "Live tracking status ➔",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        // 5. Nearby Insights (3x2) & Trust Score Meter (3x2) Side-by-Side Bento Grid Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nearby Insights (col-span 3)
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "MARKET ACTIVITY",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1),
                            letterSpacing = 1.sp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFF3B82F6), CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("12 Farmers Nearby", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFFF97316), CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("5 Buyers Online", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                            }
                        }

                        Text(
                            "Bono East Region",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Trust Score / Reliability (col-span 3)
                Card(
                    onClick = { viewModel.setTab("analytics") },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)), // blue-50
                    border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Canvas(modifier = Modifier.size(54.dp)) {
                                drawCircle(
                                    color = Color(0xFFDBEAFE),
                                    radius = size.minDimension / 2 - 4f,
                                    style = Stroke(width = 12f)
                                )
                                drawArc(
                                    color = Color(0xFF3B82F6),
                                    startAngle = -90f,
                                    sweepAngle = 360f * 0.95f,
                                    useCenter = false,
                                    style = Stroke(width = 12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                            }
                            Text(
                                "95%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1D4ED8)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "TRUST SCORE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E40AF),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // 6. Navigation Tabs Shortcuts (Optional Action Pills)
        item {
            Column {
                Text("Quick Navigate", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF064E3B))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val actions = listOf(
                        Triple("Produce Market", Icons.Default.Storefront, "marketplace"),
                        Triple("Dispatch Map", Icons.Default.Map, "live_map"),
                        Triple("Performance Stats", Icons.Default.BarChart, "analytics")
                    )
                    actions.forEach { (label, icon, tab) ->
                        Card(
                            onClick = { viewModel.setTab(tab) },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(icon, contentDescription = label, tint = Color(0xFF064E3B), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // 7. Nearby Active Buyers looking to purchase
        item {
            Column {
                Text("Nearby Buyers Looking to Purchase", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF064E3B))
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    buyers.forEach { buyer ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(buyer.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFD1FAE5), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Verified Buyer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                                        }
                                    }
                                    Text(buyer.location, fontSize = 14.sp, color = Color(0xFF64748B))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("In Demand: ${buyer.demandVegetables}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF059669))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("${buyer.rating}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                    Text("${buyer.completedOrders} orders", fontSize = 13.sp, color = Color(0xFFCBD5E1))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { viewModel.setTab("marketplace") },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(28.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B))
                                    ) {
                                        Text("Supply", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Live Produce Listings
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Live Produce Listings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF064E3B))
                    TextButton(onClick = { viewModel.setTab("marketplace") }) {
                        Text("View All", fontSize = 15.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(listings) { listing ->
                        Card(
                            modifier = Modifier
                                .width(180.dp)
                                .height(230.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Crop Image header
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(75.dp)
                                ) {
                                    CropImage(
                                        imageUrl = listing.imageUrl,
                                        category = listing.vegetableType,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Soft color indicator badge on top right of image
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .background(
                                                color = when (listing.vegetableType) {
                                                    "Tomatoes" -> Color(0xFFEF4444)
                                                    "Okra" -> Color(0xFF10B981)
                                                    "Garden Eggs" -> Color(0xFFFBBF24)
                                                    "Peppers" -> Color(0xFFF97316)
                                                    else -> Color(0xFF3B82F6)
                                                },
                                                shape = CircleShape
                                            )
                                            .size(8.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(listing.vegetableType, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                        Text(listing.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF0F172A))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("By ${listing.farmerName}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "Verified Red Tick",
                                                tint = Color(0xFFEF4444), // RED TICK
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("⭐ ${listing.farmerRating}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("(${listing.farmerCompletedCount} sales)", fontSize = 12.sp, color = Color(0xFF475569))
                                        }
                                        Text("Loc: ${listing.location}", fontSize = 14.sp, color = Color(0xFFCBD5E1))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column {
                                            Text("Price / Unit", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                            Text("GHS ${listing.pricePerUnit}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFD1FAE5), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("${listing.quantity.toInt()} units", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 9. Recent Orders with quick tracking click
        item {
            Column {
                Text("Recent Logistics & Order History", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF064E3B))
                Spacer(modifier = Modifier.height(10.dp))
                if (orders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No orders created yet.", color = Color(0xFF475569), fontSize = 16.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        orders.forEach { order ->
                            Card(
                                onClick = { viewModel.selectOrder(order.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Order #${1000 + order.id}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFCBD5E1))
                                        Text("${order.quantity.toInt()} Bags of ${order.vegetableType}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                                        Text("Driver: ${order.transporterName}", fontSize = 14.sp, color = Color(0xFF64748B))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    when (order.status) {
                                                        "Delivered", "Payment Completed" -> Color(0xFFD1FAE5)
                                                        "In Transit" -> Color(0xFFDBEAFE)
                                                        else -> Color(0xFFFEF3C7)
                                                    },
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                order.status,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (order.status) {
                                                    "Delivered", "Payment Completed" -> Color(0xFF064E3B)
                                                    "In Transit" -> Color(0xFF1D4ED8)
                                                    else -> Color(0xFFB45309)
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("GHS ${order.totalPrice.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ---------------------- 2. SMART MARKETPLACE ----------------------
@Composable
fun CropImage(imageUrl: String, category: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(imageUrl) {
        if (!imageUrl.startsWith("res:") && !imageUrl.startsWith("http") && imageUrl.isNotEmpty()) {
            try {
                android.graphics.BitmapFactory.decodeFile(imageUrl)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else null
    }
    val resourceId = remember(imageUrl, category) {
        when {
            imageUrl == "res:img_cassava" -> com.example.R.drawable.img_cassava_1783165140582
            imageUrl == "res:img_rice" -> com.example.R.drawable.img_rice_1783165155237
            imageUrl == "res:img_irrigation_pump" -> com.example.R.drawable.img_irrigation_pump_1783165172070
            imageUrl == "res:img_cutlass" -> com.example.R.drawable.img_cutlass_1783165184506
            imageUrl == "res:img_farmland" -> com.example.R.drawable.img_farmland_1783165196652
            category.equals("cassava", ignoreCase = true) || category.equals("tuber", ignoreCase = true) -> com.example.R.drawable.img_cassava_1783165140582
            category.equals("rice", ignoreCase = true) || category.equals("cereal", ignoreCase = true) -> com.example.R.drawable.img_rice_1783165155237
            category.equals("equipment", ignoreCase = true) -> com.example.R.drawable.img_irrigation_pump_1783165172070
            category.equals("cutlass", ignoreCase = true) -> com.example.R.drawable.img_cutlass_1783165184506
            category.equals("farmland", ignoreCase = true) || category.equals("land", ignoreCase = true) -> com.example.R.drawable.img_farmland_1783165196652
            else -> null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Crop photo",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (resourceId != null) {
        Image(
            painter = painterResource(id = resourceId),
            contentDescription = "Crop photo",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (imageUrl.startsWith("http")) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Crop photo",
            modifier = modifier,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = com.example.R.drawable.img_farmland_1783165196652),
            error = painterResource(id = com.example.R.drawable.img_farmland_1783165196652)
        )
    } else {
        Image(
            painter = painterResource(id = com.example.R.drawable.img_farmland_1783165196652),
            contentDescription = "Crop photo",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun RecommendationItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFDCFCE7), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF475569))
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE0F2FE), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("Match 99%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                }
            }
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, fontSize = 13.sp, color = Color(0xFF475569), lineHeight = 14.sp)
        }
    }
}

@Composable
fun AgriNewsAndPracticesSection(viewModel: GeoHarvestViewModel) {
    var activeCategory by remember { mutableStateOf("All") }
    
    Card(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().testTag("agri_news_and_practices")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Article,
                        contentDescription = "AgriNews",
                        tint = Color(0xFF064E3B),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Agri-Intelligence Desk",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Live Updates",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                }
            }
            
            Text(
                text = "Real-time crop advice, equipment pooling options, weather alerts, and localized biosecurity warnings.",
                fontSize = 13.sp,
                color = Color(0xFF475569)
            )
            
            // Category Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All", "Weather", "Practices", "Outbreaks", "Equipment").forEach { cat ->
                    val isSelected = activeCategory == cat
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) Color(0xFF064E3B) else Color(0xFFF1F5F9),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { activeCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Bento Grid of dynamic cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                
                // 1. Weather Alert Card (Weather)
                if (activeCategory == "All" || activeCategory == "Weather") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.focusMapOn(7.5833, -1.9333, "Techiman Hub Weather Station", "Weather telemetry: 28°C with light rain alerts. Recommended crop protective covers active.", "WEATHER")
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Text("⛅", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Techiman Harvest Advisory", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E3A8A))
                                    Text("Map Link 🧭", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                }
                                Text(
                                    "Light rains predicted in Bono East tomorrow. Ensure tomato plots have clear drainage paths to avoid root rot. Postpone direct-field fertilizer spray.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E40AF),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
                
                // 2. Best Practices Card (Practices)
                if (activeCategory == "All" || activeCategory == "Practices") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Text("🍀", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Ecological Crop Protection", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF14532D))
                                Text(
                                    "Intercropping chili peppers alongside eggplant shields your main yield by decoying whiteflies. This reduces synthetic pesticide requirement by up to 45%.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
                
                // 3. Outbreak Warning Card (Outbreaks)
                if (activeCategory == "All" || activeCategory == "Outbreaks") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.focusMapOn(7.3804, -1.3681, "Ejura Outbreak Area", "Yellow Leaf Curl Virus outbreak verified in Ejura district. Quarantine boundaries enforced.", "OUTBREAK")
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column {
                            // Outbreak Cover Image
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = com.example.R.drawable.img_tomato_blight_1783587282431),
                                    contentDescription = "Ejura Yellow Leaf Curl Outbreak",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Alert Badge
                                Box(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .background(Color(0xFFEF4444), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .align(Alignment.TopStart)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Critical Alert",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "CRITICAL OUTBREAK DETECTED",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ejura: Yellow Leaf Curl Alert",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF7F1D1D)
                                    )
                                    Text(
                                        text = "Map Link 🧭",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                                
                                Text(
                                    text = "Local outbreak of whitefly-borne virus reported in maize-legume transition zone. Cooperative biological spray teams dispatched to buffer zones.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF991B1B),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
                
                // 4. Equipment Sharing Card (Equipment)
                if (activeCategory == "All" || activeCategory == "Equipment") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.focusMapOn(7.5912, -1.9275, "Mini-Tractor Rental Hub", "Bono Cooperative rental depot. 2 light tractors and 5 solar pumps available.", "EQUIPMENT")
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.dp, Color(0xFFFEF3C7)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Text("🚜", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Cooperative Equipment Pool", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF78350F))
                                    Text("Map Link 🧭", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                }
                                Text(
                                    "Mini-Tractors available for local rental (GHS 80/day). Solar irrigation pumps are fully booked this week. Book in advance for next cycle.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriceMetricBox(
    title: String,
    price: String,
    trend: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, fontSize = 12.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold)
            Text(price, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
            Text(trend, fontSize = 12.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun PriceTrendChart() {
    val prices = listOf(110f, 115f, 130f, 125f, 140f, 145f, 160f, 180f)
    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp).padding(top = 10.dp, bottom = 10.dp)) {
        val width = size.width
        val height = size.height
        val maxVal = 200f
        val minVal = 80f
        val range = maxVal - minVal
        
        val points = prices.mapIndexed { idx, p ->
            val x = width * (idx.toFloat() / (prices.size - 1))
            val y = height - (height * ((p - minVal) / range))
            androidx.compose.ui.geometry.Offset(x, y)
        }
        
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        
        drawPath(
            path = path,
            color = Color(0xFF10B981),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 5f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        
        points.forEach { pt ->
            drawCircle(
                color = Color(0xFF064E3B),
                radius = 7f,
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = pt
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(viewModel: GeoHarvestViewModel) {
    val listings by viewModel.produceListings.collectAsStateWithLifecycle()
    val marketPrices by viewModel.marketPrices.collectAsStateWithLifecycle()
    val transporters by viewModel.transporters.collectAsStateWithLifecycle()

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val subcategories by viewModel.subcategories.collectAsStateWithLifecycle()
    val marketplaceItems by viewModel.marketplaceItems.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var marketStep by remember { mutableStateOf("QUESTION") }
    val backStack = remember { mutableStateListOf<String>() }
    fun navTo(step: String) { backStack.add(marketStep); marketStep = step }
    fun goBack() { marketStep = if (backStack.isNotEmpty()) backStack.removeAt(backStack.size - 1) else "QUESTION" }

    // Detail States
    var selectedItemDetail by remember { mutableStateOf<Any?>(null) }
    var selectedItemType by remember { mutableStateOf("") } // "CROP", "EQUIP", "OFFICER", "LAND"

    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var selectedSubcategoryId by remember { mutableStateOf<Int?>(null) }

    // Dynamic Search & Filters
    var searchQuery by remember { mutableStateOf("") }
    var onlyOrganic by remember { mutableStateOf(false) }
    var onlyAvailableToday by remember { mutableStateOf(false) }
    var isNegotiableFilter by remember { mutableStateOf(false) }

    // Form States for Dynamic Sell Flow
    var sellCategoryId by remember { mutableStateOf<Int?>(null) }
    var sellSubcategoryId by remember { mutableStateOf<Int?>(null) }
    var sellTitle by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var sellUnit by remember { mutableStateOf("bag") }
    var sellQuantity by remember { mutableStateOf("10") }
    var sellLocation by remember { mutableStateOf("") }
    var sellRegion by remember { mutableStateOf("Bono East") }
    var sellDescription by remember { mutableStateOf("") }
    var sellIsOrganic by remember { mutableStateOf(false) }
    var sellSellerName by remember { mutableStateOf("") }
    var sellSellerPhone by remember { mutableStateOf("") }

    // Custom Fields
    var sellSoilType by remember { mutableStateOf("Sandy Loam") }
    var sellWaterAvailability by remember { mutableStateOf("Year-round borehole") }
    var sellIrrigationInstalled by remember { mutableStateOf(false) }
    var sellTractorAccessibility by remember { mutableStateOf(true) }
    var sellCapacity by remember { mutableStateOf("Kia Ceres (3-Ton)") }
    var sellPricePerKm by remember { mutableStateOf("") }
    var sellEstimatedArrival by remember { mutableStateOf("15 mins") }
    var sellQualifications by remember { mutableStateOf("BSc Agronomy") }
    var sellLanguages by remember { mutableStateOf("Twi, English") }
    var sellExperienceYears by remember { mutableStateOf("5") }
    var sellIsRent by remember { mutableStateOf(false) }
    var sellRentRate by remember { mutableStateOf("") }

    // Form States
    var title by remember { mutableStateOf("") }
    var vegetableType by remember { mutableStateOf("Tomatoes") }
    var quantityStr by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var collectionPoint by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedImageUrl by remember { mutableStateOf("res:img_cassava") }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val file = java.io.File(context.cacheDir, "captured_produce_${System.currentTimeMillis()}.jpg")
                val out = java.io.FileOutputStream(file)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
                selectedImageUrl = file.absolutePath
                Toast.makeText(context, "Photo captured successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error saving photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos.", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUrl = uri.toString()
            Toast.makeText(context, "Image uploaded/selected from gallery!", Toast.LENGTH_SHORT).show()
        }
    }

    // Upload / Cam Simulation
    var showPhotoSelector by remember { mutableStateOf(false) }
    var simulationCameraActive by remember { mutableStateOf(false) }

    // Detail Screen Sub-features
    var showTransitAnalyzer by remember { mutableStateOf(false) }
    var transitStartHub by remember { mutableStateOf("Accra") }
    var showCustomerCareChat by remember { mutableStateOf(false) }
    var ccHistory = remember { mutableStateListOf<Pair<String, String>>() }
    var ratingValue by remember { mutableStateOf(5) }
    var showRatingDialog by remember { mutableStateOf(false) }

    // Marketplace AI Assistant
    var aiQuery by remember { mutableStateOf("") }
    var aiAnswerText by remember { mutableStateOf("") }
    var aiIsThinking by remember { mutableStateOf(false) }
    var showLocalMarketplaceAiDialog by remember { mutableStateOf(false) }

    // Equipment Form States
    var equipName by remember { mutableStateOf("") }
    var equipPriceStr by remember { mutableStateOf("") }
    var equipIsRent by remember { mutableStateOf(false) }
    var equipRentRateStr by remember { mutableStateOf("") }
    var equipLocation by remember { mutableStateOf("") }
    var equipDealer by remember { mutableStateOf("") }
    var equipDesc by remember { mutableStateOf("") }
    var equipType by remember { mutableStateOf("Electronic") } // "Electronic", "Manual"

    // Logistics Form States
    var driverName by remember { mutableStateOf("") }
    var driverPhone by remember { mutableStateOf("") }
    var vehicleDesc by remember { mutableStateOf("") }
    var baseRateStr by remember { mutableStateOf("") }
    var ratePerKmStr by remember { mutableStateOf("") }
    var driverLoc by remember { mutableStateOf("") }

    // Land Form States
    var landTitle by remember { mutableStateOf("") }
    var landSize by remember { mutableStateOf("Hectare (2.47 Acres)") }
    var landPriceStr by remember { mutableStateOf("") }
    var landLocation by remember { mutableStateOf("") }
    var landDesc by remember { mutableStateOf("") }

    // Helper data
    val mockCereal = remember { listOf(
        ProduceListing(id = 2001, title = "White Maize Sacks", vegetableType = "Cereal", farmerName = "Kwame Boakye", farmerPhone = "+233241112233", quantity = 50.0, pricePerUnit = 180.0, location = "Tamale, Northern", collectionPoint = "Tamale Depot", description = "Sun-dried white maize grain, clean and bagged.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2002, title = "Organic Local Rice", vegetableType = "Cereal", farmerName = "Beatrice Ansah", farmerPhone = "+233208887766", quantity = 30.0, pricePerUnit = 320.0, location = "Techiman, Bono East", collectionPoint = "Coop Warehouse", description = "Fragrant long-grain local brown rice.", trustScore = 96, imageUrl = "res:img_rice"),
        ProduceListing(id = 2003, title = "Pearl Sorghum Grains", vegetableType = "Cereal", farmerName = "Iddrisu Musa", farmerPhone = "+233245550011", quantity = 40.0, pricePerUnit = 160.0, location = "Wa, Upper West", collectionPoint = "Wa Depot", description = "Traditional local sorghum grains, clean and bagged.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2004, title = "Millet Supreme Sacks", vegetableType = "Cereal", farmerName = "Alidu Moro", farmerPhone = "+233241234567", quantity = 35.0, pricePerUnit = 175.0, location = "Bolgatanga, Upper East", collectionPoint = "Bolga Lorry Station", description = "High fiber premium local millet.", trustScore = 94, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2005, title = "Yellow Corn Bumper", vegetableType = "Cereal", farmerName = "Yaw Asante", farmerPhone = "+233201112222", quantity = 80.0, pricePerUnit = 140.0, location = "Techiman, Bono East", collectionPoint = "Techiman Market", description = "Freshly dried yellow corn grains, great for poultry feed.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2006, title = "Ghana Long-Grain Rice", vegetableType = "Cereal", farmerName = "Evelyn Mensah", farmerPhone = "+233552223333", quantity = 45.0, pricePerUnit = 310.0, location = "Aveyime, Volta Region", collectionPoint = "Volta Rice Hub", description = "Polished long grain local white rice.", trustScore = 96, imageUrl = "res:img_rice"),
        ProduceListing(id = 2007, title = "Fonio Ancient Grain", vegetableType = "Cereal", farmerName = "Abena Osei", farmerPhone = "+233503334444", quantity = 15.0, pricePerUnit = 420.0, location = "Nkoranza, Bono East", collectionPoint = "Nkoranza Depot", description = "Extremely nutritious indigenous Fonio grains.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2008, title = "Millet Golden Grain", vegetableType = "Cereal", farmerName = "Bawa Ibrahim", farmerPhone = "+233204445555", quantity = 22.0, pricePerUnit = 190.0, location = "Bawku, Upper East", collectionPoint = "Bawku Depot", description = "Sweet golden grain local millet.", trustScore = 92, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2009, title = "Guinea Corn Seeds", vegetableType = "Cereal", farmerName = "Yakubu Danladi", farmerPhone = "+233245556666", quantity = 40.0, pricePerUnit = 165.0, location = "Tamale, Northern", collectionPoint = "Main Station", description = "Organic guinea corn, thoroughly winnowed.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2010, title = "Paddy Rice Sacks", vegetableType = "Cereal", farmerName = "Comfort Opoku", farmerPhone = "+233266667777", quantity = 90.0, pricePerUnit = 195.0, location = "Ejura, Ashanti", collectionPoint = "Ejura Market Hub", description = "Raw unhusked paddy rice ready for milling.", trustScore = 96, imageUrl = "res:img_rice"),
        ProduceListing(id = 2011, title = "Red Millet Grain", vegetableType = "Cereal", farmerName = "Abdulai Fuseini", farmerPhone = "+233547778888", quantity = 25.0, pricePerUnit = 185.0, location = "Yendi, Northern", collectionPoint = "Yendi Depot", description = "High-iron red millet grains, sun-dried.", trustScore = 93, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2012, title = "White Sorghum Premium", vegetableType = "Cereal", farmerName = "Fatima Alhassan", farmerPhone = "+233248889999", quantity = 55.0, pricePerUnit = 170.0, location = "Wa, Upper West", collectionPoint = "Wa Hub", description = "Cleaned white sorghum, optimal moisture content.", trustScore = 94, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2013, title = "Sweet Yellow Maize", vegetableType = "Cereal", farmerName = "Osei Kyeremeh", farmerPhone = "+233209990000", quantity = 60.0, pricePerUnit = 155.0, location = "Dormaa, Bono", collectionPoint = "Dormaa Lorry Park", description = "Premium quality yellow corn, sweet and dry.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2014, title = "Broken Rice Grains", vegetableType = "Cereal", farmerName = "Rebecca Mensah", farmerPhone = "+233551112222", quantity = 35.0, pricePerUnit = 220.0, location = "Aveyime, Volta Region", collectionPoint = "Mill Gate", description = "Affordable local broken rice, perfect for local dishes.", trustScore = 97, imageUrl = "res:img_rice"),
        ProduceListing(id = 2015, title = "Millet Flour Grain Sacks", vegetableType = "Cereal", farmerName = "Musa Zakari", farmerPhone = "+233502223333", quantity = 18.0, pricePerUnit = 240.0, location = "Navrongo, Upper East", collectionPoint = "Navrongo Depot", description = "Carefully stored high grade millet grains.", trustScore = 96, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400")
    )}
    val mockTuber = remember { listOf(
        ProduceListing(id = 2101, title = "Premium Pona Yam", vegetableType = "Tuber", farmerName = "Kofi Mensah", farmerPhone = "+233559876543", quantity = 40.0, pricePerUnit = 140.0, location = "Techiman, Bono East", collectionPoint = "Techiman Market", description = "Sweet, dry texture Grade-A yams.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2102, title = "Fresh Cassava Sacks", vegetableType = "Tuber", farmerName = "Abena Boateng", farmerPhone = "+233205554433", quantity = 60.0, pricePerUnit = 90.0, location = "Sunyani, Bono", collectionPoint = "Lorry Station", description = "Freshly harvested cassava tubers.", trustScore = 95, imageUrl = "res:img_cassava"),
        ProduceListing(id = 2103, title = "Organic Cocoyam", vegetableType = "Tuber", farmerName = "Kwabena Arthur", farmerPhone = "+233244112233", quantity = 25.0, pricePerUnit = 130.0, location = "Kumasi, Ashanti", collectionPoint = "Anloga Market", description = "Premium pink cocoyam tubers, fresh from farm.", trustScore = 96, imageUrl = "https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2104, title = "Sweet Sweet Potatoes", vegetableType = "Tuber", farmerName = "Vida Gyamfi", farmerPhone = "+233551239876", quantity = 50.0, pricePerUnit = 80.0, location = "Wenchi, Bono East", collectionPoint = "Wenchi Market", description = "Orange-fleshed sweet potatoes, extremely sweet.", trustScore = 94, imageUrl = "https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2105, title = "Water Yam Tubers", vegetableType = "Tuber", farmerName = "Stephen Ofori", farmerPhone = "+233245551111", quantity = 30.0, pricePerUnit = 110.0, location = "Kintampo, Bono East", collectionPoint = "Waterfalls Junction", description = "High moisture water yams, great for traditional dishes.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2106, title = "Red Yam Selection", vegetableType = "Tuber", farmerName = "Ama Serwaah", farmerPhone = "+233244445555", quantity = 35.0, pricePerUnit = 135.0, location = "Tuobodom, Bono East", collectionPoint = "Tuobodom Station", description = "High-quality red skin yams, highly nutritious.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2107, title = "Tiger Nuts (Atadwe)", vegetableType = "Tuber", farmerName = "Amma Dankwa", farmerPhone = "+233203334444", quantity = 15.0, pricePerUnit = 240.0, location = "Bodwesango, Ashanti", collectionPoint = "Bodwesango Depot", description = "Premium dried sweet tiger nuts.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2108, title = "Irish Potatoes Ghana", vegetableType = "Tuber", farmerName = "Kojo Antwi", farmerPhone = "+233557778888", quantity = 45.0, pricePerUnit = 180.0, location = "Nkoranza, Bono East", collectionPoint = "Coop Gate 2", description = "Fresh round white Irish potatoes.", trustScore = 96, imageUrl = "https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2109, title = "Ginger Root Sacks", vegetableType = "Tuber", farmerName = "Yaw Boateng", farmerPhone = "+233558889999", quantity = 28.0, pricePerUnit = 220.0, location = "Tanoso, Bono East", collectionPoint = "Tanoso Shed", description = "Highly aromatic fresh ginger root, super spicy.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 21010, title = "Yellow Yam Choice", vegetableType = "Tuber", farmerName = "Theresa Appiah", farmerPhone = "+233501237777", quantity = 22.0, pricePerUnit = 125.0, location = "Ejura, Ashanti", collectionPoint = "Ejura Hub", description = "Fleshy yellow yams, harvested fresh.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 21011, title = "Dried Cassava Chips (Kokonte)", vegetableType = "Tuber", farmerName = "Charles Sarpong", farmerPhone = "+233249991111", quantity = 60.0, pricePerUnit = 75.0, location = "Atebubu, Bono East", collectionPoint = "Atebubu Depot", description = "Sun-dried crispy cassava chips.", trustScore = 93, imageUrl = "res:img_cassava"),
        ProduceListing(id = 21012, title = "Taro Root Tubers", vegetableType = "Tuber", farmerName = "Grace Boateng", farmerPhone = "+233201235555", quantity = 20.0, pricePerUnit = 150.0, location = "Goaso, Ahafo Region", collectionPoint = "Goaso Hub", description = "Tender fresh taro root (cocoyam variety).", trustScore = 94, imageUrl = "https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 21013, title = "Cassava Flour Sacks", vegetableType = "Tuber", farmerName = "Daniel Mensah", farmerPhone = "+233502345678", quantity = 35.0, pricePerUnit = 160.0, location = "Sunyani, Bono", collectionPoint = "Lorry Park Gate A", description = "Finely milled local cassava starch/flour.", trustScore = 96, imageUrl = "res:img_cassava"),
        ProduceListing(id = 21014, title = "Hausa Potatoes", vegetableType = "Tuber", farmerName = "Bintu Moro", farmerPhone = "+233245678901", quantity = 15.0, pricePerUnit = 190.0, location = "Tamale, Northern", collectionPoint = "Tamale Depot", description = "Nutritious tiny sweet Hausa potatoes.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 21015, title = "Giant Forest Yams", vegetableType = "Tuber", farmerName = "Ebenezer Quansah", farmerPhone = "+233543332222", quantity = 18.0, pricePerUnit = 160.0, location = "Mampong, Ashanti", collectionPoint = "Mampong Hub", description = "Extra large forest soil Pona yams.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?auto=format&fit=crop&q=80&w=400")
    )}
    val mockWheat = remember { listOf(
        ProduceListing(id = 2201, title = "Local Spelt Wheat Sacks", vegetableType = "Wheat", farmerName = "Mary Appiah", farmerPhone = "+233205559999", quantity = 30.0, pricePerUnit = 250.0, location = "Koforidua, Eastern", collectionPoint = "Koforidua Market", description = "High-protein organically grown wheat.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2202, title = "Winter Red Wheat", vegetableType = "Wheat", farmerName = "Ekow Hammond", farmerPhone = "+233243338888", quantity = 25.0, pricePerUnit = 270.0, location = "Ejura, Ashanti", collectionPoint = "Ejura Depot", description = "Perfect red wheat grains, cleaned.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2203, title = "Golden Durum Wheat", vegetableType = "Wheat", farmerName = "Seth Owusu", farmerPhone = "+233208889999", quantity = 40.0, pricePerUnit = 295.0, location = "Techiman, Bono East", collectionPoint = "Market Gate 2", description = "Hard durum wheat suitable for pasta/bread.", trustScore = 96, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2204, title = "White Spelt Wheat Sacks", vegetableType = "Wheat", farmerName = "Agnes Mensah", farmerPhone = "+233551231111", quantity = 20.0, pricePerUnit = 260.0, location = "Sunyani, Bono", collectionPoint = "Shed 4", description = "Premium white spelt wheat grains.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2205, title = "High Protein Wheat Bran", vegetableType = "Wheat", farmerName = "Kwame Baah", farmerPhone = "+233245553333", quantity = 50.0, pricePerUnit = 130.0, location = "Kumasi, Ashanti", collectionPoint = "Kumasi Depot", description = "Finely separated high-protein wheat bran.", trustScore = 94, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2206, title = "Cracked Wheat Sacks", vegetableType = "Wheat", farmerName = "Salifu Moro", farmerPhone = "+233501112222", quantity = 35.0, pricePerUnit = 190.0, location = "Tamale, Northern", collectionPoint = "Lorry Park Depot", description = "Cracked local wheat grain, great for breakfast meals.", trustScore = 96, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2207, title = "Spring White Wheat", vegetableType = "Wheat", farmerName = "Cecilia Arthur", farmerPhone = "+233558882222", quantity = 15.0, pricePerUnit = 280.0, location = "Wenchi, Bono East", collectionPoint = "Wenchi Hub", description = "Premium quality spring white wheat, hand-picked.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2208, title = "Organic Heritage Wheat", vegetableType = "Wheat", farmerName = "Eric Dankwah", farmerPhone = "+233203456789", quantity = 18.0, pricePerUnit = 320.0, location = "Techiman, Bono East", collectionPoint = "Organic Guild", description = "Rare heirloom wheat varieties, organic.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2209, title = "Einkorn Ancient Wheat", vegetableType = "Wheat", farmerName = "Elizabeth Osei", farmerPhone = "+233246789012", quantity = 12.0, pricePerUnit = 350.0, location = "Nkoranza, Bono East", collectionPoint = "Nkoranza Depot", description = "Traditional Einkorn wheat grains, low gluten.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2210, title = "Emmer Grain Sacks", vegetableType = "Wheat", farmerName = "Isaac Appiah", farmerPhone = "+233541234567", quantity = 22.0, pricePerUnit = 310.0, location = "Ejura, Ashanti", collectionPoint = "Ejura Junction", description = "Nutritious Emmer grains, excellent for baking.", trustScore = 96, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2211, title = "Soft Pastry Wheat", vegetableType = "Wheat", farmerName = "Lucy Antwi", farmerPhone = "+233207771111", quantity = 30.0, pricePerUnit = 245.0, location = "Koforidua, Eastern", collectionPoint = "Koforidua Market", description = "Low gluten wheat grain optimized for pastries.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2212, title = "Ground Semolina Grain", vegetableType = "Wheat", farmerName = "Kwaku Addo", farmerPhone = "+233249002222", quantity = 45.0, pricePerUnit = 285.0, location = "Kumasi, Ashanti", collectionPoint = "Kejetia Hub", description = "Semolina quality cracked wheat grain.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2213, title = "Whole Grain Wheat Kernels", vegetableType = "Wheat", farmerName = "Beatrice Boateng", farmerPhone = "+233553334444", quantity = 55.0, pricePerUnit = 265.0, location = "Sunyani, Bono", collectionPoint = "Sunyani Hub", description = "Pure whole grain wheat kernels, untouched.", trustScore = 94, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2214, title = "Red Fife Wheat Sacks", vegetableType = "Wheat", farmerName = "Musa Danladi", farmerPhone = "+233204446666", quantity = 16.0, pricePerUnit = 330.0, location = "Tamale, Northern", collectionPoint = "Tamale Gate", description = "Red fife wheat variety, sun-dried.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2215, title = "Certified Seed Wheat", vegetableType = "Wheat", farmerName = "Johannes Aboagye", farmerPhone = "+233245556677", quantity = 25.0, pricePerUnit = 400.0, location = "Techiman, Bono East", collectionPoint = "Agri Depot", description = "High yield planting wheat grains.", trustScore = 99, imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400")
    )}
    val mockSeed = remember { listOf(
        ProduceListing(id = 2301, title = "Hybrid Maize Seeds", vegetableType = "Seed", farmerName = "Johannes Aboagye", farmerPhone = "+233245556677", quantity = 100.0, pricePerUnit = 450.0, location = "Techiman, Bono East", collectionPoint = "Extension Station", description = "Certified pest-resistant seeds.", trustScore = 99, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2302, title = "Ghana Cocoa Seeds", vegetableType = "Seed", farmerName = "Yaw Asante", farmerPhone = "+233203332211", quantity = 50.0, pricePerUnit = 550.0, location = "Kumasi, Ashanti", collectionPoint = "COCOBOD Hub", description = "F1 Cocoa beans ready for nursery.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1587132137056-bfbf0166836e?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2303, title = "Tomato F1 Gold Seeds", vegetableType = "Seed", farmerName = "Ama Serwaah", farmerPhone = "+233244445555", quantity = 30.0, pricePerUnit = 120.0, location = "Tuobodom, Bono East", collectionPoint = "Tuobodom Hub", description = "Disease-free premium hybrid tomato seeds.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2304, title = "Scotch Bonnet Hot Pepper Seeds", vegetableType = "Seed", farmerName = "Abena Mansa", farmerPhone = "+233242223333", quantity = 15.0, pricePerUnit = 95.0, location = "Kintampo, Bono East", collectionPoint = "Kintampo Hub", description = "Premium germinating spicy pepper seeds.", trustScore = 96, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2305, title = "Tender Okra Seeds", vegetableType = "Seed", farmerName = "Kofi Mensah", farmerPhone = "+233206667777", quantity = 40.0, pricePerUnit = 85.0, location = "Techiman, Bono East", collectionPoint = "Market Gate 4", description = "Pest resistant high yielding okra seeds.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2306, title = "Organic Cabbage Seeds", vegetableType = "Seed", farmerName = "Ekow Hammond", farmerPhone = "+233243338888", quantity = 25.0, pricePerUnit = 150.0, location = "Ejura, Ashanti", collectionPoint = "Ejura Depot", description = "High germination rates certified cabbage seeds.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2307, title = "Ghana White Rice Seeds", vegetableType = "Seed", farmerName = "Comfort Opoku", farmerPhone = "+233266667777", quantity = 60.0, pricePerUnit = 280.0, location = "Ejura, Ashanti", collectionPoint = "Ejura Hub", description = "High yield lowland paddy rice seeds.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2308, title = "Red Cowpea Seeds Pack", vegetableType = "Seed", farmerName = "Agnes Boateng", farmerPhone = "+233208887766", quantity = 35.0, pricePerUnit = 110.0, location = "Tamale, Northern", collectionPoint = "Tamale Depot", description = "Inoculated certified cowpea seeds.", trustScore = 96, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2309, title = "Sweet Watermelon Seeds", vegetableType = "Seed", farmerName = "Stephen Ofori", farmerPhone = "+233249001122", quantity = 22.0, pricePerUnit = 135.0, location = "Sunyani, Bono", collectionPoint = "Shed 2", description = "Certified Sugarbaby watermelon seeds.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2310, title = "Soybean Hybrid Seeds", vegetableType = "Seed", farmerName = "Alidu Moro", farmerPhone = "+233241234567", quantity = 80.0, pricePerUnit = 340.0, location = "Bolgatanga, Upper East", collectionPoint = "Bolga Station", description = "Pest resilient inoculant-ready soybean seeds.", trustScore = 94, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2311, title = "Bono Groundnut Seeds", vegetableType = "Seed", farmerName = "Stephen Ofori", farmerPhone = "+233249001122", quantity = 45.0, pricePerUnit = 180.0, location = "Sunyani, Bono", collectionPoint = "Shed 1", description = "High-oil variety groundnut seeds for planting.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2312, title = "Garden Egg Gold Seeds", vegetableType = "Seed", farmerName = "Yaw Boateng", farmerPhone = "+233558889999", quantity = 18.0, pricePerUnit = 115.0, location = "Tanoso, Bono East", collectionPoint = "Shed Gate 2", description = "Round white garden eggplant seeds.", trustScore = 96, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2313, title = "Carrot Nantes Seeds", vegetableType = "Seed", farmerName = "Grace Boateng", farmerPhone = "+233201235555", quantity = 12.0, pricePerUnit = 160.0, location = "Goaso, Ahafo Region", collectionPoint = "Goaso Hub", description = "High viability Nantes sweet carrot seeds.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2314, title = "Organic Onion Seeds", vegetableType = "Seed", farmerName = "Musa Zakari", farmerPhone = "+233502223333", quantity = 20.0, pricePerUnit = 195.0, location = "Bawku, Upper East", collectionPoint = "Bawku Station", description = "Galmi red premium onion seeds.", trustScore = 94, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2315, title = "Certified Sunflower Seeds", vegetableType = "Seed", farmerName = "Isaac Appiah", farmerPhone = "+233541234567", quantity = 30.0, pricePerUnit = 210.0, location = "Ejura, Ashanti", collectionPoint = "Ejura Hub", description = "High-yield oilseed sunflower seeds.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1551754625-702917c30418?auto=format&fit=crop&q=80&w=400")
    )}
    val mockLegumes = remember { listOf(
        ProduceListing(id = 2401, title = "Red Cowpea Sacks", vegetableType = "Legumes", farmerName = "Agnes Boateng", farmerPhone = "+233208887766", quantity = 35.0, pricePerUnit = 210.0, location = "Tamale, Northern", collectionPoint = "Tamale Station", description = "Cleaned organic cowpeas.", trustScore = 96, imageUrl = "https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2402, title = "Bono Groundnuts Sacks", vegetableType = "Legumes", farmerName = "Stephen Ofori", farmerPhone = "+233249001122", quantity = 40.0, pricePerUnit = 280.0, location = "Sunyani, Bono", collectionPoint = "Bono Depot", description = "Premium dried local groundnuts.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2403, title = "Bambara Groundnuts Sacks", vegetableType = "Legumes", farmerName = "Alidu Moro", farmerPhone = "+233241234567", quantity = 25.0, pricePerUnit = 320.0, location = "Bolgatanga, Upper East", collectionPoint = "Lorry Park Gate A", description = "Traditional highly nutritious Bambara beans.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2404, title = "Yellow Soybeans Bumper", vegetableType = "Legumes", farmerName = "Musa Danladi", farmerPhone = "+233204446666", quantity = 50.0, pricePerUnit = 190.0, location = "Tamale, Northern", collectionPoint = "Depot Shed 1", description = "High protein soybeans, thoroughly sorted.", trustScore = 94, imageUrl = "https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2405, title = "Pigeon Peas Bags", vegetableType = "Legumes", farmerName = "Bawa Ibrahim", farmerPhone = "+233204445555", quantity = 20.0, pricePerUnit = 240.0, location = "Bawku, Upper East", collectionPoint = "Bawku Hub", description = "Premium sun-dried pigeon pea grains.", trustScore = 93, imageUrl = "https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2406, title = "White Cowpea Sacks", vegetableType = "Legumes", farmerName = "Fatima Alhassan", farmerPhone = "+233248889999", quantity = 45.0, pricePerUnit = 215.0, location = "Wa, Upper West", collectionPoint = "Wa Depot", description = "Clean white cowpeas, high germination quality.", trustScore = 96, imageUrl = "https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2407, title = "Broad Fava Beans", vegetableType = "Legumes", farmerName = "Isaac Appiah", farmerPhone = "+233541234567", quantity = 15.0, pricePerUnit = 260.0, location = "Ejura, Ashanti", collectionPoint = "Ejura Depot", description = "Fleshy organic fava beans, sun-dried.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2408, title = "Green Lentils Premium", vegetableType = "Legumes", farmerName = "Agnes Mensah", farmerPhone = "+233551231111", quantity = 30.0, pricePerUnit = 290.0, location = "Sunyani, Bono", collectionPoint = "Lorry Park Gate B", description = "Imported quality green lentils grown locally.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2409, title = "Lima Beans Premium", vegetableType = "Legumes", farmerName = "Comfort Opoku", farmerPhone = "+233266667777", quantity = 18.0, pricePerUnit = 230.0, location = "Ejura, Ashanti", collectionPoint = "Ejura Station", description = "Butter-rich large lima beans.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2410, title = "Black Eyed Peas Choice", vegetableType = "Legumes", farmerName = "Amma Dankwa", farmerPhone = "+233203334444", quantity = 38.0, pricePerUnit = 225.0, location = "Koforidua, Eastern", collectionPoint = "Koforidua Hub", description = "Traditional black eyed pea grains.", trustScore = 96, imageUrl = "https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2411, title = "Kidney Beans Red Sacks", vegetableType = "Legumes", farmerName = "Kwaku Addo", farmerPhone = "+233249002222", quantity = 22.0, pricePerUnit = 310.0, location = "Kumasi, Ashanti", collectionPoint = "Anloga Shed", description = "Dark red kidney beans, high starch.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2412, title = "Mung Beans Organic", vegetableType = "Legumes", farmerName = "Grace Boateng", farmerPhone = "+233201235555", quantity = 12.0, pricePerUnit = 270.0, location = "Goaso, Ahafo Region", collectionPoint = "Goaso Hub", description = "High protein green mung beans.", trustScore = 94, imageUrl = "https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2413, title = "Chickpeas Sacks Premium", vegetableType = "Legumes", farmerName = "Elizabeth Osei", farmerPhone = "+233246789012", quantity = 25.0, pricePerUnit = 340.0, location = "Nkoranza, Bono East", collectionPoint = "Nkoranza Gate 1", description = "Creamy round chickpeas, high yield variety.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2414, title = "String Green Beans Sacks", vegetableType = "Legumes", farmerName = "Abena Osei", farmerPhone = "+233503334444", quantity = 16.0, pricePerUnit = 150.0, location = "Wenchi, Bono East", collectionPoint = "Wenchi Depot", description = "Freshly harvested organic green string beans.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2415, title = "Bambara Gold Sacks", vegetableType = "Legumes", farmerName = "Yaw Asante", farmerPhone = "+233203332211", quantity = 40.0, pricePerUnit = 330.0, location = "Techiman, Bono East", collectionPoint = "Coop Shed", description = "Premium selected yellow Bambara groundnuts.", trustScore = 99, imageUrl = "https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&q=80&w=400")
    )}
    val mockFruits = remember { listOf(
        ProduceListing(id = 2501, title = "Sweet Kent Mangoes", vegetableType = "Fruits", farmerName = "Abena Sarfo", farmerPhone = "+233245551122", quantity = 80.0, pricePerUnit = 80.0, location = "Techiman, Bono East", collectionPoint = "Highway Junction", description = "Fleshy organic Kent mangoes.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1601004890684-d8cbf643f5f2?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2502, title = "Sugarloaf Pineapples", vegetableType = "Fruits", farmerName = "Maud Adjaye", farmerPhone = "+233201112244", quantity = 150.0, pricePerUnit = 60.0, location = "Nsawam, Eastern", collectionPoint = "Nsawam Factory", description = "Highly demanded sweet pineapples.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1550258987-190a2d41a8ba?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2503, title = "Tuobodom Watermelons", vegetableType = "Fruits", farmerName = "Yaw Boateng", farmerPhone = "+233558889999", quantity = 120.0, pricePerUnit = 45.0, location = "Tuobodom, Bono East", collectionPoint = "Tuobodom Gate A", description = "Extra sweet large watermelons, freshly cut.", trustScore = 96, imageUrl = "https://images.unsplash.com/photo-1587049352846-4a222e784d38?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2504, title = "Golden Papaya (Copo)", vegetableType = "Fruits", farmerName = "Grace Boateng", farmerPhone = "+233201235555", quantity = 40.0, pricePerUnit = 55.0, location = "Goaso, Ahafo Region", collectionPoint = "Goaso Depot", description = "Firm and sweet local yellow papaya.", trustScore = 94, imageUrl = "https://images.unsplash.com/photo-1517282009859-f000ec3b26fe?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2505, title = "Sunyani Sweet Oranges", vegetableType = "Fruits", farmerName = "Agnes Mensah", farmerPhone = "+233551231111", quantity = 200.0, pricePerUnit = 35.0, location = "Sunyani, Bono", collectionPoint = "Sunyani Market", description = "Juicy seedless sweet oranges, crates of 100.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1547514701-42782101795e?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2506, title = "Bono Cavendish Bananas", vegetableType = "Fruits", farmerName = "Osei Kyeremeh", farmerPhone = "+233209990000", quantity = 90.0, pricePerUnit = 40.0, location = "Dormaa, Bono", collectionPoint = "Dormaa Depot", description = "Large Cavendish yellow bananas, premium bunch.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2507, title = "Grade-A Hass Avocados", vegetableType = "Fruits", farmerName = "Kojo Antwi", farmerPhone = "+233557778888", quantity = 70.0, pricePerUnit = 120.0, location = "Mampong, Ashanti", collectionPoint = "Mampong Hub", description = "Rich buttery organic Hass avocados.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2508, title = "Coastal Sweet Coconuts", vegetableType = "Fruits", farmerName = "Ebenezer Quansah", farmerPhone = "+233543332222", quantity = 300.0, pricePerUnit = 2.5, location = "Saltpond, Central Region", collectionPoint = "Coastal Highway Shed", description = "Fresh sweet green coconuts (Kuboo) loaded with juice.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1584270354949-c26b0d5b4a0c?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2509, title = "Pink Guava Basket", vegetableType = "Fruits", farmerName = "Comfort Opoku", farmerPhone = "+233266667777", quantity = 50.0, pricePerUnit = 50.0, location = "Ejura, Ashanti", collectionPoint = "Ejura Depot", description = "Highly fragrant pink local guavas.", trustScore = 96, imageUrl = "https://images.unsplash.com/photo-1534080391025-a17c68b7470c?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2510, title = "Wenchi Seedless Limes", vegetableType = "Fruits", farmerName = "Elizabeth Osei", farmerPhone = "+233246789012", quantity = 60.0, pricePerUnit = 70.0, location = "Wenchi, Bono East", collectionPoint = "Wenchi Market", description = "Highly acidic large seedless green limes.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1590502593747-42a996133562?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2511, title = "Yellow Acidic Lemons", vegetableType = "Fruits", farmerName = "Kofi Mensah", farmerPhone = "+233206667777", quantity = 40.0, pricePerUnit = 85.0, location = "Techiman, Bono East", collectionPoint = "Lorry Station 4", description = "Juicy large yellow lemons, high vitamin C.", trustScore = 96, imageUrl = "https://images.unsplash.com/photo-1608686207856-001b95cf60ca?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2512, title = "Kintampo Wild Passionfruit", vegetableType = "Fruits", farmerName = "Abena Mansa", farmerPhone = "+233242223333", quantity = 35.0, pricePerUnit = 110.0, location = "Kintampo, Bono East", collectionPoint = "Waterfalls Shed", description = "Very fragrant wild yellow passionfruits.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1527661591475-527312dd65f5?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2513, title = "Sweet Local Tangerines", vegetableType = "Fruits", farmerName = "Daniel Mensah", farmerPhone = "+233502345678", quantity = 110.0, pricePerUnit = 45.0, location = "Sunyani, Bono", collectionPoint = "Shed Gate 1", description = "Sweet thin-skinned local tangerines.", trustScore = 94, imageUrl = "https://images.unsplash.com/photo-1591522031776-81534f553f19?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2514, title = "African Star Apple (Alasa)", vegetableType = "Fruits", farmerName = "Maame Yaa", farmerPhone = "+233201119999", quantity = 45.0, pricePerUnit = 60.0, location = "Techiman, Bono East", collectionPoint = "Highway Walk", description = "Sour-sweet local premium Alasa fruits, freshly plucked.", trustScore = 97, imageUrl = "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?auto=format&fit=crop&q=80&w=400"),
        ProduceListing(id = 2515, title = "Sweet Forest Jackfruit", vegetableType = "Fruits", farmerName = "Charles Sarpong", farmerPhone = "+233249991111", quantity = 15.0, pricePerUnit = 140.0, location = "Goaso, Ahafo Region", collectionPoint = "Goaso Hub", description = "Giant forest jackfruit, extremely sweet and pulpy.", trustScore = 95, imageUrl = "https://images.unsplash.com/photo-1590855325852-c3da0f17109a?auto=format&fit=crop&q=80&w=400")
    )}

    val electronicEquip = remember { mutableStateListOf(
        mapOf("name" to "Solar-Powered Irrigation Pump", "price" to 1800.0, "isRent" to true, "rent" to 150.0, "loc" to "Techiman", "dealer" to "AgriTech Ghana", "rating" to 4.9, "desc" to "Solar pump with drip kit for up to 1 hectare.", "imageUrl" to "res:img_irrigation_pump"),
        mapOf("name" to "Digital Crop Moisture Meter", "price" to 450.0, "isRent" to false, "rent" to 0.0, "loc" to "Accra", "dealer" to "AgroElectronics", "rating" to 4.7, "desc" to "Crop moisture detector for grains.", "imageUrl" to "https://images.unsplash.com/photo-1557223562-6c77ef16210f?auto=format&fit=crop&q=80&w=400")
    )}
    val nonElectronicEquip = remember { mutableStateListOf(
        mapOf("name" to "Stainless Steel Cutlass", "price" to 75.0, "isRent" to false, "rent" to 0.0, "loc" to "Techiman", "dealer" to "Coop Store", "rating" to 4.9, "desc" to "Genuine durable high-carbon steel cutlass.", "imageUrl" to "res:img_cutlass"),
        mapOf("name" to "Manual Hand-Push Seed Seeder", "price" to 320.0, "isRent" to true, "rent" to 30.0, "loc" to "Sunyani", "dealer" to "Bono Tools", "rating" to 4.8, "desc" to "Consistently sows maize/cowpeas without bending.", "imageUrl" to "https://images.unsplash.com/photo-1593113598332-cd288d649433?auto=format&fit=crop&q=80&w=400")
    )}

    val extOfficers = remember { listOf(
        mapOf("name" to "Dr. Emmanuel Osei", "spec" to "Soil Nutrition & Crop Health", "loc" to "Techiman, Bono East", "rating" to "4.9", "exp" to "8 yrs", "bio" to "Assisting growers with organic soil enrichment & fertilizer pre-testing.", "imageUrl" to "https://images.unsplash.com/photo-1573164713988-8665fc963095?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Agnes Boateng", "spec" to "Irrigation & Sustainable Farms", "loc" to "Sunyani, Bono", "rating" to "4.8", "exp" to "5 yrs", "bio" to "Specialist in drip irrigation layouts and dry-season water planning.", "imageUrl" to "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Kofi Mensah", "spec" to "Post-Harvest Management", "loc" to "Techiman, Bono East", "rating" to "4.7", "exp" to "6 yrs", "bio" to "Cereal and grains storage specialist, focused on minimizing insect infestations.", "imageUrl" to "https://images.unsplash.com/photo-1573164713988-8665fc963095?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Ama Serwaah", "spec" to "Organic Vegetable Breeding", "loc" to "Tuobodom, Bono East", "rating" to "4.9", "exp" to "9 yrs", "bio" to "Organic tomato and pepper specialist, guiding hybrid greenhouse seeds.", "imageUrl" to "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Yaw Asante", "spec" to "Agribusiness Finance Advisor", "loc" to "Kumasi, Ashanti", "rating" to "4.8", "exp" to "7 yrs", "bio" to "Guides cooperative groups in micro-financing and Mobile Money escrow.", "imageUrl" to "https://images.unsplash.com/photo-1573164713988-8665fc963095?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Abena Boateng", "spec" to "Agroforestry & Tree Crops", "loc" to "Sunyani, Bono", "rating" to "4.6", "exp" to "4 yrs", "bio" to "Specialist in intercropping yams with cocoa nursery and forest yams.", "imageUrl" to "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Kwabena Arthur", "spec" to "Sustainable Soil Hydrology", "loc" to "Kumasi, Ashanti", "rating" to "4.8", "exp" to "10 yrs", "bio" to "Drip irrigation and rainwater harvesting system architecture.", "imageUrl" to "https://images.unsplash.com/photo-1573164713988-8665fc963095?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Vida Gyamfi", "spec" to "Pest & Disease Control Desk", "loc" to "Wenchi, Bono East", "rating" to "4.7", "exp" to "5 yrs", "bio" to "Aids smallholders with ecological pesticide formulations.", "imageUrl" to "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Stephen Ofori", "spec" to "Legume Nitrogen Fixation", "loc" to "Sunyani, Bono", "rating" to "4.9", "exp" to "11 yrs", "bio" to "Cowpea and peanut organic inoculation, maximizing yields.", "imageUrl" to "https://images.unsplash.com/photo-1573164713988-8665fc963095?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Yaw Boateng", "spec" to "Greenhouse Agronomics", "loc" to "Tanoso, Bono East", "rating" to "4.8", "exp" to "8 yrs", "bio" to "Expert in temperature-controlled nursery of high value pepper/tomato.", "imageUrl" to "https://images.unsplash.com/photo-1573164713988-8665fc963095?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Amma Dankwa", "spec" to "Root and Tuber Storage", "loc" to "Koforidua, Eastern", "rating" to "4.7", "exp" to "6 yrs", "bio" to "Guides in high shelf life dry yam stack building and storage boxes.", "imageUrl" to "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Kojo Antwi", "spec" to "Soil Biology Consultant", "loc" to "Nkoranza, Bono East", "rating" to "4.9", "exp" to "12 yrs", "bio" to "Composting, organic matter, and mycorrhiza application advisor.", "imageUrl" to "https://images.unsplash.com/photo-1573164713988-8665fc963095?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Grace Boateng", "spec" to "Cooperative Trade Specialist", "loc" to "Goaso, Ahafo Region", "rating" to "4.8", "exp" to "7 yrs", "bio" to "Advises farmers on direct buyer linking and market agreements.", "imageUrl" to "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Theresa Appiah", "spec" to "High-Value Horticulture", "loc" to "Ejura, Ashanti", "rating" to "4.6", "exp" to "5 yrs", "bio" to "Specialist advisor for export quality mangoes and pineapples.", "imageUrl" to "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Musa Zakari", "spec" to "Desert Crop Agronomist", "loc" to "Bawku, Upper East", "rating" to "4.9", "exp" to "14 yrs", "bio" to "Onion, sorghum and cowpea crop specialist under dry-arid regions.", "imageUrl" to "https://images.unsplash.com/photo-1573164713988-8665fc963095?auto=format&fit=crop&q=80&w=400")
    )}
    val vetOfficers = remember { listOf(
        mapOf("name" to "Dr. Kwaku Mensah, DVM", "spec" to "Poultry & Livestock Vaccination", "loc" to "Kumasi, Ashanti", "rating" to "4.9", "exp" to "12 yrs", "bio" to "Expert in intensive poultry biosecurity and livestock disease control.", "imageUrl" to "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Abena Sarfo, Vet", "spec" to "Ruminant Clinical Diagnostics", "loc" to "Wenchi, Bono", "rating" to "4.8", "exp" to "6 yrs", "bio" to "Provides mobile herd emergency checkups and nutrition design.", "imageUrl" to "https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Dr. Kwame Baah, DVM", "spec" to "Swine & Pork Biosecurity", "loc" to "Kumasi, Ashanti", "rating" to "4.7", "exp" to "8 yrs", "bio" to "Assists regional swine farmers with virus prevention and sanitary controls.", "imageUrl" to "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Salifu Moro, Vet Officer", "spec" to "Ruminant Preventive Medicine", "loc" to "Tamale, Northern", "rating" to "4.8", "exp" to "7 yrs", "bio" to "Advises cattle herdsmen on tsetse fly protection and worm expellers.", "imageUrl" to "https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Agnes Mensah, Vet Assistant", "spec" to "Poultry Broiler Hatchery Desk", "loc" to "Sunyani, Bono", "rating" to "4.6", "exp" to "5 yrs", "bio" to "Expert in incubator health, temperature controls, and broiler chicks feed.", "imageUrl" to "https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Seth Owusu, Vet Tech", "spec" to "Livestock Parasitology", "loc" to "Techiman, Bono East", "rating" to "4.8", "exp" to "9 yrs", "bio" to "Focuses on internal parasite diagnostics and drenching schedules.", "imageUrl" to "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Dr. Elizabeth Hammond", "spec" to "Dairy Herd Lactation Health", "loc" to "Ejura, Ashanti", "rating" to "4.9", "exp" to "11 yrs", "bio" to "Advises local dairy associations on sanitary milking and milk storage.", "imageUrl" to "https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Charles Sarpong, DVM", "spec" to "Goat & Sheep Breeding", "loc" to "Atebubu, Bono East", "rating" to "4.7", "exp" to "6 yrs", "bio" to "Provides veterinary breeding selections for highly resilient local breeds.", "imageUrl" to "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Maame Yaa, Vet Assistant", "spec" to "Rabbitry & Small Animals", "loc" to "Techiman, Bono East", "rating" to "4.8", "exp" to "4 yrs", "bio" to "Provides high-yield intensive rabbitry bio-sanitation advice.", "imageUrl" to "https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Dr. Eric Dankwah, DVM", "spec" to "Epidemic Response Taskforce", "loc" to "Techiman, Bono East", "rating" to "4.9", "exp" to "15 yrs", "bio" to "Main point of contact for livestock flu vaccination campaigns.", "imageUrl" to "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Cecilia Arthur, Vet Advisor", "spec" to "Poultry Feed Formulation", "loc" to "Wenchi, Bono East", "rating" to "4.7", "exp" to "8 yrs", "bio" to "Formulates highly efficient non-chemical feed mixes.", "imageUrl" to "https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Lucy Antwi, Vet Tech", "spec" to "Animal Wound Care Hub", "loc" to "Koforidua, Eastern", "rating" to "4.8", "exp" to "7 yrs", "bio" to "Mobile surgical technician for livestock emergency surgeries.", "imageUrl" to "https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Musa Danladi, Vet Specialist", "spec" to "Dry Region Herd Health", "loc" to "Tamale, Northern", "rating" to "4.9", "exp" to "13 yrs", "bio" to "Expert in nomadic cattle heat tolerance and foot-and-mouth mitigation.", "imageUrl" to "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Ekow Hammond, Vet", "spec" to "Commercial Layer Diagnostics", "loc" to "Ejura, Ashanti", "rating" to "4.6", "exp" to "5 yrs", "bio" to "Egg-laying rate optimizations and commercial flock deworming.", "imageUrl" to "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&q=80&w=400"),
        mapOf("name" to "Bintu Moro, Vet Officer", "spec" to "Livestock Hydro-Nutrition", "loc" to "Tamale, Northern", "rating" to "4.8", "exp" to "10 yrs", "bio" to "Guides cattle water sanitization setups in Bono East borders.", "imageUrl" to "https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?auto=format&fit=crop&q=80&w=400")
    )}

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp).border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(8.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (marketStep != "QUESTION") {
                        IconButton(onClick = { goBack() }, modifier = Modifier.size(36.dp).testTag("wizard_back_btn")) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color(0xFF064E3B))
                        }
                    }
                    Column {
                        Text("Agri-Market", fontSize = 14.sp, color = Color(0xFF475569))
                        Text(
                            text = when (marketStep) {
                                "QUESTION" -> "Choose Action"
                                "SELL" -> "Sell Directory"
                                "SELL_SUBCAT" -> "Select Subcategory"
                                "SELL_DYN" -> "List an Item"
                                "BUY" -> "Buy Directory"
                                "SUBCATEGORIES" -> {
                                    val cat = categories.find { it.id == selectedCategoryId }
                                    cat?.name ?: "Subcategories"
                                }
                                "ITEMS" -> {
                                    val sub = subcategories.find { it.id == selectedSubcategoryId }
                                    sub?.name ?: "Browse Items"
                                }
                                "PRODUCE" -> "Ghana Crop Produce"
                                "CEREAL" -> "Ghana Cereals"
                                "TUBER" -> "Ghana Tubers"
                                "WHEAT" -> "Ghana Wheat"
                                "SEED" -> "Ghana Seeds"
                                "LEGUMES" -> "Ghana Legumes"
                                "VEGETABLES" -> "Ghana Vegetables"
                                "FRUITS" -> "Ghana Fruits"
                                "EQUIP" -> "Equipment Categories"
                                "E_ELEC" -> "Electronic Equipment"
                                "E_NON" -> "Non-Electronic Tools"
                                "OFFICERS" -> "Consult Officers"
                                "O_EXT" -> "Extension Officers"
                                "O_VET" -> "Veterinary Surgeons"
                                "LOGISTICS" -> "Smart Logistics"
                                "LAND_SIZE" -> "Land Size"
                                "LAND_REGION" -> "Ghana 16 Regions"
                                "DETAIL" -> "Product Profile"
                                else -> marketStep
                            },
                            fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF064E3B)
                        )
                    }
                }
                if (marketStep != "QUESTION") {
                    IconButton(onClick = { backStack.clear(); marketStep = "QUESTION"; selectedItemDetail = null }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Home, "Home", tint = Color(0xFF059669))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            when (marketStep) {
                "QUESTION" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("What do you want to buy or sell?", fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Color(0xFF064E3B))
                        Text("Secure localized transactions integrated with the Ghana national agricultural register.", fontSize = 14.sp, color = Color(0xFF475569), textAlign = TextAlign.Center)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f).height(140.dp).clickable { navTo("BUY") }.testTag("buy_card"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                                border = BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                Column(modifier = Modifier.padding(14.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                                    Icon(Icons.Default.ShoppingBasket, null, tint = Color(0xFF065F46), modifier = Modifier.size(28.dp))
                                    Column {
                                        Text("I want to buy", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF064E3B))
                                        Text("Browse crops, land, tools.", fontSize = 13.sp, color = Color(0xFF047857))
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f).height(140.dp).clickable { navTo("SELL") }.testTag("sell_card"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                                border = BorderStroke(1.dp, Color(0xFFF97316))
                            ) {
                                Column(modifier = Modifier.padding(14.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                                    Icon(Icons.Default.AddBusiness, null, tint = Color(0xFF9A3412), modifier = Modifier.size(28.dp))
                                    Column {
                                        Text("I want to sell", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF7C2D12))
                                        Text("List produce & set pricing.", fontSize = 13.sp, color = Color(0xFFC2410C))
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        // Sleek, compact small box AI Advisor widget
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable { showLocalMarketplaceAiDialog = true }
                                .testTag("marketplace_ai_small_box"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = BorderStroke(1.5.dp, Color(0xFF86EFAC))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFDCFCE7), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.SupportAgent, "AI Agent", tint = Color(0xFF15803D), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Ask AI Agri-Assistant Agent", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF166534))
                                        Text("Tap to open live agricultural support chat", fontSize = 13.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Medium)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFBBF7D0), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("OPEN CHAT", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF14532D))
                                }
                            }
                        }
                    }
                }
                "SELL" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Sell Directory", fontWeight = FontWeight.Bold, color = Color(0xFF064E3B), fontSize = 19.sp)
                        Text("List your produce, equipment, services, or land directly onto the database-driven marketplace.", fontSize = 15.sp, color = Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        categories.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { cat ->
                                    val icon = when (cat.type) {
                                        "PRODUCE" -> Icons.Default.Agriculture
                                        "EQUIP" -> Icons.Default.Handyman
                                        "SERVICES" -> Icons.Default.SupportAgent
                                        "LOGISTICS" -> Icons.Default.LocalShipping
                                        "LAND" -> Icons.Default.Landscape
                                        else -> Icons.Default.ShoppingBasket
                                    }
                                    Card(
                                        modifier = Modifier.weight(1f).height(110.dp).clickable { 
                                            sellCategoryId = cat.id
                                            navTo("SELL_SUBCAT")
                                        },
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                                            Icon(icon, null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                                            Text("List " + cat.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF1E293B))
                                        }
                                    }
                                }
                                if (row.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                "SELL_SUBCAT" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val currentCat = categories.find { it.id == sellCategoryId }
                        Text("Select Subcategory for ${currentCat?.name}", fontWeight = FontWeight.Bold, color = Color(0xFF064E3B), fontSize = 19.sp)
                        Text("Which category does your item fit best?", fontSize = 15.sp, color = Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val filteredSubcats = subcategories.filter { it.categoryId == sellCategoryId }
                        
                        if (filteredSubcats.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No subcategories found.", color = Color(0xFF475569))
                            }
                        } else {
                            filteredSubcats.chunked(2).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    row.forEach { subcat ->
                                        Card(
                                            modifier = Modifier.weight(1f).height(80.dp).clickable { 
                                                sellSubcategoryId = subcat.id
                                                
                                                // Initialize default listing attributes
                                                sellTitle = ""
                                                sellPrice = ""
                                                sellUnit = when (sellCategoryId) {
                                                    1 -> "bag"
                                                    2 -> "day"
                                                    3 -> "hr"
                                                    4 -> "km"
                                                    5 -> "acre"
                                                    else -> "piece"
                                                }
                                                sellQuantity = "1"
                                                sellLocation = "Techiman"
                                                sellRegion = "Bono East"
                                                sellDescription = ""
                                                sellIsOrganic = false
                                                sellSellerName = "Kofi Mensah"
                                                sellSellerPhone = "+233 24 555 1111"
                                                navTo("SELL_DYN")
                                            },
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.CenterStart) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Text(subcat.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF064E3B))
                                                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFCBD5E1))
                                                }
                                            }
                                        }
                                    }
                                    if (row.size < 2) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                "SELL_DYN" -> {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val subcatObj = subcategories.find { it.id == sellSubcategoryId }
                        Text("List New ${subcatObj?.name}", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Color(0xFF064E3B))
                        
                        // Picture section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(100.dp).background(Color(0xFFCBD5E1), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AddAPhoto, null, tint = Color.White, modifier = Modifier.size(40.dp))
                                }
                                Text("Add Photos / Live Capture", fontSize = 14.sp, color = Color(0xFF475569))
                            }
                        }

                        OutlinedTextField(
                            value = sellTitle,
                            onValueChange = { sellTitle = it },
                            label = { Text("Product/Service Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = sellPrice,
                                onValueChange = { sellPrice = it },
                                label = { Text("Price (GHS)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = sellUnit,
                                onValueChange = { sellUnit = it },
                                label = { Text("Unit (e.g. bag, hr)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = sellQuantity,
                                onValueChange = { sellQuantity = it },
                                label = { Text("Quantity Available") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = sellLocation,
                                onValueChange = { sellLocation = it },
                                label = { Text("Town") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = sellRegion,
                            onValueChange = { sellRegion = it },
                            label = { Text("Ghana Region") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = sellDescription,
                            onValueChange = { sellDescription = it },
                            label = { Text("Detailed Specifications & Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )

                        // Category Specific Fields
                        if (sellCategoryId == 1) { // Produce
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = sellIsOrganic, onCheckedChange = { sellIsOrganic = it })
                                Text("This crop is Certified Organic (Non-chemical)", fontSize = 15.sp)
                            }
                        } else if (sellCategoryId == 2) { // Equipment
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = sellIsRent, onCheckedChange = { sellIsRent = it })
                                Text("This equipment is for RENT (Check for lease rate)", fontSize = 15.sp)
                            }
                        } else if (sellCategoryId == 3) { // Services
                            OutlinedTextField(
                                value = sellQualifications,
                                onValueChange = { sellQualifications = it },
                                label = { Text("Professional Qualifications") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = sellLanguages,
                                onValueChange = { sellLanguages = it },
                                label = { Text("Spoken Languages") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (sellCategoryId == 4) { // Logistics
                            OutlinedTextField(
                                value = sellCapacity,
                                onValueChange = { sellCapacity = it },
                                label = { Text("Vehicle Type & Carrying Capacity") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (sellCategoryId == 5) { // Farmland
                            OutlinedTextField(
                                value = sellSoilType,
                                onValueChange = { sellSoilType = it },
                                label = { Text("Soil Type") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = sellWaterAvailability,
                                onValueChange = { sellWaterAvailability = it },
                                label = { Text("Water Source / Accessibility") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Seller Identity Info", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        OutlinedTextField(
                            value = sellSellerName,
                            onValueChange = { sellSellerName = it },
                            label = { Text("Seller Full Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = sellSellerPhone,
                            onValueChange = { sellSellerPhone = it },
                            label = { Text("Seller Contact Phone") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (sellTitle.isNotBlank() && sellPrice.isNotBlank()) {
                                    val finalItem = MarketplaceItem(
                                        categoryId = sellCategoryId ?: 1,
                                        subcategoryId = sellSubcategoryId ?: 1,
                                        name = sellTitle,
                                        price = sellPrice.toDoubleOrNull() ?: 0.0,
                                        unit = sellUnit,
                                        quantity = sellQuantity.toDoubleOrNull() ?: 1.0,
                                        description = sellDescription,
                                        location = sellLocation,
                                        region = sellRegion,
                                        imageUrl = "https://images.unsplash.com/photo-1595855759920-86582396756a?auto=format&fit=crop&q=80&w=400",
                                        isOrganic = sellIsOrganic,
                                        sellerName = sellSellerName,
                                        sellerPhone = sellSellerPhone,
                                        soilType = sellSoilType,
                                        waterAvailability = sellWaterAvailability,
                                        capacity = sellCapacity,
                                        sellerQualifications = sellQualifications,
                                        sellerLanguages = sellLanguages
                                    )
                                    viewModel.addMarketplaceItem(finalItem)
                                    Toast.makeText(context, "Listing Posted Successfully! Logged to Agri-Ledger.", Toast.LENGTH_LONG).show()
                                    backStack.clear()
                                    marketStep = "QUESTION"
                                } else {
                                    Toast.makeText(context, "Please fill in Product Name and Price.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Post Product on Agri-Ledger", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
                "SELL_PRODUCE" -> {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { goBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                            Text("Sell Farm Produce", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Color(0xFF064E3B))
                        }
                        
                        // Picture capturing / upload section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                CropImage(selectedImageUrl, vegetableType, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    OutlinedButton(onClick = {
                                        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.CAMERA
                                        )
                                        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                            try {
                                                cameraLauncher.launch(null)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Failed to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        }
                                    }) {
                                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Take Photo", fontSize = 13.sp)
                                    }
                                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Upload Photo", fontSize = 13.sp)
                                    }
                                    OutlinedButton(onClick = { showPhotoSelector = true }) {
                                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Presets", fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Product Title") }, modifier = Modifier.fillMaxWidth())
                        
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Cassava", "Rice", "Tomatoes", "Okra", "Garden Eggs", "Peppers").forEach { opt ->
                                FilterChip(
                                    selected = vegetableType == opt,
                                    onClick = {
                                        vegetableType = opt
                                        selectedImageUrl = when (opt) {
                                            "Cassava" -> "res:img_cassava"
                                            "Rice" -> "res:img_rice"
                                            "Tomatoes" -> "https://images.unsplash.com/photo-1595855759920-86582396756a?auto=format&fit=crop&q=80&w=400"
                                            "Okra" -> "https://images.unsplash.com/photo-1628155930542-3c7a64e2c833?auto=format&fit=crop&q=80&w=400"
                                            else -> "res:img_farmland"
                                        }
                                        val rec = marketPrices.find { it.vegetableType == opt }
                                        if (rec != null) priceStr = rec.recommendedPrice.toString()
                                    },
                                    label = { Text(opt) }
                                )
                            }
                        }
                        OutlinedTextField(value = quantityStr, onValueChange = { quantityStr = it }, label = { Text("Quantity (Bags/Crates)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("Price per unit (GHS)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Farm Location") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = collectionPoint, onValueChange = { collectionPoint = it }, label = { Text("Collection Junction") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())

                        Button(
                            onClick = {
                                if (title.isNotBlank() && quantityStr.isNotBlank() && priceStr.isNotBlank()) {
                                    viewModel.addProduceListing(
                                        title, vegetableType, quantityStr.toDoubleOrNull() ?: 10.0,
                                        priceStr.toDoubleOrNull() ?: 100.0, if (location.isBlank()) "Techiman" else location,
                                        if (collectionPoint.isBlank()) "Depot" else collectionPoint, description,
                                        selectedImageUrl
                                    )
                                    Toast.makeText(context, "Produce listed successfully!", Toast.LENGTH_SHORT).show()
                                    goBack()
                                } else {
                                    Toast.makeText(context, "Fill in required fields.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Post on Agri-Ledger", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "SELL_EQUIP" -> {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { goBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                            Text("List Equipment for Rent or Sale", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Color(0xFF064E3B))
                        }

                        // Image uploading / presets
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                CropImage(selectedImageUrl, "equipment", modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    OutlinedButton(onClick = {
                                        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.CAMERA
                                        )
                                        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                            try {
                                                cameraLauncher.launch(null)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Failed to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        }
                                    }) {
                                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Take Photo", fontSize = 13.sp)
                                    }
                                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Upload Photo", fontSize = 13.sp)
                                    }
                                    OutlinedButton(onClick = {
                                        selectedImageUrl = "res:img_irrigation_pump"
                                        Toast.makeText(context, "Selected preset equipment image!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Preset", fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(value = equipName, onValueChange = { equipName = it }, label = { Text("Equipment Name (e.g., Irrigation Pump)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = equipDealer, onValueChange = { equipDealer = it }, label = { Text("Dealer Name / Owner") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = equipLocation, onValueChange = { equipLocation = it }, label = { Text("Available Location") }, modifier = Modifier.fillMaxWidth())
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Rent out equipment?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Switch(checked = equipIsRent, onCheckedChange = { equipIsRent = it })
                        }

                        if (equipIsRent) {
                            OutlinedTextField(value = equipRentRateStr, onValueChange = { equipRentRateStr = it }, label = { Text("Rental Rate (GHS/day)") }, modifier = Modifier.fillMaxWidth())
                        } else {
                            OutlinedTextField(value = equipPriceStr, onValueChange = { equipPriceStr = it }, label = { Text("Outright Sale Price (GHS)") }, modifier = Modifier.fillMaxWidth())
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Electronic", "Manual").forEach { type ->
                                Button(
                                    onClick = { equipType = type },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (equipType == type) Color(0xFF059669) else Color(0xFFE2E8F0),
                                        contentColor = if (equipType == type) Color.White else Color(0xFF475569)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(type)
                                }
                            }
                        }

                        OutlinedTextField(value = equipDesc, onValueChange = { equipDesc = it }, label = { Text("Description & Specifications") }, modifier = Modifier.fillMaxWidth())

                        Button(
                            onClick = {
                                if (equipName.isNotBlank() && equipLocation.isNotBlank()) {
                                    val newEquip = mapOf(
                                        "name" to equipName,
                                        "price" to (equipPriceStr.toDoubleOrNull() ?: 150.0),
                                        "isRent" to equipIsRent,
                                        "rent" to (equipRentRateStr.toDoubleOrNull() ?: 0.0),
                                        "loc" to equipLocation,
                                        "dealer" to (if (equipDealer.isBlank()) "Local Owner" else equipDealer),
                                        "rating" to 5.0,
                                        "desc" to equipDesc,
                                        "imageUrl" to selectedImageUrl
                                    )
                                    if (equipType == "Electronic") {
                                        electronicEquip.add(newEquip)
                                    } else {
                                        nonElectronicEquip.add(newEquip)
                                    }
                                    Toast.makeText(context, "Equipment listed successfully!", Toast.LENGTH_SHORT).show()
                                    goBack()
                                } else {
                                    Toast.makeText(context, "Fill in required fields.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Post Equipment", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "SELL_LOGISTICS" -> {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { goBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                            Text("Register Logistics Service", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Color(0xFF064E3B))
                        }

                        OutlinedTextField(value = driverName, onValueChange = { driverName = it }, label = { Text("Driver / Company Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = driverPhone, onValueChange = { driverPhone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = vehicleDesc, onValueChange = { vehicleDesc = it }, label = { Text("Vehicle Type (e.g., Kia Ceres, 3-Ton)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = driverLoc, onValueChange = { driverLoc = it }, label = { Text("Base Location (Town)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = baseRateStr, onValueChange = { baseRateStr = it }, label = { Text("Base Fare (GHS)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = ratePerKmStr, onValueChange = { ratePerKmStr = it }, label = { Text("Rate Per Kilometer (GHS/km)") }, modifier = Modifier.fillMaxWidth())

                        val coroutineScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                if (driverName.isNotBlank() && driverPhone.isNotBlank() && vehicleDesc.isNotBlank()) {
                                    val transporter = Transporter(
                                        name = driverName,
                                        phone = driverPhone,
                                        vehicle = vehicleDesc,
                                        rating = 5.0,
                                        trustScore = 100,
                                        ratePerKm = ratePerKmStr.toDoubleOrNull() ?: 15.0,
                                        baseRate = baseRateStr.toDoubleOrNull() ?: 120.0,
                                        etaMinutes = 15,
                                        completedDeliveries = 1,
                                        cancellationRate = 0
                                    )
                                    coroutineScope.launch {
                                        viewModel.insertTransporterDirectly(transporter)
                                        Toast.makeText(context, "Logistics vehicle registered successfully on local blockchain!", Toast.LENGTH_SHORT).show()
                                        goBack()
                                    }
                                } else {
                                    Toast.makeText(context, "Fill in required fields.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Register Driver", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "SELL_LAND" -> {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { goBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                            Text("Lease Out Farmland", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Color(0xFF064E3B))
                        }

                        // Image uploading / presets
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                CropImage(selectedImageUrl, "land", modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    OutlinedButton(onClick = {
                                        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.CAMERA
                                        )
                                        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                            try {
                                                cameraLauncher.launch(null)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Failed to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        }
                                    }) {
                                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Take Photo", fontSize = 13.sp)
                                    }
                                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Upload Photo", fontSize = 13.sp)
                                    }
                                    OutlinedButton(onClick = {
                                        selectedImageUrl = "res:img_farmland"
                                        Toast.makeText(context, "Selected preset farmland image!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Preset", fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(value = landTitle, onValueChange = { landTitle = it }, label = { Text("Land Listing Title (e.g., 2 Acres Fertile Land)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = landLocation, onValueChange = { landLocation = it }, label = { Text("Location (Town)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = landPriceStr, onValueChange = { landPriceStr = it }, label = { Text("Lease Price (GHS / year)") }, modifier = Modifier.fillMaxWidth())

                        Text("Select Land Size", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Plot (70 x 100 ft)", "Acre (43,560 sq ft)", "Hectare (2.47 Acres)", "Multi-Acre Plantation").forEach { size ->
                                FilterChip(
                                    selected = landSize == size,
                                    onClick = { landSize = size },
                                    label = { Text(size) }
                                )
                            }
                        }

                        OutlinedTextField(value = landDesc, onValueChange = { landDesc = it }, label = { Text("Description & Soil Qualities (e.g., Near water stream)") }, modifier = Modifier.fillMaxWidth())

                        Button(
                            onClick = {
                                if (landTitle.isNotBlank() && landLocation.isNotBlank() && landPriceStr.isNotBlank()) {
                                    viewModel.addProduceListing(
                                        title = "$landTitle ($landSize)",
                                        vegetableType = "Land",
                                        quantity = 1.0,
                                        pricePerUnit = landPriceStr.toDoubleOrNull() ?: 1500.0,
                                        location = landLocation,
                                        collectionPoint = "Direct Site",
                                        description = landDesc,
                                        imageUrl = selectedImageUrl
                                    )
                                    Toast.makeText(context, "Farmland lease listing posted successfully!", Toast.LENGTH_SHORT).show()
                                    goBack()
                                } else {
                                    Toast.makeText(context, "Fill in required fields.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Post Farmland", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "BUY" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Buy Directory", fontWeight = FontWeight.Bold, color = Color(0xFF064E3B), fontSize = 19.sp)
                        Text("Browse professional, database-driven agricultural categories loaded instantly from Room.", fontSize = 15.sp, color = Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        categories.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { cat ->
                                    val icon = when (cat.type) {
                                        "PRODUCE" -> Icons.Default.Agriculture
                                        "EQUIP" -> Icons.Default.Handyman
                                        "SERVICES" -> Icons.Default.SupportAgent
                                        "LOGISTICS" -> Icons.Default.LocalShipping
                                        "LAND" -> Icons.Default.Landscape
                                        else -> Icons.Default.ShoppingBasket
                                    }
                                    Card(
                                        modifier = Modifier.weight(1f).height(110.dp).clickable { 
                                            selectedCategoryId = cat.id
                                            navTo("SUBCATEGORIES")
                                        },
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                                            Icon(icon, null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                                            Text(cat.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF1E293B))
                                        }
                                    }
                                }
                                if (row.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                "SUBCATEGORIES" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val currentCat = categories.find { it.id == selectedCategoryId }
                        Text("Browse ${currentCat?.name ?: "Subcategories"}", fontWeight = FontWeight.Bold, color = Color(0xFF064E3B), fontSize = 19.sp)
                        Text("Select a specific subcategory to find listings.", fontSize = 15.sp, color = Color(0xFF475569))
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val filteredSubcats = subcategories.filter { it.categoryId == selectedCategoryId }
                        
                        if (filteredSubcats.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No subcategories found for this category.", color = Color(0xFF475569), fontSize = 16.sp)
                            }
                        } else {
                            filteredSubcats.chunked(2).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    row.forEach { subcat ->
                                        Card(
                                            modifier = Modifier.weight(1f).height(80.dp).clickable { 
                                                selectedSubcategoryId = subcat.id
                                                // Reset filters and search
                                                searchQuery = ""
                                                onlyOrganic = false
                                                onlyAvailableToday = false
                                                isNegotiableFilter = false
                                                navTo("ITEMS")
                                            },
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.CenterStart) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Text(subcat.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF064E3B))
                                                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFCBD5E1))
                                                }
                                            }
                                        }
                                    }
                                    if (row.size < 2) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                "ITEMS" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                        val subcat = subcategories.find { it.id == selectedSubcategoryId }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(subcat?.name ?: "Marketplace Items", fontWeight = FontWeight.Bold, color = Color(0xFF064E3B), fontSize = 19.sp)
                            val totalItems = marketplaceItems.count { it.subcategoryId == selectedSubcategoryId }
                            Text("$totalItems Listings", fontSize = 14.sp, color = Color(0xFF475569))
                        }

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search products...") },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF475569)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF059669),
                                focusedLabelColor = Color(0xFF059669)
                            ),
                            singleLine = true
                        )

                        // Filters Row
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = onlyOrganic,
                                onClick = { onlyOrganic = !onlyOrganic },
                                label = { Text("Organic Only", fontSize = 13.sp) }
                            )
                            FilterChip(
                                selected = onlyAvailableToday,
                                onClick = { onlyAvailableToday = !onlyAvailableToday },
                                label = { Text("Available Today", fontSize = 13.sp) }
                            )
                            FilterChip(
                                selected = isNegotiableFilter,
                                onClick = { isNegotiableFilter = !isNegotiableFilter },
                                label = { Text("Negotiable", fontSize = 13.sp) }
                            )
                        }

                        // Filtered List
                        val filteredItems = marketplaceItems.filter { item ->
                            item.subcategoryId == selectedSubcategoryId &&
                            (searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true) || item.description.contains(searchQuery, ignoreCase = true)) &&
                            (!onlyOrganic || item.isOrganic) &&
                            (!onlyAvailableToday || item.isAvailableToday) &&
                            (!isNegotiableFilter || item.isNegotiable)
                        }

                        ListMarketplaceItemsGrid(filteredItems) { item ->
                            selectedItemDetail = item
                            selectedItemType = "MARKETPLACE_ITEM"
                            navTo("DETAIL")
                        }
                    }
                }
                "PRODUCE" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select Crop Category", fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                        
                        val categories = listOf(
                            "Cereal" to "CEREAL", "Tuber" to "TUBER", "Wheat" to "WHEAT",
                            "Seed" to "SEED", "Legumes" to "LEGUMES", "Vegetables" to "VEGETABLES", "Fruits" to "FRUITS"
                        )

                        categories.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { (lbl, step) ->
                                    Card(
                                        modifier = Modifier.weight(1f).height(80.dp).clickable { navTo(step) },
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.CenterStart) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(lbl, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF064E3B))
                                                Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFCBD5E1))
                                            }
                                        }
                                    }
                                }
                                if (row.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                "CEREAL" -> { ListCropsGrid(mockCereal) { selectedItemDetail = it; selectedItemType = "CROP"; navTo("DETAIL") } }
                "TUBER" -> { ListCropsGrid(mockTuber) { selectedItemDetail = it; selectedItemType = "CROP"; navTo("DETAIL") } }
                "WHEAT" -> { ListCropsGrid(mockWheat) { selectedItemDetail = it; selectedItemType = "CROP"; navTo("DETAIL") } }
                "SEED" -> { ListCropsGrid(mockSeed) { selectedItemDetail = it; selectedItemType = "CROP"; navTo("DETAIL") } }
                "LEGUMES" -> { ListCropsGrid(mockLegumes) { selectedItemDetail = it; selectedItemType = "CROP"; navTo("DETAIL") } }
                "VEGETABLES" -> {
                    val userVegs = listings.filter { it.vegetableType in listOf("Tomatoes", "Okra", "Garden Eggs", "Peppers") }
                    val items = if (userVegs.isNotEmpty()) userVegs else listOf(
                        ProduceListing(id = 2601, title = "Bono Tomatoes Crate", vegetableType = "Tomatoes", farmerName = "Beatrice Ansah", farmerPhone = "+233208887766", quantity = 25.0, pricePerUnit = 120.0, location = "Techiman, Bono East", collectionPoint = "Coop Depot", description = "Fresh local red tomatoes.", trustScore = 98, imageUrl = "https://images.unsplash.com/photo-1595855759920-86582396756a?auto=format&fit=crop&q=80&w=400")
                    )
                    ListCropsGrid(items) { selectedItemDetail = it; selectedItemType = "CROP"; navTo("DETAIL") }
                }
                "FRUITS" -> { ListCropsGrid(mockFruits) { selectedItemDetail = it; selectedItemType = "CROP"; navTo("DETAIL") } }
                "EQUIP" -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(modifier = Modifier.weight(1f).height(120.dp).clickable { navTo("E_ELEC") }) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                                Icon(Icons.Default.Bolt, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                                Column {
                                    Text("Electronic", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text("Solar pumps, sensors.", fontSize = 12.sp, color = Color(0xFF475569))
                                }
                            }
                        }
                        Card(modifier = Modifier.weight(1f).height(120.dp).clickable { navTo("E_NON") }) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                                Icon(Icons.Default.Build, null, tint = Color(0xFF475569), modifier = Modifier.size(24.dp))
                                Column {
                                    Text("Manual Tools", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text("Cutlasses, seeders.", fontSize = 12.sp, color = Color(0xFF475569))
                                }
                            }
                        }
                    }
                }
                "E_ELEC" -> { ListEquipGrid(electronicEquip) { selectedItemDetail = it; selectedItemType = "EQUIP"; navTo("DETAIL") } }
                "E_NON" -> { ListEquipGrid(nonElectronicEquip) { selectedItemDetail = it; selectedItemType = "EQUIP"; navTo("DETAIL") } }
                "OFFICERS" -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(modifier = Modifier.weight(1f).height(120.dp).clickable { navTo("O_EXT") }) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                                Icon(Icons.Default.School, null, tint = Color(0xFF0D9488), modifier = Modifier.size(24.dp))
                                Text("Extension Officers", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            }
                        }
                        Card(modifier = Modifier.weight(1f).height(120.dp).clickable { navTo("O_VET") }) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                                Icon(Icons.Default.Pets, null, tint = Color.Red, modifier = Modifier.size(24.dp))
                                Text("Veterinarians", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            }
                        }
                    }
                }
                "O_EXT" -> { ListOfficersGrid(extOfficers) { selectedItemDetail = it; selectedItemType = "OFFICER"; navTo("DETAIL") } }
                "O_VET" -> { ListOfficersGrid(vetOfficers) { selectedItemDetail = it; selectedItemType = "OFFICER"; navTo("DETAIL") } }
                "LOGISTICS" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Available Transporters", fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                        transporters.forEach { carrier ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedItemDetail = mapOf(
                                        "name" to carrier.name,
                                        "vehicle" to carrier.vehicle,
                                        "loc" to "Techiman Hub",
                                        "ratePerKm" to carrier.ratePerKm,
                                        "baseRate" to carrier.baseRate,
                                        "desc" to "Verified Agri-carrier with logistics ledger tracking.",
                                        "imageUrl" to "res:img_farmland"
                                    )
                                    selectedItemType = "EQUIP"
                                    navTo("DETAIL")
                                },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(carrier.name, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                        Text(carrier.vehicle, fontSize = 14.sp, color = Color(0xFF475569))
                                        Text("Rate: GHS ${carrier.baseRate} + GHS ${carrier.ratePerKm}/km", fontSize = 13.sp, color = Color(0xFF475569))
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF475569))
                                }
                            }
                        }
                    }
                }
                "LAND_SIZE" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select Farm Land Size", fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                        
                        val lands = listOf(
                            "Hectare (2.47 Acres)" to "res:img_farmland",
                            "Plot (70 x 100 ft)" to "https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&q=80&w=400",
                            "Semi-Plot (50 x 70 ft)" to "res:img_farmland",
                            "All Sizes" to "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&q=80&w=400"
                        )

                        lands.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { (size, img) ->
                                    Card(
                                        modifier = Modifier.weight(1f).height(120.dp).clickable {
                                            selectedItemDetail = ProduceListing(
                                                id = 3001,
                                                title = "$size Premium Farmland",
                                                vegetableType = "Land",
                                                farmerName = "Techiman Agricultural Trust",
                                                farmerPhone = "+233245558888",
                                                quantity = 1.0,
                                                pricePerUnit = 2500.0,
                                                location = "Bono East Region",
                                                collectionPoint = "Highway entrance",
                                                description = "Fertile loamy soil perfect for cereals and tubers. Accessible by tractor with irrigation link.",
                                                trustScore = 99,
                                                imageUrl = img
                                            )
                                            selectedItemType = "LAND"
                                            navTo("DETAIL")
                                        },
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Column {
                                            CropImage(img, "farmland", modifier = Modifier.fillMaxWidth().height(70.dp))
                                            Box(modifier = Modifier.fillMaxWidth().padding(6.dp), contentAlignment = Alignment.CenterStart) {
                                                Text(size, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "LAND_REGION" -> {
                    // Regions selection fallback
                }
                "DETAIL" -> {
                    ProductDetailView(
                        item = selectedItemDetail,
                        type = selectedItemType,
                        showTransitAnalyzer = showTransitAnalyzer,
                        onToggleTransit = { showTransitAnalyzer = it },
                        transitStartHub = transitStartHub,
                        onTransitHubChange = { transitStartHub = it },
                        showCustomerCare = showCustomerCareChat,
                        onToggleCustomerCare = { showCustomerCareChat = it },
                        ccHistory = ccHistory,
                        onSendCcMessage = { msg ->
                            ccHistory.add("You" to msg)
                            ccHistory.add("Agent" to "Akwaaba! I've logged this trade. We are escrowing this with the farmer. An officer will ping you shortly.")
                        },
                        ratingValue = ratingValue,
                        onRatingChange = { ratingValue = it },
                        showRatingDialog = showRatingDialog,
                        onToggleRating = { showRatingDialog = it },
                        viewModel = viewModel,
                        context = context
                    )
                }
            }
        }
    }

    // Capture Simulation Modal
    if (simulationCameraActive) {
        AlertDialog(
            onDismissRequest = { simulationCameraActive = false },
            title = { Text("Live Photo Capture") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Align your cassava, tubers or cereals in the box:")
                    Box(
                        modifier = Modifier.size(200.dp).border(2.dp, Color(0xFF10B981), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CropImage(selectedImageUrl, vegetableType, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                        Box(modifier = Modifier.size(140.dp).border(1.dp, Color.White.copy(alpha = 0.5f)))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    simulationCameraActive = false
                    Toast.makeText(context, "Captured high-res crop photo!", Toast.LENGTH_SHORT).show()
                }) { Text("Capture Shutter") }
            },
            dismissButton = {
                TextButton(onClick = { simulationCameraActive = false }) { Text("Cancel") }
            }
        )
    }

    // Preset Photo Selection Modal
    if (showPhotoSelector) {
        AlertDialog(
            onDismissRequest = { showPhotoSelector = false },
            title = { Text("Select Crop Photo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val list = listOf(
                        "Cassava Tuber" to "res:img_cassava",
                        "Rice Grains" to "res:img_rice",
                        "Irrigation Pump" to "res:img_irrigation_pump",
                        "Steel Cutlass" to "res:img_cutlass",
                        "Fertile Farmland" to "res:img_farmland"
                    )
                    list.forEach { (lbl, img) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedImageUrl = img
                                showPhotoSelector = false
                            }
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                CropImage(img, "crop", modifier = Modifier.size(40.dp).clip(CircleShape))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(lbl, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoSelector = false }) { Text("Close") }
            }
        )
    }

    if (showLocalMarketplaceAiDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showLocalMarketplaceAiDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                border = BorderStroke(1.5.dp, Color(0xFF86EFAC))
            ) {
                ChatSheetContent(viewModel) {
                    showLocalMarketplaceAiDialog = false
                }
            }
        }
    }
}

@Composable
fun ListCropsGrid(list: List<ProduceListing>, onSelect: (ProduceListing) -> Unit) {
    val chunks = remember(list) { list.chunked(2) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        items(chunks) { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { crop ->
                    Card(
                        modifier = Modifier.weight(1f).clickable { onSelect(crop) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            CropImage(crop.imageUrl, crop.vegetableType, modifier = Modifier.fillMaxWidth().height(100.dp))
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(crop.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF064E3B))
                                Text("GHS ${crop.pricePerUnit}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF059669))
                                Text("Loc: ${crop.location}", fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1)
                                Text("Farmer: ${crop.farmerName}", fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1)
                            }
                        }
                    }
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ListMarketplaceItemsGrid(list: List<MarketplaceItem>, onSelect: (MarketplaceItem) -> Unit) {
    if (list.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(48.dp))
                Text("No items found under this subcategory", color = Color(0xFF475569), fontSize = 17.sp)
            }
        }
    } else {
        val chunks = remember(list) { list.chunked(2) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(chunks) { rowItems ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { item ->
                        Card(
                            modifier = Modifier.weight(1f).clickable { onSelect(item) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column {
                                Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                                    CropImage(item.imageUrl, if (item.categoryId == 1) "crop" else "item", modifier = Modifier.fillMaxSize())
                                    if (item.isOrganic) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .background(Color(0xFFDCFCE7), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("ORGANIC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                        }
                                    }
                                }
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(item.name, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF064E3B))
                                    Text("GHS ${item.price} / ${item.unit}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF059669))
                                    Text("Loc: ${item.location}, ${item.region}", fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1)
                                    Text("Seller: ${item.sellerName}", fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1)
                                }
                            }
                        }
                    }
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun ListEquipGrid(list: List<Map<String, Any>>, onSelect: (Map<String, Any>) -> Unit) {
    val chunks = remember(list) { list.chunked(2) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        items(chunks) { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { eq ->
                    Card(
                        modifier = Modifier.weight(1f).clickable { onSelect(eq) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            CropImage(eq["imageUrl"].toString(), "equipment", modifier = Modifier.fillMaxWidth().height(100.dp))
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(eq["name"].toString(), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF1E293B))
                                Text("GHS ${eq["price"]}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0D9488))
                                Text("Dealer: ${eq["dealer"]}", fontSize = 12.sp, color = Color(0xFF475569))
                            }
                        }
                    }
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ListOfficersGrid(list: List<Map<String, String>>, onSelect: (Map<String, String>) -> Unit) {
    val chunks = remember(list) { list.chunked(2) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        items(chunks) { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { officer ->
                    Card(
                        modifier = Modifier.weight(1f).clickable { onSelect(officer) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            CropImage(officer["imageUrl"] ?: "", "officer", modifier = Modifier.fillMaxWidth().height(100.dp))
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(officer["name"] ?: "", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, color = Color(0xFF1E293B))
                                Text(officer["spec"] ?: "", fontSize = 12.sp, color = Color(0xFF0D9488), maxLines = 1)
                                Text("Rating: ${officer["rating"]}★", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                            }
                        }
                    }
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ProductDetailView(
    item: Any?,
    type: String,
    showTransitAnalyzer: Boolean,
    onToggleTransit: (Boolean) -> Unit,
    transitStartHub: String,
    onTransitHubChange: (String) -> Unit,
    showCustomerCare: Boolean,
    onToggleCustomerCare: (Boolean) -> Unit,
    ccHistory: List<Pair<String, String>>,
    onSendCcMessage: (String) -> Unit,
    ratingValue: Int,
    onRatingChange: (Int) -> Unit,
    showRatingDialog: Boolean,
    onToggleRating: (Boolean) -> Unit,
    viewModel: GeoHarvestViewModel,
    context: android.content.Context
) {
    if (item == null) return

    val title = remember(item) {
        if (item is ProduceListing) item.title 
        else if (item is MarketplaceItem) item.name
        else (item as Map<String, Any>)["name"].toString()
    }
    val imageUrl = remember(item) {
        if (item is ProduceListing) item.imageUrl 
        else if (item is MarketplaceItem) item.imageUrl
        else (item as Map<String, Any>)["imageUrl"].toString()
    }
    val price = remember(item) {
        if (item is ProduceListing) item.pricePerUnit 
        else if (item is MarketplaceItem) item.price
        else (item as Map<String, Any>)["price"].toString().toDoubleOrNull() ?: 0.0
    }
    val farmerName = remember(item) {
        if (item is ProduceListing) item.farmerName 
        else if (item is MarketplaceItem) item.sellerName
        else (item as Map<String, Any>)["dealer"]?.toString() ?: (item as Map<String, Any>)["name"].toString()
    }
    val farmerPhone = remember(item) {
        if (item is ProduceListing) item.farmerPhone 
        else if (item is MarketplaceItem) item.sellerPhone
        else "+233 24 555 1111"
    }
    val location = remember(item) {
        if (item is ProduceListing) item.location 
        else if (item is MarketplaceItem) "${item.location}, ${item.region}"
        else (item as Map<String, Any>)["loc"].toString()
    }
    val description = remember(item) {
        if (item is ProduceListing) item.description 
        else if (item is MarketplaceItem) item.description
        else (item as Map<String, Any>)["desc"]?.toString() ?: ""
    }

    var showNegotiator by remember { mutableStateOf(false) }
    var negotiationOffer by remember { mutableStateOf("") }
    var negotiationStatus by remember { mutableStateOf("none") } // none, sent, countered, closed
    var farmerCounterPrice by remember { mutableStateOf(200) }

    var showBulkConsolidator by remember { mutableStateOf(false) }
    var bulkQuantityRequired by remember { mutableStateOf("500") }
    var bulkOrderPlaced by remember { mutableStateOf(false) }

    var showHarvestReservation by remember { mutableStateOf(false) }
    var reservationCrates by remember { mutableStateOf("10") }
    var harvestReservationPlaced by remember { mutableStateOf(false) }

    var showPriceAdvisor by remember { mutableStateOf(false) }
    var showMomoEscrow by remember { mutableStateOf(false) }
    var escrowPhone by remember { mutableStateOf("") }
    var escrowProvider by remember { mutableStateOf("MTN Mobile Money") }
    var escrowOtp by remember { mutableStateOf("") }
    var escrowStep by remember { mutableStateOf("FORM") } // FORM, OTP, SUCCESS
    var escrowAmount by remember { mutableStateOf(0.0) }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CropImage(imageUrl, type, modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)))
        
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, color = Color(0xFF064E3B))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Price", fontSize = 13.sp, color = Color(0xFF475569))
                    Text("GHS $price", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF059669))
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Location", fontSize = 13.sp, color = Color(0xFF475569))
                    Text(location, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF1E293B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Text(description, fontSize = 15.sp, color = Color.DarkGray)

        if (item is MarketplaceItem) {
            // Badges Row
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.isOrganic) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))) {
                        Text("🌿 ORGANIC", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                if (item.isNegotiable) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))) {
                        Text("🤝 NEGOTIABLE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                if (item.isAvailableToday) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE))) {
                        Text("⚡ AVAILABLE TODAY", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                if (item.isDeliveryAvailable) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF))) {
                        Text("🚚 DELIVERY AVAILABLE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }

            // Seller Profile Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Seller Information", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF064E3B))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AccountCircle, null, tint = Color(0xFF475569), modifier = Modifier.size(36.dp))
                        Column {
                            Text(item.sellerName.ifBlank { "Kofi Mensah" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                                Text(" ${item.sellerRating} • ${item.sellerCompletedJobs} Deals Completed", fontSize = 14.sp, color = Color(0xFF475569))
                            }
                        }
                    }
                    if (item.sellerQualifications.isNotBlank()) {
                        Text("Qualifications: ${item.sellerQualifications}", fontSize = 14.sp, color = Color.DarkGray)
                    }
                    if (item.sellerLanguages.isNotBlank()) {
                        Text("Languages: ${item.sellerLanguages}", fontSize = 14.sp, color = Color.DarkGray)
                    }
                }
            }

            // Category Specific Fields
            if (item.categoryId == 5) { // Farmland details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Farmland Specifications", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF475569))
                        Text("• Soil Type: ${item.soilType.ifBlank { "Sandy Loam" }}", fontSize = 14.sp)
                        Text("• Water Source: ${item.waterAvailability.ifBlank { "Borehole / River" }}", fontSize = 14.sp)
                        Text("• Irrigation Installed: ${if (item.irrigation.isNotBlank()) item.irrigation else "Yes"}", fontSize = 14.sp)
                        Text("• Tractor Accessibility: ${item.accessibility.ifBlank { "Excellent" }}", fontSize = 14.sp)
                    }
                }
            } else if (item.categoryId == 4) { // Logistics details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Logistics Information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF475569))
                        Text("• Vehicle Capacity: ${item.capacity.ifBlank { "Kia Ceres (3-Ton)" }}", fontSize = 14.sp)
                        Text("• Pricing: GHS ${item.pricePerKm} per km", fontSize = 14.sp)
                        Text("• Estimated Arrival: ${item.estimatedArrival.ifBlank { "15 mins" }}", fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Actions Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    viewModel.addEngagementActivity("Mary Appiah (You)", "Buyer", "Initiated negotiation with $farmerName")
                    Toast.makeText(context, "Initiated call to $farmerPhone. Logger registered on Agri-Ledger!", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B))
            ) {
                Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Contact Farmer", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { onToggleTransit(!showTransitAnalyzer) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Analyze Transportation Cost", fontWeight = FontWeight.Bold)
            }

            if (showTransitAnalyzer) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Transit Cost Calculator", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF78350F))
                        Text("Select your delivery destination Hub in Ghana:", fontSize = 14.sp)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Accra", "Kumasi", "Tamale", "Koforidua").forEach { hub ->
                                FilterChip(
                                    selected = transitStartHub == hub,
                                    onClick = { onTransitHubChange(hub) },
                                    label = { Text(hub, fontSize = 13.sp) }
                                )
                            }
                        }

                        val distance = remember(transitStartHub, location) {
                            if (transitStartHub == "Accra") 370 else if (transitStartHub == "Kumasi") 125 else if (transitStartHub == "Tamale") 280 else 320
                        }

                        Text("Calculated Distance from Farm to $transitStartHub: $distance km", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        listOf(
                            Triple("Aboboyaa (Tricycle)", 50.0, 4.0),
                            Triple("Kia Bongo (Light Truck)", 120.0, 8.0),
                            Triple("Hyundai Mighty (Heavy Truck)", 220.0, 12.0)
                        ).forEach { (v, base, perKm) ->
                            val total = base + (perKm * distance)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(v, fontSize = 14.sp)
                                Text("GHS $total", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFB45309))
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { onToggleCustomerCare(!showCustomerCare) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
            ) {
                Icon(Icons.Default.SupportAgent, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Contact Customer Care", fontWeight = FontWeight.Bold)
            }

            if (showCustomerCare) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDFA)),
                    border = BorderStroke(1.dp, Color(0xFF0D9488))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Trade Escrow & Customer Support", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF115E59))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ccHistory.forEach { (sender, txt) ->
                                Text("$sender: $txt", fontSize = 14.sp, fontWeight = if (sender == "You") FontWeight.Normal else FontWeight.Bold)
                            }
                        }

                        var input by remember { mutableStateOf("") }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                placeholder = { Text("Ask support...") },
                                modifier = Modifier.weight(1f).height(44.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                            )
                            IconButton(onClick = {
                                if (input.isNotBlank()) {
                                    onSendCcMessage(input)
                                    input = ""
                                }
                            }) {
                                Icon(Icons.Default.Send, null)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { onToggleRating(true) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B7280))
            ) {
                Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rate Product", fontWeight = FontWeight.Bold)
            }

            // --- A. NEGOTIATE PRICE ---
            Button(
                onClick = { showNegotiator = !showNegotiator },
                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("negotiate_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Icon(Icons.Default.ThumbUp, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Negotiate Price", fontWeight = FontWeight.Bold)
            }

            if (showNegotiator) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("negotiator_panel"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = BorderStroke(1.dp, Color(0xFF34D399))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🤝 Interactive Price Negotiator", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF064E3B))
                        Text("Ghana agrobusiness runs on negotiation! Propose an offer and seal the agreement on Agri-Ledger.", fontSize = 14.sp, color = Color.DarkGray)

                        if (negotiationStatus == "none") {
                            OutlinedTextField(
                                value = negotiationOffer,
                                onValueChange = { negotiationOffer = it },
                                label = { Text("Enter Your Offer (GHS)") },
                                placeholder = { Text("e.g. 180") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
                            )
                            Button(
                                onClick = {
                                    val offerVal = negotiationOffer.toDoubleOrNull()
                                    if (offerVal != null) {
                                        viewModel.addEngagementActivity("Mary Appiah (You)", "Buyer", "Proposed offer of GHS $offerVal for $title")
                                        negotiationStatus = "sent"
                                    } else {
                                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Submit Offer", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (negotiationStatus == "sent") {
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("You offered: GHS $negotiationOffer", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("Farmer $farmerName is evaluating...", fontSize = 14.sp, color = Color(0xFF475569))
                                }
                            }
                            Button(
                                onClick = {
                                    negotiationStatus = "countered"
                                    farmerCounterPrice = (negotiationOffer.toDoubleOrNull() ?: 180.0).toInt() + 20
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Receive Counter-Offer", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (negotiationStatus == "countered") {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Your Offer: GHS $negotiationOffer", fontSize = 14.sp, color = Color(0xFF475569))
                                    Text("Farmer's Counter: GHS $farmerCounterPrice", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF92400E))
                                    Text("Message: \"My produce is organic and premium. Let us meet in the middle at GHS $farmerCounterPrice!\"", fontSize = 14.sp, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        viewModel.addEngagementActivity("Mary Appiah (You)", "Buyer", "Accepted negotiated deal of GHS $farmerCounterPrice for $title")
                                        negotiationStatus = "closed"
                                        Toast.makeText(context, "Deal Sealed on Agri-Ledger at GHS $farmerCounterPrice!", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                ) {
                                    Text("Accept & Close Deal", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (negotiationStatus == "closed") {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16A34A))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Deal Closed! Agreement registered on Agri-Ledger at GHS $farmerCounterPrice.", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                }
                            }
                        }
                    }
                }
            }

            // --- B. BULK PURCHASE CONSOLIDATOR ---
            Button(
                onClick = { showBulkConsolidator = !showBulkConsolidator },
                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("bulk_purchase_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Icon(Icons.Default.Layers, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bulk Purchase Multi-Farmer", fontWeight = FontWeight.Bold)
            }

            if (showBulkConsolidator) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("bulk_panel"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFF60A5FA))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📦 Multi-Farmer Bulk Consolidator", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF1E3A8A))
                        Text("Buying in bulk for restaurant or export? Enter your requested volume (kg) and GeoHarvest will combine stock from multiple certified regional farmers into a single optimized shipping container.", fontSize = 14.sp, color = Color.DarkGray)

                        if (!bulkOrderPlaced) {
                            OutlinedTextField(
                                value = bulkQuantityRequired,
                                onValueChange = { bulkQuantityRequired = it },
                                label = { Text("Required Quantity (kg)") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("GeoHarvest AI Allocation Plan:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E3A8A))
                            Text("• Farmer Kofi Mensah (Techiman): 200 kg allocated", fontSize = 14.sp)
                            Text("• Farmer Beatrice Ansah (Techiman): 150 kg allocated", fontSize = 14.sp)
                            Text("• Farmer Abena Boateng (Sunyani): 150 kg allocated", fontSize = 14.sp)
                            Text("• Transit: Consolidated Truck departing Techiman Center Depot with unified GHS bill of lading", fontSize = 14.sp, color = Color(0xFF2563EB))

                            Button(
                                onClick = {
                                    bulkOrderPlaced = true
                                    viewModel.addEngagementActivity("Mary Appiah (You)", "Buyer", "Placed bulk consolidated order of $bulkQuantityRequired kg tomatoes")
                                    Toast.makeText(context, "Consolidated bulk order of $bulkQuantityRequired kg tomatoes placed!", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Order Consolidated Bulk Shipment", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, null, tint = Color(0xFF16A34A))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Success! Consolidated shipment order of $bulkQuantityRequired kg Tomatoes booked on Agri-Ledger. Consolidated tracking ID: GH-BULK-918", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                }
                            }
                        }
                    }
                }
            }

            // --- C. HARVEST RESERVATION ---
            Button(
                onClick = { showHarvestReservation = !showHarvestReservation },
                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("harvest_reservation_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reserve Pre-Harvest", fontWeight = FontWeight.Bold)
            }

            if (showHarvestReservation) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("reservation_panel"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = BorderStroke(1.dp, Color(0xFFFBBF24))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🌾 Guaranteed Pre-Harvest Reservation", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF78350F))
                        Text("Guarantee your inventory before harvest begins. Farmers get guaranteed markets; you lock in premium supply.", fontSize = 14.sp, color = Color.DarkGray)

                        if (!harvestReservationPlaced) {
                            OutlinedTextField(
                                value = reservationCrates,
                                onValueChange = { reservationCrates = it },
                                label = { Text("Crates to Reserve") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
                            )
                            Text("Estimated Harvest Date: August 15, 2026", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            Text("Deposit Required: 10% Reservation Escrow Deposit", fontSize = 14.sp, color = Color(0xFF475569))

                            Button(
                                onClick = {
                                    harvestReservationPlaced = true
                                    viewModel.addEngagementActivity("Mary Appiah (You)", "Buyer", "Reserved $reservationCrates crates pre-harvest tomatoes")
                                    Toast.makeText(context, "Pre-harvest reservation for $reservationCrates crates placed successfully!", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Book Pre-Harvest Escrow Reservation", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, null, tint = Color(0xFF16A34A))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Reservation Confirmed! Reserved $reservationCrates crates of premium tomatoes. Your escrow guarantee contract is sealed under ID GH-RES-402.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                }
                            }
                        }
                    }
                }
            }

            // --- D. SMART PRICE APPRAISAL ADVISOR ---
            Button(
                onClick = { showPriceAdvisor = !showPriceAdvisor },
                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("price_advisor_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Icon(Icons.Default.Analytics, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Price Appraisal Advisor", fontWeight = FontWeight.Bold)
            }

            if (showPriceAdvisor) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("price_advisor_panel"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📊 AI Market Valuation Desk", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Premium Deal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                        }
                        
                        Text(
                            text = "Based on current regional wholesale indexes across main Ghana hub markets, this listing is valued at GHS $price.",
                            fontSize = 13.sp,
                            color = Color(0xFF475569)
                        )

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        Text("Regional Tomato Indexes (per crate / bag):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• Techiman Hub (Bono East)", fontSize = 13.sp, color = Color(0xFF475569))
                            Text("GHS 120 - GHS 140", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• Kumasi Anloga Market (Ashanti)", fontSize = 13.sp, color = Color(0xFF475569))
                            Text("GHS 150 - GHS 165", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• Accra Agbogbloshie (Greater Accra)", fontSize = 13.sp, color = Color(0xFF475569))
                            Text("GHS 170 - GHS 195", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "💡 Advisor Verdict: Excellent buy! The farmer is offering direct-from-farm gate prices. Consolidating transport with a nearby vehicle will maximize your profit margins by up to 34%.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E40AF)
                            )
                        }
                    }
                }
            }

            // --- E. MOMO ESCROW DOWNPAYMENT LOCK-IN ---
            Button(
                onClick = { 
                    showMomoEscrow = !showMomoEscrow 
                    escrowStep = "FORM"
                    escrowAmount = price * 0.1
                },
                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("momo_escrow_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Icon(Icons.Default.Payment, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Secure 10% Escrow Lock-In", fontWeight = FontWeight.Bold)
            }

            if (showMomoEscrow) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("momo_escrow_panel"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = BorderStroke(1.dp, Color(0xFF059669))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔐 Secure Mobile Money Escrow", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF064E3B))
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Escrow Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        }
                        
                        if (escrowStep == "FORM") {
                            Text(
                                "Lock this crop listing so no other buyers can claim it. Your 10% deposit (GHS ${String.format("%.2f", escrowAmount)}) is held in a secure, audited smart escrow pool until you verify successful delivery.",
                                fontSize = 13.sp,
                                color = Color(0xFF1F2937)
                            )

                            Text("Select Mobile Money Network:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF064E3B))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("MTN Mobile Money", "Telecel Cash", "AT Money").forEach { prov ->
                                    val isSel = escrowProvider == prov
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSel) Color(0xFF047857) else Color.White,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSel) Color(0xFF047857) else Color(0xFFCBD5E1),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { escrowProvider = prov }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            prov.split(" ")[0],
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else Color(0xFF475569)
                                        )
                                    }
                                }
                            }

                            Text("Enter MoMo Phone Number:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF064E3B))
                            OutlinedTextField(
                                value = escrowPhone,
                                onValueChange = { escrowPhone = it },
                                placeholder = { Text("e.g. 0541112233") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
                                leadingIcon = { Icon(Icons.Default.PhoneAndroid, null, tint = Color(0xFF475569)) }
                            )

                            Button(
                                onClick = {
                                    if (escrowPhone.length >= 9) {
                                        escrowStep = "OTP"
                                    } else {
                                        Toast.makeText(context, "Please enter a valid MoMo number", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Initiate Secure Downpayment", fontWeight = FontWeight.Bold)
                            }
                        } else if (escrowStep == "OTP") {
                            Text(
                                "🔒 Dynamic OTP Authorization",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF064E3B)
                            )
                            Text(
                                "We have sent a secure authorization prompt to $escrowPhone via $escrowProvider. Enter the 4-digit token received:",
                                fontSize = 13.sp,
                                color = Color(0xFF475569)
                            )

                            OutlinedTextField(
                                value = escrowOtp,
                                onValueChange = { escrowOtp = it },
                                placeholder = { Text("e.g. 8492") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, letterSpacing = 4.sp),
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF475569)) }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { escrowStep = "FORM" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Back")
                                }
                                Button(
                                    onClick = {
                                        if (escrowOtp.length == 4) {
                                            escrowStep = "SUCCESS"
                                            viewModel.addEngagementActivity("Mary Appiah (You)", "Buyer", "Sealed secure escrow lock of GHS ${String.format("%.2f", escrowAmount)} for $title")
                                        } else {
                                            Toast.makeText(context, "Please enter the 4-digit OTP code", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Verify & Authorize", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (escrowStep == "SUCCESS") {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape)
                                        .padding(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Text(
                                    "Escrow Downpayment Sealed!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF047857)
                                )
                                Text(
                                    "GHS ${String.format("%.2f", escrowAmount)} is securely locked in the smart pool. Listing status updated to 'RESERVED' on Agri-Ledger.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF065F46),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    "Transaction Token: GH-TXN-${(100000..999999).random()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }
                }
            }

            // --- GET TRANSPORT BUTTON (LOGISTICS LINK) ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = BorderStroke(1.5.dp, Color(0xFF3B82F6).copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = "Get Transport",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Agri-Logistics Connection",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                    
                    Text(
                        text = "Link instantly with certified local transporters (Okada, Aboboyaa, Kia Bongo, Tractor) to deliver $title from $farmerName directly to your location with real-time GPS path tracking and AI route optimization.",
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )

                    Button(
                        onClick = {
                            viewModel.showCropDensityOnMap(title)
                            Toast.makeText(context, "Locating numerous $title on the map!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("locate_all_crop_on_map_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Locate Crops",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LOCATE ALL NEARBY ON MAP",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                    
                    Button(
                        onClick = {
                            viewModel.activeMarketplaceTransportItem.value = item
                            viewModel.activeMarketplaceTransportType.value = type
                            viewModel.setTab("live_map")
                            Toast.makeText(context, "Opening Map... Select your transport vehicle to start tracing!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("get_transport_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Get Transport",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GET TRANSPORT",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showRatingDialog) {
        AlertDialog(
            onDismissRequest = { onToggleRating(false) },
            title = { Text("Rate $title") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Rating:")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { onRatingChange(star) }) {
                                Icon(
                                    imageVector = if (star <= ratingValue) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onToggleRating(false)
                    Toast.makeText(context, "Thank you! Rating submitted.", Toast.LENGTH_SHORT).show()
                }) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { onToggleRating(false) }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MarketplaceAiWidget(
    query: String,
    onQueryChange: (String) -> Unit,
    answerText: String,
    isThinking: Boolean,
    onAsk: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        border = BorderStroke(1.dp, Color(0xFF86EFAC))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SupportAgent, null, tint = Color(0xFF15803D), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Agri-Assistant", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF166534))
            }
            
            Text("Ask about current market crop prices, transit calculations, or how the register works.", fontSize = 13.sp, color = Color(0xFF475569))

            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Tomato prices?", "Transit calculations?", "Cassava demand?").forEach { chip ->
                    FilterChip(
                        selected = false,
                        onClick = { onAsk(chip) },
                        label = { Text(chip, fontSize = 12.sp) }
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Ask anything...") },
                    modifier = Modifier.weight(1f).height(44.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                )
                Button(
                    onClick = { if (query.isNotBlank()) onAsk(query) },
                    modifier = Modifier.height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF166534))
                ) {
                    Text("Ask", fontSize = 14.sp)
                }
            }

            if (isThinking) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF166534))
            }

            if (answerText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(answerText, fontSize = 14.sp, color = Color(0xFF166534), modifier = Modifier.padding(10.dp))
                }
            }
        }
    }
}

// ---------------------- 3. SMART LOGISTICS & LIVE TIMELINE ----------------------
@Composable
fun LogisticsScreen(viewModel: GeoHarvestViewModel) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val selectedOrder by viewModel.selectedOrder.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocalShipping, contentDescription = "Truck", tint = Color(0xFF475569), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No shipments booked yet.", fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    Text("Create a listing purchase to book transport.", color = Color(0xFF475569), fontSize = 15.sp)
                }
            }
            return@Column
        }

        val activeOrder = selectedOrder ?: orders.firstOrNull()

        if (activeOrder != null) {
            // Horizontal list to switch between shipments
            Text(
                "Active Supply Logistics Shipments",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(orders) { o ->
                    val isSel = activeOrder.id == o.id
                    Card(
                        onClick = { viewModel.selectOrder(o.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            "Shipment #${1000 + o.id} (${o.vegetableType})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable shipment details
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val userLat by viewModel.userLatitude.collectAsStateWithLifecycle()
                val userLng by viewModel.userLongitude.collectAsStateWithLifecycle()
                val locationGranted by viewModel.locationPermissionGranted.collectAsStateWithLifecycle()

                GhanaGpsMap(
                    userLat = userLat,
                    userLng = userLng,
                    locationGranted = locationGranted,
                    activeOrderStage = activeOrder.trackingProgress,
                    onRequestLocation = {
                        // Action handled by the Scaffold location request
                    }
                )

                // Driver & Shipment Details Panel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable { viewModel.showUserProfileDialog(activeOrder.transporterName, "Transporter") },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Driver avatar simulation icon
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Driver", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(activeOrder.transporterName, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                            Text(activeOrder.transporterVehicle, fontSize = 15.sp, color = Color(0xFF475569))
                            Text("Delivery Cost: GHS ${activeOrder.deliveryCost.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Phone, contentDescription = "Call Driver", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Bolt-Style Delivery Timeline
                Column {
                    Text("Bolt-Style Shipment Timeline", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))

                    val timelineStages = listOf(
                        "Order Confirmed" to 1,
                        "Waiting for Driver" to 2,
                        "Driver Assigned" to 3,
                        "Driver Arriving" to 4,
                        "Produce Picked Up" to 5,
                        "In Transit" to 6,
                        "Approaching Destination" to 7,
                        "Delivered" to 8,
                        "Payment Completed" to 9
                    )

                    timelineStages.forEach { (label, stageNum) ->
                        val isDone = activeOrder.trackingProgress >= stageNum
                        val isCurrent = activeOrder.trackingProgress == stageNum

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Timeline circle with line connector
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = when {
                                            isDone -> MaterialTheme.colorScheme.primary
                                            isCurrent -> MaterialTheme.colorScheme.secondary
                                            else -> Color(0xFFCBD5E1)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(14.dp))
                                } else if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = label,
                                fontSize = 17.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isDone -> MaterialTheme.colorScheme.onBackground
                                    isCurrent -> MaterialTheme.colorScheme.secondary
                                    else -> Color(0xFF475569)
                                }
                            )

                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ---------------------- 4. INTEGRATED LIVE MAP & AGRICULTURAL INTEL ----------------------

enum class MapLocationType {
    FARM, MARKET, WAREHOUSE, TRANSPORTER, INTERMEDIATE
}

data class SelectedMapPin(
    val title: String,
    val desc: String,
    val type: MapLocationType,
    val contact: String?,
    val lat: Double? = null,
    val lng: Double? = null,
    val extraDetails: Map<String, String> = emptyMap()
)

data class SearchTarget(
    val title: String,
    val type: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double,
    val contact: String? = null
)

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Radius of earth in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

data class TransportOption(
    val id: String,
    val icon: String,
    val name: String,
    val cap: String,
    val rate: String,
    val desc: String
)

@Composable
fun LiveMapScreen(viewModel: GeoHarvestViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val userLat by viewModel.userLatitude.collectAsStateWithLifecycle()
    val userLng by viewModel.userLongitude.collectAsStateWithLifecycle()
    val locationGranted by viewModel.locationPermissionGranted.collectAsStateWithLifecycle()

    val focusLat by viewModel.mapFocusLat.collectAsStateWithLifecycle()
    val focusLng by viewModel.mapFocusLng.collectAsStateWithLifecycle()
    val focusTitle by viewModel.mapFocusTitle.collectAsStateWithLifecycle()
    val focusDesc by viewModel.mapFocusDesc.collectAsStateWithLifecycle()
    val focusType by viewModel.mapFocusType.collectAsStateWithLifecycle()
    val cropDensityToShow by viewModel.cropDensityToShow.collectAsStateWithLifecycle()

    var isPageLoaded by remember { mutableStateOf(false) }
    var isTilted by remember { mutableStateOf(false) }
    var isTrafficVisible by remember { mutableStateOf(false) }
    var isOfflineMode by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    
    var selectedPin by remember { mutableStateOf<SelectedMapPin?>(null) }
    var isNavigationActive by remember { mutableStateOf(false) }
    var isNavigationMinimized by remember { mutableStateOf(false) }
    
    // Live Navigation HUD state
    var currentSpeed by remember { mutableIntStateOf(0) }
    var remainingDistance by remember { mutableDoubleStateOf(0.0) }
    var remainingTimeMinutes by remember { mutableIntStateOf(0) }
    var remainingRatio by remember { mutableDoubleStateOf(1.0) }
    var arrivalTimeStr by remember { mutableStateOf("") }
    var stepInstructions by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentStepIndex by remember { mutableIntStateOf(0) }

    var showGisMenu by remember { mutableStateOf(false) }
    var showDemoPanel by remember { mutableStateOf(false) }
    var isHeatmapEnabled by remember { mutableStateOf(false) }
    var isFloodEnabled by remember { mutableStateOf(false) }
    var isWeatherEnabled by remember { mutableStateOf(false) }
    var isRoadQualityEnabled by remember { mutableStateOf(false) }
    var isColdStorageEnabled by remember { mutableStateOf(false) }
    var isMarketDemandEnabled by remember { mutableStateOf(false) }
    var isTransportDensityEnabled by remember { mutableStateOf(false) }
    var isSmartRouteEnabled by remember { mutableStateOf(false) }

    // Advanced Agri-Logistics Mapping Variables
    val activeTransportItem by viewModel.activeMarketplaceTransportItem.collectAsStateWithLifecycle()
    val activeTransportType by viewModel.activeMarketplaceTransportType.collectAsStateWithLifecycle()
    var isMarketplaceTracingActive by remember { mutableStateOf(false) }
    var selectedMarketplaceVehicle by remember { mutableStateOf("Kia Bongo") }
    var isAiFastRouterActive by remember { mutableStateOf(true) }

    var activeGeofenceAlert by remember { mutableStateOf<String?>(null) }
    var selectedVehicleId by remember { mutableStateOf("bongo") } // "bike", "bongo", "truck"
    var chosenTransportForMap by remember { mutableStateOf<String?>(null) }
    var isDispatchingTransporter by remember { mutableStateOf(false) }
    var dispatchProgress by remember { mutableStateOf(0.0f) }
    var matchedDriverName by remember { mutableStateOf<String?>(null) }
    var isMultiStopMode by remember { mutableStateOf(false) }
    var selectedStops by remember { mutableStateOf(emptyList<SearchTarget>()) }
    var selectedMapStyle by remember { mutableStateOf("voyager") } // "voyager", "dark", "osm"
    var showOfflineDialog by remember { mutableStateOf(false) }
    var downloadingRegionId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(0) }
    var cachedRegions by remember { mutableStateOf(setOf("Techiman Hub Basin")) }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentSuccess by remember { mutableStateOf(false) }
    var momoNumber by remember { mutableStateOf("") }
    var selectedMomoProvider by remember { mutableStateOf("MTN Mobile Money") }
    var activeChatMessages by remember(selectedPin) {
        mutableStateOf(
            selectedPin?.let { pin ->
                val greeting = when(pin.type) {
                    MapLocationType.MARKET -> "Hi! I am Amma Osei, a Tomatoes Wholesaler. Are you ready to supply fresh sliced tomatoes to Tuobodom market today?"
                    MapLocationType.TRANSPORTER -> "Akwaaba! I am available for immediate transit with my vehicle. Where is the loading point, and what crops are we carrying?"
                    else -> "Hello there! Akwaaba to GeoHarvest. Let me know if you want to chat."
                }
                listOf(pin.title to greeting)
            } ?: emptyList()
        )
    }
    var currentChatMessageText by remember { mutableStateOf("") }

    // TextToSpeech for Voice Navigation
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    DisposableEffect(context) {
        val instance = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                // Configured successfully
            }
        }
        tts = instance
        onDispose {
            instance.stop()
            instance.shutdown()
        }
    }

    // High fidelity search target database
    val searchTargets = remember {
        listOf(
            SearchTarget("Techiman", "COMMUNITY", "Bono East Regional Capital & Market Hub", 7.5833, -1.9333),
            SearchTarget("Tuobodom", "COMMUNITY", "Vegetable Greenhouses Area", 7.6333, -1.9000),
            SearchTarget("Kumasi", "COMMUNITY", "Central Cargo Distribution Point", 6.6666, -1.6166),
            SearchTarget("Accra", "COMMUNITY", "Southern Demand & Export Market", 5.5600, -0.2050),
            SearchTarget("Tamale", "COMMUNITY", "Northern Grain & Livestock Link", 9.4000, -0.8500),
            SearchTarget("Sunyani", "COMMUNITY", "Bono Regional Capital Hub", 7.3349, -2.3124),
            SearchTarget("Ejura", "COMMUNITY", "Maize & Legumes Transit Center", 7.3804, -1.3681),
            SearchTarget("Nkoranza", "COMMUNITY", "Grain Warehouses and Depots", 7.5645, -1.7011),
            SearchTarget("Kintampo", "COMMUNITY", "Bono East Waterfalls Transit Hub", 8.0538, -1.7289),
            SearchTarget("Nsawam", "COMMUNITY", "Pineapple and Fruit Plantations", 5.8078, -0.3503),
            
            // Farmers
            SearchTarget("Kwame Boakye", "FARM", "Bumper White Maize Fields", 9.4000, -0.8500, "+233241112233"),
            SearchTarget("Beatrice Ansah", "FARM", "Golden Rice Valley (Techiman)", 7.5833, -1.9333, "+233208887766"),
            SearchTarget("Kofi Mensah", "FARM", "Yam Gardens (Techiman)", 7.5910, -1.9210, "+233559876543"),
            SearchTarget("Abena Boateng", "FARM", "Cassava Estate (Sunyani)", 7.3349, -2.3124, "+233205554433"),
            SearchTarget("Abena Sarfo", "FARM", "Mango Groves (Techiman)", 7.5750, -1.9450, "+233245551122"),
            SearchTarget("Maud Adjaye", "FARM", "Pineapple plantations (Nsawam)", 5.8078, -0.3503, "+233201112244"),
            SearchTarget("Yaw Boateng", "FARM", "Watermelon Ridge (Tuobodom)", 7.6333, -1.9000, "+233558889999"),
            
            // Buyers
            SearchTarget("Amma Osei (Tomatoes Wholesaler)", "BUYER", "Demand: Slicing Tomatoes (Tuobodom)", 7.6333, -1.9000, "+233246778899"),
            SearchTarget("Accra Agro Wholesalers Ltd", "BUYER", "Demand: Fruits & Veggies (Accra)", 5.5600, -0.2050, "+233201119999"),
            SearchTarget("Kumasi Food Processing Corp", "BUYER", "Demand: White Maize Sacks (Kumasi)", 6.6666, -1.6166, "+233245553333"),
            SearchTarget("Techiman Veggie Packhouse", "BUYER", "Demand: Cassava Sacks (Techiman)", 7.5810, -1.9520, "+233245556666"),
            
            // Warehouses
            SearchTarget("Bono East Central Grain Silo", "WAREHOUSE", "Capacity: 5000 Tons. Cold room: Yes", 7.5855, -1.9280, "+233203456789"),
            SearchTarget("Tamale Northern Transit Silos", "WAREHOUSE", "Capacity: 8000 Tons. Cold room: No", 9.4080, -0.8420, "+233241234567"),
            SearchTarget("Nsawam Valley Cold Logistics", "WAREHOUSE", "Capacity: 2000 Tons. Cold room: Yes", 5.8078, -0.3503, "+233201235555")
        )
    }

    val filteredSuggestions = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else searchTargets.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.subtitle.contains(searchQuery, ignoreCase = true)
        }.take(5)
    }

    // High performance responsive offline-compatible MapLibre GL and OpenFreeMap mapping core
    val mapHtml = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body { padding: 0; margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; overflow: hidden; background-color: #0f172a; }
                #map { 
                    position: absolute; 
                    top: 0; 
                    bottom: 0; 
                    width: 100%; 
                    height: 100%; 
                    will-change: transform;
                    transition: transform 0.22s cubic-bezier(0.25, 1, 0.5, 1);
                }
                
                .pin-farmer { background-color: #16a34a; border: 2.5px solid #ffffff; border-radius: 50%; box-shadow: 0 3px 6px rgba(0,0,0,0.3); width: 14px; height: 14px; cursor: pointer; }
                .pin-buyer { background-color: #2563eb; border: 2.5px solid #ffffff; border-radius: 50%; box-shadow: 0 3px 6px rgba(0,0,0,0.3); width: 16px; height: 16px; cursor: pointer; }
                .pin-warehouse { background-color: #475569; border: 2.5px solid #ffffff; border-radius: 6px; box-shadow: 0 3px 6px rgba(0,0,0,0.3); width: 18px; height: 18px; cursor: pointer; color: white; font-size: 10px; font-weight: bold; text-align: center; line-height: 14px; }
                .pin-driver { background-color: #eab308; border: 2.5px solid #ffffff; border-radius: 50%; box-shadow: 0 3px 6px rgba(0,0,0,0.3); width: 16px; height: 16px; cursor: pointer; }
                .pin-user { background-color: #06b6d4; border: 3px solid #ffffff; border-radius: 50%; box-shadow: 0 0 12px #06b6d4; width: 16px; height: 16px; }
                
                .pin-driver-card {
                    background: #1e293b;
                    border: 2px solid #eab308;
                    border-radius: 50%;
                    box-shadow: 0 4px 10px rgba(234, 179, 8, 0.4);
                    width: 32px;
                    height: 32px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    position: relative;
                    cursor: pointer;
                    transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
                }
                .pin-driver-pulse {
                    position: absolute;
                    top: -2px; left: -2px; right: -2px; bottom: -2px;
                    border: 1px solid #eab308;
                    border-radius: 50%;
                    animation: driver-radar-pulse 2s infinite;
                    pointer-events: none;
                }
                @keyframes driver-radar-pulse {
                    0% { transform: scale(1); opacity: 0.8; }
                    100% { transform: scale(1.6); opacity: 0; }
                }

                .pulse-user {
                    position: absolute;
                    width: 100%;
                    height: 100%;
                    border-radius: 50%;
                    border: 2px solid #06b6d4;
                    animation: radar-pulse 1.8s infinite;
                }
                @keyframes radar-pulse {
                    0% { transform: scale(0.85); opacity: 0.8; }
                    70% { transform: scale(2.2); opacity: 0; }
                    100% { transform: scale(0.85); opacity: 0; }
                }

                .custom-pin { background: transparent !important; border: none !important; }
                .custom-anim-pin { background: transparent !important; border: none !important; }

                .pin-harvest-item {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    position: relative;
                    cursor: pointer;
                    animation: float-slow 3s ease-in-out infinite alternate;
                }
                @keyframes float-slow {
                    0% { transform: translateY(0px) scale(1); }
                    100% { transform: translateY(-8px) scale(1.05); }
                }
                .pulse-green-ring {
                    position: absolute;
                    width: 32px;
                    height: 32px;
                    border: 2px solid #10b981;
                    border-radius: 50%;
                    animation: circle-pulse 2s infinite;
                    pointer-events: none;
                }
                .pulse-orange-ring {
                    position: absolute;
                    width: 32px;
                    height: 32px;
                    border: 2px solid #f97316;
                    border-radius: 50%;
                    animation: circle-pulse 2s infinite;
                    pointer-events: none;
                }
                .pulse-yellow-ring {
                    position: absolute;
                    width: 32px;
                    height: 32px;
                    border: 2px solid #eab308;
                    border-radius: 50%;
                    animation: circle-pulse 2s infinite;
                    pointer-events: none;
                }
                .pulse-brown-ring {
                    position: absolute;
                    width: 32px;
                    height: 32px;
                    border: 2px solid #b45309;
                    border-radius: 50%;
                    animation: circle-pulse 2s infinite;
                    pointer-events: none;
                }
                .pulse-blue-ring {
                    position: absolute;
                    width: 32px;
                    height: 32px;
                    border: 2px solid #06b6d4;
                    border-radius: 50%;
                    animation: circle-pulse 2s infinite;
                    pointer-events: none;
                }
                @keyframes circle-pulse {
                    0% { transform: scale(0.85); opacity: 0.8; }
                    100% { transform: scale(2.2); opacity: 0; }
                }

                .leaflet-popup-content-wrapper {
                    background: #1e293b !important;
                    color: white !important;
                    border-radius: 8px !important;
                    padding: 4px 8px !important;
                    font-size: 13px !important;
                    box-shadow: 0 4px 6px rgba(0,0,0,0.3) !important;
                }
                .leaflet-popup-content { margin: 8px 12px !important; }
                .leaflet-popup-tip { background: #1e293b !important; }
                .leaflet-tooltip {
                    background: #1e293b !important;
                    color: white !important;
                    border: 1px solid #334155 !important;
                    border-radius: 6px !important;
                    padding: 4px 8px !important;
                    font-size: 12px !important;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.25) !important;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                // Initialize standard Leaflet Map
                var mapInstance = L.map('map', {
                    zoomControl: false,
                    attributionControl: false
                }).setView([7.5833, -1.9333], 10);

                var tileLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19
                }).addTo(mapInstance);

                var map = mapInstance;
                window.map = mapInstance;

                function setMapStyle(styleId) {
                    var styleUrl = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
                    if (styleId === 'dark') styleUrl = 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png';
                    else if (styleId === 'osm' || styleId === 'bright') styleUrl = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
                    else if (styleId === 'voyager') styleUrl = 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png';
                    tileLayer.setUrl(styleUrl);
                }

                var farmerGroup = L.layerGroup().addTo(mapInstance);
                var buyerGroup = L.layerGroup().addTo(mapInstance);
                var warehouseGroup = L.layerGroup().addTo(mapInstance);
                var driverGroup = L.layerGroup().addTo(mapInstance);
                var trafficGroup = L.layerGroup().addTo(mapInstance);
                var navigationGroup = L.layerGroup().addTo(mapInstance);
                var multiStopGroup = L.layerGroup().addTo(mapInstance);
                var roadNetworksGroup = L.layerGroup().addTo(mapInstance);
                var resourceGroup = L.layerGroup().addTo(mapInstance);

                 // Draw Custom Road Networks, Waterways, and Water Bodies (High Visibility, High Fidelity)
                function drawCustomRoadsAndFootpaths() {
                    roadNetworksGroup.clearLayers();
                    
                    // --- WATER BODIES ---
                    // Lake Volta (Detailed Polygon representing Lake Volta Reservoir, the largest artificial reservoir in the world)
                    var lakeCoords = [
                        [6.18, 0.05],   // Akosombo Dam Area
                        [6.35, -0.15],  // Kwahu arm
                        [6.60, -0.25],  // Afram arm
                        [7.05, -0.32],  // Yeji / Kete-Krachi arm
                        [7.50, -0.25],
                        [8.05, -0.15],  // Northern arm
                        [8.22, -0.79],  // Yeji port area
                        [8.15, -0.10],
                        [7.80, -0.02],  // Kete Krachi
                        [7.45, 0.05],
                        [7.15, 0.12],
                        [6.80, 0.18],
                        [6.45, 0.15],
                        [6.25, 0.08]
                    ];
                    L.polygon(lakeCoords, {
                        color: '#0284c7',
                        fillColor: '#38bdf8',
                        fillOpacity: 0.45,
                        weight: 2,
                        dashArray: '2, 4'
                    }).addTo(roadNetworksGroup).bindTooltip("<b style='color:#38bdf8;'>🌊 Lake Volta Reservoir</b><br><small>Waterways Transport & Inland Fishery Corridor</small>", { sticky: true });

                    // River Tributaries (White Volta, Black Volta) feeding the Lake
                    L.polyline([[9.4000, -0.8500], [8.8000, -0.9000], [8.2200, -0.7900]], { color: '#0ea5e9', weight: 4.5, opacity: 0.75, dashArray: '8, 8' }).addTo(roadNetworksGroup).bindTooltip("🌊 White Volta River Transport Channel", { sticky: true });
                    L.polyline([[8.0538, -1.7289], [8.1000, -1.3000], [8.2200, -0.7900]], { color: '#0ea5e9', weight: 4.5, opacity: 0.75, dashArray: '8, 8' }).addTo(roadNetworksGroup).bindTooltip("🌊 Black Volta River Segment", { sticky: true });

                    // Lake Volta Cargo Waterway Line (Transit corridor for inland shipping of grains and yams)
                    L.polyline([[8.2200, -0.7900], [7.8000, -0.0200], [6.2800, 0.0500]], { color: '#0ea5e9', weight: 5, opacity: 0.6, dashArray: '5, 12' }).addTo(roadNetworksGroup).bindTooltip("🚢 Lake Volta Inland Waterway Shipping Lane", { sticky: true });

                    // --- NATIONAL HIGHWAYS (N1 / N6 / N10 Primary Arterials) ---
                    // styled as solid dual-lane professional graphics (thick slate border + bright core)
                    var routes = [
                        { name: "N6 Highway (Accra - Nsawam - Kumasi Core Logistics Trunk)", coords: [[5.5600, -0.2050], [5.8078, -0.3503], [6.1500, -0.8500], [6.4500, -1.2500], [6.6666, -1.6166]], color: '#f59e0b', width: 6.5 },
                        { name: "N10 Highway (Kumasi - Techiman - Kintampo - Tamale Northern Corridor)", coords: [[6.6666, -1.6166], [7.1000, -1.7500], [7.3804, -1.3681], [7.5833, -1.9333], [7.8500, -1.8000], [8.0538, -1.7289], [8.6000, -1.4000], [9.4000, -0.8500]], color: '#f59e0b', width: 6.5 },
                        { name: "R12 Regional Highway (Sunyani - Techiman West Connect)", coords: [[7.3349, -2.3124], [7.4500, -2.1200], [7.5833, -1.9333]], color: '#10b981', width: 5.5 },
                        { name: "R18 Regional Highway (Techiman - Tuobodom Valley Highway)", coords: [[7.5833, -1.9333], [7.6050, -1.9150], [7.6333, -1.9000]], color: '#10b981', width: 5.5 },
                        { name: "R24 Regional Interconnector (Techiman - Nkoranza - Ejura Loop)", coords: [[7.5833, -1.9333], [7.5750, -1.8200], [7.5645, -1.7011], [7.4600, -1.5500], [7.3804, -1.3681]], color: '#e2e8f0', width: 5.0 }
                    ];

                    routes.forEach(function(r) {
                        // Background road casing (gives a professional highway outline look)
                        L.polyline(r.coords, { color: '#1e293b', weight: r.width + 4, lineCap: 'round', opacity: 0.95 }).addTo(roadNetworksGroup);
                        // Foreground highway core
                        L.polyline(r.coords, { color: r.color, weight: r.width, lineCap: 'round', opacity: 1.0 }).addTo(roadNetworksGroup)
                         .bindTooltip("<b style='color:#f59e0b;'>🛣️ " + r.name + "</b><br><small>Primary agricultural transport link</small>", { sticky: true });
                        // Dotted center dashed line
                        L.polyline(r.coords, { color: '#ffffff', weight: 1.2, lineCap: 'round', opacity: 0.8, dashArray: '6, 12' }).addTo(roadNetworksGroup);
                    });

                    // --- RURAL FOOTPATHS AND FEEDER TRACKS ---
                    // Feeder paths connecting farms directly to central highways
                    var paths = [
                        { name: "🌾 Tuobodom Greenhouse Feeder Track", coords: [[7.6333, -1.9000], [7.6200, -1.9200], [7.6050, -1.9150]], color: '#ca8a04' },
                        { name: "🍠 Wenchi Cassava Valley Feeder Track", coords: [[7.7390, -2.1100], [7.6800, -2.0000], [7.5833, -1.9333]], color: '#ca8a04' },
                        { name: "🥔 Kintampo Forest Yam Access Path", coords: [[8.0520, -1.7300], [7.9800, -1.7100], [8.0538, -1.7289]], color: '#ca8a04' },
                        { name: "🌽 Ejura Maize Belt Feeder Road", coords: [[7.3800, -1.3600], [7.3804, -1.3681]], color: '#caca00' }
                    ];

                    paths.forEach(function(p) {
                        L.polyline(p.coords, { color: '#451a03', weight: 4.5, lineCap: 'round', opacity: 0.85 }).addTo(roadNetworksGroup);
                        L.polyline(p.coords, { color: p.color, weight: 2.2, lineCap: 'round', opacity: 1.0, dashArray: '4, 6' }).addTo(roadNetworksGroup)
                         .bindTooltip("🚶 " + p.name + " (Dotted Feeder Path)", { sticky: true });
                    });
                }
                drawCustomRoadsAndFootpaths();

                var activeGeofencesTriggered = {};
                var geofences = [
                    { name: "Techiman Central Market Geofence", coords: [7.5833, -1.9333], radius: 2500, type: "market", info: "Automatic Weigh-bridge Queueing Active" },
                    { name: "Tuobodom Greenhouse Geofence", coords: [7.6333, -1.9000], radius: 1800, type: "farm", info: "Organic Certification Zone" },
                    { name: "Bono East Warehousing Geofence", coords: [7.5855, -1.9280], radius: 1200, type: "warehouse", info: "RFID Batch Loading Zone" }
                ];
                var geofenceLayers = [];
                function renderGeofences() {
                    geofenceLayers.forEach(function(l) { map.removeLayer(l); });
                    geofenceLayers = [];
                    geofences.forEach(function(g) {
                        var color = g.type === "market" ? "#2563eb" : (g.type === "farm" ? "#16a34a" : "#475569");
                        var circle = L.circle(g.coords, {
                            radius: g.radius,
                            color: color,
                            fillColor: color,
                            fillOpacity: 0.12,
                            weight: 2,
                            dashArray: "5, 5"
                        }).addTo(map);
                        circle.bindTooltip("🛡️ " + g.name + "<br><small>" + g.info + "</small>", { sticky: true });
                        geofenceLayers.push(circle);
                    });
                }
                renderGeofences();

                function checkGeofences(lat, lng) {
                    geofences.forEach(function(g) {
                        var p1 = L.latLng(lat, lng);
                        var p2 = L.latLng(g.coords[0], g.coords[1]);
                        var dist = p1.distanceTo(p2); // distance in meters
                        var isInside = dist <= g.radius;

                        if (isInside && !activeGeofencesTriggered[g.name]) {
                            activeGeofencesTriggered[g.name] = true;
                            if (window.AndroidInterface && window.AndroidInterface.onGeofenceTriggered) {
                                window.AndroidInterface.onGeofenceTriggered(g.name, "ENTER", "Entering geofence: " + g.name + " (" + g.info + ")");
                            }
                        } else if (!isInside && activeGeofencesTriggered[g.name]) {
                            delete activeGeofencesTriggered[g.name];
                            if (window.AndroidInterface && window.AndroidInterface.onGeofenceTriggered) {
                                window.AndroidInterface.onGeofenceTriggered(g.name, "LEAVE", "Exited geofence: " + g.name);
                            }
                        }
                    });
                }

                function drawMultiStopRoute(pointsJsonStr) {
                    multiStopGroup.clearLayers();
                    var points = JSON.parse(pointsJsonStr);
                    if (points.length < 2) return;

                    var routePoints = [];
                    for (var i = 0; i < points.length; i++) {
                        routePoints.push([points[i].lat, points[i].lng]);
                        
                        var el = document.createElement('div');
                        el.style.backgroundColor = '#10b981';
                        el.style.color = '#ffffff';
                        el.style.borderRadius = '50%';
                        el.style.border = '2px solid #ffffff';
                        el.style.width = '20px';
                        el.style.height = '20px';
                        el.style.fontSize = '11px';
                        el.style.fontWeight = 'bold';
                        el.style.textAlign = 'center';
                        el.style.lineHeight = '16px';
                        el.innerHTML = (i + 1).toString();
                        
                        var icon = L.divIcon({ className: 'custom-pin', html: el, iconSize: [20, 20], iconAnchor: [10, 10] });
                        L.marker([points[i].lat, points[i].lng], { icon: icon })
                            .addTo(multiStopGroup)
                            .bindTooltip("Stop " + (i + 1) + ": " + points[i].label, { permanent: true, direction: 'top' });
                    }

                    var outline = L.polyline(routePoints, { color: '#1e293b', weight: 8, lineCap: 'round' }).addTo(multiStopGroup);
                    L.polyline(routePoints, { color: '#10b981', weight: 4, lineCap: 'round', strokeDasharray: '6, 6' }).addTo(multiStopGroup);
                    
                    map.fitBounds(outline.getBounds(), { padding: [60, 60] });
                }

                function clearMultiStopRoute() {
                    multiStopGroup.clearLayers();
                }

                var hubs = {
                    "techiman": [7.5833, -1.9333],
                    "tuobodom": [7.6333, -1.9000],
                    "kumasi": [6.6666, -1.6166],
                    "accra": [5.5600, -0.2050],
                    "tamale": [9.4000, -0.8500],
                    "sunyani": [7.3349, -2.3124],
                    "ejura": [7.3804, -1.3681],
                    "nkoranza": [7.5645, -1.7011],
                    "kintampo": [8.0538, -1.7289],
                    "nsawam": [5.8078, -0.3503]
                };

                var dbFarmers = [
                    { id: 1, name: "Kwame Boakye", farm: "Bumper Maize Fields", type: "Grains", crop: "White Maize Sacks", qty: "50 Sacks", contact: "+233241112233", coords: hubs["tamale"], color: "#eab308" },
                    { id: 2, name: "Beatrice Ansah", farm: "Golden Rice Valley", type: "Grains", crop: "Organic Local Rice", qty: "30 Sacks", contact: "+233208887766", coords: hubs["techiman"], color: "#eab308" },
                    { id: 3, name: "Kofi Mensah", farm: "Premium Pona Gardens", type: "Vegetables", crop: "Premium Pona Yam", qty: "40 Crates", contact: "+233559876543", coords: [7.5910, -1.9210], color: "#16a34a" },
                    { id: 4, name: "Abena Boateng", farm: "Abena Cassava Estate", type: "Vegetables", crop: "Fresh Cassava Sacks", qty: "60 Sacks", contact: "+233205554433", coords: hubs["sunyani"], color: "#16a34a" },
                    { id: 5, name: "Abena Sarfo", farm: "Sarfo Mango Groves", type: "Fruits", crop: "Sweet Kent Mangoes", qty: "80 Crates", contact: "+233245551122", coords: [7.5750, -1.9450], color: "#f97316" },
                    { id: 6, name: "Maud Adjaye", farm: "Sugarloaf Pine Depot", type: "Fruits", crop: "Sugarloaf Pineapples", qty: "150 Crates", contact: "+233201112244", coords: hubs["nsawam"], color: "#f97316" },
                    { id: 7, name: "Yaw Boateng", farm: "Tuobodom Watermelons", type: "Fruits", crop: "Tuobodom Watermelons", qty: "120 Crates", contact: "+233558889999", coords: hubs["tuobodom"], color: "#f97316" }
                ];

                var dbResources = [
                    // Vegetables (tomatoes, chilli, okra, garden eggs)
                    { id: 401, name: "Premium Tomatoes Greenhouses", desc: "Organic Greenhouse tomatoes, rich red variety", type: "vegetable", icon: "🍅", coords: [7.5875, -1.9380], contact: "+233245551234" },
                    { id: 402, name: "Bono Chilli Plantation", desc: "Spicy Red and Green pepper plantations", type: "vegetable", icon: "🌶️", coords: [7.5790, -1.9290], contact: "+233201112244" },
                    { id: 403, name: "Techiman Okra Cooperative Hub", desc: "Fresh organic okra ready for daily collection", type: "vegetable", icon: "🥬", coords: [7.5899, -1.9425], contact: "+233552223344" },
                    { id: 404, name: "Garden Eggs Cultivation Site", desc: "High-yield eggplant harvest fields", type: "vegetable", icon: "🍆", coords: [7.5710, -1.9360], contact: "+233549998888" },

                    // Cereals (maize, local rice, sorghum)
                    { id: 411, name: "Bumper White Maize Fields", desc: "Sun-dried premium white maize sacks", type: "cereal", icon: "🌽", coords: [7.5820, -1.9210], contact: "+233241112233" },
                    { id: 412, name: "Bono Golden Rice Valley", desc: "Long-grain local polished brown and white rice", type: "cereal", icon: "🌾", coords: [7.5940, -1.9350], contact: "+233208887766" },
                    { id: 413, name: "High-Yield Sorghum Station", desc: "Premium quality crop seeds and grains storage", type: "cereal", icon: "🌾", coords: [7.5850, -1.9160], contact: "+233245553333" },

                    // Equipments (solar pump, mini-tractor, crop sprayer)
                    { id: 421, name: "Solar Irrigation Pumps Depot", desc: "Eco-friendly solar pumps available for rental", type: "equipment", icon: "⚙️", coords: [7.5840, -1.9460], contact: "+233551113333" },
                    { id: 422, name: "Mini-Tractor Rental Hub", desc: "Lightweight, robust tractors for cooperative farming", type: "equipment", icon: "🚜", coords: [7.5912, -1.9275], contact: "+233246781122" },
                    { id: 423, name: "Backpack Crop Sprayers", desc: "Electrostatic sprayers for ecological pest control", type: "equipment", icon: "🎒", coords: [7.5765, -1.9495], contact: "+233201112255" },

                    // Farms (eco-farm, youth greenhouse)
                    { id: 431, name: "Techiman Youth Eco-Farm", desc: "A smart research farm teaching crop rotation", type: "farm", icon: "🏡", coords: [7.5975, -1.9450], contact: "+233244111222" },
                    { id: 432, name: "Greenhouse Tech Hub", desc: "Precision humidity & temperature greenhouse farming", type: "farm", icon: "🛖", coords: [7.5730, -1.9220], contact: "+233550001112" },

                    // Water bodies (irrigation canal, reservoir lake)
                    { id: 441, name: "Techiman Central Irrigation Canal", desc: "Paved irrigation channel supplying surrounding tomato plots", type: "water", icon: "💧", coords: [7.5890, -1.9490], contact: "Cooperative Water Board" },
                    { id: 442, name: "Agro-Reservoir Lake", desc: "Substantial freshwater reservoir for dry-season backup", type: "water", icon: "🌊", coords: [7.5680, -1.9540], contact: "Regional Water Authority" }
                ];

                var dbBuyers = [
                    { id: 101, name: "Amma Osei (Tomatoes Wholesaler)", demand: "Slicing Tomatoes", qty: "150 Crates", price: "GHS 110/crate", contact: "+233246778899", coords: [7.6333, -1.9000], image: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=150", rating: 4.9 },
                    { id: 102, name: "Accra Agro Wholesalers Ltd", demand: "Fruits & Vegetables", qty: "200 Crates", price: "GHS 120/crate", contact: "+233201119999", coords: hubs["accra"], image: "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=150", rating: 4.7 },
                    { id: 103, name: "Kumasi Food Processing Corp", demand: "White Maize Sacks", qty: "500 Sacks", price: "GHS 190/sack", contact: "+233245553333", coords: hubs["kumasi"], image: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=150", rating: 4.8 },
                    { id: 104, name: "Techiman Veggie Packhouse", demand: "Fresh Cassava Sacks", qty: "300 Sacks", price: "GHS 95/sack", contact: "+233245556666", coords: [7.5810, -1.9520], image: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=150", rating: 4.6 }
                ];

                var dbWarehouses = [
                    { id: 201, name: "Bono East Central Grain Silo", cap: "5,000 Tons", avail: "1,800 Tons", cold: "Yes", contact: "+233203456789", coords: [7.5855, -1.9280] },
                    { id: 202, name: "Tamale Northern Transit Silos", cap: "8,000 Tons", avail: "3,500 Tons", cold: "No", contact: "+233241234567", coords: [9.4080, -0.8420] },
                    { id: 203, name: "Nsawam Valley Cold Logistics", cap: "2,000 Tons", avail: "800 Tons", cold: "Yes", contact: "+233201235555", coords: hubs["nsawam"] }
                ];

                // 15 Ghana Animated Means of Transport (Okadas, Aboboyaas, Light and Heavy Trucks, Tractors)
                var dbDrivers = [
                    { id: 301, name: "Alhaji Ibrahim", vehicle: "🚚 Kia Bongo Mini Truck", status: "Online", eta: "5 mins", coords: [7.5840, -1.9110], heading: 45, phone: "+233241112233", rating: 4.9, pricePerKm: 4.5, image: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=150" },
                    { id: 302, name: "Kwame Baffour", vehicle: "🚛 Hyundai Mighty Truck", status: "Online", eta: "8 mins", coords: [7.5920, -1.9320], heading: 120, phone: "+233208887766", rating: 4.8, pricePerKm: 8.5, image: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=150" },
                    { id: 303, name: "Yaw Mensah", vehicle: "🏍️ Okada Motorbike", status: "Online", eta: "3 mins", coords: [7.5812, -1.9420], heading: 270, phone: "+233245551234", rating: 4.7, pricePerKm: 1.5, image: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=150" },
                    { id: 304, name: "Kofi Owusu", vehicle: "🛺 Aboboyaa Tricycle", status: "Online", eta: "6 mins", coords: [7.5860, -1.9250], heading: 90, phone: "+233552223344", rating: 4.6, pricePerKm: 3.0, image: "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=150" },
                    { id: 305, name: "Emmanuel Osei", vehicle: "🚜 Massey Power Tractor", status: "Online", eta: "15 mins", coords: [7.6350, -1.8950], heading: 180, phone: "+233551113333", rating: 4.9, pricePerKm: 10.0, image: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=150" },
                    { id: 306, name: "Alhassan Fuseini", vehicle: "🚚 Ford F-150 Pickup", status: "Online", eta: "10 mins", coords: [7.5645, -1.7011], heading: 330, phone: "+233246781122", rating: 4.8, pricePerKm: 6.0, image: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=150" },
                    { id: 307, name: "Amg Boateng", vehicle: "🚛 Isuzu Tipper Heavy", status: "Online", eta: "14 mins", coords: [8.0538, -1.7289], heading: 60, phone: "+233204561122", rating: 4.9, pricePerKm: 12.0, image: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=150" },
                    { id: 308, name: "Nana Yaw", vehicle: "🚚 Kia Ceres Mini", status: "Online", eta: "9 mins", coords: [7.5750, -1.9450], heading: 15, phone: "+233241113355", rating: 4.7, pricePerKm: 4.0, image: "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=150" },
                    { id: 309, name: "Kwadwo Asante", vehicle: "🏍️ Royal Delivery Bike", status: "Online", eta: "4 mins", coords: [7.5810, -1.9520], heading: 210, phone: "+233549998888", rating: 4.5, pricePerKm: 1.5, image: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=150" },
                    { id: 310, name: "Uncle Atta", vehicle: "🚐 Nissan Urvan Bus", status: "Online", eta: "11 mins", coords: [7.5855, -1.9280], heading: 135, phone: "+233201112255", rating: 4.8, pricePerKm: 5.0, image: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=150" },
                    { id: 311, name: "Ebenezer Lartey", vehicle: "🚚 Cargo Van Express", status: "Online", eta: "7 mins", coords: [7.5950, -1.9150], heading: 315, phone: "+233244111222", rating: 4.6, pricePerKm: 4.2, image: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=150" },
                    { id: 312, name: "Papa Nii", vehicle: "🚛 Refrigerated Veggie", status: "Online", eta: "12 mins", coords: [7.5880, -1.9400], heading: 240, phone: "+233550001112", rating: 4.9, pricePerKm: 9.0, image: "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=150" },
                    { id: 313, name: "Mustapha Ali", vehicle: "🚚 Flatbed Trailer", status: "Online", eta: "20 mins", coords: [7.5500, -1.9600], heading: 80, phone: "+233209998877", rating: 4.7, pricePerKm: 11.5, image: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=150" },
                    { id: 314, name: "Bright Ampofo", vehicle: "🏍️ Haojin Okada", status: "Online", eta: "2 mins", coords: [7.5800, -1.9300], heading: 190, phone: "+233247776655", rating: 4.8, pricePerKm: 1.5, image: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=150" },
                    { id: 315, name: "Kojo Frimpong", vehicle: "🛺 Tricycle Express", status: "Online", eta: "5 mins", coords: [7.5833, -1.9333], heading: 10, phone: "+233206665544", rating: 4.6, pricePerKm: 3.0, image: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=150" }
                ];

                var userMarker = null;
                var activeRoutePolyline = null;
                var navigationTimer = null;
                var isTrafficVisible = false;
                var isTilted = false;
                var mapHeading = 0;
                var userCoords = hubs["techiman"];

                // Crop density layers group
                var cropDensityGroup = L.layerGroup().addTo(mapInstance);
                
                var cropFarmsDB = {
                    "oranges": [
                        { name: "Agyei Orange Grove", qty: "250 Crates", price: "GHS 35", farmer: "Kwaku Agyei", phone: "+233241113355", coords: [7.5850, -1.9350], emoji: "🍊" },
                        { name: "Bono Sweet Citrus Farm", qty: "400 Crates", price: "GHS 32", farmer: "Akua Mansah", phone: "+233201112255", coords: [7.6380, -1.8950], emoji: "🍊" },
                        { name: "Sunyani West Citrus Coop", qty: "320 Crates", price: "GHS 30", farmer: "Vida Boateng", phone: "+233549998888", coords: [7.3400, -2.3000], emoji: "🍊" },
                        { name: "Wenchi Organic Orange Field", qty: "180 Crates", price: "GHS 38", farmer: "Osei Kuffour", phone: "+233246781122", coords: [7.7420, -2.1050], emoji: "🍊" },
                        { name: "Tuobodom Valley Citrus", qty: "500 Crates", price: "GHS 31", farmer: "Yaw Mensah", phone: "+233245551234", coords: [7.6450, -1.9200], emoji: "🍊" },
                        { name: "Nkoranza Citrus Aggregation Depot", qty: "600 Crates", price: "GHS 33", farmer: "Amma Serwaa", phone: "+233552223344", coords: [7.5610, -1.6950], emoji: "🍊" }
                    ],
                    "tomatoes": [
                        { name: "Tuobodom Greenhouse Tomatoes", qty: "150 Crates", price: "GHS 110", farmer: "Amma Osei", phone: "+233244111222", coords: [7.6310, -1.9020], emoji: "🍅" },
                        { name: "Wenchi Irrigation Tomato Plots", qty: "300 Crates", price: "GHS 95", farmer: "Kwame Boakye", phone: "+233551113333", coords: [7.7390, -2.1100], emoji: "🍅" },
                        { name: "Techiman Central Tomato Field", qty: "180 Crates", price: "GHS 105", farmer: "Beatrice Ansah", phone: "+233208887766", coords: [7.5810, -1.9360], emoji: "🍅" },
                        { name: "Nkoranza Tomato Cooperative", qty: "240 Crates", price: "GHS 98", farmer: "Kofi Mensah", phone: "+233241112233", coords: [7.5650, -1.7050], emoji: "🍅" },
                        { name: "Kintampo Tomato Irrigation Scheme", qty: "200 Crates", price: "GHS 102", farmer: "Abena Boateng", phone: "+233549998888", coords: [8.0520, -1.7300], emoji: "🍅" }
                    ],
                    "okra": [
                        { name: "Techiman Okra Coop Hub", qty: "80 Bags", price: "GHS 45", farmer: "Mary Appiah", phone: "+233245556666", coords: [7.5899, -1.9425], emoji: "🥬" },
                        { name: "Tuobodom Okra Green Field", qty: "120 Bags", price: "GHS 42", farmer: "Kojo Frimpong", phone: "+233206665544", coords: [7.6350, -1.8980], emoji: "🥬" },
                        { name: "Nkoranza Organic Okra", qty: "95 Bags", price: "GHS 40", farmer: "Nana Kwame", phone: "+233550001112", coords: [7.5680, -1.7030], emoji: "🥬" }
                    ],
                    "peppers": [
                        { name: "Bono Chilli Plantation", qty: "90 Sacks", price: "GHS 60", farmer: "Yaw Boateng", phone: "+233201112255", coords: [7.5790, -1.9290], emoji: "🌶️" },
                        { name: "Tuobodom Spicy Pepper Plots", qty: "140 Sacks", price: "GHS 55", farmer: "Abena Sarfo", phone: "+233244111222", coords: [7.6340, -1.9050], emoji: "🌶️" },
                        { name: "Kintampo Habanero Fields", qty: "75 Sacks", price: "GHS 65", farmer: "Maud Adjaye", phone: "+233209998877", coords: [8.0550, -1.7220], emoji: "🌶️" }
                    ]
                };

                var driverMarkers = {};

                function renderStaticMarkers() {
                    farmerGroup.clearLayers();
                    buyerGroup.clearLayers();
                    warehouseGroup.clearLayers();

                    dbFarmers.forEach(function(f) {
                        var el = document.createElement('div');
                        el.className = 'pin-farmer';
                        el.style.backgroundColor = f.color;
                        el.style.width = '14px';
                        el.style.height = '14px';
                        var icon = L.divIcon({ className: 'custom-pin', html: el, iconSize: [14, 14], iconAnchor: [7, 7] });
                        var marker = L.marker(f.coords, { icon: icon }).addTo(farmerGroup);
                        marker.on('click', function() {
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onPinSelected(f.name, f.farm + " • Crop: " + f.crop + " (" + f.qty + ")", "FARM", f.contact, f.coords[0], f.coords[1], JSON.stringify(f));
                            }
                        });
                    });

                    dbBuyers.forEach(function(b) {
                        var el = document.createElement('div');
                        el.className = 'pin-buyer';
                        el.style.width = '16px';
                        el.style.height = '16px';
                        var icon = L.divIcon({ className: 'custom-pin', html: el, iconSize: [16, 16], iconAnchor: [8, 8] });
                        var marker = L.marker(b.coords, { icon: icon }).addTo(buyerGroup);
                        marker.on('click', function() {
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onPinSelected(b.name, "Demand: " + b.demand + " (" + b.qty + "). Offer: " + b.price, "MARKET", b.contact, b.coords[0], b.coords[1], JSON.stringify(b));
                            }
                        });
                    });

                    dbWarehouses.forEach(function(w) {
                        var el = document.createElement('div');
                        el.className = 'pin-warehouse';
                        el.style.width = '18px';
                        el.style.height = '18px';
                        el.innerHTML = "<div style='color:white; font-size:9px; font-weight:bold; text-align:center;'>W</div>";
                        var icon = L.divIcon({ className: 'custom-pin', html: el, iconSize: [18, 18], iconAnchor: [9, 9] });
                        var marker = L.marker(w.coords, { icon: icon }).addTo(warehouseGroup);
                        marker.on('click', function() {
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onPinSelected(w.name, "Warehouse Silo. Capacity: " + w.cap + " (Available: " + w.avail + ")", "WAREHOUSE", w.contact, w.coords[0], w.coords[1], JSON.stringify(w));
                            }
                        });
                    });

                    resourceGroup.clearLayers();
                    dbResources.forEach(function(res) {
                        var el = document.createElement('div');
                        el.className = 'pin-resource';
                        
                        var bgCol = '#10B981';
                        if (res.type === 'vegetable') bgCol = '#EF4444';
                        else if (res.type === 'cereal') bgCol = '#EAB308';
                        else if (res.type === 'equipment') bgCol = '#3B82F6';
                        else if (res.type === 'farm') bgCol = '#8B5CF6';
                        else if (res.type === 'water') bgCol = '#06B6D4';

                        el.style.backgroundColor = bgCol;
                        el.style.width = '24px';
                        el.style.height = '24px';
                        el.style.borderRadius = '50%';
                        el.style.border = '2px solid #ffffff';
                        el.style.boxShadow = '0 0 6px rgba(0,0,0,0.3)';
                        el.style.display = 'flex';
                        el.style.justifyContent = 'center';
                        el.style.alignItems = 'center';
                        el.style.fontSize = '12px';
                        el.innerHTML = res.icon;

                        var icon = L.divIcon({ className: 'custom-pin', html: el, iconSize: [24, 24], iconAnchor: [12, 12] });
                        var marker = L.marker(res.coords, { icon: icon }).addTo(resourceGroup);
                        
                        var popupContent = '<div style="font-family: system-ui, -apple-system, sans-serif; padding: 4px; min-width: 170px; color: white;">' +
                            '<div style="display: flex; align-items: center; gap: 6px; margin-bottom: 6px;">' +
                                '<span style="font-size: 18px;">' + res.icon + '</span>' +
                                '<strong style="font-size: 13px; color: #f8fafc;">' + res.name + '</strong>' +
                            '</div>' +
                            '<div style="color: #cbd5e1; font-size: 11px; margin-bottom: 8px; line-height: 1.4;">' + res.desc + '</div>' +
                            '<div style="color: #94a3b8; font-size: 10px; margin-bottom: 8px;">Contact: ' + res.contact + '</div>' +
                            '<button onclick="startNavigationSimulation(' + res.coords[0] + ', ' + res.coords[1] + '); map.closePopup();" ' +
                                    'style="width: 100%; background: #10b981; color: white; border: none; padding: 6px 10px; border-radius: 6px; font-weight: bold; font-size: 11px; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 4px; box-shadow: 0 1px 2px rgba(0,0,0,0.2);">' +
                                '🧭 Get Directions' +
                            '</button>' +
                        '</div>';
                        marker.bindPopup(popupContent);
                        
                        marker.on('click', function() {
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onPinSelected(res.name, res.desc + " | Contact: " + res.contact, res.type.toUpperCase(), res.contact, res.coords[0], res.coords[1], JSON.stringify(res));
                            }
                        });
                    });
                }

                function updateDriverPositions() {
                    dbDrivers.forEach(function(d) {
                        var el = document.createElement('div');
                        el.className = 'pin-driver-card';
                        
                        var emoji = "🚚";
                        if (d.vehicle.includes("Okada") || d.vehicle.includes("Bike")) emoji = "🏍️";
                        else if (d.vehicle.includes("Aboboyaa") || d.vehicle.includes("Tricycle")) emoji = "🛺";
                        else if (d.vehicle.includes("Tractor")) emoji = "🚜";
                        else if (d.vehicle.includes("Heavy") || d.vehicle.includes("Mighty")) emoji = "🚛";
                        
                        el.innerHTML = "<div style='font-size:16px; transform: rotate(" + d.heading + "deg);'>" + emoji + "</div>" +
                                       "<div class='pin-driver-pulse'></div>";
                        
                        var icon = L.divIcon({ className: 'custom-pin', html: el, iconSize: [32, 32], iconAnchor: [16, 16] });
                        
                        if (driverMarkers[d.id]) {
                            driverMarkers[d.id].setLatLng(d.coords);
                            driverMarkers[d.id].setIcon(icon);
                        } else {
                            var marker = L.marker(d.coords, { icon: icon }).addTo(driverGroup);
                            
                            var popupContent = 
                                '<div style="font-family: system-ui, -apple-system, sans-serif; padding: 4px; min-width: 170px; color: white;">' +
                                    '<strong style="font-size: 13px; color: #f8fafc;">' + d.name + '</strong>' +
                                    '<div style="color: #cbd5e1; font-size: 11px; margin-top: 4px;">Vehicle: <strong>' + d.vehicle + '</strong></div>' +
                                    '<div style="color: #cbd5e1; font-size: 11px;">Rate: <strong>GHS ' + d.pricePerKm + '/km</strong></div>' +
                                    '<div style="color: #cbd5e1; font-size: 11px; margin-bottom: 8px;">ETA to Market: <strong>' + d.eta + '</strong></div>' +
                                    '<button onclick="triggerMarketplaceLogisticsLink(\'' + d.vehicle + '\', ' + d.coords[0] + ', ' + d.coords[1] + ', ' + userCoords[0] + ', ' + userCoords[1] + '); map.closePopup();" ' +
                                            'style="width: 100%; background: #2563eb; color: white; border: none; padding: 5px 8px; border-radius: 4px; font-weight: bold; font-size: 10px; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 4px;">' +
                                        '🚚 Match & Trace' +
                                    '</button>' +
                                '</div>';
                            marker.bindPopup(popupContent);
                            driverMarkers[d.id] = marker;
                        }
                    });
                }

                function showCropDensity(cropName) {
                    cropDensityGroup.clearLayers();
                    var normalized = cropName.toLowerCase();
                    var farms = [];
                    
                    if (normalized.includes("orange")) {
                        farms = cropFarmsDB["oranges"];
                    } else if (normalized.includes("tomato")) {
                        farms = cropFarmsDB["tomatoes"];
                    } else if (normalized.includes("okra")) {
                        farms = cropFarmsDB["okra"];
                    } else if (normalized.includes("pepper") || normalized.includes("chilli") || normalized.includes("habanero")) {
                        farms = cropFarmsDB["peppers"];
                    } else {
                        var emoji = "🌾";
                        if (normalized.includes("maize") || normalized.includes("corn")) emoji = "🌽";
                        else if (normalized.includes("rice")) emoji = "🌾";
                        else if (normalized.includes("yam") || normalized.includes("potato") || normalized.includes("cassava") || normalized.includes("tuber")) emoji = "🥔";
                        else if (normalized.includes("eggplant") || normalized.includes("egg")) emoji = "🍆";
                        else if (normalized.includes("melon") || normalized.includes("watermelon")) emoji = "🍉";
                        else if (normalized.includes("mango")) emoji = "🥭";
                        else if (normalized.includes("pineapple")) emoji = "🍍";
                        
                        var farmers = ["Amma Serwaa", "Kwame Boakye", "Vida Gyamfi", "Kofi Owusu", "Abena Boateng", "Yao Appiah", "Ibrahim Ali"];
                        var centers = [
                            [7.5833, -1.9333],
                            [7.6333, -1.9000],
                            [7.7410, -2.1030],
                            [7.5645, -1.7011],
                            [7.3349, -2.3124],
                            [8.0538, -1.7289]
                        ];
                        for (var i = 0; i < 9; i++) {
                            var center = centers[i % centers.length];
                            var lat = center[0] + (Math.sin(i * 45) * 0.03);
                            var lng = center[1] + (Math.cos(i * 45) * 0.03);
                            farms.push({
                                name: farmers[i % farmers.length].split(" ")[1] + " Organic " + cropName + " Farm",
                                qty: (150 + Math.floor(Math.random() * 300)) + " units",
                                price: "GHS " + (15 + Math.floor(Math.random() * 45)),
                                farmer: farmers[i % farmers.length],
                                phone: "+2332455512" + i + i,
                                coords: [lat, lng],
                                emoji: emoji
                            });
                        }
                    }

                    if (farms && farms.length > 0) {
                        var bounds = [];
                        farms.forEach(function(f) {
                            var el = document.createElement('div');
                            el.className = 'pin-crop-density';
                            el.style.backgroundColor = '#047857';
                            el.style.width = '34px';
                            el.style.height = '34px';
                            el.style.borderRadius = '50%';
                            el.style.border = '2.5px solid #ffffff';
                            el.style.boxShadow = '0 0 15px rgba(4, 120, 87, 0.8)';
                            el.style.display = 'flex';
                            el.style.justifyContent = 'center';
                            el.style.alignItems = 'center';
                            el.style.fontSize = '18px';
                            el.style.position = 'relative';
                            el.style.cursor = 'pointer';
                            el.innerHTML = f.emoji;

                            var radar = document.createElement('div');
                            radar.style.position = 'absolute';
                            radar.style.width = '100%';
                            radar.style.height = '100%';
                            radar.style.borderRadius = '50%';
                            radar.style.border = '2px solid #047857';
                            radar.style.top = '0';
                            radar.style.left = '0';
                            radar.style.boxSizing = 'border-box';
                            radar.style.animation = 'radar-pulse 2s infinite';
                            el.appendChild(radar);

                            var icon = L.divIcon({ className: 'custom-pin', html: el, iconSize: [34, 34], iconAnchor: [17, 17] });
                            var marker = L.marker(f.coords, { icon: icon }).addTo(cropDensityGroup);
                            bounds.push(f.coords);

                            var popupContent = 
                                '<div style="font-family: system-ui, -apple-system, sans-serif; padding: 6px; min-width: 220px; color: white;">' +
                                    '<div style="display: flex; align-items: center; gap: 8px; margin-bottom: 6px;">' +
                                        '<span style="font-size: 24px;">' + f.emoji + '</span>' +
                                        '<div>' +
                                            '<strong style="font-size: 14px; color: #f8fafc; display:block;">' + f.name + '</strong>' +
                                            '<span style="font-size: 10px; color: #34d399; font-weight: bold; background: rgba(52, 211, 153, 0.15); padding: 1px 4px; border-radius: 4px;">CERTIFIED PRODUCER</span>' +
                                        '</div>' +
                                    '</div>' +
                                    '<div style="background: #334155; border-radius: 6px; padding: 6px; margin: 8px 0; font-size: 12px; display: grid; grid-template-columns: 1f 1f; gap: 4px;">' +
                                        '<div>🌾 <span style="color: #cbd5e1;">Stock:</span> <strong>' + f.qty + '</strong></div>' +
                                        '<div>💰 <span style="color: #cbd5e1;">Price:</span> <strong>' + f.price + '</strong></div>' +
                                        '<div style="grid-column: span 2;">👨‍🌾 <span style="color: #cbd5e1;">Farmer:</span> <strong>' + f.farmer + '</strong></div>' +
                                    '</div>' +
                                    '<div style="color: #cbd5e1; font-size: 10px; margin-bottom: 8px;">📞 Contact: <strong>' + f.phone + '</strong></div>' +
                                    '<div style="display: flex; flex-direction: column; gap: 6px;">' +
                                        '<button onclick="startNavigationSimulation(' + f.coords[0] + ', ' + f.coords[1] + '); map.closePopup();" ' +
                                                'style="width: 100%; background: #10b981; color: white; border: none; padding: 7px 10px; border-radius: 6px; font-weight: bold; font-size: 11px; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 4px; box-shadow: 0 1px 2px rgba(0,0,0,0.2);">' +
                                            '🧭 Get Directions' +
                                        '</button>' +
                                        '<div style="font-size: 9px; color: #94a3b8; font-weight: bold; margin-top: 2px;">⚡ MATCH VEHICLE TYPE:</div>' +
                                        '<div style="display: grid; grid-template-columns: repeat(4, 1f); gap: 4px;">' +
                                            '<button onclick="triggerMarketplaceLogisticsLink(\'Okada\', ' + f.coords[0] + ', ' + f.coords[1] + ', ' + userCoords[0] + ', ' + userCoords[1] + '); map.closePopup();" style="background:#1e293b; border:1px solid #eab308; border-radius:4px; padding:4px; cursor:pointer; font-size:12px; title:\'Okada\'">🏍️</button>' +
                                            '<button onclick="triggerMarketplaceLogisticsLink(\'Aboboyaa\', ' + f.coords[0] + ', ' + f.coords[1] + ', ' + userCoords[0] + ', ' + userCoords[1] + '); map.closePopup();" style="background:#1e293b; border:1px solid #eab308; border-radius:4px; padding:4px; cursor:pointer; font-size:12px; title:\'Aboboyaa\'">🛺</button>' +
                                            '<button onclick="triggerMarketplaceLogisticsLink(\'Kia Bongo\', ' + f.coords[0] + ', ' + f.coords[1] + ', ' + userCoords[0] + ', ' + userCoords[1] + '); map.closePopup();" style="background:#1e293b; border:1px solid #eab308; border-radius:4px; padding:4px; cursor:pointer; font-size:12px; title:\'Kia Bongo\'">🚚</button>' +
                                            '<button onclick="triggerMarketplaceLogisticsLink(\'Tractor\', ' + f.coords[0] + ', ' + f.coords[1] + ', ' + userCoords[0] + ', ' + userCoords[1] + '); map.closePopup();" style="background:#1e293b; border:1px solid #eab308; border-radius:4px; padding:4px; cursor:pointer; font-size:12px; title:\'Tractor\'">🚜</button>' +
                                        '</div>' +
                                    '</div>' +
                                '</div>';
                            marker.bindPopup(popupContent);
                        });
                        
                        if (bounds.length > 0) {
                            map.fitBounds(L.latLngBounds(bounds), { padding: [50, 50] });
                        }
                    }
                }

                // Simulate Live Realtime Position Animating Movement of Vehicles (Okadas, Bongos, Tractors)
                setInterval(function() {
                    dbDrivers.forEach(function(d) {
                        var speed = 0.00012; // animated delta speed
                        var angle = (d.heading || 0) * Math.PI / 180;
                        d.coords[0] += Math.sin(angle) * speed;
                        d.coords[1] += Math.cos(angle) * speed;

                        // Jitter direction randomly
                        if (Math.random() > 0.8) {
                            d.heading = (d.heading + (Math.random() * 40 - 20) + 360) % 360;
                        }

                        // Constrain vehicles to remain within regional bounding box
                        if (d.coords[0] < 7.3 || d.coords[0] > 8.1 || d.coords[1] < -2.4 || d.coords[1] > -1.5) {
                            d.heading = (d.heading + 180) % 360;
                        }
                    });
                    updateDriverPositions();
                }, 1500);

                function sendToKotlin(type, id, title, desc, contact, lat, lng) {
                    if (window.AndroidInterface) {
                        var extraData = JSON.stringify({
                            "id": id,
                            "type": type,
                            "title": title,
                            "desc": desc,
                            "contact": contact,
                            "lat": lat,
                            "lng": lng
                        });
                        window.AndroidInterface.onPinSelected(title, desc, type, contact, lat, lng, extraData);
                    }
                }

                function getHaversineDist(p1, p2) {
                    var dx = p1[0] - p2[0];
                    var dy = p1[1] - p2[1];
                    return Math.sqrt(dx*dx + dy*dy);
                }

                function calculateAndDrawRoute(targetLat, targetLng) {
                    navigationGroup.clearLayers();
                    var start = userCoords;
                    var end = [targetLat, targetLng];
                    var routePoints = [start];

                    if (isSmartRouteActive) {
                        // Safe detour routing point to avoid eastern flash-flooded plain
                        var safeDetour = [7.3349, -2.3124]; // Sunyani / Safe Western transit corridor
                        routePoints.push(safeDetour);
                    } else {
                        var minHub = null;
                        var minDist = 99999;
                        for (var k in hubs) {
                            var hCoord = hubs[k];
                            var d1 = getHaversineDist(start, hCoord);
                            var d2 = getHaversineDist(hCoord, end);
                            if (d1 + d2 < minDist && d1 > 0.05 && d2 > 0.05) {
                                minDist = d1 + d2;
                                minHub = hCoord;
                            }
                        }
                        if (minHub) {
                            routePoints.push(minHub);
                        }
                    }
                    routePoints.push(end);

                    var outline = L.polyline(routePoints, { className: 'road-outline', color: '#1e293b', weight: 8, opacity: 0.9 }).addTo(navigationGroup);
                    L.polyline(routePoints, { className: 'road-glowing', color: '#06b6d4', weight: 4, opacity: 1.0 }).addTo(navigationGroup);
                    activeRoutePolyline = outline;
                    map.fitBounds(outline.getBounds(), { padding: [50, 50] });

                    var totalKm = getHaversineDist(start, end) * 111.1;
                    var travelTimeMins = Math.round(totalKm / 0.85);
                    if (travelTimeMins < 5) travelTimeMins = 8;

                    var arrivalTime = new Date();
                    arrivalTime.setMinutes(arrivalTime.getMinutes() + travelTimeMins);
                    var arrivalStr = arrivalTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

                    var instructions = isSmartRouteActive ? [
                        "Start on primary transit route in Techiman market",
                        "⚠️ FLOOD DETOUR DETECTED: Avoiding Volta transit corridor",
                        "Re-routed safely via paved Western Sunyani Highway",
                        "Arrive safely at destination with zero flood hazard"
                    ] : [
                        "Start on primary transit route in Techiman market",
                        "In " + Math.round(totalKm * 0.4 * 10) / 10 + " km, take second exit at logistics junction",
                        "Continue straight on the central distribution highway",
                        "Arrive at destination on your right"
                    ];

                    if (window.AndroidInterface) {
                        window.AndroidInterface.onRouteCalculated(totalKm, travelTimeMins, arrivalStr, JSON.stringify(instructions));
                    }
                    return routePoints;
                }

                function startNavigationSimulation(targetLat, targetLng) {
                    cancelNavigationSimulation();
                    var routePoints = calculateAndDrawRoute(targetLat, targetLng);
                    var currentIdx = 0;
                    var subSteps = 15;
                    var stepIdx = 0;

                    function runStep() {
                        if (currentIdx >= routePoints.length - 1) {
                            if (window.AndroidInterface) {
                                window.AndroidInterface.speak("Arrived at your destination.");
                            }
                            cancelNavigationSimulation();
                            return;
                        }
                        var pStart = routePoints[currentIdx];
                        var pEnd = routePoints[currentIdx + 1];

                        var lat = pStart[0] + (pEnd[0] - pStart[0]) * (stepIdx / subSteps);
                        var lng = pStart[1] + (pEnd[1] - pStart[1]) * (stepIdx / subSteps);
                        userCoords = [lat, lng];
                        updateUserMarker();

                        var angle = Math.atan2(pEnd[1] - pStart[1], pEnd[0] - pStart[0]) * 180 / Math.PI;
                        mapHeading = 90 - angle;
                        applyMapRotationAndTilt();
                        map.setView(userCoords, map.getZoom());

                        var remainingRatio = 1.0 - ((currentIdx * subSteps + stepIdx) / ((routePoints.length - 1) * subSteps));
                        var currentSpeed = 50 + Math.floor(Math.random() * 15);

                        if (window.AndroidInterface) {
                            window.AndroidInterface.onNavigationProgress(currentSpeed, remainingRatio);
                        }

                        stepIdx++;
                        if (stepIdx >= subSteps) {
                            stepIdx = 0;
                            currentIdx++;
                            var stepMsg = "Continuing along logistics transit highway.";
                            if (currentIdx === routePoints.length - 1) {
                                stepMsg = "Arriving at destination in 100 meters.";
                            }
                            if (window.AndroidInterface) {
                                window.AndroidInterface.speak(stepMsg);
                            }
                        }
                        navigationTimer = setTimeout(runStep, 150);
                    }

                    if (window.AndroidInterface) {
                        window.AndroidInterface.speak("Starting navigation guidance. Proceed to highlighted route.");
                    }
                    runStep();
                }

                function cancelNavigationSimulation() {
                    if (navigationTimer) {
                        clearTimeout(navigationTimer);
                        navigationTimer = null;
                    }
                    navigationGroup.clearLayers();
                    activeRoutePolyline = null;
                    mapHeading = 0;
                    applyMapRotationAndTilt();
                }

                function updateUserMarker() {
                    var userIcon = L.divIcon({
                        className: 'custom-pin pin-user',
                        html: "<div style='position:relative; width:100%; height:100%;'><div class='pulse-user'></div></div>",
                        iconSize: [16, 16],
                        iconAnchor: [8, 8]
                    });
                    if (userMarker) {
                        userMarker.setLatLng(userCoords);
                    } else {
                        userMarker = L.marker(userCoords, { icon: userIcon }).addTo(map);
                    }
                    if (typeof checkGeofences === "function") {
                        checkGeofences(userCoords[0], userCoords[1]);
                    }
                }

                var animationMarker = null;
                function triggerVehicleAnimation(vehicleType) {
                    if (animationMarker) {
                        map.removeLayer(animationMarker);
                        animationMarker = null;
                    }

                    map.setView(userCoords, 14);

                    var startLat = userCoords[0] + 0.008;
                    var startLng = userCoords[1] + 0.008;

                    var iconEmoji = "🚚";
                    if (vehicleType === "bike") iconEmoji = "🏍️";
                    else if (vehicleType === "aboboyaa") iconEmoji = "🛺";
                    else if (vehicleType === "bongo") iconEmoji = "🚚";
                    else if (vehicleType === "tractor") iconEmoji = "🚜";
                    else if (vehicleType === "truck") iconEmoji = "🚛";
                    else if (vehicleType === "taxi") iconEmoji = "🚕";
                    else if (vehicleType === "cold_van") iconEmoji = "🚐";

                    var el = document.createElement('div');
                    el.innerHTML = "<div style='font-size: 28px; animation: bounce 0.6s infinite alternate; text-shadow: 0 0 4px #000;'>" + iconEmoji + "</div>";
                    
                    var icon = L.divIcon({
                        className: 'custom-anim-pin',
                        html: el,
                        iconSize: [36, 36],
                        iconAnchor: [18, 18]
                    });

                    animationMarker = L.marker([startLat, startLng], { icon: icon }).addTo(map);

                    var style = document.createElement('style');
                    style.type = 'text/css';
                    style.innerHTML = '@keyframes bounce { from { transform: translateY(0); } to { transform: translateY(-12px); } }';
                    document.getElementsByTagName('head')[0].appendChild(style);

                    var steps = 50;
                    var currentStep = 0;
                    var interval = setInterval(function() {
                        if (currentStep >= steps) {
                            clearInterval(interval);
                            if (window.AndroidInterface && window.AndroidInterface.speak) {
                                window.AndroidInterface.speak("Your selected " + vehicleType + " transport is approaching your pickup location.");
                            }
                            el.innerHTML = "<div style='font-size: 38px; transform: scale(1.4); transition: transform 0.4s; text-shadow: 0 0 6px #000;'>" + iconEmoji + "</div>";
                            setTimeout(function() {
                                if (animationMarker) {
                                    map.removeLayer(animationMarker);
                                    animationMarker = null;
                                }
                            }, 6000);
                            return;
                        }
                        var lat = startLat + (userCoords[0] - startLat) * (currentStep / steps);
                        var lng = startLng + (userCoords[1] - startLng) * (currentStep / steps);
                        if (animationMarker) {
                            animationMarker.setLatLng([lat, lng]);
                        }
                        currentStep++;
                    }, 60);
                }

                var marketplaceLinkMarker = null;
                var marketplaceLinkLine = null;
                var sellerMarker = null;
                var buyerMarker = null;

                function triggerMarketplaceLogisticsLink(vehicleType, sellerLat, sellerLng, buyerLat, buyerLng) {
                    if (marketplaceLinkMarker) { map.removeLayer(marketplaceLinkMarker); marketplaceLinkMarker = null; }
                    if (marketplaceLinkLine) { map.removeLayer(marketplaceLinkLine); marketplaceLinkLine = null; }
                    if (sellerMarker) { map.removeLayer(sellerMarker); sellerMarker = null; }
                    if (buyerMarker) { map.removeLayer(buyerMarker); buyerMarker = null; }

                    var bounds = L.latLngBounds([[sellerLat, sellerLng], [buyerLat, buyerLng]]);
                    map.fitBounds(bounds, { padding: [80, 80] });

                    var sellerIcon = L.divIcon({
                        className: 'custom-pin',
                        html: "<div style='display:flex; flex-direction:column; align-items:center;'>" +
                              "<div style='font-size: 26px; filter: drop-shadow(0px 2px 4px rgba(0,0,0,0.5));'>🚜</div>" +
                              "<div style='background:#064e3b; color:white; font-size:10px; font-weight:bold; padding:2px 6px; border-radius:4px; border:1px solid white; white-space:nowrap;'>SELLER / PRODUCER</div>" +
                              "</div>",
                        iconSize: [80, 50],
                        iconAnchor: [40, 25]
                    });
                    sellerMarker = L.marker([sellerLat, sellerLng], { icon: sellerIcon }).addTo(map);

                    var buyerIcon = L.divIcon({
                        className: 'custom-pin',
                        html: "<div style='display:flex; flex-direction:column; align-items:center;'>" +
                              "<div style='position:relative; width:20px; height:20px;'>" +
                              "<div style='width:16px; height:16px; background:#2563eb; border:3px solid white; border-radius:50%; box-shadow:0 0 10px #2563eb;'></div>" +
                              "<div style='position:absolute; top:-2px; left:-2px; width:20px; height:20px; border:2px solid #2563eb; border-radius:50%; animation:radar-pulse 1.8s infinite;'></div>" +
                              "</div>" +
                              "<div style='background:#1e3a8a; color:white; font-size:10px; font-weight:bold; padding:2px 6px; border-radius:4px; border:1px solid white; white-space:nowrap; margin-top:2px;'>BUYER / ME</div>" +
                              "</div>",
                        iconSize: [80, 50],
                        iconAnchor: [40, 10]
                    });
                    buyerMarker = L.marker([buyerLat, buyerLng], { icon: buyerIcon }).addTo(map);

                    marketplaceLinkLine = L.polyline([[sellerLat, sellerLng], [buyerLat, buyerLng]], {
                        color: '#2563eb',
                        weight: 6,
                        opacity: 0.8,
                        dashArray: '10, 8',
                        lineCap: 'round'
                    }).addTo(map);

                    var iconEmoji = "🚚";
                    if (vehicleType === "bike" || vehicleType === "Okada") iconEmoji = "🏍️";
                    else if (vehicleType === "aboboyaa" || vehicleType === "Aboboyaa") iconEmoji = "🛺";
                    else if (vehicleType === "bongo" || vehicleType === "Kia Bongo") iconEmoji = "🚚";
                    else if (vehicleType === "tractor" || vehicleType === "Tractor") iconEmoji = "🚜";
                    else if (vehicleType === "truck" || vehicleType === "Heavy Truck") iconEmoji = "🚛";
                    else if (vehicleType === "taxi" || vehicleType === "Taxi") iconEmoji = "🚕";

                    var transEl = document.createElement('div');
                    transEl.innerHTML = "<div style='font-size: 32px; animation: bounce 0.6s infinite alternate; filter: drop-shadow(0 2px 5px rgba(0,0,0,0.5));'>" + iconEmoji + "</div>" +
                                       "<div style='background:#d97706; color:white; font-size:9px; font-weight:extrabold; padding:1px 4px; border-radius:3px; border:0.5px solid white; white-space:nowrap; margin-top:-2px;'>TRANSPORTER</div>";
                    
                    var transIcon = L.divIcon({
                        className: 'custom-anim-pin',
                        html: transEl,
                        iconSize: [80, 50],
                        iconAnchor: [40, 25]
                    });

                    marketplaceLinkMarker = L.marker([sellerLat, sellerLng], { icon: transIcon }).addTo(map);

                    var steps = 100;
                    var currentStep = 0;
                    var interval = setInterval(function() {
                        if (currentStep >= steps) {
                            clearInterval(interval);
                            if (window.AndroidInterface && window.AndroidInterface.speak) {
                                window.AndroidInterface.speak("Your matched " + vehicleType + " transporter has safely delivered the goods to your destination.");
                            }
                            return;
                        }
                        var lat = sellerLat + (buyerLat - sellerLat) * (currentStep / steps);
                        var lng = sellerLng + (buyerLng - sellerLng) * (currentStep / steps);
                        if (marketplaceLinkMarker) {
                            marketplaceLinkMarker.setLatLng([lat, lng]);
                        }
                        currentStep++;
                    }, 120);
                }

                var gisOverlays = {};
                var isSmartRouteActive = false;

                function toggleSmartRoute(isEnabled) {
                    isSmartRouteActive = isEnabled;
                }

                function toggleGISLayer(layerId, isEnabled) {
                    if (gisOverlays[layerId]) {
                        map.removeLayer(gisOverlays[layerId]);
                        delete gisOverlays[layerId];
                    }
                    if (!isEnabled) return;

                    var group = L.layerGroup();
                    if (layerId === 'heatmap') {
                        L.circle([7.5833, -1.9333], { radius: 15000, color: '#10b981', fillColor: '#10b981', fillOpacity: 0.35 }).addTo(group).bindTooltip("High Tomato Production", { permanent: true, direction: "center" });
                        L.circle([7.6333, -1.9000], { radius: 10000, color: '#10b981', fillColor: '#10b981', fillOpacity: 0.35 }).addTo(group);
                        L.circle([5.5600, -0.2050], { radius: 25000, color: '#3b82f6', fillColor: '#3b82f6', fillOpacity: 0.3 }).addTo(group).bindTooltip("High Tomato Demand", { permanent: true });
                        L.circle([6.6666, -1.6166], { radius: 20000, color: '#3b82f6', fillColor: '#3b82f6', fillOpacity: 0.3 }).addTo(group);
                        L.circle([5.5600, -0.2050], { radius: 12000, color: '#f97316', fillColor: '#f97316', fillOpacity: 0.4 }).addTo(group).bindTooltip("Highest Tomato Prices (GHS 200/crate)", { permanent: true });
                    }
                    else if (layerId === 'flood') {
                        L.polygon([
                            [7.4, -1.5],
                            [7.5, -1.3],
                            [7.2, -1.2],
                            [7.1, -1.4]
                        ], { color: '#dc2626', fillColor: '#dc2626', fillOpacity: 0.45 }).addTo(group).bindTooltip("⚠️ DANGER: FLASH FLOOD ZONE", { permanent: true });
                        L.polyline([[7.4, -1.5], [7.2, -1.2]], { color: '#dc2626', weight: 6, dashArray: "10, 10" }).addTo(group).bindTooltip("🚫 BLOCKED: Volta Transit Highway", { permanent: true });
                    }
                    else if (layerId === 'weather') {
                        L.marker([7.5833, -1.9333]).addTo(group).bindTooltip("⛅ Techiman: Rain Alert (22°C)", { permanent: true });
                        L.marker([6.6666, -1.6166]).addTo(group).bindTooltip("⛈️ Kumasi: Storm Warning (24°C)", { permanent: true });
                        L.marker([9.4000, -0.8500]).addTo(group).bindTooltip("☀️ Tamale: Dry Conditions (34°C)", { permanent: true });
                    }
                    else if (layerId === 'road_quality') {
                        L.polyline([[7.5833, -1.9333], [6.6666, -1.6166]], { color: '#16a34a', weight: 5 }).addTo(group).bindTooltip("Good Road: Highway 100% Paved", { permanent: false });
                        L.polyline([[7.5833, -1.9333], [7.3804, -1.3681]], { color: '#dc2626', weight: 4 }).addTo(group).bindTooltip("Bad Road: Severe Potholes", { permanent: false });
                        L.polyline([[7.3804, -1.3681], [6.6666, -1.6166]], { color: '#eab308', weight: 4 }).addTo(group).bindTooltip("Seasonal Transit Road", { permanent: false });
                    }
                    else if (layerId === 'cold_storage') {
                        L.marker([7.5855, -1.9280]).addTo(group).bindTooltip("❄️ Bono East Cold Storage Silo (Active)", { permanent: true });
                        L.marker([5.8078, -0.3503]).addTo(group).bindTooltip("❄️ Nsawam Cold Room Aggregator", { permanent: true });
                    }
                    else if (layerId === 'market_demand') {
                        L.circle([7.5833, -1.9333], { radius: 8000, color: '#16a34a', fillColor: '#16a34a', fillOpacity: 0.5 }).addTo(group).bindTooltip("Demand: HIGH (Green)", { permanent: true });
                        L.circle([7.3349, -2.3124], { radius: 8000, color: '#eab308', fillColor: '#eab308', fillOpacity: 0.5 }).addTo(group).bindTooltip("Demand: MEDIUM (Yellow)", { permanent: true });
                        L.circle([9.4000, -0.8500], { radius: 8000, color: '#dc2626', fillColor: '#dc2626', fillOpacity: 0.5 }).addTo(group).bindTooltip("Demand: LOW (Red)", { permanent: true });
                    }
                    else if (layerId === 'transport_density') {
                        L.marker([7.5800, -1.9200]).addTo(group).bindTooltip("🚛 Transporter Available (Kia Ceres)", { permanent: false });
                        L.marker([7.5900, -1.9400]).addTo(group).bindTooltip("🚛 Transporter Available (Kia Bongo)", { permanent: false });
                        L.marker([6.6700, -1.6100]).addTo(group).bindTooltip("🚛 Heavy Carrier (Hyundai Mighty)", { permanent: false });
                    }

                    group.addTo(map);
                    gisOverlays[layerId] = group;
                }

                function setMapSettings(tilt, traffic, userLat, userLng) {
                    isTilted = tilt;
                    isTrafficVisible = traffic;
                    if (userLat && userLng) {
                        userCoords = [userLat, userLng];
                        updateUserMarker();
                    }
                    applyMapRotationAndTilt();
                    applyTrafficOverlay();
                }

                function applyMapRotationAndTilt() {
                    var mapEl = document.getElementById('map');
                    var transformStr = "";
                    if (isTilted) {
                        transformStr += "rotateX(45deg) scale(1.22) translateY(-5%) ";
                    } else {
                        transformStr += "rotateX(0deg) scale(1.0) ";
                    }
                    if (mapHeading !== 0) {
                        transformStr += "rotateZ(" + mapHeading + "deg) ";
                    }
                    mapEl.style.transform = transformStr;
                    mapEl.style.transformOrigin = "center bottom";
                }

                function applyTrafficOverlay() {
                    trafficGroup.clearLayers();
                    if (!isTrafficVisible) return;

                    var trafficSegments = [
                        { points: [hubs["techiman"], hubs["tuobodom"]], status: "heavy" },
                        { points: [hubs["techiman"], hubs["sunyani"]], status: "moderate" }
                    ];

                    trafficSegments.forEach(function(seg) {
                        L.polyline(seg.points, { className: 'traffic-' + seg.status }).addTo(trafficGroup);
                    });
                }

                function searchAndCenter(lat, lng, label) {
                    map.flyTo([lat, lng], 14, { duration: 1.5 });
                    userCoords = [lat - 0.002, lng - 0.002];
                    updateUserMarker();
                }

                 var demoGroup = L.layerGroup().addTo(mapInstance);
                var demoTimer = null;
                var demoVehicles = [];

                function clearDemos() {
                    demoGroup.clearLayers();
                    if (demoTimer) {
                        clearInterval(demoTimer);
                        demoTimer = null;
                    }
                    demoVehicles.forEach(function(v) {
                        if (v.marker) map.removeLayer(v.marker);
                        if (v.line) map.removeLayer(v.line);
                        if (v.interval) clearInterval(v.interval);
                    });
                    demoVehicles = [];
                }

                function addDemoNode(coords, name, emoji, ringClass, bgStyle) {
                    var el = document.createElement('div');
                    el.className = 'pin-harvest-item';
                    el.style.zIndex = "1000";
                    el.innerHTML = "<div class='" + ringClass + "'></div><div style='font-size:24px; z-index:10;'>" + emoji + "</div>" +
                                   "<div style='background:" + bgStyle + "; color:white; font-size:9.5px; font-weight:bold; padding:2px 5px; border-radius:4px; border:1.5px solid white; white-space:nowrap; margin-top:2px; box-shadow:0 2px 4px rgba(0,0,0,0.35); text-shadow:1px 1px 1px rgba(0,0,0,0.8);'>" + name + "</div>";
                    var icon = L.divIcon({ className: 'custom-anim-pin', html: el, iconSize: [130, 65], iconAnchor: [65, 32.5] });
                    var m = L.marker(coords, { icon: icon }).addTo(demoGroup);
                    m.bindTooltip("<b style='color:#67e8f9;'>📍 Hub Node: " + name + "</b><br><small>Fully active in animation matrix</small>", { sticky: true });
                }

                function demonstrateAnimation(demoType) {
                    clearDemos();
                    
                    var bounds = [];
                    var msg = "";

                    if (demoType === 'maize') {
                        msg = "Demonstrating White Maize & Cereals Supply Chain Flow across Northern, Central, and Coastal networks";
                        
                        // Add professional supply chain nodes
                        addDemoNode(hubs["ejura"], "Ejura Grains Belt", "🌽", "pulse-green-ring", "#065f46");
                        addDemoNode(hubs["nkoranza"], "Nkoranza Fields", "🌾", "pulse-green-ring", "#065f46");
                        addDemoNode(hubs["tamale"], "Tamale Silos", "🌾", "pulse-green-ring", "#0f172a");
                        addDemoNode(hubs["techiman"], "Techiman Central Silo", "🏢", "pulse-blue-ring", "#1e3a8a");
                        addDemoNode(hubs["kumasi"], "Kumasi Regional Grains Hub", "🏪", "pulse-blue-ring", "#1e3a8a");
                        addDemoNode(hubs["accra"], "Accra Market Terminal", "🏪", "pulse-blue-ring", "#0f172a");
                        addDemoNode([8.2200, -0.7900], "Yeji Port", "⚓", "pulse-blue-ring", "#0284c7");
                        addDemoNode([6.2800, 0.0500], "Akosombo Port", "⚓", "pulse-blue-ring", "#0284c7");

                        // Spawn multiple parallel animated vehicles
                        runMovingDemoVehicle("🚜 Ejura Tractor (Maize Crop)", "🚜", [[7.3804, -1.3681], [7.5645, -1.7011], [7.5833, -1.9333]], '#10b981', '🌽');
                        runMovingDemoVehicle("🛺 Nkoranza Local Cart", "🛺", [[7.5645, -1.7011], [7.5833, -1.9333]], '#34d399', '🌾');
                        runMovingDemoVehicle("🚚 Tamale Cargo (Northern Millet)", "🚚", [[9.4000, -0.8500], [8.0538, -1.7289], [7.5833, -1.9333]], '#059669', '🌾');
                        runMovingDemoVehicle("🚢 Lake Volta Grain Barge", "🚢", [[8.2200, -0.7900], [7.8000, -0.0200], [6.2800, 0.0500]], '#38bdf8', '🌽');
                        runMovingDemoVehicle("🚛 Heavy Semi (Bulk Grain)", "🚛", [[7.5833, -1.9333], [6.6666, -1.6166]], '#047857', '🌽');
                        runMovingDemoVehicle("🚛 Inter-State Transport (Maize Supply)", "🚛", [[6.6666, -1.6166], [5.8078, -0.3503], [5.5600, -0.2050]], '#065f46', '🌽');

                        bounds = [[5.5, -2.0], [9.5, -0.2]];
                    }
                    else if (demoType === 'fruits') {
                        msg = "Demonstrating Citrus & Tropical Fruits Cold Logistics Cascade from western groves to cold stores";
                        
                        // Add nodes
                        addDemoNode(hubs["sunyani"], "Sunyani Citrus Grove", "🍊", "pulse-orange-ring", "#c2410c");
                        addDemoNode([7.7420, -2.1050], "Wenchi Mango Orchard", "🥭", "pulse-orange-ring", "#c2410c");
                        addDemoNode(hubs["tuobodom"], "Tuobodom Pineapple Farms", "🍍", "pulse-orange-ring", "#c2410c");
                        addDemoNode(hubs["nsawam"], "Nsawam Cold Storage", "❄️🏢", "pulse-blue-ring", "#0369a1");
                        addDemoNode(hubs["kumasi"], "Kumasi Fresh Hub", "❄️🏪", "pulse-blue-ring", "#0284c7");
                        addDemoNode(hubs["accra"], "Accra Fresh Market", "🏪", "pulse-blue-ring", "#0f172a");

                        // Spawn cold chain fleet
                        runMovingDemoVehicle("🚐 Sunyani Cold Van (Citrus Cargo)", "🚐", [[7.3349, -2.3124], [6.6666, -1.6166]], '#f97316', '🍊');
                        runMovingDemoVehicle("🚚 Wenchi Mango Carrier", "🚚", [[7.7420, -2.1050], [7.5833, -1.9333]], '#f59e0b', '🥭');
                        runMovingDemoVehicle("🛺 Tuobodom pineapple cart", "🛺", [[7.6333, -1.9000], [7.5833, -1.9333]], '#ca8a04', '🍍');
                        runMovingDemoVehicle("🚚 Techiman Cold-Chain Shuttle", "🚚", [[7.5833, -1.9333], [6.6666, -1.6166]], '#f97316', '🥭');
                        runMovingDemoVehicle("🚛 Refrigerated Semi-Trailer", "🚛", [[6.6666, -1.6166], [5.8078, -0.3503]], '#ea580c', '🍍');
                        runMovingDemoVehicle("🚐 Local Delivery Van", "🚐", [[5.8078, -0.3503], [5.5600, -0.2050]], '#dd6b20', '🍊');

                        bounds = [[5.5, -2.4], [7.8, -0.3]];
                    }
                    else if (demoType === 'tubers') {
                        msg = "Demonstrating Yam & Tuber Footpath Rural Logistics and wholesale dispatch";
                        
                        // Add nodes
                        addDemoNode([8.0520, -1.7300], "Kintampo Yam Plots", "🥔", "pulse-brown-ring", "#78350f");
                        addDemoNode([7.7390, -2.1100], "Wenchi Cassava Valley", "🍠", "pulse-brown-ring", "#78350f");
                        addDemoNode(hubs["techiman"], "Techiman Market Tuber Zone", "🏪", "pulse-blue-ring", "#b45309");
                        addDemoNode(hubs["kumasi"], "Kumasi Tuber Terminal", "🏪", "pulse-blue-ring", "#b45309");
                        addDemoNode(hubs["accra"], "Accra Makola Tuber Depot", "🏪", "pulse-blue-ring", "#78350f");

                        // Spawn footpath collection & bulk delivery
                        runMovingDemoVehicle("🏍️ Kintampo Okada (Yam Express)", "🏍️", [[8.0520, -1.7300], [8.0538, -1.7289], [7.5833, -1.9333]], '#a16207', '🥔');
                        runMovingDemoVehicle("🏍️ Wenchi Okada (Cassava Link)", "🏍️", [[7.7390, -2.1100], [7.5833, -1.9333]], '#854d0e', '🍠');
                        runMovingDemoVehicle("🛺 Field Aboboyaa (Tuber Feed)", "🛺", [[7.6333, -1.9000], [7.5833, -1.9333]], '#b45309', '🥔');
                        runMovingDemoVehicle("🚚 Kia Ceres Yam Transporter", "🚚", [[7.5833, -1.9333], [6.6666, -1.6166]], '#78350f', '🥔');
                        runMovingDemoVehicle("🚛 Heavy Tuber Freight (Accra Bulk)", "🚛", [[6.6666, -1.6166], [5.8078, -0.3503], [5.5600, -0.2050]], '#451a03', '🥔');

                        bounds = [[5.5, -2.2], [8.2, -0.3]];
                    }
                    else if (demoType === 'cashcrops') {
                        msg = "Demonstrating Premium Cocoa & Cashew Premium Export Logistics to Tema Deepwater Port";
                        
                        // Add nodes
                        addDemoNode([7.5790, -1.9290], "Bono Premium Cocoa", "🍫", "pulse-yellow-ring", "#854d0e");
                        addDemoNode(hubs["sunyani"], "Sunyani Cashew Coop", "🥜", "pulse-yellow-ring", "#ca8a04");
                        addDemoNode(hubs["kumasi"], "Kumasi Cocoa Board Warehouse", "🏢", "pulse-blue-ring", "#1e3a8a");
                        addDemoNode([5.6200, -0.0100], "Tema Deepwater Port", "🛳️🚢", "pulse-blue-ring", "#1e3a8a");
                        addDemoNode(hubs["accra"], "Accra Port Gate", "🏢", "pulse-blue-ring", "#0f172a");

                        // Spawn cash crops export corridors
                        runMovingDemoVehicle("🚚 Sunyani Cashew Collector", "🚚", [[7.3349, -2.3124], [6.6666, -1.6166]], '#ca8a04', '🥜');
                        runMovingDemoVehicle("🚚 Bono West Cocoa Shuttle", "🚚", [[7.5790, -1.9290], [6.6666, -1.6166]], '#854d0e', '🍫');
                        runMovingDemoVehicle("🚛 Heavy Export Flatbed (Cocoa Bags)", "🚛", [[6.6666, -1.6166], [5.8078, -0.3503], [5.6200, -0.0100]], '#eab308', '🍫');
                        runMovingDemoVehicle("🚛 Tema Cashew Container Freight", "🚛", [[6.6666, -1.6166], [5.8078, -0.3503], [5.6200, -0.0100]], '#a16207', '🥜');
                        runMovingDemoVehicle("🚢 Container Vessel (Cocoa Export)", "🚢", [[5.6200, -0.0100], [5.5000, 0.1500]], '#1e3a8a', '🍫');

                        bounds = [[5.4, -2.4], [7.7, 0.2]];
                    }
                    else if (demoType === 'all_transport') {
                        msg = "Activating complete countrywide synchronized agricultural fleet simulation";
                        
                        // Register ALL nodes globally
                        addDemoNode(hubs["ejura"], "Ejura Grains Belt", "🌽", "pulse-green-ring", "#065f46");
                        addDemoNode(hubs["nkoranza"], "Nkoranza Fields", "🌾", "pulse-green-ring", "#065f46");
                        addDemoNode(hubs["tamale"], "Tamale Silos", "🌾", "pulse-green-ring", "#0f172a");
                        addDemoNode(hubs["techiman"], "Techiman Silos", "🏢", "pulse-blue-ring", "#1e3a8a");
                        addDemoNode(hubs["kumasi"], "Kumasi Central Market", "🏪", "pulse-blue-ring", "#0284c7");
                        addDemoNode(hubs["sunyani"], "Sunyani Cashew & Citrus", "🍊", "pulse-orange-ring", "#c2410c");
                        addDemoNode([7.7420, -2.1050], "Wenchi Orchard", "🥭", "pulse-orange-ring", "#c2410c");
                        addDemoNode(hubs["nsawam"], "Nsawam Cold Storage", "❄️🏢", "pulse-blue-ring", "#0369a1");
                        addDemoNode([8.0520, -1.7300], "Kintampo Yam plots", "🥔", "pulse-brown-ring", "#78350f");
                        addDemoNode([5.6200, -0.0100], "Tema Deepwater Port", "🛳️", "pulse-blue-ring", "#1e3a8a");
                        addDemoNode(hubs["accra"], "Accra Terminal", "🏪", "pulse-blue-ring", "#0f172a");

                        // Spawn over 15 parallel animations representing fully busy agricultural logistics
                        runMovingDemoVehicle("🚜 Ejura Tractor", "🚜", [[7.3804, -1.3681], [7.5645, -1.7011], [7.5833, -1.9333]], '#10b981', '🌽');
                        runMovingDemoVehicle("🛺 Nkoranza Local Cart", "🛺", [[7.5645, -1.7011], [7.5833, -1.9333]], '#34d399', '🌾');
                        runMovingDemoVehicle("🚚 Tamale Cargo Truck", "🚚", [[9.4000, -0.8500], [8.0538, -1.7289], [7.5833, -1.9333]], '#059669', '🌾');
                        runMovingDemoVehicle("🚢 Volta Lake Barge", "🚢", [[8.2200, -0.7900], [7.8000, -0.0200], [6.2800, 0.0500]], '#38bdf8', '🌽');
                        
                        runMovingDemoVehicle("🚐 Sunyani Cold Van", "🚐", [[7.3349, -2.3124], [6.6666, -1.6166]], '#f97316', '🍊');
                        runMovingDemoVehicle("🚚 Wenchi Mango Carrier", "🚚", [[7.7420, -2.1050], [7.5833, -1.9333]], '#f59e0b', '🥭');
                        runMovingDemoVehicle("🛺 Tuobodom pineapple cart", "🛺", [[7.6333, -1.9000], [7.5833, -1.9333]], '#ca8a04', '🍍');
                        runMovingDemoVehicle("🚛 Refrigerated Semi-Trailer", "🚛", [[6.6666, -1.6166], [5.8078, -0.3503]], '#ea580c', '🍍');
                        
                        runMovingDemoVehicle("🏍️ Kintampo Okada", "🏍️", [[8.0520, -1.7300], [8.0538, -1.7289], [7.5833, -1.9333]], '#a16207', '🥔');
                        runMovingDemoVehicle("🏍️ Wenchi Okada", "🏍️", [[7.7390, -2.1100], [7.5833, -1.9333]], '#854d0e', '🍠');
                        runMovingDemoVehicle("🚚 Kia Ceres Yam Truck", "🚚", [[7.5833, -1.9333], [6.6666, -1.6166]], '#78350f', '🥔');
                        runMovingDemoVehicle("🚛 Heavy Tuber Freight", "🚛", [[6.6666, -1.6166], [5.8078, -0.3503], [5.5600, -0.2050]], '#451a03', '🥔');

                        runMovingDemoVehicle("🚚 Sunyani Cashew Bus", "🚚", [[7.3349, -2.3124], [6.6666, -1.6166]], '#ca8a04', '🥜');
                        runMovingDemoVehicle("🚛 Heavy Cocoa Flatbed", "🚛", [[6.6666, -1.6166], [5.8078, -0.3503], [5.6200, -0.0100]], '#eab308', '🍫');
                        runMovingDemoVehicle("🚢 Deepsea Cocoa Vessel", "🚢", [[5.6200, -0.0100], [5.5000, 0.1500]], '#1e3a8a', '🍫');

                        bounds = [[5.4, -2.4], [9.5, 0.2]];
                    }

                    if (bounds.length > 0) {
                        map.fitBounds(L.latLngBounds(bounds), { padding: [80, 80] });
                    }

                    if (window.AndroidInterface && window.AndroidInterface.speak) {
                        window.AndroidInterface.speak(msg);
                    }
                }

                function runMovingDemoVehicle(label, vehicleEmoji, pathPoints, trailColor, cargoEmoji) {
                    var line = L.polyline(pathPoints, {
                        color: trailColor,
                        weight: 3.5,
                        opacity: 0.8,
                        dashArray: '5, 8'
                    }).addTo(demoGroup);

                    var el = document.createElement('div');
                    el.style.display = 'flex';
                    el.style.flexDirection = 'column';
                    el.style.alignItems = 'center';
                    el.innerHTML = "<div style='position:relative;'>" +
                                        "<div style='font-size:26px; animation: bounce 0.6s infinite alternate;'>" + vehicleEmoji + "</div>" +
                                        "<div style='position:absolute; top:-12px; right:-6px; font-size:14px; animation: float-slow 1.8s infinite;'>" + cargoEmoji + "</div>" +
                                   "</div>" +
                                   "<div style='background:rgba(15,23,42,0.92); color:white; font-size:8.5px; font-weight:bold; padding:1px 4px; border-radius:3px; border:0.5px solid " + trailColor + "; white-space:nowrap; pointer-events:none; box-shadow:0 2px 4px rgba(0,0,0,0.3); text-shadow:1px 1px 1px rgba(0,0,0,0.8);'>" + label + "</div>";

                    var icon = L.divIcon({
                        className: 'custom-anim-pin',
                        html: el,
                        iconSize: [120, 50],
                        iconAnchor: [60, 25]
                    });

                    var marker = L.marker(pathPoints[0], { icon: icon }).addTo(demoGroup);

                    var currentIdx = 0;
                    var subSteps = 50;
                    var stepIdx = 0;

                    var interval = setInterval(function() {
                        if (currentIdx >= pathPoints.length - 1) {
                            currentIdx = 0;
                            stepIdx = 0;
                        }

                        var pStart = pathPoints[currentIdx];
                        var pEnd = pathPoints[currentIdx + 1];

                        var lat = pStart[0] + (pEnd[0] - pStart[0]) * (stepIdx / subSteps);
                        var lng = pStart[1] + (pEnd[1] - pStart[1]) * (stepIdx / subSteps);

                        marker.setLatLng([lat, lng]);

                        stepIdx++;
                        if (stepIdx >= subSteps) {
                            stepIdx = 0;
                            currentIdx++;
                        }
                    }, 40);

                    demoVehicles.push({ marker: marker, line: line, interval: interval });
                }

                renderStaticMarkers();
                updateDriverPositions();
                updateUserMarker();
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    // Instantiating WebView cleanly
    val webView = remember {
        android.webkit.WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            
            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun onPinSelected(title: String, desc: String, typeStr: String, contact: String?, lat: Double, lng: Double, extraDataJson: String) {
                    val type = when (typeStr) {
                        "FARM" -> MapLocationType.FARM
                        "MARKET" -> MapLocationType.MARKET
                        "WAREHOUSE" -> MapLocationType.WAREHOUSE
                        "TRANSPORTER" -> MapLocationType.TRANSPORTER
                        else -> MapLocationType.INTERMEDIATE
                    }
                    val detailsMap = mutableMapOf<String, String>()
                    try {
                        val jsonObj = org.json.JSONObject(extraDataJson)
                        val keys = jsonObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            detailsMap[key] = jsonObj.optString(key, "")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    selectedPin = SelectedMapPin(
                        title = title,
                        desc = desc,
                        type = type,
                        contact = contact,
                        lat = lat,
                        lng = lng,
                        extraDetails = detailsMap
                    )
                }

                @android.webkit.JavascriptInterface
                fun onRouteCalculated(distanceKm: Double, timeMins: Int, arrivalTime: String, instructionsJsonArray: String) {
                    remainingDistance = distanceKm
                    remainingTimeMinutes = timeMins
                    arrivalTimeStr = arrivalTime
                    try {
                        val jsonArray = org.json.JSONArray(instructionsJsonArray)
                        val list = mutableListOf<String>()
                        for (i in 0 until jsonArray.length()) {
                            list.add(jsonArray.getString(i))
                        }
                        stepInstructions = list
                        currentStepIndex = 0
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                @android.webkit.JavascriptInterface
                fun onNavigationProgress(speed: Int, ratio: Double) {
                    currentSpeed = speed
                    remainingRatio = ratio
                    remainingDistance = Math.max(0.0, remainingDistance * ratio)
                    remainingTimeMinutes = Math.max(0, (remainingTimeMinutes * ratio).toInt())
                    
                    if (stepInstructions.isNotEmpty()) {
                        val stepSize = 1.0 / stepInstructions.size
                        val progress = 1.0 - ratio
                        currentStepIndex = Math.min(stepInstructions.size - 1, (progress / stepSize).toInt())
                    }
                }

                @android.webkit.JavascriptInterface
                fun speak(text: String) {
                    if (!isMuted) {
                        tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "GeoHarvestVoice")
                    }
                }

                @android.webkit.JavascriptInterface
                fun onGeofenceTriggered(zoneName: String, eventType: String, message: String) {
                    if (eventType == "ENTER") {
                        activeGeofenceAlert = message
                        speak(message)
                    } else {
                        activeGeofenceAlert = null
                    }
                }
            }, "AndroidInterface")

            webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isPageLoaded = true
                }
            }
            loadDataWithBaseURL("https://leafletjs.com", mapHtml, "text/html", "UTF-8", null)
        }
    }

    LaunchedEffect(isPageLoaded, isTilted, isTrafficVisible, userLat, userLng) {
        if (isPageLoaded) {
            val uLat = userLat ?: 7.5833
            val uLng = userLng ?: -1.9333
            webView.evaluateJavascript("setMapSettings($isTilted, $isTrafficVisible, $uLat, $uLng)", null)
        }
    }

    LaunchedEffect(isPageLoaded, chosenTransportForMap) {
        if (isPageLoaded && chosenTransportForMap != null) {
            webView.evaluateJavascript("triggerVehicleAnimation('$chosenTransportForMap')", null)
        }
    }

    LaunchedEffect(isPageLoaded, isHeatmapEnabled) {
        if (isPageLoaded) {
            webView.evaluateJavascript("toggleGISLayer('heatmap', $isHeatmapEnabled)", null)
        }
    }
    LaunchedEffect(isPageLoaded, isFloodEnabled) {
        if (isPageLoaded) {
            webView.evaluateJavascript("toggleGISLayer('flood', $isFloodEnabled)", null)
        }
    }
    LaunchedEffect(isPageLoaded, isWeatherEnabled) {
        if (isPageLoaded) {
            webView.evaluateJavascript("toggleGISLayer('weather', $isWeatherEnabled)", null)
        }
    }
    LaunchedEffect(isPageLoaded, isRoadQualityEnabled) {
        if (isPageLoaded) {
            webView.evaluateJavascript("toggleGISLayer('road_quality', $isRoadQualityEnabled)", null)
        }
    }
    LaunchedEffect(isPageLoaded, isColdStorageEnabled) {
        if (isPageLoaded) {
            webView.evaluateJavascript("toggleGISLayer('cold_storage', $isColdStorageEnabled)", null)
        }
    }
    LaunchedEffect(isPageLoaded, isMarketDemandEnabled) {
        if (isPageLoaded) {
            webView.evaluateJavascript("toggleGISLayer('market_demand', $isMarketDemandEnabled)", null)
        }
    }
    LaunchedEffect(isPageLoaded, isTransportDensityEnabled) {
        if (isPageLoaded) {
            webView.evaluateJavascript("toggleGISLayer('transport_density', $isTransportDensityEnabled)", null)
        }
    }
    LaunchedEffect(isPageLoaded, isSmartRouteEnabled) {
        if (isPageLoaded) {
            webView.evaluateJavascript("toggleSmartRoute($isSmartRouteEnabled)", null)
        }
    }

    LaunchedEffect(isPageLoaded, cropDensityToShow) {
        val crop = cropDensityToShow
        if (isPageLoaded && crop != null) {
            webView.evaluateJavascript("showCropDensity('${crop.replace("'", "\\'")}')", null)
            viewModel.cropDensityToShow.value = null
        }
    }

    LaunchedEffect(isPageLoaded, focusLat, focusLng) {
        if (isPageLoaded && focusLat != null && focusLng != null) {
            val title = focusTitle ?: "Focused Location"
            val desc = focusDesc ?: ""
            val type = focusType ?: "FARM"
            
            webView.evaluateJavascript("map.setView([$focusLat, $focusLng], 14);", null)
            
            val escapedTitle = title.replace("'", "\\'")
            val escapedDesc = desc.replace("'", "\\'")
            
            webView.evaluateJavascript("""
                var tempIcon = L.divIcon({
                    className: 'custom-pin',
                    html: '<div style="background-color: #ef4444; width: 26px; height: 26px; border-radius: 50%; border: 3px solid white; box-shadow: 0 0 10px rgba(239, 68, 68, 0.8); display: flex; justify-content: center; align-items: center; font-size: 14px;">🎯</div>',
                    iconSize: [26, 26],
                    iconAnchor: [13, 13]
                });
                var tempMarker = L.marker([$focusLat, $focusLng], { icon: tempIcon }).addTo(map);
                var tempPopup = L.popup()
                    .setLatLng([$focusLat, $focusLng])
                    .setContent('<div style="font-family: system-ui; padding: 4px; min-width: 170px; color: white;"><strong style="font-size: 13px; color: #f8fafc;">' + '$escapedTitle' + '</strong><div style="color: #cbd5e1; font-size: 11px; margin-top: 4px; line-height: 1.4;">' + '$escapedDesc' + '</div><button onclick="startNavigationSimulation(' + $focusLat + ', ' + $focusLng + '); map.closePopup();" style="width: 100%; background: #10b981; color: white; border: none; padding: 6px 10px; border-radius: 6px; font-weight: bold; font-size: 11px; margin-top: 8px; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 4px;">🧭 Get Directions</button></div>')
                    .openOn(map);
            """.trimIndent(), null)
            
            selectedPin = SelectedMapPin(
                title = title,
                desc = desc,
                type = when(type) {
                    "MARKET" -> MapLocationType.MARKET
                    "TRANSPORTER" -> MapLocationType.TRANSPORTER
                    "WAREHOUSE" -> MapLocationType.WAREHOUSE
                    else -> MapLocationType.FARM
                },
                contact = "+233245551234",
                lat = focusLat!!,
                lng = focusLng!!,
                extraDetails = emptyMap()
            )
            viewModel.clearMapFocus()
        }
    }

    LaunchedEffect(isPageLoaded, selectedMapStyle) {
        if (isPageLoaded) {
            webView.evaluateJavascript("setMapStyle('$selectedMapStyle')", null)
        }
    }

    LaunchedEffect(isPageLoaded, isMultiStopMode, selectedStops) {
        if (isPageLoaded) {
            if (isMultiStopMode && selectedStops.isNotEmpty()) {
                val jsonArray = org.json.JSONArray().apply {
                    selectedStops.forEach {
                        put(org.json.JSONObject().apply {
                            put("lat", it.lat)
                            put("lng", it.lng)
                            put("label", it.title)
                        })
                    }
                }
                webView.evaluateJavascript("drawMultiStopRoute('$jsonArray')", null)
            } else {
                webView.evaluateJavascript("clearMultiStopRoute()", null)
            }
        }
    }

    if (chosenTransportForMap == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .statusBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Icon(
                imageVector = Icons.Default.LocalShipping,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Select a Mean of Transport",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Which carrier type is dispatching or delivering your crops near Techiman today?",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            val transports = listOf(
                TransportOption("bike", "🏍️", "Okada Motorbike", "100kg max", "GHS 1.5/km", "Fastest route for small crop bags"),
                TransportOption("aboboyaa", "🛺", "Aboboyaa Tricycle", "600kg max", "GHS 3.0/km", "Local farm-to-highway transit"),
                TransportOption("bongo", "🚚", "Kia Bongo Mini Truck", "1.5 Ton max", "GHS 4.5/km", "Standard local farm distribution"),
                TransportOption("tractor", "🚜", "Massey Power Tractor", "3 Ton max", "GHS 10.0/km", "Muddy off-road field hauler"),
                TransportOption("truck", "🚛", "Hyundai Mighty Truck", "5 Ton max", "GHS 8.5/km", "Heavy long-haul highway cargo"),
                TransportOption("taxi", "🚕", "Hatchback Taxi", "200kg max", "GHS 2.2/km", "Suburban small-packet express delivery"),
                TransportOption("cold_van", "🚐", "Refrigerated Cold Van", "2 Ton max", "GHS 6.5/km", "Fresh cold chain vegetable preservation")
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(transports) { t ->
                    Card(
                        onClick = {
                            chosenTransportForMap = t.id
                            selectedVehicleId = t.id
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.5.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 68.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = t.icon,
                                fontSize = 32.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = t.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = t.rate,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Color(0xFF10B981)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Cap: ${t.cap}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "• ${t.desc}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Render WebView Map Container
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )

        // --- AGRI-LOGISTICS LINK TRACING SHEET OVERLAY ---
        activeTransportItem?.let { item ->
            val title = remember(item) {
                if (item is ProduceListing) item.title 
                else if (item is MarketplaceItem) item.name
                else (item as? Map<String, Any>?)?.get("name")?.toString() ?: "Crops"
            }
            val farmerName = remember(item) {
                if (item is ProduceListing) item.farmerName 
                else if (item is MarketplaceItem) item.sellerName
                else (item as? Map<String, Any>?)?.get("farmerName")?.toString() ?: (item as? Map<String, Any>?)?.get("dealer")?.toString() ?: "Farmer"
            }
            val locationName = remember(item) {
                if (item is ProduceListing) item.location 
                else if (item is MarketplaceItem) item.location
                else (item as? Map<String, Any>?)?.get("loc")?.toString() ?: "Techiman"
            }

            // Standard locations coordinates
            val sellerLat = remember(locationName) {
                if (locationName.contains("Tuobodom", ignoreCase = true)) 7.6333 
                else if (locationName.contains("Kintampo", ignoreCase = true)) 8.0538
                else if (locationName.contains("Nkoranza", ignoreCase = true)) 7.5645
                else if (locationName.contains("Sunyani", ignoreCase = true)) 7.3349
                else if (locationName.contains("Kumasi", ignoreCase = true)) 6.6666
                else 7.5833 // default Techiman
            }
            val sellerLng = remember(locationName) {
                if (locationName.contains("Tuobodom", ignoreCase = true)) -1.9000
                else if (locationName.contains("Kintampo", ignoreCase = true)) -1.7289
                else if (locationName.contains("Nkoranza", ignoreCase = true)) -1.7011
                else if (locationName.contains("Sunyani", ignoreCase = true)) -2.3124
                else if (locationName.contains("Kumasi", ignoreCase = true)) -1.6166
                else -1.9333 // default Techiman
            }

            val bLat = userLat ?: 7.5833
            val bLng = userLng ?: -1.9333

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .zIndex(15f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)),
                border = BorderStroke(2.dp, Color(0xFF3B82F6))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.2f), CircleShape)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = "Tracing",
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "AGRI-LOGISTICS TRACING",
                                    color = Color(0xFF60A5FA),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = "$title ($locationName)",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = {
                                viewModel.activeMarketplaceTransportItem.value = null
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, "Close", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF334155)))

                    if (!isMarketplaceTracingActive) {
                        Text(
                            text = "Step 1: Select Means of Transport",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        // Transport Selector Horizontal Scroll Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val vehicles = listOf(
                                Triple("Okada", "🏍️", "GHS 1.5/km"),
                                Triple("Aboboyaa", "🛺", "GHS 3.0/km"),
                                Triple("Kia Bongo", "🚚", "GHS 6.0/km"),
                                Triple("Tractor", "🚜", "GHS 10.0/km")
                            )
                            vehicles.forEach { (vName, emoji, rates) ->
                                val isSel = selectedMarketplaceVehicle == vName
                                Card(
                                    onClick = { selectedMarketplaceVehicle = vName },
                                    modifier = Modifier.width(115.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) Color(0xFF2563EB) else Color(0xFF1E293B)
                                    ),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isSel) Color(0xFF60A5FA) else Color(0xFF334155)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(emoji, fontSize = 24.sp)
                                        Text(vName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(rates, color = Color.LightGray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // AI Optimization Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                .clickable { isAiFastRouterActive = !isAiFastRouterActive }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape)
                                        .padding(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = "AI",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text("🧠 Fast AI Route Optimization", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Bypass muddy roads, track real-time traffic", color = Color.LightGray, fontSize = 11.sp)
                                }
                            }
                            Switch(
                                checked = isAiFastRouterActive,
                                onCheckedChange = { isAiFastRouterActive = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF10B981),
                                    checkedTrackColor = Color(0xFF064E3B)
                                )
                            )
                        }

                        // Dispatch Match Button
                        Button(
                            onClick = {
                                isMarketplaceTracingActive = true
                                // Trigger Leaflet map function via WebView
                                webView.evaluateJavascript(
                                    "triggerMarketplaceLogisticsLink('$selectedMarketplaceVehicle', $sellerLat, $sellerLng, $bLat, $bLng)",
                                    null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("dispatch_marketplace_transport_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Navigation, null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("MATCH & DISPATCH CARRIER", color = Color.White, fontWeight = FontWeight.Black)
                            }
                        }
                    } else {
                        // Tracing Active HUD with AI Assistant Overlay
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = when (selectedMarketplaceVehicle) {
                                        "Okada" -> "🏍️"
                                        "Aboboyaa" -> "🛺"
                                        "Kia Bongo" -> "🚚"
                                        else -> "🚜"
                                    },
                                    fontSize = 24.sp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Matched Transporter: Kwaku Ananse", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Vehicle: $selectedMarketplaceVehicle • Status: IN TRANSIT", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Origin: $farmerName's Farm ($locationName) → Destination: Me", color = Color.LightGray, fontSize = 11.sp)
                            }
                        }

                        // AI Assistant fast advice speech bubbles
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.SmartToy, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Text("GeoHarvest AI Dispatcher", color = Color(0xFF10B981), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                                Text(
                                    text = if (isAiFastRouterActive) {
                                        "\"AI Optimized route locked! Matchmaker linked $selectedMarketplaceVehicle to $farmerName's cargo. We bypassed the muddy bypass road. Transit velocity is running 24% faster! Trace the vehicle on the map.\""
                                    } else {
                                        "\"Standard route active. Tracing $selectedMarketplaceVehicle transit along standard highways. Expected ETA: 18 mins. Watch map direction path for progress.\""
                                    },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isMarketplaceTracingActive = false
                                    webView.evaluateJavascript("map.setView([$bLat, $bLng], 12)", null)
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                            ) {
                                Text("Reset Dispatch")
                            }
                            
                            Button(
                                onClick = {
                                    viewModel.addEngagementActivity("Mary Appiah (You)", "Buyer", "Received fresh cargo from $farmerName via $selectedMarketplaceVehicle transporter!")
                                    viewModel.activeMarketplaceTransportItem.value = null
                                    Toast.makeText(context, "Cargo delivery received & logged to Agri-Ledger!", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("Confirm Receipt", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Geofencing Real-Time Alert Overlay
        activeGeofenceAlert?.let { alertMsg ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .zIndex(10f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                border = BorderStroke(2.dp, Color(0xFFEF4444))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f), CircleShape)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Geofence Warning",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "🛡️ GEOFENCE SAFETY SHIELD",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alertMsg,
                                color = Color(0xFFFECACA),
                                fontSize = 14.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            activeGeofenceAlert = null 
                            Toast.makeText(context, "Shield Alert Dismissed", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Mute Alert", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Mobile Money Escrow Payment Dialog
        if (showPaymentDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showPaymentDialog = false
                    paymentSuccess = false
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payment, "Secure MoMo Escrow", tint = Color(0xFFF59E0B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GeoHarvest MoMo Escrow", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!paymentSuccess) {
                            Text(
                                text = "Secure your agricultural purchase via our locked multi-sig agricultural escrow ledger. Funds are only disbursed upon visual delivery receipt confirmation.",
                                color = Color(0xFFCBD5E1),
                                fontSize = 15.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Amount Display
                            Text("Transaction Escrow Value", color = Color(0xFF475569), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "GHS $paymentAmount",
                                color = Color(0xFF10B981),
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Black
                            )

                            // Provider Selection Selector
                            Text("Select Mobile Wallet Operator", color = Color(0xFF475569), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("MTN MoMo", "Telecel", "AirtelTigo").forEach { provider ->
                                    val isSelected = selectedMomoProvider == provider
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.5.dp,
                                                if (isSelected) Color(0xFFF59E0B) else Color(0xFF475569),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedMomoProvider = provider }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = provider,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Phone number field input
                            Text("Enter Recipient MoMo Phone Number", color = Color(0xFF475569), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = momoNumber,
                                onValueChange = { momoNumber = it },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. 0241112233", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                singleLine = true
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape)
                                        .padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Text("MoMo Escrow Secured!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                                Text(
                                    text = "Your payment of GHS $paymentAmount has been processed successfully and locked in escrow.",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 15.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                                Text(
                                    text = "Transaction ID: TXN-MOMO-789214",
                                    color = Color(0xFF475569),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    if (!paymentSuccess) {
                        Button(
                            onClick = { paymentSuccess = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Process Escrow", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { 
                                showPaymentDialog = false
                                paymentSuccess = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Done", color = Color.White, fontSize = 15.sp)
                        }
                    }
                },
                dismissButton = {
                    if (!paymentSuccess) {
                        TextButton(onClick = { showPaymentDialog = false }) {
                            Text("Cancel", color = Color(0xFFCBD5E1), fontSize = 15.sp)
                        }
                    }
                },
                containerColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Offline Map Caching Region Selector Dialogue
        if (showOfflineDialog) {
            AlertDialog(
                onDismissRequest = { showOfflineDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDownload, "Offline Caching", tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Region Map Caching Manager", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Download and store detailed GIS maps locally to maintain offline GPS navigation tracking while in remote agricultural zones.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 15.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        listOf(
                            Triple("Techiman Hub Core", "14.2 MB", true),
                            Triple("Kintampo North Corridors", "8.5 MB", false),
                            Triple("Nkoranza Transit Grid", "6.1 MB", false)
                        ).forEach { (region, size, isCached) ->
                            var cachedState by remember { mutableStateOf(isCached) }
                            var isDownloading by remember { mutableStateOf(false) }
                            
                            LaunchedEffect(isDownloading) {
                                if (isDownloading) {
                                    kotlinx.coroutines.delay(2000)
                                    isDownloading = false
                                    cachedState = true
                                    isOfflineMode = true
                                    Toast.makeText(context, "$region map caching complete!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(region, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("Size: $size", color = Color(0xFF475569), fontSize = 13.sp)
                                    }
                                    
                                    if (isDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color(0xFF10B981),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Button(
                                            onClick = {
                                                if (!cachedState) {
                                                    isDownloading = true
                                                } else {
                                                    cachedState = false
                                                    Toast.makeText(context, "Deleted cache for $region", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (cachedState) Color(0xFF047857) else Color(0xFF2563EB)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (cachedState) "Cached" else "Download",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showOfflineDialog = false }) {
                        Text("Done", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF0F172A),
                textContentColor = Color.White,
                titleContentColor = Color.White
            )
        }

        // Floating Search & Autocomplete
        if (!isNavigationActive) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text("Search farms, markets or warehouses...", color = Color(0xFF475569), fontSize = 17.sp)
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { 
                                    searchQuery = it
                                    isSearchFocused = true
                                },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 17.sp),
                                singleLine = true,
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { 
                                    searchQuery = ""
                                    isSearchFocused = false
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, "Clear", tint = Color(0xFFCBD5E1), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Autocomplete suggestion card list
                if (filteredSuggestions.isNotEmpty() && isSearchFocused) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column {
                            filteredSuggestions.forEach { target ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchQuery = target.title
                                            isSearchFocused = false
                                            webView.evaluateJavascript("searchAndCenter(${target.lat}, ${target.lng}, '${target.title}')", null)
                                            
                                            val type = when (target.type) {
                                                "FARM" -> MapLocationType.FARM
                                                "BUYER" -> MapLocationType.MARKET
                                                "WAREHOUSE" -> MapLocationType.WAREHOUSE
                                                else -> MapLocationType.INTERMEDIATE
                                            }
                                            selectedPin = SelectedMapPin(
                                                title = target.title,
                                                desc = target.subtitle,
                                                type = type,
                                                contact = target.contact,
                                                lat = target.lat,
                                                lng = target.lng
                                            )
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = when (target.type) {
                                        "FARM" -> Icons.Default.Info
                                        "BUYER" -> Icons.Default.Layers
                                        "WAREHOUSE" -> Icons.Default.Layers
                                        else -> Icons.Default.MyLocation
                                    }
                                    Icon(icon, "Location", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(target.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(target.subtitle, color = Color(0xFF475569), fontSize = 14.sp)
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }

        // Right side floating controls
        Column(
            modifier = Modifier
                .padding(
                    end = 16.dp,
                    bottom = if (isNavigationActive) {
                        if (isNavigationMinimized) 140.dp else 340.dp
                    } else if (selectedPin != null) {
                        340.dp
                    } else {
                        100.dp
                    }
                )
                .align(Alignment.BottomEnd),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Locate Me
            FloatingActionButton(
                onClick = {
                    val uLat = userLat ?: 7.5833
                    val uLng = userLng ?: -1.9333
                    webView.evaluateJavascript("map.flyTo([$uLat, $uLng], 14, {duration: 1.2})", null)
                },
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF06B6D4),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.MyLocation, "Locate Me", modifier = Modifier.size(18.dp))
            }

            // 3D Tilt perspective toggle
            FloatingActionButton(
                onClick = { isTilted = !isTilted },
                containerColor = Color(0xFF1E293B),
                contentColor = if (isTilted) Color(0xFF10B981) else Color(0xFFCBD5E1),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Layers, "Toggle 3D View", modifier = Modifier.size(18.dp))
            }

            // Traffic overlay toggle
            FloatingActionButton(
                onClick = { 
                    isTrafficVisible = !isTrafficVisible 
                    val statusStr = if (isTrafficVisible) "Traffic overlay active" else "Traffic overlay disabled"
                    Toast.makeText(context, statusStr, Toast.LENGTH_SHORT).show()
                },
                containerColor = Color(0xFF1E293B),
                contentColor = if (isTrafficVisible) Color(0xFFEAB308) else Color(0xFFCBD5E1),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Traffic, "Traffic Map", modifier = Modifier.size(18.dp))
            }

            // Offline Area Caching toggle - Open Regional Manager Dialog
            FloatingActionButton(
                onClick = {
                    showOfflineDialog = true
                },
                containerColor = Color(0xFF1E293B),
                contentColor = if (isOfflineMode) Color(0xFF06B6D4) else Color(0xFFCBD5E1),
                modifier = Modifier.size(48.dp).testTag("offline_map_manager_button")
            ) {
                Icon(Icons.Default.CloudDownload, "Download Map Area", modifier = Modifier.size(18.dp))
            }

            // Map Style Switcher (Standard / Dark Slate / OpenStreetMap)
            var currentStyleIndex by remember { mutableStateOf(0) }
            val mapStyles = listOf("voyager", "dark", "osm")
            val styleIcons = listOf("🗺️", "🌑", "🌍")
            val styleLabels = listOf("Standard View", "Slate Dark Map", "OpenStreetMap Live")

            FloatingActionButton(
                onClick = {
                    currentStyleIndex = (currentStyleIndex + 1) % mapStyles.size
                    val targetStyle = mapStyles[currentStyleIndex]
                    webView.evaluateJavascript("setMapStyle('$targetStyle')", null)
                    Toast.makeText(context, "Map Style: ${styleLabels[currentStyleIndex]}", Toast.LENGTH_SHORT).show()
                },
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF10B981),
                modifier = Modifier.size(48.dp).testTag("map_style_switcher_button")
            ) {
                Text(
                    text = styleIcons[currentStyleIndex],
                    fontSize = 21.sp,
                    textAlign = TextAlign.Center
                )
            }

            // GIS Advanced Layers menu toggle
            FloatingActionButton(
                onClick = { showGisMenu = !showGisMenu },
                containerColor = Color(0xFF1E293B),
                contentColor = if (showGisMenu) Color(0xFF10B981) else Color(0xFFCBD5E1),
                modifier = Modifier.size(48.dp).testTag("gis_layers_button")
            ) {
                Icon(Icons.Default.Map, "GIS Layers", modifier = Modifier.size(20.dp))
            }

            // Animation Demo Menu toggle
            FloatingActionButton(
                onClick = { 
                    showDemoPanel = !showDemoPanel
                    if (showDemoPanel) {
                        showGisMenu = false
                    } else {
                        webView.evaluateJavascript("clearDemos()", null)
                    }
                },
                containerColor = Color(0xFF1E293B),
                contentColor = if (showDemoPanel) Color(0xFFEAB308) else Color(0xFFCBD5E1),
                modifier = Modifier.size(48.dp).testTag("animation_demo_button")
            ) {
                Icon(Icons.Default.Agriculture, "Live Animations", modifier = Modifier.size(22.dp))
            }
        }

        // Animation Demonstration Interactive Panel
        if (showDemoPanel) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .width(320.dp)
                    .align(Alignment.BottomStart)
                    .testTag("animation_demo_panel"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFFEAB308))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌾 Agri-Animation Demo Center", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        IconButton(onClick = { 
                            showDemoPanel = false 
                            webView.evaluateJavascript("clearDemos()", null)
                        }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(16.dp))
                        }
                    }
                    Text("Select a crop or logistics category to demonstrate dynamic map animations with pulsing fields, transport vehicles, and cargo trajectories.", fontSize = 12.sp, color = Color(0xFF94A3B8))

                    HorizontalDivider(color = Color(0xFF334155))

                    // 1. White Maize & Cereals
                    Button(
                        onClick = {
                            webView.evaluateJavascript("demonstrateAnimation('maize')", null)
                            Toast.makeText(context, "Simulating White Maize & Cereals flow...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF065F46)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🌽 White Maize & Cereals Flow", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // 2. Mixed Fruits Cascade
                    Button(
                        onClick = {
                            webView.evaluateJavascript("demonstrateAnimation('fruits')", null)
                            Toast.makeText(context, "Simulating Citrus & Fruits cascade...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2410C)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🍊 Tropical Fruits Cold Logistics", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // 3. Premium Pona Yam & Tubers
                    Button(
                        onClick = {
                            webView.evaluateJavascript("demonstrateAnimation('tubers')", null)
                            Toast.makeText(context, "Simulating Pona Yam & Tubers flow...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78350F)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🥔 Pona Yam & Tubers Footpaths", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // 4. Premium Cocoa & Cashew Cash Crops
                    Button(
                        onClick = {
                            webView.evaluateJavascript("demonstrateAnimation('cashcrops')", null)
                            Toast.makeText(context, "Simulating Cocoa & Cashew Export routes...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF854D0E)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🍫 Cocoa & Cashew Export Lines", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // 5. Simultaneous Fleet Dispatch
                    Button(
                        onClick = {
                            webView.evaluateJavascript("demonstrateAnimation('all_transport')", null)
                            Toast.makeText(context, "Dispatching full logistics fleet...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🚚 Dispatch Multi-Modal Fleet", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    // Stop/Reset
                    OutlinedButton(
                        onClick = {
                            webView.evaluateJavascript("clearDemos()", null)
                            Toast.makeText(context, "Simulations stopped and cleared", Toast.LENGTH_SHORT).show()
                        },
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🛑 Stop & Reset Animations", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // GIS Advanced Layers Interactive Controller Panel
        if (showGisMenu) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .width(320.dp)
                    .align(Alignment.BottomStart)
                    .testTag("gis_layers_panel"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFF10B981))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌍 GIS Intelligence Layers", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 17.sp)
                        IconButton(onClick = { showGisMenu = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(16.dp))
                        }
                    }
                    Text("Select spatial telemetry overlays modeling production, weather & demand.", fontSize = 13.sp, color = Color(0xFF475569))

                    HorizontalDivider(color = Color(0xFF334155))

                    // 1. Heat Maps
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Heat Maps (Prod & Price)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Highlights high-yield/highest price hubs", color = Color(0xFF475569), fontSize = 12.sp)
                        }
                        Switch(
                            checked = isHeatmapEnabled,
                            onCheckedChange = { isHeatmapEnabled = it },
                            modifier = Modifier.scale(0.8f).testTag("switch_heatmap")
                        )
                    }

                    // 2. Flood Alert
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("⚠️ Flood Zones & Blocked Roads", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Volta Basin hazard zones overlay", color = Color(0xFF475569), fontSize = 12.sp)
                        }
                        Switch(
                            checked = isFloodEnabled,
                            onCheckedChange = { isFloodEnabled = it },
                            modifier = Modifier.scale(0.8f).testTag("switch_flood")
                        )
                    }

                    // 3. Weather Layer
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("⛈️ Live Weather Layers", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Rain, temp, and storm alerts", color = Color(0xFF475569), fontSize = 12.sp)
                        }
                        Switch(
                            checked = isWeatherEnabled,
                            onCheckedChange = { isWeatherEnabled = it },
                            modifier = Modifier.scale(0.8f).testTag("switch_weather")
                        )
                    }

                    // 4. Road Quality
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("🛣️ Road Quality Analyzer", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Paved, potholes & seasonal tracks", color = Color(0xFF475569), fontSize = 12.sp)
                        }
                        Switch(
                            checked = isRoadQualityEnabled,
                            onCheckedChange = { isRoadQualityEnabled = it },
                            modifier = Modifier.scale(0.8f).testTag("switch_road")
                        )
                    }

                    // 5. Cold Storage
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("❄️ Cold Storage & Warehouses", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Locate nearby active aggregation rooms", color = Color(0xFF475569), fontSize = 12.sp)
                        }
                        Switch(
                            checked = isColdStorageEnabled,
                            onCheckedChange = { isColdStorageEnabled = it },
                            modifier = Modifier.scale(0.8f).testTag("switch_cold_storage")
                        )
                    }

                    // 6. Market Demand
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("📈 Market Demand Indicators", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Color-coded high, med, low zones", color = Color(0xFF475569), fontSize = 12.sp)
                        }
                        Switch(
                            checked = isMarketDemandEnabled,
                            onCheckedChange = { isMarketDemandEnabled = it },
                            modifier = Modifier.scale(0.8f).testTag("switch_demand")
                        )
                    }

                    // 7. Transport Density
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("🚚 Transport Carrier Density", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Realtime available carrier vectors", color = Color(0xFF475569), fontSize = 12.sp)
                        }
                        Switch(
                            checked = isTransportDensityEnabled,
                            onCheckedChange = { isTransportDensityEnabled = it },
                            modifier = Modifier.scale(0.8f).testTag("switch_transport_density")
                        )
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    // 8. Smart Routing Detours
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("🔄 Smart Route Detour Optimization", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 14.sp)
                            Text("Avoids bad roads & floods automatically", color = Color(0xFF475569), fontSize = 12.sp)
                        }
                        Switch(
                            checked = isSmartRouteEnabled,
                            onCheckedChange = { isSmartRouteEnabled = it },
                            modifier = Modifier.scale(0.8f).testTag("switch_smart_routing")
                        )
                    }
                }
            }
        }

        // Selected Pin Detail Bottom sheet Card
        selectedPin?.let { pin ->
            if (!isNavigationActive) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(24.dp))
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.5.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(40.dp)
                                .height(4.dp)
                                .background(Color(0xFF475569), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val badgeColor = when (pin.type) {
                                MapLocationType.FARM -> Color(0xFF10B981)
                                MapLocationType.MARKET -> Color(0xFF3B82F6)
                                MapLocationType.WAREHOUSE -> Color(0xFF64748B)
                                MapLocationType.TRANSPORTER -> Color(0xFFEAB308)
                                else -> Color(0xFF06B6D4)
                            }
                            Box(
                                modifier = Modifier
                                    .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = pin.type.name,
                                    color = badgeColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            IconButton(onClick = { selectedPin = null }) {
                                Icon(Icons.Default.Close, "Close", tint = Color(0xFFCBD5E1))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Premium Ghanaian Profile Card Header (Black names & ratings)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape)
                                    .border(1.5.dp, Color(0xFF10B981), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pin.title.take(2).uppercase(),
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(pin.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, "Rating", tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    val ratingVal = if (pin.title.contains("Amma")) 4.9 else if (pin.title.contains("Alhaji")) 4.9 else if (pin.title.contains("Kwame")) 4.8 else 4.7
                                    Text("$ratingVal • Verified Ghanaian Profile", color = Color(0xFFCBD5E1), fontSize = 14.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(pin.desc, color = Color(0xFFCBD5E1), fontSize = 15.sp, lineHeight = 16.sp)
                        
                        var computedDistance = 0.0
                        userLat?.let { uLat ->
                            userLng?.let { uLng ->
                                pin.lat?.let { pLat ->
                                    pin.lng?.let { pLng ->
                                        computedDistance = calculateDistance(uLat, uLng, pLat, pLng)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Directions, "Distance", tint = Color(0xFF06B6D4), modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Road Distance: ${String.format("%.1f", computedDistance)} km",
                                                color = Color(0xFF06B6D4),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Display price of transporter calculated and displayed in RED
                        if (pin.type == MapLocationType.TRANSPORTER) {
                            val ratePerKm = if (pin.title.contains("Alhaji")) 4.5 else if (pin.title.contains("Kwame")) 8.5 else if (pin.title.contains("Yaw")) 1.5 else 3.0
                            val estimatedFare = 15.0 + (computedDistance * ratePerKm)
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("ESTIMATED TRANSIT FARE", color = Color(0xFFCBD5E1), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "GHS ${String.format("%.2f", estimatedFare)}",
                                        color = Color.Red, // Highlighted calculated price in RED
                                        fontWeight = FontWeight.Black,
                                        fontSize = 23.sp
                                    )
                                    Text(
                                        text = "Base Rate: GHS $ratePerKm / km (Incl. Fuel Surcharge)",
                                        color = Color(0xFF475569),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Direct Live Chat Section
                        var showChatBox by remember { mutableStateOf(false) }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showChatBox = !showChatBox },
                            colors = ButtonDefaults.buttonColors(containerColor = if (showChatBox) Color(0xFF10B981) else Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Chat, "Chat", tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showChatBox) "Close Direct Chat Portal" else "Open Direct Chat Portal", fontSize = 14.sp, color = Color.White)
                        }

                        if (showChatBox) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(activeChatMessages) { (sender, msg) ->
                                            val isMe = sender == "You"
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (isMe) Color(0xFF10B981) else Color(0xFF475569),
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                                ) {
                                                    Column {
                                                        Text(sender, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                                                        Text(msg, fontSize = 14.sp, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = currentChatMessageText,
                                            onValueChange = { currentChatMessageText = it },
                                            placeholder = { Text("Type message here...", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                                            modifier = Modifier
                                                .weight(1f),
                                            shape = RoundedCornerShape(24.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedContainerColor = Color(0xFF0F172A),
                                                unfocusedContainerColor = Color(0xFF0F172A),
                                                focusedBorderColor = Color(0xFF10B981),
                                                unfocusedBorderColor = Color(0xFF475569)
                                            ),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                if (currentChatMessageText.isNotBlank()) {
                                                    val text = currentChatMessageText
                                                    currentChatMessageText = ""
                                                    activeChatMessages = activeChatMessages + ("You" to text)
                                                    coroutineScope.launch {
                                                        kotlinx.coroutines.delay(1200)
                                                        val reply = when (pin.type) {
                                                            MapLocationType.MARKET -> "Excellent! Amma here. I can buy your produce for GHS 110/crate. Let's secure payment on escrow!"
                                                            MapLocationType.TRANSPORTER -> "Yes boss, I am starting my engine. Alhaji Ibrahim is on the way! Send me the route."
                                                            else -> "Thank you! I will confirm this transaction on the Agri-Ledger shortly."
                                                        }
                                                        activeChatMessages = activeChatMessages + (pin.title to reply)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(36.dp).background(Color(0xFF10B981), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Send, "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // AGRI-SURGE RIDE-HAILING ESTIMATOR SUB-CARD (Bolt/Yango Format)
                        if (computedDistance > 0.1 && pin.type != MapLocationType.TRANSPORTER) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "⚡ GeoHarvest Carrier Dispatch",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    // Vehicle Options Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            Triple("bike", "🏍️ Bike", "100kg max"),
                                            Triple("bongo", "🛺 Bongo", "1.5T max"),
                                            Triple("truck", "🚛 Mighty", "5T max")
                                        ).forEach { (id, name, cap) ->
                                            val isSelected = selectedVehicleId == id
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        if (isSelected) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Transparent,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        1.5.dp,
                                                        if (isSelected) Color(0xFF10B981) else Color(0xFF475569),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedVehicleId = id }
                                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(cap, color = Color(0xFF475569), fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Surcharge & Fare Math
                                    val basePrice = when(selectedVehicleId) { "bike" -> 10.0; "bongo" -> 45.0; else -> 120.0 }
                                    val ratePerKm = when(selectedVehicleId) { "bike" -> 1.5; "bongo" -> 4.2; else -> 8.5 }
                                    val surge = if (isFloodEnabled) 1.8 else if (isWeatherEnabled) 1.4 else if (isRoadQualityEnabled) 1.2 else 1.0
                                    val surgeLabel = if (isFloodEnabled) "⚠️ FLOOD SURGE 1.8x" else if (isWeatherEnabled) "⛈️ WEATHER SURGE 1.4x" else if (isRoadQualityEnabled) "🛣️ TERRAIN SURGE 1.2x" else "Standard Rate (1.0x)"
                                    val estimatedFare = (basePrice + (computedDistance * ratePerKm)) * surge
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Surcharge Rate", color = Color(0xFF475569), fontSize = 12.sp)
                                            Text(surgeLabel, color = if (surge > 1.0) Color(0xFFF59E0B) else Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("ESTIMATED FARE", color = Color(0xFF475569), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("GHS ${String.format("%.2f", estimatedFare)}", color = Color(0xFF10B981), fontWeight = FontWeight.Black, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // CARRIER MATCHING LOADER OR ACTION BUTTON ROW
                        if (isDispatchingTransporter) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f)),
                                border = BorderStroke(1.5.dp, Color(0xFF10B981))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (matchedDriverName == null) {
                                        CircularProgressIndicator(
                                            progress = { dispatchProgress },
                                            color = Color(0xFF10B981),
                                            trackColor = Color(0xFF334155),
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Matching nearest crop transporter...",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Scanning Bono East logistics grids",
                                            color = Color(0xFF475569),
                                            fontSize = 13.sp
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape)
                                                .padding(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Success",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Carrier Dispatched Successfully!",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Driver: $matchedDriverName",
                                            color = Color(0xFF06B6D4),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Button(
                                            onClick = {
                                                isDispatchingTransporter = false
                                                matchedDriverName = null
                                                pin.lat?.let { pLat ->
                                                    pin.lng?.let { pLng ->
                                                        isNavigationActive = true
                                                        isNavigationMinimized = false
                                                        webView.evaluateJavascript("startNavigationSimulation($pLat, $pLng)", null)
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Navigation, "Navigate", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Start Navigation with Dispatched Carrier", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Center Location Button
                                Button(
                                    onClick = {
                                        pin.lat?.let { pLat ->
                                            pin.lng?.let { pLng ->
                                                webView.evaluateJavascript("map.setView([$pLat, $pLng], 14)", null)
                                                Toast.makeText(context, "Centered map on ${pin.title}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.weight(1.1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.MyLocation, "Locate", tint = Color(0xFF06B6D4), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Location", color = Color(0xFF06B6D4), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                // Mobile Money Pay Button
                                if (pin.type == MapLocationType.MARKET) {
                                    Button(
                                        onClick = {
                                            paymentAmount = "1500.00"
                                            showPaymentDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Payment, "Pay", tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pay Goods", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                pin.contact?.let { phone ->
                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                        modifier = Modifier.weight(0.9f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, "Call", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call", fontSize = 13.sp)
                                    }
                                }
                                
                                if (pin.type != MapLocationType.TRANSPORTER) {
                                    Button(
                                        onClick = {
                                            isDispatchingTransporter = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Navigation, "Dispatch", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Dispatch", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            pin.lat?.let { pLat ->
                                                pin.lng?.let { pLng ->
                                                    isNavigationActive = true
                                                    isNavigationMinimized = false
                                                    webView.evaluateJavascript("startNavigationSimulation($pLat, $pLng)", null)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Navigation, "Navigate", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Navigate", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

        // Live Navigation Cockpit HUD panel
        if (isNavigationActive) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(2.dp, Color(0xFF059669))
            ) {
                if (isNavigationMinimized) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF059669).copy(alpha = 0.2f), CircleShape)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = "Navigating",
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Route to ${selectedPin?.title ?: "Agriculture Depot"}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${String.format("%.1f", remainingDistance)} km left • $remainingTimeMinutes mins ($arrivalTimeStr)",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 13.sp
                                )
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isMuted = !isMuted }) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Mute Voice",
                                    tint = if (isMuted) Color(0xFF475569) else Color(0xFF059669),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = { isNavigationMinimized = false }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Expand Guidance",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF059669).copy(alpha = 0.2f), CircleShape)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = "Navigating",
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Active Route Guidance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text(selectedPin?.title ?: "Agriculture Depot", color = Color(0xFFCBD5E1), fontSize = 14.sp)
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isMuted = !isMuted }) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = "Mute Voice",
                                        tint = if (isMuted) Color(0xFF475569) else Color(0xFF059669)
                                    )
                                }
                                IconButton(onClick = { isNavigationMinimized = true }) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Minimize Guidance",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SPEED", color = Color(0xFF475569), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("$currentSpeed", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("km/h", color = Color(0xFF475569), fontSize = 13.sp)
                                }
                            }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("REMAINING DISTANCE", color = Color(0xFF475569), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(String.format("%.1f", remainingDistance), color = Color(0xFF06B6D4), fontSize = 27.sp, fontWeight = FontWeight.Black)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("km", color = Color(0xFF475569), fontSize = 13.sp)
                                }
                            }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ETA", color = Color(0xFF475569), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("$remainingTimeMinutes mins ($arrivalTimeStr)", color = Color(0xFFEAB308), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        if (stepInstructions.isNotEmpty() && currentStepIndex < stepInstructions.size) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Directions,
                                        contentDescription = "Direction",
                                        tint = Color(0xFF06B6D4),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stepInstructions[currentStepIndex],
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        LinearProgressIndicator(
                            progress = { remainingRatio.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF059669),
                            trackColor = Color(0xFF334155),
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                isNavigationActive = false
                                webView.evaluateJavascript("cancelNavigationSimulation()", null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Cancel, "Stop", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel Navigation Route", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
}

// ---------------------- 5. PROFILE SCREEN & ACTIVITY SCREEN ----------------------
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ProfileScreen(viewModel: GeoHarvestViewModel) {
    val authName by viewModel.authenticatedUserName.collectAsStateWithLifecycle()
    val authEmail by viewModel.authenticatedUserEmail.collectAsStateWithLifecycle()
    val authType by viewModel.authenticatedUserType.collectAsStateWithLifecycle()
    val authIdentifier by viewModel.authenticatedUserIdentifier.collectAsStateWithLifecycle()
    val authSelfie by viewModel.authenticatedUserSelfie.collectAsStateWithLifecycle()
    val listings by viewModel.produceListings.collectAsStateWithLifecycle()
    val activities by viewModel.engagementActivities.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showProfileCameraDialog by remember { mutableStateOf(false) }
    var isProfileCameraCapturing by remember { mutableStateOf(false) }
    var profileCameraCaptureProgress by remember { mutableStateOf(0f) }
    var isProfileFrontCamera by remember { mutableStateOf(true) }
    var isProfileFlashOn by remember { mutableStateOf(false) }
    
    var showBonusDialog by remember { mutableStateOf(false) }
    var hasClaimedBonus by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    val profileGalleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.updateProfilePicture(uri.toString())
            Toast.makeText(context, "Profile photo updated from gallery!", Toast.LENGTH_SHORT).show()
            showProfileCameraDialog = false
        }
    }

    val mockProfilePhotos = listOf(
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=150",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=150",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=150",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=150"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().testTag("profile_id_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Deep midnight slate
            border = BorderStroke(1.5.dp, Color(0xFF059669))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header of ID Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF064E3B))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GHANA DIGITAL AGRI-ID REGISTER",
                            color = Color(0xFF10B981),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "VERIFIED",
                                color = Color(0xFF34D399),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Card Body
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Profile Photo (Left Column)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clickable { showProfileCameraDialog = true },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        val imageModifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                        
                        if (authSelfie != null && (authSelfie!!.startsWith("http") || authSelfie!!.startsWith("content"))) {
                            AsyncImage(
                                model = authSelfie,
                                contentDescription = "Profile selfie",
                                modifier = imageModifier,
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = imageModifier.background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = "Default Avatar",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF059669))
                                .border(1.dp, Color.White, CircleShape)
                                .clickable {
                                    showProfileCameraDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Edit photo",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Vital Information (Right Column)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = authName ?: "Ghanaian Farmer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color.White
                        )
                        
                        val cardId = if (authType == "Google") authEmail ?: "google_verified" else authIdentifier?.take(16) ?: "GH-CARD-9018-8"
                        Text(
                            text = "NIA ID: $cardId",
                            fontSize = 14.sp,
                            color = Color(0xFFCBD5E1),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "COOP: Bono East #TE-2025",
                            fontSize = 14.sp,
                            color = Color(0xFFCBD5E1),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "DISTRICT: Techiman Central",
                            fontSize = 14.sp,
                            color = Color(0xFFCBD5E1),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "RATING: ⭐ 4.9 | TRUST: 98%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Text("My Business & Registered Activities", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF064E3B))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Role: Lead Cooperative Producer & Agronomist",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                }
                
                Text(
                    text = "Specializing in wholesale hydroponic & organic greenhouses and logistics distribution of Bono East vegetables, cereals, and seed grains. Registered Techiman central supplier since Jan 2025.",
                    fontSize = 14.sp,
                    color = Color(0xFF475569),
                    lineHeight = 20.sp
                )

                androidx.compose.material3.HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // High-End Metric Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Sold Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Sold", color = Color(0xFF065F46), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("GHS 32,400.00", color = Color(0xFF047857), fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("18 Transactions", color = Color(0xFF059669), fontSize = 11.sp)
                        }
                    }

                    // Total Purchased Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Purchased", color = Color(0xFF1E40AF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("GHS 14,850.00", color = Color(0xFF1D4ED8), fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("8 Farm Inputs", color = Color(0xFF2563EB), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bonus Qualification Button
                Button(
                    onClick = { showBonusDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("check_bonus_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasClaimedBonus) Color(0xFF10B981) else Color(0xFF059669)
                    )
                ) {
                    Icon(
                        imageVector = if (hasClaimedBonus) Icons.Default.CheckCircle else Icons.Default.CardGiftcard,
                        contentDescription = "Bonus",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasClaimedBonus) "Bonus Claimed successfully!" else "Check Cooperative Bonus Status",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Text("My Active Produce Listings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF064E3B))

        val userListings = listings.filter { it.farmerName == authName }
        if (userListings.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("You haven't listed any produce yet.", fontSize = 15.sp, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.addProduceListing(
                                title = "Fresh Bono East Tomatoes",
                                vegetableType = "Tomatoes",
                                quantity = 35.0,
                                pricePerUnit = 120.0,
                                location = "Techiman",
                                collectionPoint = "Cooperative Central Depot",
                                description = "Premium grade organic red tomatoes, harvested this morning."
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("List Tomatoes Template", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                userListings.forEach { listing ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(45.dp)
                                        .background(
                                            color = when (listing.vegetableType) {
                                                "Tomatoes" -> Color(0xFFFEE2E2)
                                                "Okra" -> Color(0xFFD1FAE5)
                                                "Garden Eggs" -> Color(0xFFFEF3C7)
                                                "Peppers" -> Color(0xFFFFEDD5)
                                                else -> Color(0xFFDBEAFE)
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Agriculture,
                                        contentDescription = null,
                                        tint = when (listing.vegetableType) {
                                            "Tomatoes" -> Color(0xFFEF4444)
                                            "Okra" -> Color(0xFF10B981)
                                            "Garden Eggs" -> Color(0xFFD97706)
                                            "Peppers" -> Color(0xFFF97316)
                                            else -> Color(0xFF2563EB)
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(listing.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                    Text("Qty: ${listing.quantity.toInt()} Crates | GHS ${listing.pricePerUnit} each", fontSize = 14.sp, color = Color(0xFF475569))
                                }
                            }
                            Text(
                                "GHS ${listing.quantity * listing.pricePerUnit}",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF064E3B)
                            )
                        }
                    }
                }
            }
        }

        Text("Recent Ledger Logs & Actions", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF064E3B))

        val userActivities = activities.filter { it.profileName == authName }
        if (userActivities.isEmpty()) {
            val defaultLogs = listOf(
                "Biometric face identity verification registered on Ghana Ledger" to "Just now",
                "Successfully synchronized 1 offline listing with Bono hub" to "30 mins ago",
                "Completed transaction review and engagement feedback for logistics carrier" to "2 hours ago"
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                defaultLogs.forEach { (log, time) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(log, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(time, fontSize = 12.sp, color = Color(0xFF475569))
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                userActivities.forEach { act ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(act.actionText, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(act.timestampString, fontSize = 12.sp, color = Color(0xFF475569))
                        }
                    }
                }
            }
        }
    }

    if (showProfileCameraDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showProfileCameraDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.5.dp, Color(0xFFEF4444))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Secure Biometric Update",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        IconButton(onClick = { showProfileCameraDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Text(
                        "Align your face inside the glowing red biometric frame to verify and update your digital registry photo.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    // Polished Red Biometric Camera Viewport Box
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .shadow(8.dp, CircleShape)
                            .background(Color(0xFF1E293B), CircleShape)
                            .border(
                                width = 3.5.dp,
                                color = if (profileCameraCaptureProgress > 0.9f) Color(0xFF10B981) else Color(0xFFEF4444),
                                shape = CircleShape
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cameraPermissionState.status.isGranted) {
                            CameraPreview(
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                isFrontCamera = isProfileFrontCamera
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(Icons.Default.VideocamOff, contentDescription = null, tint = Color(0xFF94A3B8))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Permission Required", color = Color.White, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = { cameraPermissionState.launchPermissionRequest() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Allow", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }

                        // Sci-fi crosshair alignment canvas overlay
                        Canvas(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
                            val w = size.width
                            val h = size.height
                            // Circular guidelines
                            drawCircle(
                                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                radius = w * 0.4f,
                                style = Stroke(width = 1.5f)
                            )
                            // Scan sweep line
                            if (isProfileCameraCapturing) {
                                val lineY = h * profileCameraCaptureProgress
                                drawLine(
                                    color = Color(0xFFEF4444),
                                    start = Offset(0f, lineY),
                                    end = Offset(w, lineY),
                                    strokeWidth = 2.5f
                                )
                            }
                        }
                    }

                    // Progress Loader
                    if (isProfileCameraCapturing) {
                        LinearProgressIndicator(
                            progress = { profileCameraCaptureProgress },
                            color = Color(0xFFEF4444),
                            trackColor = Color(0xFF334155),
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                        )
                        Text(
                            "Analyzing facial points: ${(profileCameraCaptureProgress * 100).toInt()}%",
                            color = Color(0xFFF59E0B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Flip Camera button
                        OutlinedButton(
                            onClick = { isProfileFrontCamera = !isProfileFrontCamera },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Flip", fontSize = 12.sp)
                        }

                        // Torch Button
                        OutlinedButton(
                            onClick = { isProfileFlashOn = !isProfileFlashOn },
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(if (isProfileFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isProfileFlashOn) "Torch On" else "Torch Off", fontSize = 11.sp)
                        }
                    }

                    // Capture selfie button
                    Button(
                        onClick = {
                            scope.launch {
                                isProfileCameraCapturing = true
                                profileCameraCaptureProgress = 0f
                                for (p in 1..20) {
                                    delay(60)
                                    profileCameraCaptureProgress = p / 20f
                                }
                                isProfileCameraCapturing = false
                                viewModel.updateProfilePicture("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=150")
                                Toast.makeText(context, "Face biometric verify complete! Photo updated.", Toast.LENGTH_SHORT).show()
                                showProfileCameraDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture Live Photo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    // Upload from gallery fallback
                    OutlinedButton(
                        onClick = { profileGalleryLauncher.launch("image/*") },
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Photo from Gallery", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showBonusDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showBonusDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, Color(0xFF10B981))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "Bonus Icon",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(52.dp)
                    )

                    Text(
                        text = "Cooperative High-Yield Bonus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Text(
                        text = "The Bono East Agro-Ledger analyzes verified sales, registered ID validity, and cooperative trust scores to reward high-performing partners.",
                        color = Color(0xFF475569),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    androidx.compose.material3.HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Checklist items
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, "Passed", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("National biometric ID verification registered", color = Color(0xFF334155), fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, "Passed", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cooperative trust rating above 90% (Current: 98%)", color = Color(0xFF334155), fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, "Passed", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Total sold exceeding GHS 15,000 threshold", color = Color(0xFF334155), fontSize = 13.sp)
                        }
                    }

                    androidx.compose.material3.HorizontalDivider(color = Color(0xFFF1F5F9))

                    if (!hasClaimedBonus) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFECFDF5), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "🎉 QUALIFIED CRITERIA MET!",
                                    color = Color(0xFF065F46),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Claimable Cash Bonus: GHS 1,200.00",
                                    color = Color(0xFF047857),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                hasClaimedBonus = true
                                viewModel.addNotification(
                                    title = "Cooperative cash bonus received! 💰",
                                    message = "GHS 1,200.00 has been successfully deposited into your MTN Mobile Money wallet."
                                )
                                Toast.makeText(context, "Bonus GHS 1,200.00 paid successfully to Mobile Money!", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Claim Bonus to MTN MoMo Wallet", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "✅ GHS 1,200.00 Claimed & Dispatched to MoMo!",
                                color = Color(0xFF475569),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showBonusDialog = false },
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityScreen(viewModel: GeoHarvestViewModel) {
    var activityStep by remember { mutableStateOf("QUESTION") } // "QUESTION", "WALLET", "BASKET", "MONITORING", "DASHBOARD"
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val listings by viewModel.produceListings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var walletBalance by remember { mutableStateOf(2450.00) }
    val memoHistory = remember {
        mutableStateListOf(
            Triple("Payment for 50 Crate Tomatoes to Johannes Aboagye", "GHS 1,200.00", "2 mins ago"),
            Triple("MoMo Received from Beatrice Ansah (Accra)", "+GHS 850.00", "1 hour ago"),
            Triple("Diesel fueling logistics expense reimbursement", "GHS 350.00", "Yesterday")
        )
    }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    
    var payeeName by remember { mutableStateOf("") }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentMemo by remember { mutableStateOf("") }
    var paymentPhone by remember { mutableStateOf("") }
    var selectedCarrier by remember { mutableStateOf("MTN MoMo") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Step Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp)
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (activityStep != "QUESTION") {
                    IconButton(
                        onClick = { activityStep = "QUESTION" },
                        modifier = Modifier.size(36.dp).testTag("activity_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color(0xFF0F172A))
                    }
                }
                Column {
                    Text("Farm Inspections & Financials", fontSize = 14.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    Text(
                        text = when (activityStep) {
                            "QUESTION" -> "Select Activity Section"
                            "WALLET" -> "Wallet Ledger & QR"
                            "BASKET" -> "Selected Produce Basket"
                            "MONITORING" -> "Soil & Crop Monitoring"
                            "DASHBOARD" -> "Analytics Dashboard"
                            else -> activityStep
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            }
            if (activityStep != "QUESTION") {
                IconButton(onClick = { activityStep = "QUESTION" }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Home, "Home", tint = Color(0xFF059669))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (activityStep) {
                "QUESTION" -> {
                    Text(
                        text = "What would you like to view?",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Review ledger accounts, verify your purchase basket, monitor ground soil sensors, and view demand dashboards.",
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4 Short Rectangular Boxes in Grid / List Style
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Wallet Card Box
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(82.dp)
                                .clickable { activityStep = "WALLET" }
                                .testTag("activity_wallet_box"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                            border = BorderStroke(1.5.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFFD1FAE5), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AccountBalanceWallet, null, tint = Color(0xFF065F46), modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Wallet Ledger & Payments", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF064E3B))
                                    Text("Digital balance, banking selector, momo, QR pay.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                }
                            }
                        }

                        // 2. Basket Card Box
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(82.dp)
                                .clickable { activityStep = "BASKET" }
                                .testTag("activity_basket_box"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            border = BorderStroke(1.5.dp, Color(0xFF3B82F6))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFFDBEAFE), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ShoppingBasket, null, tint = Color(0xFF1E40AF), modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Selected Produce Basket", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF1E3A8A))
                                    Text("Verify your selected and pending crop commodities.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                                }
                            }
                        }

                        // 3. Monitoring Card Box
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(82.dp)
                                .clickable { activityStep = "MONITORING" }
                                .testTag("activity_monitoring_box"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                            border = BorderStroke(1.5.dp, Color(0xFFA855F7))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFFF3E8FF), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Analytics, null, tint = Color(0xFF6B21A8), modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Yield & Soil Monitoring", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF581C87))
                                    Text("Ground moisture levels, tractor machinery leasing.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7E22CE))
                                }
                            }
                        }

                        // 4. Dashboard Card Box
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(82.dp)
                                .clickable { activityStep = "DASHBOARD" }
                                .testTag("activity_dashboard_box"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                            border = BorderStroke(1.5.dp, Color(0xFFF97316))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFFFFEDD5), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF9A3412), modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Analytics Dashboard", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF7C2D12))
                                    Text("Most purchased vegetables & regional volume trends.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))
                                }
                            }
                        }
                    }
                }

                "WALLET" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.5.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("GeoHarvest Digital Balance", color = Color(0xFF10B981), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("GHS ${String.format("%.2f", walletBalance)}", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                            
                            Spacer(modifier = Modifier.height(18.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Pay for Goods Button
                                Button(
                                    onClick = { 
                                        payeeName = "Kofi Mensah"
                                        paymentAmount = "150.00"
                                        paymentMemo = "Equipment logistics rent payment"
                                        paymentPhone = "0244123456"
                                        showPaymentDialog = true 
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("momo_pay_btn")
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pay for Goods", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                }

                                // Pay via QR Button
                                Button(
                                    onClick = { showQrScannerDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("qr_scan_pay_btn")
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pay via QR", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    // Select Banking System Header
                    Text("Select Active Banking System", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF0F172A))
                    
                    // Buttons for banking systems as requested
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val bankingSystems = listOf("MTN MoMo", "Telecel Cash", "AT Money", "GhanaPay")
                        bankingSystems.forEach { name ->
                            val isSelected = selectedCarrier == name
                            Button(
                                onClick = { 
                                    selectedCarrier = name
                                    Toast.makeText(context, "$name set as primary banking gateway!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF059669) else Color(0xFFF1F5F9),
                                    contentColor = if (isSelected) Color.White else Color(0xFF0F172A)
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Memo Transaction History", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF0F172A))
                        Text("MoMo Status: SECURE", fontSize = 13.sp, color = Color(0xFF059669), fontWeight = FontWeight.Black)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        memoHistory.forEach { (text, amount, time) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.5.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                        Text(time, fontSize = 13.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(amount, fontWeight = FontWeight.Black, fontSize = 16.sp, color = if (amount.startsWith("+")) Color(0xFF059669) else Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }

                "BASKET" -> {
                    Text("My Selected Commodities & Bookings", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Color(0xFF0F172A))
                    
                    // Pre-populated selected commodities so it's never empty, telling which ones are selected
                    val basketItems = remember {
                        listOf(
                            Triple("White Maize Sacks", "Kwame Boakye", "GHS 180.00 / sack"),
                            Triple("Premium Pona Yam", "Kofi Mensah", "GHS 140.00 / tuber"),
                            Triple("Fresh Cassava Sacks", "Abena Boateng", "GHS 90.00 / sack")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        basketItems.forEachIndexed { idx, (title, farmer, price) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.5.dp, Color(0xFF3B82F6))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(0xFF3B82F6), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF0F172A))
                                            Text("Supplier: $farmer", fontSize = 14.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                            Text("Agreed rate: $price", fontSize = 14.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("SELECTED", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF1E40AF))
                                    }
                                }
                            }
                        }

                        if (orders.isNotEmpty()) {
                            orders.forEach { order ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.5.dp, Color(0xFF10B981))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(Color(0xFF10B981), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("${order.quantity.toInt()} Crates of ${order.vegetableType}", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF0F172A))
                                                Text("Supplier: ${order.farmerName}", fontSize = 14.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                                Text("Order reference: #ORDER-${2000 + order.id}", fontSize = 14.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(order.status, fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF059669))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "MONITORING" -> {
                    Text("Commodity Sensor & Soil Monitoring", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Color(0xFF0F172A))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Techiman Ground Moisture: 68%", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF0F172A))
                                Text("Optimal zones for vegetables watering indexes.", fontSize = 14.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("Regional Soil & Weather Advisors", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF0F172A))
                    
                    val otherOptions = listOf(
                        Triple("Techiman Soil Analysis Desk", "Registered guidance & soil nutrient testing kit booking.", "BOOK KIT"),
                        Triple("Ghana Cooperative Seed Advisory", "Certified climate-resilient hybrid seed distribution catalogs.", "VIEW SEEDS"),
                        Triple("Agricultural Machinery Leasing", "Government-subsidized tractor pool reservation desk.", "LEASE DESK")
                    )
                    otherOptions.forEach { (title, desc, action) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.5.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(desc, fontSize = 14.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { Toast.makeText(context, "Consultation requested for $title!", Toast.LENGTH_SHORT).show() },
                                    border = BorderStroke(1.5.dp, Color(0xFF059669)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(action, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF059669))
                                }
                            }
                        }
                    }
                }

                "DASHBOARD" -> {
                    Text("Wholesale Purchase & Volume Statistics", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Color(0xFF0F172A))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Most Purchased Vegetables & Volume Demand", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF0F172A))
                            
                            val purchases = listOf(
                                "Tomatoes (Bono Fresh)" to 0.65f,
                                "Okra (Tuobodom Special)" to 0.20f,
                                "Peppers (Wenchi Habanero)" to 0.15f
                            )
                            
                            purchases.forEach { (label, pct) ->
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                                        Text("${(pct * 100).toInt()}% Volume Purchased", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF059669))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        color = Color(0xFF059669),
                                        trackColor = Color(0xFFF1F5F9),
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Bono East High Demand Crops", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("1. Tomatoes (High volumes from Tuobodom greenhouse)\n2. Yam (Pona Grade-A sweet tubers)\n3. Maize (Dried White premium seeds)", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                        }
                    }
                }
            }
        }
    }

    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFFFCC00))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Ghana MoMo Payment", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Select Carrier Protocol", fontSize = 14.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("MTN MoMo", "Telecel Cash", "GhanaPay").forEach { carrier ->
                            val sel = selectedCarrier == carrier
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (sel) Color(0xFF064E3B) else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .border(1.dp, if (sel) Color(0xFF059669) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedCarrier = carrier }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(carrier, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.White else Color.DarkGray)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = payeeName,
                        onValueChange = { payeeName = it },
                        label = { Text("Recipient Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = paymentPhone,
                        onValueChange = { paymentPhone = it },
                        label = { Text("MoMo Account Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = { Text("Amount (GHS)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = paymentMemo,
                        onValueChange = { paymentMemo = it },
                        label = { Text("Transaction Memo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = paymentAmount.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0 && payeeName.isNotBlank() && paymentPhone.isNotBlank()) {
                            walletBalance -= amt
                            memoHistory.add(0, Triple("Paid GHS ${String.format("%.2f", amt)} to $payeeName: $paymentMemo", "-GHS ${String.format("%.2f", amt)}", "Just now"))
                            viewModel.addNotification("Payment Transferred Successfully", "GHS ${String.format("%.2f", amt)} sent to $payeeName ($paymentPhone) via $selectedCarrier.")
                            Toast.makeText(context, "Payment dispatched securely via Gh-Link Strategy!", Toast.LENGTH_LONG).show()
                            showPaymentDialog = false
                        } else {
                            Toast.makeText(context, "Please enter valid payment details.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.testTag("confirm_momo_pay_btn")
                ) {
                    Text("Confirm Payment", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showQrScannerDialog) {
        AlertDialog(
            onDismissRequest = { showQrScannerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF3B82F6))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure QR Payment Gateway", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Aiming camera at Agri-Invoice QR Code to finalize your payment securely.",
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Simulated neon viewfinder box with pulse animation
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                            .border(2.dp, Color(0xFF10B981), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Scan guidelines
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        )
                        // Scanning laser line simulation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Color(0xFF10B981))
                                .align(Alignment.Center)
                        )
                        Text(
                            "[ SCANNING CAMERA ACTIVE ]",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                        )
                    }

                    Text(
                        "Simulate Scannable Agri-Invoices:",
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    // Button to simulate scanning Farm Produce Invoice
                    Button(
                        onClick = {
                            val amt = 350.00
                            walletBalance -= amt
                            memoHistory.add(0, Triple("Paid GHS 350.00 via QR: 15 Crates Pineapples", "-GHS 350.00", "Just now"))
                            viewModel.addNotification("QR Produce Payment Approved", "GHS 350.00 cleared for 15 Crates of Pineapples via secure QR scan.")
                            Toast.makeText(context, "Farm Produce Invoice paid! Verification Code: TXN-QR-PROD-582319", Toast.LENGTH_LONG).show()
                            showQrScannerDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)), // Green for produce
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Scan Farm Produce Invoice (Pineapples): GHS 350.00", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Button to simulate scanning Logistics Service Invoice
                    Button(
                        onClick = {
                            val amt = 180.00
                            walletBalance -= amt
                            memoHistory.add(0, Triple("Paid GHS 180.00 via QR: Alhaji Ibrahim Logistics", "-GHS 180.00", "Just now"))
                            viewModel.addNotification("QR Logistics Payment Approved", "GHS 180.00 cleared for Alhaji Ibrahim Ford Transit Transport via secure QR scan.")
                            Toast.makeText(context, "Logistics Invoice paid! Verification Code: TXN-QR-LOGS-994121", Toast.LENGTH_LONG).show()
                            showQrScannerDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)), // Blue for logistics
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Scan Logistics Invoice (Alhaji Ibrahim): GHS 180.00", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQrScannerDialog = false }) {
                    Text("Close Scanner", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ---------------------- 6. FLOATING MODAL AI CHATBOT ----------------------
@Composable
fun ChatSheetContent(viewModel: GeoHarvestViewModel, onDismiss: () -> Unit) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Scroll to bottom when messages list size changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(550.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Chat Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = "AI", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("GeoHarvest AI Advisor", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Powered by Gemini 1.5 Flash", fontSize = 14.sp, color = Color(0xFF475569))
                }
            }
            Row {
                IconButton(onClick = { viewModel.clearChat() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = Color(0xFF475569))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF475569))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(10.dp))

        // Contextual fast query chips
        val suggestions = listOf("cheapest transport", "tomato price", "how to upload")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(suggestions) { item ->
                SuggestionChip(
                    onClick = {
                        textInput = when (item) {
                            "cheapest transport" -> "Who offers the cheapest transport?"
                            "tomato price" -> "What is today's tomato price in Bono East?"
                            "how to upload" -> "How do I upload my produce?"
                            else -> ""
                        }
                    },
                    label = { Text(item, fontSize = 14.sp) }
                )
            }
        }

        // Messages Thread (using LazyColumn)
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                val isAi = msg.sender == "AI"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .background(
                                color = if (isAi) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                                else MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isAi) 0.dp else 12.dp,
                                    bottomEnd = if (isAi) 12.dp else 0.dp
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = if (isAi) MaterialTheme.colorScheme.outlineVariant else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = msg.text,
                                fontSize = 16.sp,
                                color = if (isAi) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            // Interactive Action Cards inside AI bubbles
                            if (isAi) {
                                val lowercaseText = msg.text.lowercase()
                                val personasFound = mutableListOf<Pair<String, String>>() // Name to Role
                                if (lowercaseText.contains("alhaji") || lowercaseText.contains("ibrahim")) {
                                    personasFound.add("Alhaji Ibrahim" to "Transporter")
                                }
                                if (lowercaseText.contains("emmanuel") || lowercaseText.contains("osei")) {
                                    personasFound.add("Emmanuel Osei" to "Transporter")
                                }
                                if (lowercaseText.contains("kwabena") || lowercaseText.contains("mensah")) {
                                    personasFound.add("Kwabena Mensah" to "Transporter")
                                }
                                if (lowercaseText.contains("beatrice") || lowercaseText.contains("ansah")) {
                                    personasFound.add("Beatrice Ansah" to "Buyer")
                                }
                                if (lowercaseText.contains("mary") || lowercaseText.contains("appiah")) {
                                    personasFound.add("Mary Appiah" to "Buyer")
                                }
                                if (lowercaseText.contains("kwaku") || lowercaseText.contains("addo")) {
                                    personasFound.add("Kwaku Addo" to "Buyer")
                                }
                                if (lowercaseText.contains("amma") || lowercaseText.contains("osei")) {
                                    personasFound.add("Amma Osei" to "Buyer")
                                }

                                if (personasFound.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        personasFound.distinctBy { it.first }.forEach { (name, role) ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                ),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = if (role == "Transporter") Icons.Default.LocalShipping else Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "$name ($role)",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        // Direct WhatsApp Style Chat
                                                        Button(
                                                            onClick = {
                                                                onDismiss()
                                                                val greeting = when(role) {
                                                                    "Transporter" -> "Akwaaba! Alhaji here. I am available for immediate transit with my vehicle. Where is the loading point, and what crops are we carrying?"
                                                                    else -> "Hi! I am $name. Are you ready to supply fresh vegetables today?"
                                                                }
                                                                viewModel.startWhatsAppChat(name, role, "", greeting)
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.weight(1f).height(34.dp)
                                                        ) {
                                                            Icon(Icons.Default.Chat, null, modifier = Modifier.size(12.dp), tint = Color.White)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        }

                                                        // Show on map
                                                        Button(
                                                            onClick = {
                                                                onDismiss()
                                                                val (lat, lng) = when (name) {
                                                                    "Alhaji Ibrahim" -> 7.5833 to -1.9333
                                                                    "Emmanuel Osei" -> 7.6333 to -1.9000
                                                                    "Kwabena Mensah" -> 7.7410 to -2.1030
                                                                    "Beatrice Ansah" -> 7.5645 to -1.7011
                                                                    "Mary Appiah" -> 7.3349 to -2.3124
                                                                    "Kwaku Addo" -> 8.0538 to -1.7289
                                                                    "Amma Osei" -> 7.6310 to -1.9020
                                                                    else -> 7.9465 to -1.0232
                                                                }
                                                                viewModel.focusMapOn(lat, lng, name, "$role profile loaded from AI suggestions", role.uppercase())
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.weight(1f).height(34.dp)
                                                        ) {
                                                            Icon(Icons.Default.Map, null, modifier = Modifier.size(12.dp), tint = Color.White)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Map", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("GeoHarvest AI is consulting market feeds...", fontSize = 14.sp, color = Color(0xFF475569))
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(10.dp))

        // Input bottom bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask about prices, transport, uploads...", fontSize = 16.sp) },
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text_field"),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendChatMessage(textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .size(44.dp)
                    .testTag("send_chat_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier,
        update = { previewView ->
            val cameraProvider = try {
                cameraProviderFuture.get()
            } catch (e: Exception) {
                null
            }
            if (cameraProvider != null) {
                cameraProvider.unbindAll()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val cameraSelector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                
                try {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", exc)
                }
            }
        }
    )
}

enum class OnboardingStep {
    INPUT,
    OTP_VERIFICATION,
    GHANA_CARD_INPUT,
    GMAIL_VERIFICATION,
    CAMERA_CAPTURE
}

enum class AuthMethodType {
    GHANA_CARD,
    PHONE,
    GOOGLE
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun OnboardingSignInScreen(viewModel: GeoHarvestViewModel) {
    var userNameInput by remember { mutableStateOf("") }
    var ghanaCardInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Mobile & Ghana Card, 1: Google & Guest
    var validationError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Google selection states
    var googleSelectedName by remember { mutableStateOf("Johannes Aboagye") }
    var googleSelectedEmail by remember { mutableStateOf("johannesaboagye123@gmail.com") }
    var showGoogleSelector by remember { mutableStateOf(false) }

    // Conditional Identity Check toggle
    var requireIdentityVerification by remember { mutableStateOf(true) }

    // Firebase state flows
    val firebaseAuthLoading by viewModel.firebaseAuthLoading.collectAsStateWithLifecycle()
    val firebaseAuthError by viewModel.firebaseAuthError.collectAsStateWithLifecycle()
    val firebaseAuthStatus by viewModel.firebaseAuthStatus.collectAsStateWithLifecycle()
    val firebaseVerificationId by viewModel.firebaseVerificationId.collectAsStateWithLifecycle()

    // Custom Multi-Step Authentication States
    var onboardingStep by remember { mutableStateOf(OnboardingStep.INPUT) }
    var authMethod by remember { mutableStateOf<AuthMethodType?>(null) }
    
    // OTP States
    var otpInput by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var otpResendTimer by remember { mutableStateOf(30) }
    var showSmsBanner by remember { mutableStateOf(false) }
    var smsBannerText by remember { mutableStateOf("") }
    
    // Simulated Camera States
    var isFrontCamera by remember { mutableStateOf(true) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var captureProgress by remember { mutableStateOf(0f) }
    var hasCaptured by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Helper to extract Android Activity for Firebase Verification
    fun android.content.Context.findActivity(): android.app.Activity? {
        var currentContext = this
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }
    val activity = context.findActivity()
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    var uploadedSelfieUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val selfieUploadLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            uploadedSelfieUri = uri
            hasCaptured = true
        }
    }

    // SMS Code Notification Trigger
    LaunchedEffect(onboardingStep) {
        if (onboardingStep == OnboardingStep.OTP_VERIFICATION) {
            otpInput = ""
            smsBannerText = "Your GeoHarvest secure authentication OTP is 1957. Enter this code to verify your mobile number."
            delay(1200)
            showSmsBanner = true
            delay(9000)
            showSmsBanner = false
        }
    }

    // OTP Resend Countdown
    LaunchedEffect(onboardingStep, otpResendTimer) {
        if (onboardingStep == OnboardingStep.OTP_VERIFICATION && otpResendTimer > 0) {
            delay(1000)
            otpResendTimer--
        }
    }

    // Infinite scanning laser line animation inside simulated camera
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Slate 50
    ) {
        // Decorative background gradient circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFEF4444).copy(alpha = 0.03f), // Ghana Red Accent
                radius = size.minDimension * 0.45f,
                center = Offset(size.width * 0.1f, size.height * 0.15f)
            )
            drawCircle(
                color = Color(0xFFFBBF24).copy(alpha = 0.04f), // Ghana Gold Accent
                radius = size.minDimension * 0.4f,
                center = Offset(size.width * 0.9f, size.height * 0.5f)
            )
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = 0.04f), // Ghana Green Accent
                radius = size.minDimension * 0.35f,
                center = Offset(size.width * 0.2f, size.height * 0.85f)
            )
        }

        // Real-time Push Notification Simulation for SMS Verification
        androidx.compose.animation.AnimatedVisibility(
            visible = showSmsBanner,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF1E293B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notification",
                            tint = Color(0xFFFFB000),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GeoHarvest SecurID SMS",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "just now",
                                color = Color(0xFF475569),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = smsBannerText,
                            color = Color(0xFFCBD5E1),
                            fontSize = 14.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Main content area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(54.dp))

            // Ghana National Colors Banner Accent
            Row(
                modifier = Modifier
                    .width(120.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFEF4444))) // Red
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFFBBF24))) // Gold
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF10B981))) // Green
            }

            // App Logo & Title
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFFEFF6FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Grass,
                    contentDescription = "Logo",
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = "GeoHarvest Ghana",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Track vegetables, check fair market prices, and book drivers in Bono East. Fast, secure, and offline-first.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ⚡ FAST-TRACK INSTANT LOGIN (One-Tap Access)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = BorderStroke(1.5.dp, Color(0xFF3B82F6))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Fast-track",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FAST-TRACK INSTANT LOGIN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                    Text(
                        text = "Having issues receiving SMS or Gmail OTP? Skip verification and sign in instantly using these verified test accounts.",
                        fontSize = 14.sp,
                        color = Color(0xFF1E40AF),
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.signInWithGoogle(
                                    name = "Johannes Aboagye",
                                    email = "johannesaboagye123@gmail.com",
                                    selfiePhoto = "avatar_google"
                                )
                                Toast.makeText(context, "Welcome back, Johannes Aboagye!", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Johannes (Google)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        
                        Button(
                            onClick = {
                                viewModel.signInWithPhoneAndGhanaCard(
                                    phoneNo = "245559999",
                                    cardNo = "GHA-712345678-9",
                                    name = "Kofi Mensah",
                                    selfiePhoto = "avatar_biometric"
                                )
                                Toast.makeText(context, "Welcome back, Kofi Mensah!", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Kofi (Mobile)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Step-by-Step Interactive Forms
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (onboardingStep) {
                        // STEP 1: INITIAL INPUT / OPTION SELECTOR
                        OnboardingStep.INPUT -> {
                            Text(
                                text = "Secure Sign In Portal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Segmented Selector Pills (Merged options into exactly 2 tabs)
                            ScrollableTabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = Color(0xFFF1F5F9),
                                edgePadding = 4.dp,
                                indicator = { TabRowDefaults.SecondaryIndicator(color = Color.Transparent) },
                                divider = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .height(50.dp)
                            ) {
                                val tabs = listOf("Mobile & Ghana Card", "Google & Guest")
                                tabs.forEachIndexed { index, title ->
                                    val selected = selectedTab == index
                                    Tab(
                                        selected = selected,
                                        onClick = {
                                            selectedTab = index
                                            validationError = null
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        },
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (selected) Color(0xFF059669) else Color.Transparent),
                                        text = {
                                            Text(
                                                text = title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) Color.White else Color(0xFF475569)
                                            )
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            if (isLoading) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = Color(0xFF059669))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Accessing Bono East Registry...", fontSize = 15.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
                                    }
                                }
                            } else {
                                when (selectedTab) {
                                    0 -> { // Merged: Mobile Number & Ghana Card Unified Flow
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text(
                                                text = "Ghana Card credentials will be requested sequentially after OTP verification.",
                                                fontSize = 14.sp,
                                                color = Color(0xFF475569),
                                                lineHeight = 15.sp
                                            )

                                            OutlinedTextField(
                                                value = userNameInput,
                                                onValueChange = {
                                                    userNameInput = it
                                                    validationError = null
                                                },
                                                placeholder = { Text("E.g. Kofi Mensah", color = Color(0xFF475569)) },
                                                label = { Text("Full Name") },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = "Name Icon",
                                                        tint = Color(0xFF059669)
                                                    )
                                                },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color(0xFF0F172A),
                                                    unfocusedTextColor = Color(0xFF0F172A),
                                                    focusedContainerColor = Color.White,
                                                    unfocusedContainerColor = Color.White,
                                                    errorContainerColor = Color.White,
                                                    focusedBorderColor = Color(0xFF059669),
                                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                                    focusedLabelColor = Color(0xFF059669),
                                                    unfocusedLabelColor = Color(0xFF475569)
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                singleLine = true,
                                                isError = validationError != null
                                            )

                                            OutlinedTextField(
                                                value = phoneInput,
                                                onValueChange = {
                                                    phoneInput = it.filter { c -> c.isDigit() || c == ' ' }
                                                    validationError = null
                                                },
                                                placeholder = { Text("24 555 9999", color = Color(0xFF475569)) },
                                                label = { Text("Ghanaian Telephone Number") },
                                                leadingIcon = {
                                                    Row(
                                                        modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("🇬🇭", fontSize = 23.sp)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("+233", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                                    }
                                                },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color(0xFF0F172A),
                                                    unfocusedTextColor = Color(0xFF0F172A),
                                                    focusedContainerColor = Color.White,
                                                    unfocusedContainerColor = Color.White,
                                                    errorContainerColor = Color.White,
                                                    focusedBorderColor = Color(0xFF059669),
                                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                                    focusedLabelColor = Color(0xFF059669),
                                                    unfocusedLabelColor = Color(0xFF475569)
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                isError = validationError != null
                                            )

                                            if (validationError != null) {
                                                Text(
                                                    text = validationError!!,
                                                    color = Color.Red,
                                                    fontSize = 14.sp,
                                                    modifier = Modifier.padding(start = 4.dp)
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    if (userNameInput.isBlank()) {
                                                        validationError = "Please enter your name"
                                                    } else if (phoneInput.isBlank()) {
                                                        validationError = "Please enter your telephone number"
                                                    } else if (phoneInput.length < 7) {
                                                        validationError = "Please enter a valid telephone number"
                                                    } else {
                                                        focusManager.clearFocus()
                                                        keyboardController?.hide()
                                                        scope.launch {
                                                            isLoading = true
                                                            val formattedPhone = if (phoneInput.startsWith("+")) phoneInput else "+233${phoneInput.trimStart('0')}"
                                                            if (activity != null) {
                                                                viewModel.startFirebasePhoneVerification(
                                                                    phoneNumber = formattedPhone,
                                                                    activity = activity,
                                                                    onCodeSent = { _ ->
                                                                        isLoading = false
                                                                        authMethod = AuthMethodType.PHONE
                                                                        onboardingStep = OnboardingStep.OTP_VERIFICATION
                                                                    },
                                                                    onError = { err ->
                                                                        isLoading = false
                                                                        validationError = err
                                                                    }
                                                                )
                                                            } else {
                                                                delay(1200)
                                                                isLoading = false
                                                                authMethod = AuthMethodType.PHONE
                                                                onboardingStep = OnboardingStep.OTP_VERIFICATION
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                                    .testTag("phone_login_btn"),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                            ) {
                                                Text("Get Verification OTP", fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                    1 -> { // Google & Guest Mode
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            if (showGoogleSelector) {
                                                // Dynamic Google Account Selector panel
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                                    shape = RoundedCornerShape(16.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Text("Select a Google Account", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF475569))
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        
                                                        val accounts = listOf(
                                                            "Johannes Aboagye" to "johannesaboagye123@gmail.com",
                                                            "Ama Osei" to "ama.osei.coop@gmail.com",
                                                            "Emmanuel Kyeremeh" to "e.kyeremeh.techiman@gmail.com"
                                                        )
                                                        accounts.forEach { (name, email) ->
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clickable {
                                                                        focusManager.clearFocus()
                                                                        keyboardController?.hide()
                                                                        googleSelectedName = name
                                                                        googleSelectedEmail = email
                                                                        showGoogleSelector = false
                                                                        // Proceed to Gmail Verification
                                                                        scope.launch {
                                                                            isLoading = true
                                                                            delay(800)
                                                                            isLoading = false
                                                                            authMethod = AuthMethodType.GOOGLE
                                                                            onboardingStep = OnboardingStep.GMAIL_VERIFICATION
                                                                        }
                                                                    }
                                                                    .padding(vertical = 8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF0284C7))
                                                                Spacer(modifier = Modifier.width(10.dp))
                                                                Column {
                                                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                                    Text(email, fontSize = 14.sp, color = Color(0xFF475569))
                                                                }
                                                            }
                                                            HorizontalDivider(color = Color(0xFFCBD5E1).copy(alpha = 0.5f))
                                                        }
                                                        
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        TextButton(onClick = { showGoogleSelector = false }) {
                                                            Text("Cancel", fontSize = 14.sp, color = Color.Red)
                                                        }
                                                    }
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = { showGoogleSelector = true },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(52.dp)
                                                        .testTag("google_login_btn"),
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.AccountCircle,
                                                            contentDescription = "Google Icon",
                                                            tint = Color(0xFF4285F4),
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(
                                                            "Continue with Google",
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF1E293B)
                                                        )
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                                                Text(
                                                    "OR",
                                                    modifier = Modifier.padding(horizontal = 12.dp),
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF475569),
                                                    fontWeight = FontWeight.Bold
                                                )
                                                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.signInAsGuest()
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                                    .testTag("guest_login_btn"),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = "Guest Icon",
                                                        tint = Color.White
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        "Browse as Guest (No Sign In)",
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // STEP 2: PHONE OTP VERIFICATION FORM
                        OnboardingStep.OTP_VERIFICATION -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onboardingStep = OnboardingStep.INPUT }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "OTP Verification",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "We have sent a 4-digit authentication code to +233 $phoneInput via simulated SMS.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = otpInput,
                                onValueChange = {
                                    if (it.length <= 4) {
                                        otpInput = it.filter { char -> char.isDigit() }
                                        otpError = null
                                    }
                                },
                                placeholder = { Text("Enter 4-digit code", color = Color(0xFF475569)) },
                                label = { Text("One-Time Password (OTP)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color(0xFF059669))
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    errorContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFF059669),
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedLabelColor = Color(0xFF059669),
                                    unfocusedLabelColor = Color(0xFF475569)
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                isError = otpError != null
                            )

                            if (otpError != null) {
                                Text(
                                    text = otpError!!,
                                    color = Color.Red,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(start = 4.dp).align(Alignment.Start)
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                border = BorderStroke(1.dp, Color(0xFFFEF3C7))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "💡 Quick Tip: Look at the SMS pop-up banner or enter '1957' to instantly proceed.",
                                        fontSize = 14.sp,
                                        color = Color(0xFFB45309),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                otpInput = "1957"
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                                scope.launch {
                                                    isLoading = true
                                                    delay(500)
                                                    isLoading = false
                                                    onboardingStep = OnboardingStep.GHANA_CARD_INPUT
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Auto-Fill OTP", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.signInWithPhoneAndGhanaCard(
                                                    phoneNo = if (phoneInput.isBlank()) "245559999" else phoneInput,
                                                    cardNo = "GHA-712345678-9",
                                                    name = if (userNameInput.isBlank()) "Kofi Mensah" else userNameInput,
                                                    selfiePhoto = "avatar_biometric"
                                                )
                                                Toast.makeText(context, "Successfully Logged In (Bypassed Verification)!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Bypass & Login", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (otpInput.length != 4) {
                                        otpError = "OTP must be exactly 4 digits."
                                    } else {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        scope.launch {
                                            isLoading = true
                                            delay(1000)
                                            isLoading = false
                                            // Proceed to Ghana Card sequential input as requested
                                            onboardingStep = OnboardingStep.GHANA_CARD_INPUT
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("verify_otp_btn"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Text("Verify & Capture Face", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Didn't receive code? ", fontSize = 14.sp, color = Color(0xFF475569))
                                Text(
                                    text = if (otpResendTimer > 0) "Resend in ${otpResendTimer}s" else "Resend SMS",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (otpResendTimer > 0) Color(0xFF475569) else Color(0xFF0284C7),
                                    modifier = Modifier.clickable(enabled = otpResendTimer == 0) {
                                        otpResendTimer = 30
                                        smsBannerText = "Your NEW GeoHarvest secure authentication OTP is 1957. Valid for 10 minutes."
                                        showSmsBanner = true
                                        scope.launch {
                                            delay(7000)
                                            showSmsBanner = false
                                        }
                                    }
                                )
                            }
                        }

                        // SEQUENTIAL STEP: GHANA CARD INPUT
                        OnboardingStep.GHANA_CARD_INPUT -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onboardingStep = OnboardingStep.OTP_VERIFICATION }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ghana Card Verification",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "To complete your secure profile registration, please enter your valid Ghana Card ID. Your card will be checked against the National Identification Authority registry.",
                                fontSize = 15.sp,
                                color = Color(0xFF475569),
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = ghanaCardInput,
                                onValueChange = {
                                    ghanaCardInput = it.uppercase()
                                    validationError = null
                                },
                                placeholder = { Text("GHA-7XXXXXXXX-X", color = Color(0xFF475569)) },
                                label = { Text("Ghana Card Number") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CreditCard,
                                        contentDescription = "Ghana Card Icon",
                                        tint = Color(0xFF059669)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    errorContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFF059669),
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedLabelColor = Color(0xFF059669),
                                    unfocusedLabelColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                isError = validationError != null
                            )

                            if (validationError != null) {
                                Text(
                                    text = validationError!!,
                                    color = Color.Red,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(start = 4.dp).align(Alignment.Start)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (ghanaCardInput.isBlank()) {
                                        validationError = "Please enter your Ghana Card number"
                                    } else if (!ghanaCardInput.startsWith("GHA-") || ghanaCardInput.length < 8) {
                                        validationError = "Must start with GHA- and be at least 8 characters."
                                    } else {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        scope.launch {
                                            isLoading = true
                                            delay(1000)
                                            isLoading = false
                                            // Go to selfie camera capture
                                            onboardingStep = OnboardingStep.CAMERA_CAPTURE
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("ghana_card_verify_btn"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Text("Continue to Live Selfie Scan", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // SEQUENTIAL STEP: GMAIL VERIFICATION
                        OnboardingStep.GMAIL_VERIFICATION -> {
                            var gmailOtpInput by remember { mutableStateOf("") }
                            var gmailOtpError by remember { mutableStateOf<String?>(null) }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onboardingStep = OnboardingStep.INPUT }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gmail Verification",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "A secure verification code has been sent to your Gmail inbox ($googleSelectedEmail). Please verify your identity before we proceed.",
                                fontSize = 15.sp,
                                color = Color(0xFF475569),
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Simulated verification code is: 888999",
                                            fontSize = 14.sp,
                                            color = Color(0xFF1E40AF),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                gmailOtpInput = "888999"
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                                scope.launch {
                                                    isLoading = true
                                                    delay(500)
                                                    isLoading = false
                                                    onboardingStep = OnboardingStep.CAMERA_CAPTURE
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Auto-Fill Code", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.signInWithGoogle(
                                                    name = googleSelectedName,
                                                    email = googleSelectedEmail,
                                                    selfiePhoto = "avatar_google"
                                                )
                                                Toast.makeText(context, "Successfully Logged In (Bypassed Verification)!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Bypass & Login", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = gmailOtpInput,
                                onValueChange = {
                                    gmailOtpInput = it.filter { char -> char.isDigit() }
                                    gmailOtpError = null
                                },
                                placeholder = { Text("6-Digit Code", color = Color(0xFF475569)) },
                                label = { Text("Gmail OTP Code") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF059669)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    errorContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFF059669),
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedLabelColor = Color(0xFF059669),
                                    unfocusedLabelColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("gmail_otp_input"),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                isError = gmailOtpError != null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            if (gmailOtpError != null) {
                                Text(
                                    text = gmailOtpError!!,
                                    color = Color.Red,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(start = 4.dp).align(Alignment.Start)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (gmailOtpInput != "888999" && gmailOtpInput.length < 4) {
                                        gmailOtpError = "Invalid Gmail OTP. Use code 888999 or bypass below."
                                    } else {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        scope.launch {
                                            isLoading = true
                                            delay(1000)
                                            isLoading = false
                                            onboardingStep = OnboardingStep.CAMERA_CAPTURE
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("gmail_verify_otp_btn"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Text("Verify Gmail & Proceed", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    scope.launch {
                                        isLoading = true
                                        delay(800)
                                        isLoading = false
                                        viewModel.signInAsGuest()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("gmail_bypass_access_btn"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFF059669)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669))
                            ) {
                                Text("Skip & Grant Full Guest Access", fontWeight = FontWeight.Bold)
                            }
                        }

                        // STEP 3: SCI-FI FACE SCAN / CAMERA CAPTURE FLOW
                        OnboardingStep.CAMERA_CAPTURE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        // Back button behaves contextually based on auth method
                                        onboardingStep = if (authMethod == AuthMethodType.PHONE) {
                                            OnboardingStep.OTP_VERIFICATION
                                        } else {
                                            OnboardingStep.INPUT
                                        }
                                        hasCaptured = false
                                        isCapturing = false
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Live Facial Security Scan",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = when (authMethod) {
                                    AuthMethodType.GHANA_CARD -> "Face-biometric check: scan selfie to register your secure identity against national records."
                                    AuthMethodType.GOOGLE -> "Google verification: confirm live selfie to authenticate your linked Google services."
                                    else -> "Securing Telephone register: capture selfie to authorize your device wallet."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            // Futuristic Face Scanner viewport box
                            Box(
                                modifier = Modifier
                                    .size(230.dp)
                                    .shadow(8.dp, CircleShape)
                                    .background(Color(0xFF0F172A), CircleShape)
                                    .border(
                                        width = 3.5.dp,
                                        color = if (hasCaptured) Color(0xFF10B981) else Color(0xFFEF4444).copy(
                                            alpha = if (isFlashOn) 0.95f else 0.6f
                                        ),
                                        shape = CircleShape
                                    )
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cameraPermissionState.status.isGranted) {
                                    CameraPreview(
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        isFrontCamera = isFrontCamera
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF1E293B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                                            Icon(Icons.Default.VideocamOff, contentDescription = "Camera Perm Needed", tint = Color(0xFFCBD5E1))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Camera Permission Required", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Button(
                                                onClick = { cameraPermissionState.launchPermissionRequest() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                modifier = Modifier.height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Allow", fontSize = 12.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }

                                // Face Contour Canvas Scanner Drawing
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                ) {
                                    val width = size.width
                                    val height = size.height

                                    // Sci-fi scanner grid background
                                    val gridSpacing = 24f
                                    for (x in 0..(width / gridSpacing).toInt()) {
                                        drawLine(
                                            color = Color(0xFF1E293B).copy(alpha = 0.4f),
                                            start = Offset(x * gridSpacing, 0f),
                                            end = Offset(x * gridSpacing, height),
                                            strokeWidth = 1f
                                        )
                                    }
                                    for (y in 0..(height / gridSpacing).toInt()) {
                                        drawLine(
                                            color = Color(0xFF1E293B).copy(alpha = 0.4f),
                                            start = Offset(0f, y * gridSpacing),
                                            end = Offset(width, y * gridSpacing),
                                            strokeWidth = 1f
                                        )
                                    }

                                    // Stylized Face Outline Shape (Biometric Mesh)
                                    drawOval(
                                        color = if (hasCaptured) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFF059669).copy(alpha = 0.08f),
                                        topLeft = Offset(width * 0.26f, height * 0.16f),
                                        size = androidx.compose.ui.geometry.Size(width * 0.48f, height * 0.58f)
                                    )

                                    // Face Neon Border Aligner Contour
                                    drawOval(
                                        color = if (hasCaptured) Color(0xFF10B981).copy(alpha = 0.8f) else Color(0xFF10B981).copy(alpha = 0.4f),
                                        topLeft = Offset(width * 0.26f, height * 0.16f),
                                        size = androidx.compose.ui.geometry.Size(width * 0.48f, height * 0.58f),
                                        style = Stroke(width = 2.5f)
                                    )

                                    // Draw Shoulders Shape
                                    drawArc(
                                        color = if (hasCaptured) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFF059669).copy(alpha = 0.08f),
                                        startAngle = 180f,
                                        sweepAngle = 180f,
                                        useCenter = true,
                                        topLeft = Offset(width * 0.16f, height * 0.72f),
                                        size = androidx.compose.ui.geometry.Size(width * 0.68f, height * 0.4f)
                                    )

                                    // Eye, nose, chin keypoint tracking markers
                                    val lEye = Offset(width * 0.4f, height * 0.38f)
                                    val rEye = Offset(width * 0.6f, height * 0.38f)
                                    val nosePt = Offset(width * 0.5f, height * 0.48f)
                                    val mouthPt = Offset(width * 0.5f, height * 0.58f)
                                    val chinPt = Offset(width * 0.5f, height * 0.68f)

                                    val keypoints = listOf(lEye, rEye, nosePt, mouthPt, chinPt)
                                    keypoints.forEach { pt ->
                                        // Draw crosshair targets
                                        drawLine(
                                            color = if (hasCaptured) Color(0xFF10B981) else Color(0xFF60A5FA),
                                            start = Offset(pt.x - 7f, pt.y),
                                            end = Offset(pt.x + 7f, pt.y),
                                            strokeWidth = 2f
                                        )
                                        drawLine(
                                            color = if (hasCaptured) Color(0xFF10B981) else Color(0xFF60A5FA),
                                            start = Offset(pt.x, pt.y - 7f),
                                            end = Offset(pt.x, pt.y + 7f),
                                            strokeWidth = 2f
                                        )
                                        drawCircle(
                                            color = if (hasCaptured) Color(0xFF10B981) else Color(0xFF3B82F6),
                                            radius = 2.5f,
                                            center = pt
                                        )
                                    }

                                    // Real-time Glowing Laser Scan Sweep bar
                                    if (!hasCaptured) {
                                        val lineY = height * scanProgress
                                        drawLine(
                                            color = Color(0xFF10B981),
                                            start = Offset(0f, lineY),
                                            end = Offset(width, lineY),
                                            strokeWidth = 3.5f
                                        )
                                        // Scan shade gradient glow
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xFF10B981).copy(alpha = 0.22f), Color.Transparent),
                                                startY = lineY,
                                                endY = (lineY + 50f).coerceAtMost(height)
                                            ),
                                            topLeft = Offset(0f, lineY),
                                            size = androidx.compose.ui.geometry.Size(width, (height - lineY).coerceAtMost(50f))
                                        )
                                    }
                                }

                                // Interactive Overlay Statuses
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isFrontCamera) "[FRONT_CAM]" else "[REAR_CAM]",
                                            color = Color(0xFF60A5FA),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "LIGHT: ${if (isFlashOn) "BOOST" else "AUTO"}",
                                            color = if (isFlashOn) Color(0xFFFFD700) else Color(0xFF10B981),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (isCapturing) {
                                        Text(
                                            text = "ANALYZING REGISTRY ${(captureProgress * 100).toInt()}%",
                                            color = Color(0xFFF59E0B),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    } else if (hasCaptured) {
                                        Text(
                                            text = "IDENTITY VERIFIED 100%",
                                            color = Color(0xFF10B981),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "ALIGN FACE IN GREEN CIRCLE",
                                            color = Color(0xFF10B981),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "FPS: 30.0",
                                            color = Color(0xFF475569),
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "BIOMETRICS: ACTIVE",
                                            color = Color(0xFF475569),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // High intensity camera flash white burst simulation
                                if (isCapturing && captureProgress < 0.2f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Viewport control options row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { isFrontCamera = !isFrontCamera },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlipCameraAndroid,
                                        contentDescription = "Flip Camera",
                                        tint = Color.DarkGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Flip", fontSize = 14.sp, color = Color.DarkGray)
                                }

                                OutlinedButton(
                                    onClick = { isFlashOn = !isFlashOn },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isFlashOn) Color(0xFFFFFBEB) else Color.White
                                    ),
                                    border = BorderStroke(1.dp, if (isFlashOn) Color(0xFFFBBF24) else Color(0xFFE2E8F0)),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                        contentDescription = "Toggle Flash",
                                        tint = if (isFlashOn) Color(0xFFD97706) else Color.DarkGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isFlashOn) "Torch On" else "Torch Off",
                                        fontSize = 14.sp,
                                        color = if (isFlashOn) Color(0xFFD97706) else Color.DarkGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (isCapturing) {
                                LinearProgressIndicator(
                                    progress = { captureProgress },
                                    color = Color(0xFF059669),
                                    trackColor = Color(0xFFE2E8F0),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Saving photo...",
                                    fontSize = 14.sp,
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (hasCaptured) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Saved",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Live picture saved!",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                    
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                // Sign in complete, dispatch to ViewModel
                                                when (authMethod) {
                                                    AuthMethodType.PHONE, AuthMethodType.GHANA_CARD -> {
                                                        viewModel.signInWithPhoneAndGhanaCard(
                                                            phoneNo = "+233 $phoneInput",
                                                            cardNo = ghanaCardInput,
                                                            name = userNameInput,
                                                            selfiePhoto = if (uploadedSelfieUri != null) uploadedSelfieUri.toString() else "selfie_photo_uri_stub"
                                                        )
                                                    }
                                                    AuthMethodType.GOOGLE -> {
                                                        viewModel.signInFirebaseWithGoogle(
                                                            idToken = "simulated_google_id_token",
                                                            name = googleSelectedName,
                                                            email = googleSelectedEmail,
                                                            selfiePhoto = if (uploadedSelfieUri != null) uploadedSelfieUri.toString() else "selfie_photo_uri_stub",
                                                            onSuccess = {},
                                                            onError = { err ->
                                                                validationError = err
                                                            }
                                                        )
                                                    }
                                                    else -> {
                                                        viewModel.signInAsGuest()
                                                    }
                                                }
                                                viewModel.setTab("home")
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("go_to_home_btn"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                    ) {
                                        Text("Go to Home", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isCapturing = true
                                            captureProgress = 0f
                                            for (p in 1..5) {
                                                delay(40)
                                                captureProgress = p / 5f
                                            }
                                            isCapturing = false
                                            hasCaptured = true
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("complete_face_capture_btn"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = "Capture face",
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Take Photo", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        selfieUploadLauncher.launch("image/*")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("upload_selfie_btn"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669)),
                                    border = BorderStroke(1.5.dp, Color(0xFF059669))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = "Upload face",
                                            tint = Color(0xFF059669)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Or Upload Photo from Gallery", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Security disclaimer badge
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Your GPS location is only shared to map and trace goods securely from Bono East farms. Your data is encrypted and safe.",
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF1E40AF),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun GhanaGpsMap(
    userLat: Double?,
    userLng: Double?,
    locationGranted: Boolean,
    activeOrderStage: Int,
    onRequestLocation: () -> Unit
) {
    var zoomLevel by remember { mutableStateOf(1.0f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    var selectedMarkerInfo by remember { mutableStateOf<String?>(null) }
    
    var mapWidth by remember { mutableStateOf(360f) }
    var mapHeight by remember { mutableStateOf(320f) }

    val locations = listOf(
        MapLocation("Tuobodom (Farms)", 7.6333, -1.9000, Color(0xFF10B981), "Harvesting Hub: Okra & Tomato primary source."),
        MapLocation("Techiman (Hub)", 7.5833, -1.9333, Color(0xFFF59E0B), "Regional Wholesale Hub: Bono East logistics center."),
        MapLocation("Kumasi (Transit)", 6.6666, -1.6166, Color(0xFF3B82F6), "Central Transit Hub: Middle belt cargo routing."),
        MapLocation("Accra (Market)", 5.5600, -0.2050, Color(0xFFEF4444), "Southern Demand Market: Main tomato & pepper buyer point."),
        MapLocation("Tamale (North)", 9.4000, -0.8500, Color(0xFF8B5CF6), "Northern Gateway: Grains & onions transit connection.")
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .background(Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                panX += dragAmount.x
                                panY += dragAmount.y
                            }
                        )
                    }
                    .onSizeChanged { sizeInPx ->
                        mapWidth = sizeInPx.width.toFloat()
                        mapHeight = sizeInPx.height.toFloat()
                    }
            ) {
                val width = mapWidth
                val height = mapHeight

                fun getCanvasCoords(lat: Double, lng: Double): Offset {
                    val minLat = 4.7
                    val maxLat = 11.2
                    val minLng = -3.3
                    val maxLng = 1.2
                    
                    val xFraction = (lng - minLng) / (maxLng - minLng)
                    val yFraction = 1.0 - (lat - minLat) / (maxLat - minLat)
                    
                    val pad = 50f
                    val rx = pad + xFraction.toFloat() * (width - 2 * pad)
                    val ry = pad + yFraction.toFloat() * (height - 2 * pad)
                    
                    return Offset(
                        (rx - width/2) * zoomLevel + width/2 + panX,
                        (ry - height/2) * zoomLevel + height/2 + panY
                    )
                }

                val ghanaPath = Path().apply {
                    val p1 = getCanvasCoords(5.2, -2.8)
                    moveTo(p1.x, p1.y)
                    val p2 = getCanvasCoords(5.8, -3.1)
                    lineTo(p2.x, p2.y)
                    val p3 = getCanvasCoords(7.5, -3.1)
                    lineTo(p3.x, p3.y)
                    val p4 = getCanvasCoords(10.2, -2.8)
                    lineTo(p4.x, p4.y)
                    val p5 = getCanvasCoords(11.0, -2.5)
                    lineTo(p5.x, p5.y)
                    val p6 = getCanvasCoords(11.1, -1.0)
                    lineTo(p6.x, p6.y)
                    val p7 = getCanvasCoords(11.1, 0.0)
                    lineTo(p7.x, p7.y)
                    val p8 = getCanvasCoords(10.2, 0.5)
                    lineTo(p8.x, p8.y)
                    val p9 = getCanvasCoords(8.2, 0.8)
                    lineTo(p9.x, p9.y)
                    val p10 = getCanvasCoords(6.1, 1.1)
                    lineTo(p10.x, p10.y)
                    val p11 = getCanvasCoords(5.6, 0.8)
                    lineTo(p11.x, p11.y)
                    val p12 = getCanvasCoords(5.5, -0.2)
                    lineTo(p12.x, p12.y)
                    val p13 = getCanvasCoords(5.1, -1.5)
                    lineTo(p13.x, p13.y)
                    close()
                }

                drawPath(path = ghanaPath, color = Color(0xFFE2F0D9))
                drawPath(path = ghanaPath, color = Color(0xFF0F5132).copy(alpha = 0.2f), style = Stroke(width = 4f))

                val lakePath = Path().apply {
                    val lp1 = getCanvasCoords(6.3, -0.1)
                    moveTo(lp1.x, lp1.y)
                    val lp2 = getCanvasCoords(6.9, 0.1)
                    lineTo(lp2.x, lp2.y)
                    val lp3 = getCanvasCoords(7.3, -0.2)
                    lineTo(lp3.x, lp3.y)
                    val lp4 = getCanvasCoords(7.8, -0.2)
                    lineTo(lp4.x, lp4.y)
                    val lp5 = getCanvasCoords(7.5, -0.4)
                    lineTo(lp5.x, lp5.y)
                    val lp6 = getCanvasCoords(6.6, -0.3)
                    lineTo(lp6.x, lp6.y)
                    close()
                }
                drawPath(path = lakePath, color = Color(0xFF93C5FD))
                drawPath(path = lakePath, color = Color(0xFF3B82F6).copy(alpha = 0.5f), style = Stroke(width = 1.5f))

                val corridorPath = Path().apply {
                    val start = getCanvasCoords(7.6333, -1.9000)
                    moveTo(start.x, start.y)
                    val hub = getCanvasCoords(7.5833, -1.9333)
                    lineTo(hub.x, hub.y)
                    val transit = getCanvasCoords(6.6666, -1.6166)
                    lineTo(transit.x, transit.y)
                    val dest = getCanvasCoords(5.5600, -0.2050)
                    lineTo(dest.x, dest.y)
                }
                drawPath(path = corridorPath, color = Color(0xFFCBD5E1), style = Stroke(width = 8f))
                drawPath(path = corridorPath, color = Color(0xFFF59E0B), style = Stroke(width = 3f))

                if (activeOrderStage > 0) {
                    val stageFraction = activeOrderStage.toFloat() / 9f
                    val tCoord = when {
                        stageFraction <= 0.2f -> {
                            getCanvasCoords(7.6333 - (7.6333 - 7.5833) * (stageFraction / 0.2), -1.9000 - (-1.9000 - -1.9333) * (stageFraction / 0.2))
                        }
                        stageFraction <= 0.6f -> {
                            val ratio = (stageFraction - 0.2f) / 0.4f
                            getCanvasCoords(7.5833 - (7.5833 - 6.6666) * ratio, -1.9333 - (-1.9333 - -1.6166) * ratio)
                        }
                        else -> {
                            val ratio = (stageFraction - 0.6f) / 0.4f
                            getCanvasCoords(6.6666 - (6.6666 - 5.5600) * ratio, -1.6166 - (-1.6166 - -0.2050) * ratio)
                        }
                    }

                    drawCircle(color = Color(0xFFEF4444).copy(alpha = 0.25f), radius = 24f, center = tCoord)
                    drawCircle(color = Color(0xFFEF4444), radius = 8f, center = tCoord)
                }

                locations.forEach { city ->
                    val coords = getCanvasCoords(city.lat, city.lng)
                    drawCircle(color = Color.White, radius = 9f, center = coords)
                    drawCircle(color = city.color, radius = 6f, center = coords)
                }

                if (userLat != null && userLng != null) {
                    val uCoords = getCanvasCoords(userLat, userLng)
                    drawCircle(color = Color(0xFF2563EB).copy(alpha = 0.2f), radius = 30f, center = uCoords)
                    drawCircle(color = Color(0xFF2563EB).copy(alpha = 0.4f), radius = 16f, center = uCoords)
                    drawCircle(color = Color.White, radius = 7f, center = uCoords)
                    drawCircle(color = Color(0xFF2563EB), radius = 4f, center = uCoords)
                }
            }

            locations.forEach { city ->
                val minLat = 4.7
                val maxLat = 11.2
                val minLng = -3.3
                val maxLng = 1.2
                val xFraction = (city.lng - minLng) / (maxLng - minLng)
                val yFraction = 1.0 - (city.lat - minLat) / (maxLat - minLat)
                val pad = 50f
                val rx = pad + xFraction.toFloat() * (mapWidth - 2 * pad)
                val ry = pad + yFraction.toFloat() * (mapHeight - 2 * pad)
                val finalX = (rx - mapWidth/2) * zoomLevel + mapWidth/2 + panX
                val finalY = (ry - mapHeight/2) * zoomLevel + mapHeight/2 + panY

                if (finalX > 0 && finalX < mapWidth && finalY > 0 && finalY < mapHeight) {
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (finalX / LocalContext.current.resources.displayMetrics.density).dp - 40.dp,
                                y = (finalY / LocalContext.current.resources.displayMetrics.density).dp - 30.dp
                            )
                            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
                            .border(1.dp, city.color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable { selectedMarkerInfo = "${city.name}\n${city.description}" }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = city.name.substringBefore(" "),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }

            if (userLat != null && userLng != null) {
                val minLat = 4.7
                val maxLat = 11.2
                val minLng = -3.3
                val maxLng = 1.2
                val xFraction = (userLng - minLng) / (maxLng - minLng)
                val yFraction = 1.0 - (userLat - minLat) / (maxLat - minLat)
                val pad = 50f
                val rx = pad + xFraction.toFloat() * (mapWidth - 2 * pad)
                val ry = pad + yFraction.toFloat() * (mapHeight - 2 * pad)
                val finalX = (rx - mapWidth/2) * zoomLevel + mapWidth/2 + panX
                val finalY = (ry - mapHeight/2) * zoomLevel + mapHeight/2 + panY

                if (finalX > 0 && finalX < mapWidth && finalY > 0 && finalY < mapHeight) {
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (finalX / LocalContext.current.resources.displayMetrics.density).dp - 35.dp,
                                y = (finalY / LocalContext.current.resources.displayMetrics.density).dp + 10.dp
                            )
                            .background(Color(0xFF2563EB), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "You (GPS)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ghana Map Active", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (locationGranted) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { if (!locationGranted) onRequestLocation() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (locationGranted) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                            contentDescription = "GPS State",
                            tint = if (locationGranted) Color(0xFF15803D) else Color(0xFFB91C1C),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (locationGranted) "GPS On" else "Allow GPS",
                            color = if (locationGranted) Color(0xFF166534) else Color(0xFF991B1B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .shadow(2.dp, RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            ) {
                Column {
                    IconButton(
                        onClick = { zoomLevel = (zoomLevel + 0.2f).coerceAtMost(3.0f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.width(36.dp))
                    IconButton(
                        onClick = { zoomLevel = (zoomLevel - 0.2f).coerceAtLeast(0.6f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.width(36.dp))
                    IconButton(
                        onClick = {
                            zoomLevel = 1.0f
                            panX = 0f
                            panY = 0f
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Recenter", tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = selectedMarkerInfo != null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedMarkerInfo?.substringBefore("\n") ?: "",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            IconButton(
                                onClick = { selectedMarkerInfo = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Info", tint = Color(0xFF475569), modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedMarkerInfo?.substringAfter("\n") ?: "",
                            color = Color(0xFFCBD5E1),
                            fontSize = 14.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            if (activeOrderStage > 0 && selectedMarkerInfo == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                        .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalShipping, contentDescription = "Shipment", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Produce Truck Transit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Text(
                                text = when (activeOrderStage) {
                                    1 -> "Order Confirmed"
                                    2 -> "Waiting for Driver"
                                    3 -> "Driver Assigned"
                                    4 -> "Driver Arriving"
                                    5, 6 -> "In Transit (Techiman -> Accra)"
                                    7 -> "Approaching Accra"
                                    else -> "Delivered"
                                },
                                fontSize = 13.sp,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

data class MapLocation(
    val name: String,
    val lat: Double,
    val lng: Double,
    val color: Color,
    val description: String
)

@Composable
fun WhatsAppStyleChatOverlay(viewModel: GeoHarvestViewModel) {
    val contactName by viewModel.activeChatContactName.collectAsStateWithLifecycle()
    val contactRole by viewModel.activeChatRole.collectAsStateWithLifecycle()
    val messages by viewModel.activeChatMessagesState.collectAsStateWithLifecycle()
    
    if (contactName == null) return

    var currentMessageText by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to bottom when messages list size increases
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .zIndex(500f)
            .clickable(enabled = false) {}
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE5DDD5))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // HEADER (WhatsApp Dark Teal)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF075E54))
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.dismissWhatsAppChat() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF128C7E), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contactName?.take(1)?.uppercase(java.util.Locale.ROOT) ?: "",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contactName.orEmpty(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF25D366), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "online • $contactRole",
                                    color = Color(0xFF25D366),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Phone, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.VideoCall, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                // CHAT MESSAGE AREA
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { (sender, text) ->
                        val isMe = sender == "You"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isMe) 12.dp else 0.dp,
                                    bottomEnd = if (isMe) 0.dp else 12.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMe) Color(0xFFE7FFDB) else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .padding(vertical = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    if (!isMe) {
                                        Text(
                                            text = sender,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF075E54),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = text,
                                        color = Color(0xFF1E293B),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.align(Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "12:30 PM",
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                        if (isMe) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.DoneAll,
                                                null,
                                                tint = Color(0xFF34B7F1),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // BOTTOM INPUT BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = currentMessageText,
                        onValueChange = { currentMessageText = it },
                        placeholder = { Text("Type message...", color = Color(0xFF64748B), fontSize = 15.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = Color(0xFF0F172A)),
                        leadingIcon = {
                            Icon(
                                Icons.Default.SentimentSatisfied,
                                contentDescription = "Smiley",
                                tint = Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = "Attach",
                                tint = Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(25.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF075E54),
                            unfocusedBorderColor = Color.LightGray
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF075E54), CircleShape)
                            .clickable {
                                if (currentMessageText.isNotBlank()) {
                                    val prompt = currentMessageText
                                    currentMessageText = ""
                                    val replyText = when {
                                        prompt.lowercase().contains("price") || prompt.lowercase().contains("ghs") || prompt.lowercase().contains("negotiat") -> {
                                            "Hello! Yes, I am open to custom rates. GHS 120 per box sounds reasonable for high quality organic yield. Send dispatch details!"
                                        }
                                        prompt.lowercase().contains("where") || prompt.lowercase().contains("location") -> {
                                            "I am currently located near Techiman High Road. Ready to load immediately. Send me the coordinates!"
                                        }
                                        else -> "Excellent! Alhaji here. I am starting my engine. Please confirm the transaction on GeoHarvest and I will begin transit."
                                    }
                                    viewModel.sendWhatsAppMessage(prompt, replyText)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
