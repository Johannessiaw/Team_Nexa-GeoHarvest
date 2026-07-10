package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EquipDemo(val name: String, val price: Double, val subName: String, val subId: Int, val unit: String, val img: String)
data class LogisticsDemo(val name: String, val subName: String, val subId: Int, val rate: Double)
data class LandDemo(val name: String, val subName: String, val subId: Int, val price: Double, val unit: String)

class GeoHarvestRepository(private val dao: GeoHarvestDao) {

    val allProduceListings: Flow<List<ProduceListing>> = dao.getAllProduceListings()
    val allBuyers: Flow<List<Buyer>> = dao.getAllBuyers()
    val allOrders: Flow<List<Order>> = dao.getAllOrders()
    val allTransporters: Flow<List<Transporter>> = dao.getAllTransporters()
    val allMarketPrices: Flow<List<MarketPrice>> = dao.getAllMarketPrices()
    val allChatMessages: Flow<List<ChatMessage>> = dao.getAllChatMessages()
    val allEngagementActivities: Flow<List<EngagementActivity>> = dao.getAllEngagementActivities()
    val allUserCredentials: Flow<List<UserCredential>> = dao.getAllUserCredentials()
    val allFeedback: Flow<List<TransactionFeedback>> = dao.getAllFeedback()

    val allCategories: Flow<List<MarketplaceCategory>> = dao.getAllCategories()
    val allSubcategories: Flow<List<MarketplaceSubcategory>> = dao.getAllSubcategories()
    val allMarketplaceItems: Flow<List<MarketplaceItem>> = dao.getAllMarketplaceItems()

    suspend fun insertCategory(category: MarketplaceCategory) = withContext(Dispatchers.IO) { dao.insertCategory(category) }
    suspend fun updateCategory(category: MarketplaceCategory) = withContext(Dispatchers.IO) { dao.updateCategory(category) }
    suspend fun deleteCategory(category: MarketplaceCategory) = withContext(Dispatchers.IO) { dao.deleteCategory(category) }
    suspend fun clearCategories() = withContext(Dispatchers.IO) { dao.clearCategories() }

    suspend fun insertSubcategory(subcategory: MarketplaceSubcategory) = withContext(Dispatchers.IO) { dao.insertSubcategory(subcategory) }
    suspend fun updateSubcategory(subcategory: MarketplaceSubcategory) = withContext(Dispatchers.IO) { dao.updateSubcategory(subcategory) }
    suspend fun deleteSubcategory(subcategory: MarketplaceSubcategory) = withContext(Dispatchers.IO) { dao.deleteSubcategory(subcategory) }
    suspend fun clearSubcategories() = withContext(Dispatchers.IO) { dao.clearSubcategories() }

    suspend fun insertMarketplaceItem(item: MarketplaceItem) = withContext(Dispatchers.IO) { dao.insertMarketplaceItem(item) }
    suspend fun updateMarketplaceItem(item: MarketplaceItem) = withContext(Dispatchers.IO) { dao.updateMarketplaceItem(item) }
    suspend fun deleteMarketplaceItem(item: MarketplaceItem) = withContext(Dispatchers.IO) { dao.deleteMarketplaceItem(item) }
    suspend fun clearMarketplaceItems() = withContext(Dispatchers.IO) { dao.clearMarketplaceItems() }

    fun getFeedbackByTarget(targetName: String): Flow<List<TransactionFeedback>> = dao.getFeedbackByTarget(targetName)

    suspend fun insertFeedback(feedback: TransactionFeedback) = withContext(Dispatchers.IO) {
        dao.insertFeedback(feedback)
    }

    fun getOrderById(id: Int): Flow<Order?> = dao.getOrderById(id)

    suspend fun getActiveUserCredential(): UserCredential? = withContext(Dispatchers.IO) {
        dao.getActiveUserCredential()
    }

    suspend fun insertUserCredential(credential: UserCredential) = withContext(Dispatchers.IO) {
        dao.insertUserCredential(credential)
    }

    suspend fun deactivateAllUserCredentials() = withContext(Dispatchers.IO) {
        dao.deactivateAllUserCredentials()
    }

    fun getEngagementActivitiesByProfile(profileName: String): Flow<List<EngagementActivity>> = 
        dao.getEngagementActivitiesByProfile(profileName)

    suspend fun insertEngagementActivity(activity: EngagementActivity) = withContext(Dispatchers.IO) {
        dao.insertEngagementActivity(activity)
    }

    suspend fun clearEngagementActivities() = withContext(Dispatchers.IO) {
        dao.clearEngagementActivities()
    }

    suspend fun updateBuyer(buyer: Buyer) = withContext(Dispatchers.IO) {
        dao.updateBuyer(buyer)
    }

    suspend fun updateTransporter(transporter: Transporter) = withContext(Dispatchers.IO) {
        dao.updateTransporter(transporter)
    }

    suspend fun insertTransporter(transporter: Transporter) = withContext(Dispatchers.IO) {
        dao.insertTransporter(transporter)
    }

    suspend fun insertProduceListing(listing: ProduceListing) = withContext(Dispatchers.IO) {
        dao.insertProduceListing(listing)
    }

    suspend fun updateProduceListing(listing: ProduceListing) = withContext(Dispatchers.IO) {
        dao.updateProduceListing(listing)
    }

    suspend fun deleteProduceListing(listing: ProduceListing) = withContext(Dispatchers.IO) {
        dao.deleteProduceListing(listing)
    }

    suspend fun insertOrder(order: Order): Long = withContext(Dispatchers.IO) {
        dao.insertOrder(order)
    }

    suspend fun updateOrder(order: Order) = withContext(Dispatchers.IO) {
        dao.updateOrder(order)
    }

