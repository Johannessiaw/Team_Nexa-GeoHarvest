package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "produce_listings")
data class ProduceListing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val vegetableType: String, // Tomatoes, Okra, Garden Eggs, Peppers, Leafy Vegetables
    val farmerName: String,
    val farmerPhone: String,
    val quantity: Double, // in kg or crates/bags
    val pricePerUnit: Double, // in GHS
    val location: String, // Techiman, Tuobodom, etc.
    val region: String = "Bono East",
    val trustScore: Int = 90,
    val collectionPoint: String,
    val description: String,
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isDemo: Boolean = false,
    val isSynced: Boolean = true,
    val status: String = "AVAILABLE", // AVAILABLE, PENDING, SOLD
    val farmerRating: Double = 4.8,
    val farmerCompletedCount: Int = 18
)

@Entity(tableName = "buyers")
data class Buyer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val location: String,
    val phone: String,
    val rating: Double,
    val completedOrders: Int,
    val demandVegetables: String, // Comma-separated list of vegetables in high demand
    val trustScore: Int = 90,
    val memberSince: String = "Jan 2025"
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val produceListingId: Int,
    val vegetableType: String,
    val quantity: Double,
    val totalPrice: Double,
    val farmerName: String,
    val farmerPhone: String,
    val buyerName: String,
    val buyerPhone: String,
    val status: String, // Order Confirmed, Waiting for Driver, Driver Assigned, Driver Arriving, Produce Picked Up, In Transit, Approaching Destination, Delivered, Payment Completed
    val trackingProgress: Int = 0, // 0 to 8 matching the timeline stages
    val transporterName: String = "",
    val transporterPhone: String = "",
    val transporterVehicle: String = "",
    val deliveryCost: Double = 0.0,
    val deliveryTimeMinutes: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true
)

@Entity(tableName = "transporters")
data class Transporter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val vehicle: String,
    val rating: Double,
    val trustScore: Int = 90,
    val ratePerKm: Double,
    val baseRate: Double,
    val etaMinutes: Int,
    val completedDeliveries: Int = 15,
    val cancellationRate: Int = 2,
    val status: String = "Available" // Available, Busy
)

@Entity(tableName = "market_prices")
data class MarketPrice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vegetableType: String,
    val avgPrice: Double,
    val highestPrice: Double,
    val lowestPrice: Double,
    val recommendedPrice: Double,
    val priceTrend: String, // UP, DOWN, STABLE
    val region: String = "Bono East",
    val date: String
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val sender: String, // USER or AI
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "engagement_activities")
data class EngagementActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileName: String, // e.g. "Ama Serwaah", "Beatrice Ansah", "Kofi Mensah"
    val role: String, // e.g. "Farmer", "Buyer", "Transporter"
    val actionText: String, // e.g. "Listed 30 crates of Tuobodom Tomatoes"
    val timestampString: String, // e.g. "2 hours ago", "Yesterday"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_credentials")
data class UserCredential(
    @PrimaryKey val id: String, // e.g. email, phone, or "Guest"
    val name: String,
    val authType: String, // "MobileGhanaCard", "Google", "Guest"
    val phoneNo: String? = null,
    val cardNo: String? = null,
    val email: String? = null,
    val selfiePhoto: String? = null,
    val loginTime: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
)

@Entity(tableName = "transaction_feedback")
data class TransactionFeedback(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val targetName: String, // Name of the person being rated
    val senderName: String, // Name of the person who gave the feedback
    val serviceType: String, // "Marketplace" or "Logistics"
    val rating: Int, // 1 to 5 stars
    val feedbackText: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false
)

@Entity(tableName = "marketplace_categories")
data class MarketplaceCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // e.g., "Farm Produce", "Equipment", "Agricultural Services", "Logistics", "Available Farmland"
    val type: String, // e.g., "PRODUCE", "EQUIP", "SERVICES", "LOGISTICS", "LAND"
    val iconName: String // e.g., "Agriculture", "Handyman", "SupportAgent", "LocalShipping", "Landscape"
)

@Entity(tableName = "marketplace_subcategories")
data class MarketplaceSubcategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val name: String, // e.g., "Cereals", "Tubers", "Legumes", "Vegetables", "Fruits" etc.
    val description: String = ""
)

@Entity(tableName = "marketplace_items")
data class MarketplaceItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val subcategoryId: Int,
    val name: String,
    val price: Double,
    val unit: String = "kg", // "kg", "bag", "crate", "tonne", "litre", "piece", "acre", "hr", "km", etc.
    val quantity: Double = 1.0,
    val description: String = "",
    val location: String = "Techiman",
    val region: String = "Bono East",
    val distanceKm: Double = 12.5,
    val imageUrl: String = "", // Can store multiple comma-separated URLs
    val isNegotiable: Boolean = true,
    val rating: Double = 4.8,
    val isAvailableToday: Boolean = true,
    val isDeliveryAvailable: Boolean = true,
    val isOrganic: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Seller info / Profile / Owner
    val sellerName: String = "",
    val sellerPhone: String = "",
    val sellerRating: Double = 4.8,
    val sellerCompletedJobs: Int = 12,
    val sellerQualifications: String = "",
    val sellerLanguages: String = "",
    val sellerAvailability: String = "Available Today",
    
    // Farmland specific
    val soilType: String = "",
    val waterAvailability: String = "",
    val irrigation: String = "",
    val accessibility: String = "",
    
    // Logistics specific
    val capacity: String = "",
    val pricePerKm: Double = 0.0,
    val estimatedArrival: String = ""
)


