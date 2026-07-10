fun MarketplaceScreen(viewModel: GeoHarvestViewModel) {
    val listings by viewModel.produceListings.collectAsStateWithLifecycle()
    val marketPrices by viewModel.marketPrices.collectAsStateWithLifecycle()
    val transporters by viewModel.transporters.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    var marketStep by remember { mutableStateOf("QUESTION") }
    val backStack = remember { mutableStateListOf<String>() }
    fun navTo(step: String) { backStack.add(marketStep); marketStep = step }
    fun goBack() { marketStep = if (backStack.isNotEmpty()) backStack.removeAt(backStack.size - 1) else "QUESTION" }

    // Form states
    var title by remember { mutableStateOf("") }
    var vegetableType by remember { mutableStateOf("Tomatoes") }
    var quantityStr by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var collectionPoint by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Dialogs
    var selectedListingForPurchase by remember { mutableStateOf<ProduceListing?>(null) }
    var purchaseQuantityStr by remember { mutableStateOf("") }
    var selectedTransporter by remember { mutableStateOf<Transporter?>(null) }
    var selectedOfficer by remember { mutableStateOf<Map<String, String>?>(null) }
    var selectedEquipment by remember { mutableStateOf<Map<String, Any>?>(null) }
    var equipAction by remember { mutableStateOf("") }
    var landSize by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Helper data
    val mockCereal = remember { listOf(
        ProduceListing(2001, "Kwame Boakye", "+233241112233", "White Maize Sacks", "Cereal", 50.0, 180.0, "Tamale, Northern", "Tamale Depot", "Sun-dried white maize grain, clean and bagged.", trustScore = 98),
        ProduceListing(2002, "Beatrice Ansah", "+233208887766", "Organic Local Rice", "Cereal", 30.0, 320.0, "Techiman, Bono East", "Coop Warehouse", "Fragrant long-grain local brown rice.", trustScore = 96)
    )}
    val mockTuber = remember { listOf(
        ProduceListing(2101, "Kofi Mensah", "+233559876543", "Premium Pona Yam", "Tuber", 40.0, 140.0, "Techiman, Bono East", "Techiman Market", "Sweet, dry texture Grade-A yams.", trustScore = 97),
        ProduceListing(2102, "Abena Boateng", "+233205554433", "Fresh Cassava Sacks", "Tuber", 60.0, 90.0, "Sunyani, Bono", "Lorry Station", "Freshly harvested cassava tubers.", trustScore = 95)
    )}
    val mockWheat = remember { listOf(
        ProduceListing(2201, "Mary Appiah", "+233205559999", "Local Spelt Wheat Sacks", "Wheat", 30.0, 250.0, "Koforidua, Eastern", "Koforidua Market", "High-protein organically grown wheat.", trustScore = 98)
    )}
    val mockSeed = remember { listOf(
        ProduceListing(2301, "Johannes Aboagye", "+233245556677", "Hybrid Maize Seeds", "Seed", 100.0, 450.0, "Techiman, Bono East", "Extension Station", "Certified pest-resistant seeds.", trustScore = 99)
    )}
    val mockLegumes = remember { listOf(
        ProduceListing(2401, "Agnes Boateng", "+233208887766", "Red Cowpea Sacks", "Legumes", 35.0, 210.0, "Tamale, Northern", "Tamale Station", "Cleaned organic cowpeas.", trustScore = 96)
    )}
    val mockFruits = remember { listOf(
        ProduceListing(2501, "Abena Sarfo", "+233245551122", "Sweet Kent Mangoes", "Fruits", 80.0, 80.0, "Techiman, Bono East", "Highway Junction", "Fleshy organic Kent mangoes.", trustScore = 97)
    )}

    val electronicEquip = remember { listOf(
        mapOf("name" to "Solar-Powered Irrigation Pump", "price" to 1800.0, "isRent" to true, "rent" to 150.0, "loc" to "Techiman", "dealer" to "AgriTech Ghana", "rating" to 4.9, "desc" to "Solar pump with drip kit for up to 1 hectare."),
        mapOf("name" to "Digital Crop Moisture Meter", "price" to 450.0, "isRent" to false, "rent" to 0.0, "loc" to "Accra", "dealer" to "AgroElectronics", "rating" to 4.7, "desc" to "Crop moisture detector for grains.")
    )}
    val nonElectronicEquip = remember { listOf(
        mapOf("name" to "Stainless Steel Cutlass (Crocodile Brand)", "price" to 75.0, "isRent" to false, "rent" to 0.0, "loc" to "Techiman", "dealer" to "Coop Store", "rating" to 4.9, "desc" to "Genuine durable high-carbon steel cutlass."),
        mapOf("name" to "Manual Hand-Push Seed Seeder", "price" to 320.0, "isRent" to true, "rent" to 30.0, "loc" to "Sunyani", "dealer" to "Bono Tools", "rating" to 4.8, "desc" to "Consistently sows maize/cowpeas without bending.")
    )}

    val extOfficers = remember { listOf(
        mapOf("name" to "Dr. Emmanuel Osei", "spec" to "Soil Nutrition & Crop Health", "loc" to "Techiman, Bono East", "rating" to "4.9", "exp" to "8 yrs", "bio" to "Assisting growers with organic soil enrichment & fertilizer pre-testing."),
        mapOf("name" to "Agnes Boateng", "spec" to "Irrigation & Sustainable Farms", "loc" to "Sunyani, Bono", "rating" to "4.8", "exp" to "5 yrs", "bio" to "Specialist in drip irrigation layouts and dry-season water planning.")
    )}
    val vetOfficers = remember { listOf(
        mapOf("name" to "Dr. Kwaku Mensah, DVM", "spec" to "Poultry & Livestock Vaccination", "loc" to "Kumasi, Ashanti", "rating" to "4.9", "exp" to "12 yrs", "bio" to "Expert in intensive poultry biosecurity and livestock disease control."),
        mapOf("name" to "Abena Sarfo, Vet", "spec" to "Ruminant Clinical Diagnostics", "loc" to "Wenchi, Bono", "rating" to "4.8", "exp" to "6 yrs", "bio" to "Provides mobile herd emergency checkups and nutrition design.")
    )}

    Box(modifier = Modifier.fillMaxSize()) {
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
                            Text("Agri-Market", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = when (marketStep) {
                                    "QUESTION" -> "Choose Action"
                                    "SELL" -> "Sell Produce"
                                    "BUY" -> "Buy Directory"
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
                                    "LAND_SIZE" -> "Land Size"
                                    "LAND_REGION" -> "Ghana 16 Regions"
                                    else -> marketStep
                                },
                                fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF064E3B)
                            )
                        }
                    }
                    if (marketStep != "QUESTION") {
                        IconButton(onClick = { backStack.clear(); marketStep = "QUESTION" }, modifier = Modifier.size(36.dp)) {
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
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("What do you want to buy or sell?", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Secure localized transactions integrated with the Ghana national agricultural register.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { navTo("BUY") }.testTag("buy_card"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                                border = BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ShoppingBasket, null, tint = Color(0xFF065F46), modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("I want to buy", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF064E3B))
                                        Text("Browse crops, land, tools, logistics & officers.", fontSize = 11.sp, color = Color(0xFF047857))
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { navTo("SELL") }.testTag("sell_card"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                                border = BorderStroke(1.dp, Color(0xFFF97316))
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AddBusiness, null, tint = Color(0xFF9A3412), modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("I want to sell", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF7C2D12))
                                        Text("List produce & set pricing.", fontSize = 11.sp, color = Color(0xFFC2410C))
                                    }
                                }
                            }
                        }
                    }
                    "SELL" -> {
                        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("List Produce", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Product Title") }, modifier = Modifier.fillMaxWidth())
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Tomatoes", "Okra", "Garden Eggs", "Peppers").forEach { opt ->
                                    FilterChip(selected = vegetableType == opt, onClick = {
                                        vegetableType = opt
                                        val rec = marketPrices.find { it.vegetableType == opt }
                                        if (rec != null) priceStr = rec.recommendedPrice.toString()
                                    }, label = { Text(opt) })
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
                                            if (collectionPoint.isBlank()) "Depot" else collectionPoint, description
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
                    "BUY" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Buy Directory", fontWeight = FontWeight.Bold)
                            listOf(
                                Triple("Farm Produce", Icons.Default.Agriculture, "PRODUCE"),
                                Triple("Equipment", Icons.Default.Handyman, "EQUIP"),
                                Triple("Extension Officers", Icons.Default.SupportAgent, "OFFICERS"),
                                Triple("Logistics & Transport", Icons.Default.LocalShipping, "LOGISTICS"),
                                Triple("Available Farm Lands", Icons.Default.Landscape, "LAND_SIZE")
                            ).forEach { (lbl, icon, step) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { navTo(step) },
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(icon, null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(lbl, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                    "PRODUCE" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Select Crop Category", fontWeight = FontWeight.Bold)
                            listOf(
                                "Cereal" to "CEREAL", "Tuber" to "TUBER", "Wheat" to "WHEAT",
                                "Seed" to "SEED", "Legumes" to "LEGUMES", "Vegetables" to "VEGETABLES", "Fruits" to "FRUITS"
                            ).forEach { (lbl, step) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { navTo(step) },
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(lbl, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                    "CEREAL" -> { ListCrops(mockCereal, viewModel) { selectedListingForPurchase = it } }
                    "TUBER" -> { ListCrops(mockTuber, viewModel) { selectedListingForPurchase = it } }
                    "WHEAT" -> { ListCrops(mockWheat, viewModel) { selectedListingForPurchase = it } }
                    "SEED" -> { ListCrops(mockSeed, viewModel) { selectedListingForPurchase = it } }
                    "LEGUMES" -> { ListCrops(mockLegumes, viewModel) { selectedListingForPurchase = it } }
                    "VEGETABLES" -> {
                        val userVegs = listings.filter { it.vegetableType in listOf("Tomatoes", "Okra", "Garden Eggs", "Peppers") }
                        val items = if (userVegs.isNotEmpty()) userVegs else listOf(
                            ProduceListing(2601, "Beatrice Ansah", "+233208887766", "Bono Tomatoes Crate", "Tomatoes", 25.0, 120.0, "Techiman, Bono East", "Coop Depot", "Fresh local red tomatoes.", trustScore = 98)
                        )
                        ListCrops(items, viewModel) { selectedListingForPurchase = it }
                    }
                    "FRUITS" -> { ListCrops(mockFruits, viewModel) { selectedListingForPurchase = it } }
                    "EQUIP" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Select Equipment Type")
                            Card(modifier = Modifier.fillMaxWidth().clickable { navTo("E_ELEC") }) {
                                Row(modifier = Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.Bolt, null, tint = Color(0xFFF59E0B))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Electronic Equipment", fontWeight = FontWeight.Bold)
                                        Text("Solar pumps, crop moisture detectors.", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                            Card(modifier = Modifier.fillMaxWidth().clickable { navTo("E_NON") }) {
                                Row(modifier = Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.Build, null, tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Non-Electronic Equipment", fontWeight = FontWeight.Bold)
                                        Text("Cutlasses, manual seeders, knapsacks.", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                    "E_ELEC" -> { ListEquip(electronicEquip) { item, act -> selectedEquipment = item; equipAction = act } }
                    "E_NON" -> { ListEquip(nonElectronicEquip) { item, act -> selectedEquipment = item; equipAction = act } }
                    "OFFICERS" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Select Officer Specialty")
                            Card(modifier = Modifier.fillMaxWidth().clickable { navTo("O_EXT") }) {
                                Row(modifier = Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.School, null, tint = Color(0xFF0D9488))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Extension Officers", fontWeight = FontWeight.Bold)
                                }
                            }
                            Card(modifier = Modifier.fillMaxWidth().clickable { navTo("O_VET") }) {
                                Row(modifier = Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.Pets, null, tint = Color.Red)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Veterinary Officers", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    "O_EXT" -> { ListOfficers(extOfficers) { selectedOfficer = it } }
                    "O_VET" -> { ListOfficers(vetOfficers) { selectedOfficer = it } }
                    "LOGISTICS" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Available Transporters", fontWeight = FontWeight.Bold)
                            transporters.forEach { carrier ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(carrier.name, fontWeight = FontWeight.Bold)
                                            Text(carrier.vehicle, fontSize = 11.sp, color = Color.Gray)
                                            Text("Rate: GHS ${carrier.baseRate} + GHS ${carrier.ratePerKm}/km", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Button(onClick = { viewModel.showUserProfileDialog(carrier.name, "Transporter") }) {
                                            Text("Contact", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "LAND_SIZE" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Do you want a hectare, a plot, a semi-plot, or all various categories of farm sizes?", fontWeight = FontWeight.Bold)
                            listOf("Hectare (2.47 Acres)", "Plot (70 x 100 ft)", "Semi-Plot (50 x 70 ft)", "All Sizes").forEach { size ->
                                Card(modifier = Modifier.fillMaxWidth().clickable { landSize = size; navTo("LAND_REGION") }) {
                                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(size, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowForward, null)
                                    }
                                }
                            }
                        }
                    }
                    "LAND_REGION" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Select Region of Ghana for $landSize", fontWeight = FontWeight.Bold)
                            val regions = listOf(
                                "Bono East Region" to Pair(7.5833, -1.9333),
                                "Bono Region" to Pair(7.3333, -2.3333),
                                "Ahafo Region" to Pair(7.0000, -2.5000),
                                "Ashanti Region" to Pair(6.6667, -1.6167),
                                "Eastern Region" to Pair(6.0833, -0.2500),
                                "Greater Accra Region" to Pair(5.5500, -0.2000),
                                "Central Region" to Pair(5.1000, -1.2500),
                                "Western Region" to Pair(5.1667, -2.0000),
                                "Western North Region" to Pair(6.1667, -2.8000),
                                "Volta Region" to Pair(6.5000, 0.4667),
                                "Oti Region" to Pair(7.8333, 0.3333),
                                "Northern Region" to Pair(9.4000, -0.8500),
                                "Savannah Region" to Pair(9.1000, -1.8000),
                                "North East Region" to Pair(10.5000, -0.8000),
                                "Upper East Region" to Pair(10.7833, -0.8500),
                                "Upper West Region" to Pair(10.0667, -2.5000)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                regions.chunked(2).forEach { row ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach { (regName, coords) ->
                                            Button(
                                                onClick = {
                                                    viewModel.updateLocation(coords.first, coords.second)
                                                    viewModel.setTab("live_map")
                                                    Toast.makeText(context, "Locating $landSize in $regName on Map!", Toast.LENGTH_LONG).show()
                                                },
                                                modifier = Modifier.weight(1f).height(44.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                                border = BorderStroke(1.dp, Color.LightGray)
                                            ) {
                                                Text(regName, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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

        // Purchase Dialog
        if (selectedListingForPurchase != null) {
            val listing = selectedListingForPurchase!!
            AlertDialog(
                onDismissRequest = { selectedListingForPurchase = null },
                title = { Text("Confirm Purchase & Dispatch") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Farmer: ${listing.farmerName} (${listing.trustScore}% Trust)")
                        Text("Item: ${listing.title} | GHS ${listing.pricePerUnit}", fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = purchaseQuantityStr, onValueChange = { purchaseQuantityStr = it }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth())
                        transporters.firstOrNull()?.let { driver ->
                            Text("Assigned Transporter: ${driver.name} (${driver.vehicle})", color = Color(0xFF059669))
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val driver = transporters.firstOrNull()
                        if (driver != null) {
                            viewModel.buyProduce(listing, purchaseQuantityStr.toDoubleOrNull() ?: listing.quantity, "Mary Appiah (You)", "+233205559999", driver)
                            Toast.makeText(context, "Order placed successfully!", Toast.LENGTH_SHORT).show()
                            selectedListingForPurchase = null
                        }
                    }) { Text("Buy Now") }
                },
                dismissButton = { TextButton(onClick = { selectedListingForPurchase = null }) { Text("Cancel") } }
            )
        }

        // Officer Dialog
        if (selectedOfficer != null) {
            val officer = selectedOfficer!!
            AlertDialog(
                onDismissRequest = { selectedOfficer = null },
                title = { Text(officer["name"] ?: "") },
                text = {
                    Column {
                        Text("Specialty: ${officer["spec"]}", fontWeight = FontWeight.Bold)
                        Text("Location: ${officer["loc"]}")
                        Text("Exp: ${officer["exp"]} | Rating: ${officer["rating"]}★")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(officer["bio"] ?: "", color = Color.Gray, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.addActivityLog("Farmer", "Consulted ${officer["name"]}", "Just now")
                        Toast.makeText(context, "Consultation requested!", Toast.LENGTH_SHORT).show()
                        selectedOfficer = null
                    }) { Text("Call Officer") }
                },
                dismissButton = { TextButton(onClick = { selectedOfficer = null }) { Text("Close") } }
            )
        }

        // Equipment Dialog
        if (selectedEquipment != null) {
            val eq = selectedEquipment!!
            AlertDialog(
                onDismissRequest = { selectedEquipment = null },
                title = { Text("Confirm $equipAction") },
                text = {
                    Column {
                        Text(eq["name"].toString(), fontWeight = FontWeight.Bold)
                        Text("Dealer: ${eq["dealer"]} | Rating: ${eq["rating"]}★")
                        Text("Location: ${eq["loc"]}")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(eq["desc"].toString(), fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        val cost = if (equipAction == "Rent") eq["rent"] else eq["price"]
                        Text("Total Price: GHS $cost", fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.addActivityLog("Farmer", "Placed $equipAction request for ${eq["name"]}", "Just now")
                        Toast.makeText(context, "Request logged on local ledger!", Toast.LENGTH_SHORT).show()
                        selectedEquipment = null
                    }) { Text("Confirm") }
                },
                dismissButton = { TextButton(onClick = { selectedEquipment = null }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun ListCrops(list: List<ProduceListing>, viewModel: GeoHarvestViewModel, onSelect: (ProduceListing) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        items(list) { crop ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(crop.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${crop.trustScore}% Trust", color = Color(0xFF059669), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Producer: ${crop.farmerName}", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.clickable {
                        viewModel.showUserProfileDialog(crop.farmerName, "Farmer")
                    })
                    Text("Location: ${crop.location}", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(crop.description, fontSize = 11.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("GHS ${crop.pricePerUnit}", fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                        Button(onClick = { onSelect(crop) }, shape = RoundedCornerShape(8.dp)) {
                            Text("Buy")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListEquip(list: List<Map<String, Any>>, onSelect: (Map<String, Any>, String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        items(list) { eq ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(eq["name"].toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Dealer: ${eq["dealer"]} | Rating: ${eq["rating"]}★", fontSize = 11.sp, color = Color.Gray)
                    Text("Location: ${eq["loc"]}", fontSize = 11.sp, color = Color.Gray)
                    Text(eq["desc"].toString(), fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Buy: GHS ${eq["price"]}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            if (eq["isRent"] as Boolean) {
                                Text("Rent: GHS ${eq["rent"]}/day", fontSize = 11.sp, color = Color(0xFF059669))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (eq["isRent"] as Boolean) {
                                OutlinedButton(onClick = { onSelect(eq, "Rent") }) { Text("Rent", fontSize = 10.sp) }
                            }
                            Button(onClick = { onSelect(eq, "Buy") }) { Text("Buy", fontSize = 10.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListOfficers(list: List<Map<String, String>>, onSelect: (Map<String, String>) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        items(list) { officer ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(officer["name"] ?: "", fontWeight = FontWeight.Bold)
                        Text(officer["spec"] ?: "", color = Color(0xFF0D9488), fontSize = 11.sp)
                        Text("Loc: ${officer["loc"]} | Rating: ${officer["rating"]}★", fontSize = 10.sp, color = Color.Gray)
                    }
                    Button(onClick = { onSelect(officer) }) {
                        Text("Consult", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