    suspend fun insertChatMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        dao.insertChatMessage(message)
    }

    suspend fun clearChatMessages() = withContext(Dispatchers.IO) {
        dao.clearChatMessages()
    }

    suspend fun getUnsyncedListings(): List<ProduceListing> = withContext(Dispatchers.IO) {
        dao.getUnsyncedProduceListings()
    }

    suspend fun getUnsyncedOrders(): List<Order> = withContext(Dispatchers.IO) {
        dao.getUnsyncedOrders()
    }

    suspend fun prepopulateDemoData() = withContext(Dispatchers.IO) {
        // Clear all except chat maybe, let's keep database clean
        dao.clearBuyers()
        dao.clearMarketPrices()
        dao.clearTransporters()
        dao.clearEngagementActivities()
        dao.clearCategories()
        dao.clearSubcategories()
        dao.clearMarketplaceItems()

        // 1. Insert Categories
        val catProduce = MarketplaceCategory(id = 1, name = "Farm Produce", type = "PRODUCE", iconName = "Agriculture")
        val catEquip = MarketplaceCategory(id = 2, name = "Equipment", type = "EQUIP", iconName = "Handyman")
        val catServices = MarketplaceCategory(id = 3, name = "Agricultural Services", type = "SERVICES", iconName = "SupportAgent")
        val catLogistics = MarketplaceCategory(id = 4, name = "Smart Logistics", type = "LOGISTICS", iconName = "LocalShipping")
        val catLand = MarketplaceCategory(id = 5, name = "Available Farmland", type = "LAND", iconName = "Landscape")

        dao.insertCategory(catProduce)
        dao.insertCategory(catEquip)
        dao.insertCategory(catServices)
        dao.insertCategory(catLogistics)
        dao.insertCategory(catLand)

        // 2. Insert Subcategories
        val subcategories = listOf(
            // Produce (categoryId = 1)
            MarketplaceSubcategory(id = 101, categoryId = 1, name = "Cereals"),
            MarketplaceSubcategory(id = 102, categoryId = 1, name = "Tubers"),
            MarketplaceSubcategory(id = 103, categoryId = 1, name = "Legumes"),
            MarketplaceSubcategory(id = 104, categoryId = 1, name = "Vegetables"),
            MarketplaceSubcategory(id = 105, categoryId = 1, name = "Fruits"),
            MarketplaceSubcategory(id = 106, categoryId = 1, name = "Nuts"),
            MarketplaceSubcategory(id = 107, categoryId = 1, name = "Oil Crops"),
            MarketplaceSubcategory(id = 108, categoryId = 1, name = "Spices"),
            MarketplaceSubcategory(id = 109, categoryId = 1, name = "Herbs"),
            MarketplaceSubcategory(id = 110, categoryId = 1, name = "Cash Crops"),
            MarketplaceSubcategory(id = 111, categoryId = 1, name = "Plantation Crops"),
            MarketplaceSubcategory(id = 112, categoryId = 1, name = "Industrial Crops"),
            MarketplaceSubcategory(id = 113, categoryId = 1, name = "Medicinal Plants"),
            MarketplaceSubcategory(id = 114, categoryId = 1, name = "Tree Crops"),
            MarketplaceSubcategory(id = 115, categoryId = 1, name = "Fodder Crops"),
            MarketplaceSubcategory(id = 116, categoryId = 1, name = "Mushrooms"),
            MarketplaceSubcategory(id = 117, categoryId = 1, name = "Seedlings"),
            MarketplaceSubcategory(id = 118, categoryId = 1, name = "Seeds"),
            MarketplaceSubcategory(id = 119, categoryId = 1, name = "Flowers"),
            MarketplaceSubcategory(id = 120, categoryId = 1, name = "Bamboo"),
            MarketplaceSubcategory(id = 121, categoryId = 1, name = "Honey Products"),
            MarketplaceSubcategory(id = 122, categoryId = 1, name = "Aquaculture Products"),
            MarketplaceSubcategory(id = 123, categoryId = 1, name = "Livestock Products"),
            MarketplaceSubcategory(id = 124, categoryId = 1, name = "Dairy Products"),
            MarketplaceSubcategory(id = 125, categoryId = 1, name = "Poultry Products"),
            MarketplaceSubcategory(id = 126, categoryId = 1, name = "Processed Foods"),

            // Equipment (categoryId = 2)
            MarketplaceSubcategory(id = 201, categoryId = 2, name = "Tractors"),
            MarketplaceSubcategory(id = 202, categoryId = 2, name = "Power Tillers"),
            MarketplaceSubcategory(id = 203, categoryId = 2, name = "Harvesters"),
            MarketplaceSubcategory(id = 204, categoryId = 2, name = "Seed Drills"),
            MarketplaceSubcategory(id = 205, categoryId = 2, name = "Planters"),
            MarketplaceSubcategory(id = 206, categoryId = 2, name = "Boom Sprayers"),
            MarketplaceSubcategory(id = 207, categoryId = 2, name = "Knapsack Sprayers"),
            MarketplaceSubcategory(id = 208, categoryId = 2, name = "Water Pumps"),
            MarketplaceSubcategory(id = 209, categoryId = 2, name = "Generators"),
            MarketplaceSubcategory(id = 210, categoryId = 2, name = "Solar Pumps"),
            MarketplaceSubcategory(id = 211, categoryId = 2, name = "Ploughs"),
            MarketplaceSubcategory(id = 212, categoryId = 2, name = "Cultivators"),
            MarketplaceSubcategory(id = 213, categoryId = 2, name = "Disc Harrows"),
            MarketplaceSubcategory(id = 214, categoryId = 2, name = "Trailers"),
            MarketplaceSubcategory(id = 215, categoryId = 2, name = "Threshers"),
            MarketplaceSubcategory(id = 216, categoryId = 2, name = "Rice Mills"),
            MarketplaceSubcategory(id = 217, categoryId = 2, name = "Maize Shellers"),
            MarketplaceSubcategory(id = 218, categoryId = 2, name = "Dryers"),
            MarketplaceSubcategory(id = 219, categoryId = 2, name = "Cold Storage Equipment"),
            MarketplaceSubcategory(id = 220, categoryId = 2, name = "Greenhouse Equipment"),
            MarketplaceSubcategory(id = 221, categoryId = 2, name = "Protective Clothing"),
            MarketplaceSubcategory(id = 222, categoryId = 2, name = "Hand Tools"),
            MarketplaceSubcategory(id = 223, categoryId = 2, name = "Pruning Equipment"),
            MarketplaceSubcategory(id = 224, categoryId = 2, name = "Chainsaws"),
            MarketplaceSubcategory(id = 225, categoryId = 2, name = "Brush Cutters"),
            MarketplaceSubcategory(id = 226, categoryId = 2, name = "Irrigation Equipment"),
            MarketplaceSubcategory(id = 227, categoryId = 2, name = "Drip Irrigation Kits"),
            MarketplaceSubcategory(id = 228, categoryId = 2, name = "Fertilizer Spreaders"),
            MarketplaceSubcategory(id = 229, categoryId = 2, name = "Drone Equipment"),
            MarketplaceSubcategory(id = 230, categoryId = 2, name = "Soil Sensors"),
            MarketplaceSubcategory(id = 231, categoryId = 2, name = "Weather Stations"),
            MarketplaceSubcategory(id = 232, categoryId = 2, name = "Livestock Equipment"),
            MarketplaceSubcategory(id = 233, categoryId = 2, name = "Poultry Equipment"),
            MarketplaceSubcategory(id = 234, categoryId = 2, name = "Fish Farming Equipment"),

            // Services (categoryId = 3)
            MarketplaceSubcategory(id = 301, categoryId = 3, name = "Extension Officers"),
            MarketplaceSubcategory(id = 302, categoryId = 3, name = "Veterinarians"),
            MarketplaceSubcategory(id = 303, categoryId = 3, name = "Agronomists"),
            MarketplaceSubcategory(id = 304, categoryId = 3, name = "Crop Consultants"),
            MarketplaceSubcategory(id = 305, categoryId = 3, name = "Animal Health Specialists"),
            MarketplaceSubcategory(id = 306, categoryId = 3, name = "Soil Testing Experts"),
            MarketplaceSubcategory(id = 307, categoryId = 3, name = "Laborers"),
            MarketplaceSubcategory(id = 308, categoryId = 3, name = "Machine Operators"),
            MarketplaceSubcategory(id = 309, categoryId = 3, name = "Drone Pilots"),
            MarketplaceSubcategory(id = 310, categoryId = 3, name = "Mechanics"),
            MarketplaceSubcategory(id = 311, categoryId = 3, name = "Farm Managers"),
            MarketplaceSubcategory(id = 312, categoryId = 3, name = "Harvest Teams"),
            MarketplaceSubcategory(id = 313, categoryId = 3, name = "Irrigation Technicians"),
            MarketplaceSubcategory(id = 314, categoryId = 3, name = "Greenhouse Specialists"),
            MarketplaceSubcategory(id = 315, categoryId = 3, name = "Plant Pathologists"),
            MarketplaceSubcategory(id = 316, categoryId = 3, name = "Entomologists"),
            MarketplaceSubcategory(id = 317, categoryId = 3, name = "Agricultural Engineers"),
            MarketplaceSubcategory(id = 318, categoryId = 3, name = "Input Suppliers"),
            MarketplaceSubcategory(id = 319, categoryId = 3, name = "Seed Specialists"),
            MarketplaceSubcategory(id = 320, categoryId = 3, name = "Fertilizer Advisors"),
            MarketplaceSubcategory(id = 321, categoryId = 3, name = "Financial Advisors"),
            MarketplaceSubcategory(id = 322, categoryId = 3, name = "Insurance Advisors"),
            MarketplaceSubcategory(id = 323, categoryId = 3, name = "Certification Experts"),
            MarketplaceSubcategory(id = 324, categoryId = 3, name = "Training Providers"),

            // Logistics (categoryId = 4)
            MarketplaceSubcategory(id = 401, categoryId = 4, name = "Motorcycles"),
            MarketplaceSubcategory(id = 402, categoryId = 4, name = "Tricycles"),
            MarketplaceSubcategory(id = 403, categoryId = 4, name = "Pickup Trucks"),
            MarketplaceSubcategory(id = 404, categoryId = 4, name = "Mini Trucks"),
            MarketplaceSubcategory(id = 405, categoryId = 4, name = "Medium Trucks"),
            MarketplaceSubcategory(id = 406, categoryId = 4, name = "Heavy Trucks"),
            MarketplaceSubcategory(id = 407, categoryId = 4, name = "Refrigerated Trucks"),
            MarketplaceSubcategory(id = 408, categoryId = 4, name = "Tankers"),
            MarketplaceSubcategory(id = 409, categoryId = 4, name = "Cargo Vans"),
            MarketplaceSubcategory(id = 410, categoryId = 4, name = "Trailers"),
            MarketplaceSubcategory(id = 411, categoryId = 4, name = "Boats"),
            MarketplaceSubcategory(id = 412, categoryId = 4, name = "Delivery Companies"),
            MarketplaceSubcategory(id = 413, categoryId = 4, name = "Independent Drivers"),
            MarketplaceSubcategory(id = 414, categoryId = 4, name = "Warehouse Services"),
            MarketplaceSubcategory(id = 415, categoryId = 4, name = "Cold Storage"),
            MarketplaceSubcategory(id = 416, categoryId = 4, name = "Packaging Services"),
            MarketplaceSubcategory(id = 417, categoryId = 4, name = "Loading Services"),

            // Available Farmland (categoryId = 5)
            MarketplaceSubcategory(id = 501, categoryId = 5, name = "Land for Lease"),
            MarketplaceSubcategory(id = 502, categoryId = 5, name = "Land for Sale"),
            MarketplaceSubcategory(id = 503, categoryId = 5, name = "Community Land"),
            MarketplaceSubcategory(id = 504, categoryId = 5, name = "Private Land"),
            MarketplaceSubcategory(id = 505, categoryId = 5, name = "Government Land"),
            MarketplaceSubcategory(id = 506, categoryId = 5, name = "Commercial Farms"),
            MarketplaceSubcategory(id = 507, categoryId = 5, name = "Greenhouse Space"),
            MarketplaceSubcategory(id = 508, categoryId = 5, name = "Fish Pond Space"),
            MarketplaceSubcategory(id = 509, categoryId = 5, name = "Livestock Space"),
            MarketplaceSubcategory(id = 510, categoryId = 5, name = "Organic Farms")
        )
        subcategories.forEach { dao.insertSubcategory(it) }

        // 3. Dynamic Marketplace Items - At least 15 items per major category
        val itemsList = ArrayList<MarketplaceItem>()

        // --- FRUITS (Category 1, Subcategory 105) --- (15 items)
        val fruitsData = listOf(
            Triple("Organic Kent Mangoes", 15.0, "https://images.unsplash.com/photo-1553279768-865429fa0078?auto=format&fit=crop&q=80&w=400"),
            Triple("Sweet Navel Oranges", 12.0, "https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?auto=format&fit=crop&q=80&w=400"),
            Triple("Red Cavendish Bananas", 10.0, "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?auto=format&fit=crop&q=80&w=400"),
            Triple("Golden Sweet Pineapple", 18.0, "https://images.unsplash.com/photo-1550258987-190a2d41a8ba?auto=format&fit=crop&q=80&w=400"),
            Triple("Sugar King Watermelon", 25.0, "https://images.unsplash.com/photo-1587049352846-4a222e784d38?auto=format&fit=crop&q=80&w=400"),
            Triple("Rich Golden Papaya", 15.0, "https://images.unsplash.com/photo-1526318896980-cf78c088247c?auto=format&fit=crop&q=80&w=400"),
            Triple("Buttery Hass Avocado", 8.0, "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?auto=format&fit=crop&q=80&w=400"),
            Triple("Fresh Acid Lemon", 12.0, "https://images.unsplash.com/photo-1590502593747-42a996133562?auto=format&fit=crop&q=80&w=400"),
            Triple("Green Juicy Lime", 9.0, "https://images.unsplash.com/photo-1514756331096-242fdeb70d4a?auto=format&fit=crop&q=80&w=400"),
            Triple("Aromatic Passion Fruit", 30.0, "https://images.unsplash.com/photo-1534080564583-6be75777b70a?auto=format&fit=crop&q=80&w=400"),
            Triple("Sweet Royal Soursop", 40.0, "https://images.unsplash.com/photo-1595855759920-86582396756a?auto=format&fit=crop&q=80&w=400"),
            Triple("Ghana Honey Dates", 35.0, "https://images.unsplash.com/photo-1569422891963-c7fc6df64eb9?auto=format&fit=crop&q=80&w=400"),
            Triple("Bono Cashew Apple", 14.0, "https://images.unsplash.com/photo-1553279768-865429fa0078?auto=format&fit=crop&q=80&w=400"),
            Triple("Tangy Seedless Tangerine", 16.0, "https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?auto=format&fit=crop&q=80&w=400"),
            Triple("Fresh Coconut Jelly", 5.0, "https://images.unsplash.com/photo-1550258987-190a2d41a8ba?auto=format&fit=crop&q=80&w=400")
        )
        fruitsData.forEachIndexed { index, (name, price, img) ->
            itemsList.add(MarketplaceItem(
                id = 1000 + index, categoryId = 1, subcategoryId = 105, name = name, price = price, unit = "crate", quantity = 20.0 + index,
                description = "High quality freshly harvested $name from local orchards in Bono East region. Rich in vitamins, organic certification available.",
                location = "Tuobodom", region = "Bono East", distanceKm = 5.2 + index, imageUrl = img, isNegotiable = true, rating = 4.7 + (index % 3) * 0.1,
                sellerName = "Ama Serwaah", sellerPhone = "+233244445555", sellerRating = 4.9, sellerCompletedJobs = 34, isOrganic = true,
                isAvailableToday = true, isDeliveryAvailable = true
            ))
        }

        // --- VEGETABLES (Category 1, Subcategory 104) --- (15 items)
        val vegData = listOf(
            Triple("Tuobodom Slicing Tomatoes", 110.0, "https://images.unsplash.com/photo-1595855759920-86582396756a?auto=format&fit=crop&q=80&w=400"),
            Triple("Hot Kintampo Scotch Bonnet", 85.0, "https://images.unsplash.com/photo-1584269600464-37b1b58a9fe7?auto=format&fit=crop&q=80&w=400"),
            Triple("Tender Techiman Okra", 45.0, "https://images.unsplash.com/photo-1551754655-cd27e38d2076?auto=format&fit=crop&q=80&w=400"),
            Triple("Smooth White Garden Eggs", 55.0, "https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?auto=format&fit=crop&q=80&w=400"),
            Triple("Green Savoy Cabbage", 20.0, "https://images.unsplash.com/photo-1581078426770-6d336e5de7bf?auto=format&fit=crop&q=80&w=400"),
            Triple("Crisp Lettuce Bunches", 15.0, "https://images.unsplash.com/photo-1556881286-fc6915169721?auto=format&fit=crop&q=80&w=400"),
            Triple("Sweet Orange Carrots", 25.0, "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?auto=format&fit=crop&q=80&w=400"),
            Triple("Local White Onions", 60.0, "https://images.unsplash.com/photo-1508747703725-719777637510?auto=format&fit=crop&q=80&w=400"),
            Triple("Fresh Spring Onions", 10.0, "https://images.unsplash.com/photo-1556881286-fc6915169721?auto=format&fit=crop&q=80&w=400"),
            Triple("Hot Cayenne Pepper", 70.0, "https://images.unsplash.com/photo-1584269600464-37b1b58a9fe7?auto=format&fit=crop&q=80&w=400"),
            Triple("Riverbed Gboma Leaves", 18.0, "https://images.unsplash.com/photo-1551754655-cd27e38d2076?auto=format&fit=crop&q=80&w=400"),
            Triple("Aromatic Ginger Roots", 40.0, "https://images.unsplash.com/photo-1569422891963-c7fc6df64eb9?auto=format&fit=crop&q=80&w=400"),
            Triple("Organic Garlic Bulbs", 35.0, "https://images.unsplash.com/photo-1595855759920-86582396756a?auto=format&fit=crop&q=80&w=400"),
            Triple("Green Bell Peppers", 50.0, "https://images.unsplash.com/photo-1584269600464-37b1b58a9fe7?auto=format&fit=crop&q=80&w=400"),
            Triple("Sweet Round Pumpkin", 30.0, "https://images.unsplash.com/photo-1581078426770-6d336e5de7bf?auto=format&fit=crop&q=80&w=400")
        )
        vegData.forEachIndexed { index, (name, price, img) ->
            itemsList.add(MarketplaceItem(
                id = 1100 + index, categoryId = 1, subcategoryId = 104, name = name, price = price, unit = "bag", quantity = 15.0 + index,
                description = "Grown under traditional crop management at local riverbanks. Crisp, insect-free $name with brilliant shine and fresh aroma.",
                location = "Techiman", region = "Bono East", distanceKm = 3.1 + index, imageUrl = img, isNegotiable = true, rating = 4.8,
                sellerName = "Kofi Mensah", sellerPhone = "+233206667777", sellerRating = 4.7, sellerCompletedJobs = 52, isOrganic = false,
                isAvailableToday = true, isDeliveryAvailable = true
            ))
        }

        // --- CEREALS & TUBERS (Category 1, SubcategoryId 101 & 102) --- (15 items)
        val grainData = listOf(
            Triple("Techiman White Maize", 120.0, "https://images.unsplash.com/photo-1551754655-cd27e38d2076?auto=format&fit=crop&q=80&w=400"),
            Triple("Ghana Golden Paddy Rice", 140.0, "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&q=80&w=400"),
            Triple("Pearl Millet Sacks", 110.0, "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?auto=format&fit=crop&q=80&w=400"),
            Triple("Brown Sorghum Grains", 95.0, "https://images.unsplash.com/photo-1542990253-0d0f5be5f0ed?auto=format&fit=crop&q=80&w=400"),
            Triple("Techiman Cassava Tubers", 60.0, "https://images.unsplash.com/photo-1590005354167-6da97870c913?auto=format&fit=crop&q=80&w=400"),
            Triple("White Pona Yam", 150.0, "https://images.unsplash.com/photo-1596040033229-a9821ebd058d?auto=format&fit=crop&q=80&w=400"),
            Triple("Forest Cocoyam Corms", 130.0, "https://images.unsplash.com/photo-1574316071802-0d684efa7bf5?auto=format&fit=crop&q=80&w=400"),
            Triple("Sweet Potato Sacks", 80.0, "https://images.unsplash.com/photo-1596040033229-a9821ebd058d?auto=format&fit=crop&q=80&w=400"),
            Triple("Premium Wheat Grains", 180.0, "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
            Triple("Organic Fonio Grains", 220.0, "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?auto=format&fit=crop&q=80&w=400"),
            Triple("Pearly White Barley", 160.0, "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&q=80&w=400"),
            Triple("Sorghum Flour Bag", 100.0, "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&q=80&w=400"),
            Triple("Rye Seeds for Planting", 115.0, "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&q=80&w=400"),
            Triple("Local Oat Corms", 145.0, "https://images.unsplash.com/photo-1586444248902-2f64eddc13df?auto=format&fit=crop&q=80&w=400"),
            Triple("Arrowroot Tubers", 90.0, "https://images.unsplash.com/photo-1574316071802-0d684efa7bf5?auto=format&fit=crop&q=80&w=400")
        )
        grainData.forEachIndexed { index, (name, price, img) ->
            itemsList.add(MarketplaceItem(
                id = 1200 + index, categoryId = 1, subcategoryId = if (index < 4) 101 else 102, name = name, price = price, unit = "sack", quantity = 100.0 + index,
                description = "Bumper-crop quality $name dried to under 12% moisture level. Perfect for storage, direct consumption, or wholesale distribution.",
                location = "Nkoranza", region = "Bono East", distanceKm = 10.4 + index, imageUrl = img, isNegotiable = true, rating = 4.9,
                sellerName = "Yaw Boateng", sellerPhone = "+233558889999", sellerRating = 4.8, sellerCompletedJobs = 60, isOrganic = true,
                isAvailableToday = true, isDeliveryAvailable = true
            ))
        }

        // --- EQUIPMENT (Category 2) --- (15 items)
        val equipData = listOf(
            EquipDemo("Massey 375 Power Tractor", 1500.0, "Tractors", 201, "GHS/day", "https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&q=80&w=400"),
            EquipDemo("Agro-King Diesel Tiller", 250.0, "Power Tillers", 202, "GHS/day", "https://images.unsplash.com/photo-1592982537447-7440770cbfc9?auto=format&fit=crop&q=80&w=400"),
            EquipDemo("Rice Paddy Combined Harvester", 2800.0, "Harvesters", 203, "GHS/day", "https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&q=80&w=400"),
            EquipDemo("Seed Drill Attachment 4-Row", 350.0, "Seed Drills", 204, "GHS/day", "https://images.unsplash.com/photo-1592982537447-7440770cbfc9?auto=format&fit=crop&q=80&w=400"),
            EquipDemo("Mechanical Cassava Planter", 400.0, "Planters", 205, "GHS/day", "https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&q=80&w=400"),
            EquipDemo("Boom Sprayer Attachment", 180.0, "Boom Sprayers", 206, "GHS/day", "https://images.unsplash.com/photo-1592982537447-7440770cbfc9?auto=format&fit=crop&q=80&w=400"),
            EquipDemo("Solo 425 Knapsack Sprayer", 15.0, "Knapsack Sprayers", 207, "GHS/day", "https://images.unsplash.com/photo-1592982537447-7440770cbfc9?auto=format&fit=crop&q=80&w=400"),
            EquipDemo("Honda WP30 Water Pump", 60.0, "Water Pumps", 208, "GHS/day", "https://images.unsplash.com/photo-1585338107529-13afc5f02586?auto=format&fit=crop&q=80&w=400"),
            EquipDemo("10kVA Soundproof Generator", 120.0, "Generators", 209, "GHS/day", "https://images.unsplash.com/photo-1585338107529-13afc5f02586?auto=format&fit=crop&q=80&w=400"),
            EquipDemo("Solar Irrigation Pump 5HP", 200.0, "Solar Pumps", 210, "GHS/day", "https://images.unsplash.com/photo-1585338107529-13afc5f02586?auto=format&fit=crop&q=80&w=400"),
            EquipDemo("3-Disc Tractor Plough", 150.0, "Ploughs", 211, "GHS/day", "https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&q=80&w=400"),
            EquipDemo("Heavy Duty Rotary Cultivator", 180.0, "Cultivators", 212, "GHS/day", "https://images.unsplash.com/photo-1592982537447-7440770cbfc9?auto=format&fit=crop&q=80&w=400"),
            EquipDemo("Offset Disc Harrow 16-Plate", 220.0, "Disc Harrows", 213, "GHS/day", "https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&q=80&w=400"),
            EquipDemo("Hydraulic Tipping Trailer", 300.0, "Trailers", 214, "GHS/day", "https://images.unsplash.com/photo-1601584115197-04ecc0da31d7?auto=format&fit=crop&q=80&w=400"),
            EquipDemo("Multi-Grain Maize Thresher", 350.0, "Threshers", 215, "GHS/day", "https://images.unsplash.com/photo-1592982537447-7440770cbfc9?auto=format&fit=crop&q=80&w=400")
        )
        equipData.forEachIndexed { index, item ->
            itemsList.add(MarketplaceItem(
                id = 2000 + index, categoryId = 2, subcategoryId = item.subId, name = item.name, price = item.price, unit = item.unit, quantity = 1.0 + (index % 3),
                description = "Fully serviced agricultural machine. Rate includes driver/operator where applicable. Ideal for rapid farmland prep or post-harvest processing.",
                location = "Techiman", region = "Bono East", distanceKm = 4.5 + index, imageUrl = item.img, isNegotiable = false, rating = 4.8,
                sellerName = "Emmanuel Osei", sellerPhone = "+233551113333", sellerRating = 4.7, sellerCompletedJobs = 98,
                isAvailableToday = true, isDeliveryAvailable = true
            ))
        }

        // --- SERVICES (Category 3) --- (15 items)
        val servicesData = listOf(
            Triple("Senior Extension Officer", "Extension Officers", 301),
            Triple("Dr. Beatrice (Veterinary Care)", "Veterinarians", 302),
            Triple("Bono Crops Agronomist Service", "Agronomists", 303),
            Triple("Soil Fertility Assessment", "Soil Testing Experts", 306),
            Triple("Agro Drone Spraying Pilot", "Drone Pilots", 309),
            Triple("Tractor Mechanic & Repairer", "Mechanics", 310),
            Triple("Experienced Farm Manager", "Farm Managers", 311),
            Triple("Coop Rice Harvest Team", "Harvest Teams", 312),
            Triple("Drip Irrigation Specialist", "Irrigation Technicians", 313),
            Triple("Greenhouse Construction Team", "Greenhouse Specialists", 314),
            Triple("Plant Pathology Clinic", "Plant Pathologists", 315),
            Triple("Pest Control Entomologist", "Entomologists", 316),
            Triple("Techiman Farm Inputs Supplier", "Input Suppliers", 318),
            Triple("Seed Improvement Specialist", "Seed Specialists", 319),
            Triple("Modern Agriculture Trainer", "Training Providers", 324)
        )
        servicesData.forEachIndexed { index, (name, subName, subId) ->
            itemsList.add(MarketplaceItem(
                id = 3000 + index, categoryId = 3, subcategoryId = subId, name = name, price = 100.0 + (index * 20), unit = "session", quantity = 1.0,
                description = "Professional service by certified regional agent. Experts in $name. Languages: English, Twi, Fante. Highly reviewed across Bono East.",
                location = "Techiman", region = "Bono East", distanceKm = 2.0 + index, imageUrl = "https://images.unsplash.com/photo-1595855759920-86582396756a?auto=format&fit=crop&q=80&w=400",
                isNegotiable = true, rating = 4.9, sellerName = "Agronomist Ama", sellerPhone = "+233205556666", sellerRating = 4.9, sellerCompletedJobs = 145,
                sellerQualifications = "BSc Agriculture, KNUST", sellerLanguages = "English, Twi, Hausa", sellerAvailability = "9:00 AM - 5:00 PM",
                isAvailableToday = true
            ))
        }

        // --- LOGISTICS (Category 4) --- (15 items)
        val logisticsData = listOf(
            LogisticsDemo("Agro-Carrier Motorbike", "Motorcycles", 401, 3.5),
            LogisticsDemo("Bono Tricycle Carrier", "Tricycles", 402, 5.0),
            LogisticsDemo("Ford F-150 Pickup Express", "Pickup Trucks", 403, 8.0),
            LogisticsDemo("Kia Bongo Mini Truck", "Mini Trucks", 404, 10.0),
            LogisticsDemo("Hyundai Mighty Medium Truck", "Medium Trucks", 405, 12.0),
            LogisticsDemo("Isuzu Tipper Heavy Truck", "Heavy Trucks", 406, 18.0),
            LogisticsDemo("Refrigerated Veggie Carrier", "Refrigerated Trucks", 407, 22.0),
            LogisticsDemo("Potable Water Tanker 5000L", "Tankers", 408, 15.0),
            LogisticsDemo("Cargo Van Express", "Cargo Vans", 409, 9.0),
            LogisticsDemo("Flatbed Tractor Trailer", "Trailers", 410, 14.0),
            LogisticsDemo("Inland River Cargo Boat", "Boats", 411, 25.0),
            LogisticsDemo("Techiman Delivery Logistics Co", "Delivery Companies", 412, 11.0),
            LogisticsDemo("Independent Driver Yaw", "Independent Drivers", 413, 8.5),
            LogisticsDemo("Hub Warehouse Storage", "Warehouse Services", 414, 2.0),
            LogisticsDemo("Solar Cold Room Facility", "Cold Storage", 415, 4.0)
        )
        logisticsData.forEachIndexed { index, item ->
            itemsList.add(MarketplaceItem(
                id = 4000 + index, categoryId = 4, subcategoryId = item.subId, name = item.name, price = item.rate, unit = if (item.subId in listOf(414, 415)) "sqm/day" else "km", quantity = 1.0,
                description = "Reliable transit service suitable for ${item.name}. Experienced drivers with full GPS mapping. Vehicles are highly secured against bumper damage.",
                location = "Techiman", region = "Bono East", distanceKm = 1.5 + index, imageUrl = "https://images.unsplash.com/photo-1601584115197-04ecc0da31d7?auto=format&fit=crop&q=80&w=400",
                isNegotiable = false, rating = 4.8, sellerName = "Alhaji Ibrahim", sellerPhone = "+233247778888", sellerRating = 4.8, sellerCompletedJobs = 184,
                capacity = "Up to " + (1000 * (index + 1)) + " kg", pricePerKm = item.rate, estimatedArrival = (10 + index * 2).toString() + " mins",
                isAvailableToday = true
            ))
        }

        // --- FARMLAND (Category 5) --- (15 items)
        val landData = listOf(
            LandDemo("10-Acre Riverfront Lease", "Land for Lease", 501, 150.0, "GHS/acre/yr"),
            LandDemo("Tuobodom Organic Soil Sale", "Land for Sale", 502, 12000.0, "GHS/acre"),
            LandDemo("Community Cooperative Field", "Community Land", 503, 100.0, "GHS/acre/yr"),
            LandDemo("Private Fenced Orchard Land", "Private Land", 504, 300.0, "GHS/acre/yr"),
            LandDemo("Govt Irrigation Project Plot", "Government Land", 505, 50.0, "GHS/acre/yr"),
            LandDemo("50-Hectare Commercial Rice Flat", "Commercial Farms", 506, 400.0, "GHS/acre/yr"),
            LandDemo("Solar Greenhouse Multi-Bay", "Greenhouse Space", 507, 1200.0, "GHS/bay/yr"),
            LandDemo("Freshwater Tilapia Pond Space", "Fish Pond Space", 508, 1500.0, "GHS/pond/yr"),
            LandDemo("Fenced Poultry Pen Structure", "Livestock Space", 509, 800.0, "GHS/pen/yr"),
            LandDemo("Organic Certified Farm Bed", "Organic Farms", 510, 500.0, "GHS/acre/yr"),
            LandDemo("5-Acre Flat Fertile Ground", "Land for Lease", 501, 160.0, "GHS/acre/yr"),
            LandDemo("Tanoso Riverside Cultivation", "Land for Lease", 501, 180.0, "GHS/acre/yr"),
            LandDemo("10 Hectares Near Market Hub", "Land for Lease", 501, 200.0, "GHS/acre/yr"),
            LandDemo("Irrigated Pepper Bed", "Land for Lease", 501, 220.0, "GHS/acre/yr"),
            LandDemo("Prime Maize Cultivation Field", "Land for Lease", 501, 140.0, "GHS/acre/yr")
        )
        landData.forEachIndexed { index, item ->
            itemsList.add(MarketplaceItem(
                id = 5000 + index, categoryId = 5, subcategoryId = item.subId, name = item.name, price = item.price, unit = item.unit, quantity = 5.0 + index,
                description = "Very rich and dark organic loamy soil. Flat terrain with high water retention capacity. Direct gravel road access.",
                location = "Tanoso", region = "Bono East", distanceKm = 8.5 + index, imageUrl = "https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&q=80&w=400",
                isNegotiable = true, rating = 4.9, sellerName = "Nana Opoku", sellerPhone = "+233240001112", sellerRating = 4.9, sellerCompletedJobs = 12,
                soilType = "Sandy Loam / Clay Loam", waterAvailability = "Year-Round River Streams", irrigation = "Drip Kits & Sprinklers Installed",
                accessibility = "Excellent - 5 mins from highway",
                isAvailableToday = true
            ))
        }

        // Insert all marketplace items
        itemsList.forEach { dao.insertMarketplaceItem(it) }

        // Prepopulate Engagement Activities
        val initialActivities = listOf(
            EngagementActivity(profileName = "Ama Serwaah", role = "Farmer", actionText = "Listed 30 crates of Tuobodom Tomatoes", timestampString = "2 hours ago"),
            EngagementActivity(profileName = "Ama Serwaah", role = "Farmer", actionText = "Sold 10 crates of Tomatoes to Beatrice Ansah", timestampString = "Yesterday"),
            EngagementActivity(profileName = "Ama Serwaah", role = "Farmer", actionText = "Rated 5 stars by Beatrice Ansah", timestampString = "2 days ago"),
            EngagementActivity(profileName = "Ama Serwaah", role = "Farmer", actionText = "Joined the GeoHarvest digital registry", timestampString = "Jan 2025"),
            
            EngagementActivity(profileName = "Kofi Mensah", role = "Farmer", actionText = "Listed 15 bags of Tender Techiman Okra", timestampString = "3 hours ago"),
            EngagementActivity(profileName = "Kofi Mensah", role = "Farmer", actionText = "Initiated transit request with Emmanuel Osei", timestampString = "1 hour ago"),
            EngagementActivity(profileName = "Kofi Mensah", role = "Farmer", actionText = "Joined the GeoHarvest digital registry", timestampString = "Jan 2025"),
            
            EngagementActivity(profileName = "Yaw Boateng", role = "Farmer", actionText = "Listed 25 sacks of Smooth White Garden Eggs", timestampString = "1 day ago"),
            EngagementActivity(profileName = "Yaw Boateng", role = "Farmer", actionText = "Completed supply order of 15 sacks", timestampString = "3 days ago"),
            EngagementActivity(profileName = "Yaw Boateng", role = "Farmer", actionText = "Joined the GeoHarvest digital registry", timestampString = "Feb 2025"),
            
            EngagementActivity(profileName = "Abena Mansa", role = "Farmer", actionText = "Listed 10 sacks of Hot Kintampo Scotch Bonnet", timestampString = "4 hours ago"),
            EngagementActivity(profileName = "Abena Mansa", role = "Farmer", actionText = "Supplied 5 sacks of Hot Peppers to Mary Appiah", timestampString = "Yesterday"),
            
            EngagementActivity(profileName = "Kwame Kyeremeh", role = "Farmer", actionText = "Listed 40 bundles of Fresh Tanoso Gboma Leaves", timestampString = "5 hours ago"),
            EngagementActivity(profileName = "Kwame Kyeremeh", role = "Farmer", actionText = "Completed delivery to Techiman Wholesale Hub", timestampString = "Yesterday"),
            
            EngagementActivity(profileName = "Beatrice Ansah", role = "Buyer", actionText = "Bought 10 crates of Tomatoes from Ama Serwaah", timestampString = "Yesterday"),
            EngagementActivity(profileName = "Beatrice Ansah", role = "Buyer", actionText = "Posted purchase demand for 50 crates of peppers", timestampString = "1 day ago"),
            EngagementActivity(profileName = "Beatrice Ansah", role = "Buyer", actionText = "Rated Alhaji Ibrahim 5 stars for Tuobodom transit", timestampString = "Yesterday"),
            
            EngagementActivity(profileName = "Kwaku Addo", role = "Buyer", actionText = "Negotiating purchase of 5 sacks of Okra with Kofi Mensah", timestampString = "1 hour ago"),
            EngagementActivity(profileName = "Kwaku Addo", role = "Buyer", actionText = "Received 10 sacks of Garden Eggs from Yaw Boateng", timestampString = "2 days ago"),
            
            EngagementActivity(profileName = "Mary Appiah", role = "Buyer", actionText = "Purchased 5 sacks of Hot Peppers from Abena Mansa", timestampString = "Yesterday"),
            EngagementActivity(profileName = "Mary Appiah", role = "Buyer", actionText = "Transferred payment directly via G-Money", timestampString = "Yesterday"),
            
            EngagementActivity(profileName = "Alhaji Ibrahim", role = "Transporter", actionText = "Delivered 10 crates of Tomatoes from Tuobodom to Accra", timestampString = "Yesterday"),
            EngagementActivity(profileName = "Alhaji Ibrahim", role = "Transporter", actionText = "Received a 5-star rating from Ama Serwaah", timestampString = "Yesterday"),
            EngagementActivity(profileName = "Alhaji Ibrahim", role = "Transporter", actionText = "Completed 184 deliveries in Bono East Region", timestampString = "Jan-Jun 2026"),
            
            EngagementActivity(profileName = "Kwabena Mensah", role = "Transporter", actionText = "Completed bulk transport of 50 sacks of grains to Kumasi", timestampString = "Yesterday"),
            EngagementActivity(profileName = "Kwabena Mensah", role = "Transporter", actionText = "Received a 5-star rating from Beatrice Ansah", timestampString = "2 days ago"),
            
            EngagementActivity(profileName = "Emmanuel Osei", role = "Transporter", actionText = "Assigned to transport 5 bags of Okra for Kofi Mensah", timestampString = "1 hour ago"),
            EngagementActivity(profileName = "Emmanuel Osei", role = "Transporter", actionText = "Delivered 15 sacks of garden eggs to Techiman Hub", timestampString = "2 days ago")
        )
        initialActivities.forEach { dao.insertEngagementActivity(it) }

        // 1. Insert Market Prices (Bono East region)
        val prices = listOf(
            MarketPrice(vegetableType = "Tomatoes", avgPrice = 120.0, highestPrice = 155.0, lowestPrice = 90.0, recommendedPrice = 115.0, priceTrend = "UP", date = "Today"),
            MarketPrice(vegetableType = "Okra", avgPrice = 45.0, highestPrice = 58.0, lowestPrice = 32.0, recommendedPrice = 42.0, priceTrend = "DOWN", date = "Today"),
            MarketPrice(vegetableType = "Garden Eggs", avgPrice = 65.0, highestPrice = 78.0, lowestPrice = 50.0, recommendedPrice = 60.0, priceTrend = "STABLE", date = "Today"),
            MarketPrice(vegetableType = "Peppers", avgPrice = 85.0, highestPrice = 105.0, lowestPrice = 72.0, recommendedPrice = 82.0, priceTrend = "UP", date = "Today"),
            MarketPrice(vegetableType = "Leafy Vegetables", avgPrice = 22.0, highestPrice = 28.0, lowestPrice = 15.0, recommendedPrice = 18.0, priceTrend = "STABLE", date = "Today")
        )
        prices.forEach { dao.insertMarketPrice(it) }

        // 2. Insert Buyers
        val buyers = listOf(
            Buyer(name = "Beatrice Ansah", location = "Accra Central Market", phone = "+233 24 111 2222", rating = 4.8, completedOrders = 210, demandVegetables = "Tomatoes, Peppers", trustScore = 97),
            Buyer(name = "Kwaku Addo", location = "Kumasi Kejetia Market", phone = "+233 55 333 4444", rating = 4.6, completedOrders = 145, demandVegetables = "Okra, Garden Eggs", trustScore = 91),
            Buyer(name = "Mary Appiah", location = "Techiman Wholesale Hub", phone = "+233 20 555 6666", rating = 4.9, completedOrders = 320, demandVegetables = "Tomatoes, Leafy Vegetables", trustScore = 99)
        )
        buyers.forEach { dao.insertBuyer(it) }

        // 3. Insert Transporters
        val transporters = listOf(
            Transporter(name = "Alhaji Ibrahim", phone = "+233 24 777 8888", vehicle = "Ford Transit (Yellow/Blue)", rating = 4.8, trustScore = 95, ratePerKm = 8.0, baseRate = 150.0, etaMinutes = 12, completedDeliveries = 184, cancellationRate = 1),
            Transporter(name = "Kwabena Mensah", phone = "+233 50 999 0000", vehicle = "Hyundai Mighty Flatbed", rating = 4.9, trustScore = 98, ratePerKm = 12.0, baseRate = 220.0, etaMinutes = 20, completedDeliveries = 242, cancellationRate = 0),
            Transporter(name = "Emmanuel Osei", phone = "+233 55 111 3333", vehicle = "Isuzu Elf Tipper", rating = 4.7, trustScore = 92, ratePerKm = 10.0, baseRate = 180.0, etaMinutes = 15, completedDeliveries = 98, cancellationRate = 3)
        )
        transporters.forEach { dao.insertTransporter(it) }

        // Check if produce listings already populated, if empty insert demo
        val currentListings = dao.getAllProduceListings().firstOrNull() ?: emptyList()
        if (currentListings.isEmpty()) {
            val listings = listOf(
                ProduceListing(
                    title = "Plump Tuobodom Tomatoes",
                    vegetableType = "Tomatoes",
                    farmerName = "Ama Serwaah",
                    farmerPhone = "+233 24 444 5555",
                    quantity = 30.0, // 30 crates
                    pricePerUnit = 110.0,
                    location = "Tuobodom Farms",
                    region = "Bono East",
                    trustScore = 98,
                    collectionPoint = "Tuobodom Lorry Station",
                    description = "Freshly harvested red slicing tomatoes. Very firm and well-packed. No chemical bruises.",
                    isDemo = true
                ),
                ProduceListing(
                    title = "Tender Techiman Okra",
                    vegetableType = "Okra",
                    farmerName = "Kofi Mensah",
                    farmerPhone = "+233 20 666 7777",
                    quantity = 15.0, // 15 bags
                    pricePerUnit = 40.0,
                    location = "Techiman Outskirts",
                    region = "Bono East",
                    trustScore = 96,
                    collectionPoint = "Techiman Market Gate 4",
                    description = "Crispy green tender okra pods. Ideal for traditional soups. Harvested fresh every 2 days.",
                    isDemo = true
                ),
                ProduceListing(
                    title = "Smooth White Garden Eggs",
                    vegetableType = "Garden Eggs",
                    farmerName = "Yaw Boateng",
                    farmerPhone = "+233 55 888 9999",
                    quantity = 25.0, // 25 sacks
                    pricePerUnit = 55.0,
                    location = "Tanoso Agricultural Hub",
                    region = "Bono East",
                    trustScore = 94,
                    collectionPoint = "Tanoso Cooperative Shed",
                    description = "Grade-A round white garden eggs (eggplants). Excellent shelf life.",
                    isDemo = true
                ),
                ProduceListing(
                    title = "Hot Kintampo Scotch Bonnet",
                    vegetableType = "Peppers",
                    farmerName = "Abena Mansa",
                    farmerPhone = "+233 24 222 3333",
                    quantity = 10.0, // 10 sacks
                    pricePerUnit = 85.0,
                    location = "Kintampo North",
                    region = "Bono East",
                    trustScore = 93,
                    collectionPoint = "Kintampo Waterfall Rd Junction",
                    description = "Extremely hot and aromatic red and yellow scotch bonnet peppers.",
                    isDemo = true
                ),
                ProduceListing(
                    title = "Fresh Tanoso Gboma Leaves",
                    vegetableType = "Leafy Vegetables",
                    farmerName = "Kwame Kyeremeh",
                    farmerPhone = "+233 50 123 4567",
                    quantity = 40.0, // 40 bundles
                    pricePerUnit = 18.0,
                    location = "Tanoso Riverside",
                    region = "Bono East",
                    trustScore = 95,
                    collectionPoint = "Tanoso School Junction",
                    description = "Freshly cut organic Gboma (African eggplant) leaves. Tender and highly nutritious.",
                    isDemo = true
                )
            )
            listings.forEach { dao.insertProduceListing(it) }
        }

        // Add some completed/in-transit orders
        val currentOrders = dao.getAllOrders().firstOrNull() ?: emptyList()
        if (currentOrders.isEmpty()) {
            val orders = listOf(
                Order(
                    produceListingId = 1,
                    vegetableType = "Tomatoes",
                    quantity = 10.0,
                    totalPrice = 1100.0,
                    farmerName = "Ama Serwaah",
                    farmerPhone = "+233 24 444 5555",
                    buyerName = "Beatrice Ansah",
                    buyerPhone = "+233 24 111 2222",
                    status = "Delivered",
                    trackingProgress = 8,
                    transporterName = "Alhaji Ibrahim",
                    transporterPhone = "+233 24 777 8888",
                    transporterVehicle = "Ford Transit (Yellow/Blue)",
                    deliveryCost = 230.0,
                    deliveryTimeMinutes = 45
                ),
                Order(
                    produceListingId = 2,
                    vegetableType = "Okra",
                    quantity = 5.0,
                    totalPrice = 200.0,
                    farmerName = "Kofi Mensah",
                    farmerPhone = "+233 20 666 7777",
                    buyerName = "Kwaku Addo",
                    buyerPhone = "+233 55 333 4444",
                    status = "In Transit",
                    trackingProgress = 5,
                    transporterName = "Emmanuel Osei",
                    transporterPhone = "+233 55 111 3333",
                    transporterVehicle = "Isuzu Elf Tipper",
                    deliveryCost = 150.0,
                    deliveryTimeMinutes = 30
                )
            )
            orders.forEach { dao.insertOrder(it) }
        }

        // Insert first greeting message if empty
        val currentChats = dao.getAllChatMessages().firstOrNull() ?: emptyList()
        if (currentChats.isEmpty()) {
            dao.insertChatMessage(
                ChatMessage(
                    text = "Akwaaba! I am GeoHarvest AI Assistant. I can help you find veggie buyers, calculate transport costs, recommend pricing, and show you how to upload produce in Bono East! Ask me anything.",
                    sender = "AI"
                )
            )
        }

        // Prepopulate feedback
        val currentFeedback = dao.getAllFeedback().firstOrNull() ?: emptyList()
        if (currentFeedback.isEmpty()) {
            val initialFeedback = listOf(
                TransactionFeedback(
                    targetName = "Ama Serwaah",
                    senderName = "Beatrice Ansah",
                    serviceType = "Marketplace",
                    rating = 5,
                    feedbackText = "Exceptional tomatoes! Extremely fresh, perfectly sorted, and ready exactly at the collection point on time."
                ),
                TransactionFeedback(
                    targetName = "Ama Serwaah",
                    senderName = "Kwaku Addo",
                    serviceType = "Marketplace",
                    rating = 5,
                    feedbackText = "High quality Tuobodom tomatoes. Very honest farmer who helped load the crates."
                ),
                TransactionFeedback(
                    targetName = "Alhaji Ibrahim",
                    senderName = "Beatrice Ansah",
                    serviceType = "Logistics",
                    rating = 5,
                    feedbackText = "Fast transport to Accra. The crates were secured perfectly and arrived with zero damage."
                ),
                TransactionFeedback(
                    targetName = "Alhaji Ibrahim",
                    senderName = "Ama Serwaah",
                    serviceType = "Logistics",
                    rating = 4,
                    feedbackText = "Very reliable driver with clean vehicle. A bit delayed by road check, but great communication."
                ),
                TransactionFeedback(
                    targetName = "Emmanuel Osei",
                    senderName = "Kofi Mensah",
                    serviceType = "Logistics",
                    rating = 5,
                    feedbackText = "Excellent transport service. Quick pickup from Techiman and direct delivery."
                ),
                TransactionFeedback(
                    targetName = "Kofi Mensah",
                    senderName = "Alhaji Ibrahim",
                    serviceType = "Marketplace",
                    rating = 5,
                    feedbackText = "Superb quality Okra. Well packaged in bags, very friendly and helpful farmer."
                ),
                TransactionFeedback(
                    targetName = "Beatrice Ansah",
                    senderName = "Ama Serwaah",
                    serviceType = "Marketplace",
                    rating = 5,
                    feedbackText = "Very prompt payment and courteous communication. A pleasure to do business with!"
                )
            )
            initialFeedback.forEach { dao.insertFeedback(it) }
        }
    }
}
