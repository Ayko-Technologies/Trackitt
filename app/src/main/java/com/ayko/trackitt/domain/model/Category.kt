package com.ayko.trackitt.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ayko.trackitt.ui.theme.*

enum class TransactionCategory(
    val label: String,
    val icon: ImageVector,
    val bgColor: Color,
    val fgColor: Color,
    val hue: Float,
) {
    FOOD("Food", Icons.Filled.Restaurant, CatFoodBg, CatFoodFg, 28f),
    GROCERIES("Groceries", Icons.Filled.LocalGroceryStore, CatGroceriesBg, CatGroceriesFg, 148f),
    TRANSPORT("Transport", Icons.Filled.DirectionsBus, CatTransportBg, CatTransportFg, 220f),
    BILLS("Bills", Icons.AutoMirrored.Filled.ReceiptLong, CatBillsBg, CatBillsFg, 280f),
    SHOPPING("Shopping", Icons.Filled.ShoppingBag, CatShoppingBg, CatShoppingFg, 340f),
    TRANSFER("Transfer", Icons.Filled.SwapHoriz, CatTransferBg, CatTransferFg, 40f),
    HOUSE("House", Icons.Filled.Home, CatHouseBg, CatHouseFg, 30f),
    BEAUTY("Beauty", Icons.Filled.Spa, CatBeautyBg, CatBeautyFg, 320f),
    CHURCH("Church", Icons.Filled.Church, CatChurchBg, CatChurchFg, 270f),
    HEALTH("Health", Icons.Filled.LocalHospital, CatHealthBg, CatHealthFg, 0f),
    AIRTIME("Airtime", Icons.Filled.PhoneAndroid, CatAirtimeBg, CatAirtimeFg, 200f),
    INCOME("Income", Icons.Filled.ArrowDownward, CatIncomeBg, CatIncomeFg, 148f),
    OTHER("Other", Icons.Filled.Category, CatOtherBg, CatOtherFg, 100f),
}
