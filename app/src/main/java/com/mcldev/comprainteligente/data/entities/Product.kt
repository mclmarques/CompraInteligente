package com.mcldev.comprainteligente.data.entities
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product",
    foreignKeys = [
        ForeignKey(
            entity = Supermarket::class,
            parentColumns = ["id"],
            childColumns = ["supermarketId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("supermarketId")]  //Adding index to imrpove performance and fix room warning
)
data class Product (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val price: Float,
    val unit: String? = null,
    val supermarketId: Int,
    val date: Long
)
/*
TODO:
-> Add unit of the product for more accurate info.
This is likely to come con V2 with the introduction of AI assistance
 */
