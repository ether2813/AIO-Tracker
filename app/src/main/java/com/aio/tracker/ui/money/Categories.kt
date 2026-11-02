package com.aio.tracker.ui.money

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalMall
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.aio.tracker.data.db.TxnType

data class MoneyCategory(val name: String, val icon: ImageVector)

object Categories {
    val expense = listOf(
        MoneyCategory("Food", Icons.Rounded.Restaurant),
        MoneyCategory("Groceries", Icons.Rounded.ShoppingCart),
        MoneyCategory("Transport", Icons.Rounded.DirectionsCar),
        MoneyCategory("Fuel", Icons.Rounded.LocalGasStation),
        MoneyCategory("Shopping", Icons.Rounded.LocalMall),
        MoneyCategory("Bills", Icons.Rounded.Receipt),
        MoneyCategory("Rent", Icons.Rounded.Home),
        MoneyCategory("Health", Icons.Rounded.LocalHospital),
        MoneyCategory("Education", Icons.Rounded.School),
        MoneyCategory("Fun", Icons.Rounded.Movie),
        MoneyCategory("Travel", Icons.Rounded.Flight),
        MoneyCategory("Charity", Icons.Rounded.Favorite),
        MoneyCategory("Family", Icons.Rounded.People),
        MoneyCategory("Phone", Icons.Rounded.PhoneAndroid),
        MoneyCategory("Other", Icons.Rounded.Category),
    )
    val income = listOf(
        MoneyCategory("Salary", Icons.Rounded.Work),
        MoneyCategory("Business", Icons.Rounded.Store),
        MoneyCategory("Freelance", Icons.Rounded.Laptop),
        MoneyCategory("Gift", Icons.Rounded.CardGiftcard),
        MoneyCategory("Investment", Icons.Rounded.ShowChart),
        MoneyCategory("Refund", Icons.Rounded.Replay),
        MoneyCategory("Other", Icons.Rounded.Category),
    )

    fun forType(type: TxnType) = if (type == TxnType.EXPENSE) expense else income

    fun icon(name: String): ImageVector =
        (expense + income).firstOrNull { it.name.equals(name, ignoreCase = true) }?.icon ?: Icons.Rounded.Category
}
