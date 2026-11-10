package com.ayko.trackitt.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
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
    POCKET_MONEY("Pocket Money", Icons.Filled.AttachMoney, CatPocketMoneyBg, CatPocketMoneyFg, 42f),
    SNACKS("Snacks", Icons.Filled.Fastfood, CatSnacksBg, CatSnacksFg, 52f),
    EDUCATION("Education", Icons.Filled.School, CatEducationBg, CatEducationFg, 212f),
    GIFTS("Gifts", Icons.Filled.CardGiftcard, CatGiftsBg, CatGiftsFg, 308f),
    ELECTRONICS("Electronics", Icons.Filled.Devices, CatElectronicsBg, CatElectronicsFg, 206f),
    HOME_PROJECT("Home Project", Icons.Filled.Build, CatHomeProjectBg, CatHomeProjectFg, 122f),
    BUSINESS("Business", Icons.Filled.BusinessCenter, CatBusinessBg, CatBusinessFg, 180f),
    INCOME("Income", Icons.Filled.ArrowDownward, CatIncomeBg, CatIncomeFg, 148f),
    OTHER("Other", Icons.Filled.Category, CatOtherBg, CatOtherFg, 100f),

    /** Not given to transactions: with "Show fees" on, every fee paid is totalled here instead. */
    FEES("Fees", Icons.Filled.Percent, CatFeesBg, CatFeesFg, 60f);

    companion object {
        /** The categories a transaction can be put in. */
        val assignable: List<TransactionCategory> = entries.filter { it != FEES }
    }
}
