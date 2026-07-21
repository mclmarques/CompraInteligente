package com.mcldev.comprainteligente.data.model

import java.util.UUID

data class ScannedProduct(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var price: Float
)
