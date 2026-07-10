package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GeoHarvestDao {
    // Produce Listings
    @Query("SELECT * FROM produce_listings ORDER BY timestamp DESC")
    fun getAllProduceListings(): Flow<List<ProduceListing>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduceListing(listing: ProduceListing)

    @Update
    suspend fun updateProduceListing(listing: ProduceListing)

    @Delete
    suspend fun deleteProduceListing(listing: ProduceListing)

    @Query("SELECT * FROM produce_listings WHERE isSynced = 0")
    suspend fun getUnsyncedProduceListings(): List<ProduceListing>

    // Buyers
    @Query("SELECT * FROM buyers")
    fun getAllBuyers(): Flow<List<Buyer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuyer(buyer: Buyer)

    @Query("DELETE FROM buyers")
    suspend fun clearBuyers()

    // Orders
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getOrderById(id: Int): Flow<Order?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    @Query("SELECT * FROM orders WHERE isSynced = 0")
    suspend fun getUnsyncedOrders(): List<Order>

    // Transporters
    @Query("SELECT * FROM transporters")
    fun getAllTransporters(): Flow<List<Transporter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransporter(transporter: Transporter)

    @Query("DELETE FROM transporters")
    suspend fun clearTransporters()

    // Market Prices
    @Query("SELECT * FROM market_prices")
    fun getAllMarketPrices(): Flow<List<MarketPrice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketPrice(price: MarketPrice)

    @Query("DELETE FROM market_prices")
    suspend fun clearMarketPrices()

    // Chat Messages
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatMessages()

    // Engagement Activities
    @Query("SELECT * FROM engagement_activities ORDER BY timestamp DESC")
    fun getAllEngagementActivities(): Flow<List<EngagementActivity>>

    @Query("SELECT * FROM engagement_activities WHERE profileName = :profileName ORDER BY timestamp DESC")
    fun getEngagementActivitiesByProfile(profileName: String): Flow<List<EngagementActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEngagementActivity(activity: EngagementActivity)

    @Query("DELETE FROM engagement_activities")
    suspend fun clearEngagementActivities()

    // Updates for Buyer and Transporter
    @Update
    suspend fun updateBuyer(buyer: Buyer)

    @Update
    suspend fun updateTransporter(transporter: Transporter)

    // User Sessions
    @Query("SELECT * FROM user_credentials WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveUserCredential(): UserCredential?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserCredential(credential: UserCredential)

    @Query("UPDATE user_credentials SET isActive = 0")
    suspend fun deactivateAllUserCredentials()

    @Query("SELECT * FROM user_credentials ORDER BY loginTime DESC")
    fun getAllUserCredentials(): Flow<List<UserCredential>>

    // Transaction Feedback
    @Query("SELECT * FROM transaction_feedback ORDER BY timestamp DESC")
    fun getAllFeedback(): Flow<List<TransactionFeedback>>

    @Query("SELECT * FROM transaction_feedback WHERE targetName = :targetName ORDER BY timestamp DESC")
    fun getFeedbackByTarget(targetName: String): Flow<List<TransactionFeedback>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: TransactionFeedback)

    // Dynamic Marketplace Categories
    @Query("SELECT * FROM marketplace_categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<MarketplaceCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: MarketplaceCategory)

    @Update
    suspend fun updateCategory(category: MarketplaceCategory)

    @Delete
    suspend fun deleteCategory(category: MarketplaceCategory)

    @Query("DELETE FROM marketplace_categories")
    suspend fun clearCategories()

    // Dynamic Marketplace Subcategories
    @Query("SELECT * FROM marketplace_subcategories ORDER BY name ASC")
    fun getAllSubcategories(): Flow<List<MarketplaceSubcategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategory(subcategory: MarketplaceSubcategory)

    @Update
    suspend fun updateSubcategory(subcategory: MarketplaceSubcategory)

    @Delete
    suspend fun deleteSubcategory(subcategory: MarketplaceSubcategory)

    @Query("DELETE FROM marketplace_subcategories")
    suspend fun clearSubcategories()

    // Dynamic Marketplace Items
    @Query("SELECT * FROM marketplace_items ORDER BY timestamp DESC")
    fun getAllMarketplaceItems(): Flow<List<MarketplaceItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketplaceItem(item: MarketplaceItem)

    @Update
    suspend fun updateMarketplaceItem(item: MarketplaceItem)

    @Delete
    suspend fun deleteMarketplaceItem(item: MarketplaceItem)

    @Query("DELETE FROM marketplace_items")
    suspend fun clearMarketplaceItems()
}
