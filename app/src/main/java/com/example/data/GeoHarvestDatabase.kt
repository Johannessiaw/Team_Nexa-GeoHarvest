package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProduceListing::class,
        Buyer::class,
        Order::class,
        Transporter::class,
        MarketPrice::class,
        ChatMessage::class,
        EngagementActivity::class,
        UserCredential::class,
        TransactionFeedback::class,
        MarketplaceCategory::class,
        MarketplaceSubcategory::class,
        MarketplaceItem::class
    ],
    version = 5,
    exportSchema = false
)
abstract class GeoHarvestDatabase : RoomDatabase() {
    abstract fun geoHarvestDao(): GeoHarvestDao

    companion object {
        @Volatile
        private var INSTANCE: GeoHarvestDatabase? = null

        fun getDatabase(context: Context): GeoHarvestDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GeoHarvestDatabase::class.java,
                    "geoharvest_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
