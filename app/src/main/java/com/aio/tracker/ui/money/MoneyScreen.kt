package com.aio.tracker.ui.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aio.tracker.data.db.Loan
import com.aio.tracker.data.db.LoanDirection
import com.aio.tracker.data.db.MoneyTxn
import com.aio.tracker.data.db.TxnType
import com.aio.tracker.ui.appViewModel
import com.aio.tracker.ui.components.AppCard
import com.aio.tracker.ui.components.BarChart
import com.aio.tracker.ui.components.ConfirmDialog
import com.aio.tracker.ui.components.DateField
import com.aio.tracker.ui.components.DonutChart
import com.aio.tracker.ui.components.EmptyState
import com.aio.tracker.ui.components.HeroCard
import com.aio.tracker.ui.components.IconBadge
import com.aio.tracker.ui.components.LegendRow
import com.aio.tracker.ui.components.LinearMeter
import com.aio.tracker.ui.components.Pill
import com.aio.tracker.ui.components.SectionHeader
import com.aio.tracker.ui.components.SheetTitle
import com.aio.tracker.ui.components.StatTile
import com.aio.tracker.ui.components.screenPadding
import com.aio.tracker.ui.theme.AppTheme
import com.aio.tracker.util.Dates
import com.aio.tracker.util.Money
import com.aio.tracker.util.toLocalDate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private enum class MoneyTab(val label: String) { OVERVIEW("Overview"), TRANSACTIONS("Activity"), LENDING("Lending") }

@Composable
fun MoneyScreen() {
    val vm = appViewModel { MoneyViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val txnsState by vm.txns.collectAsStateWithLifecycle()
    val loansState by vm.loans.collectAsStateWithLifecycle()
    val txns = txnsState.orEmpty()
    val loans = loansState.orEmpty()
    val code = settings.currencyCode
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editTxn by remember { mutableStateOf<MoneyTxn?>(null) }
    var newTxnType by remember { mutableStateOf<TxnType?>(null) }
    var editLoan by remember { mutableStateOf<Loan?>(null) }
    var newLoan by remember { mutableStateOf(false) }
    var repayLoan by remember { mutableStateOf<Loan?>(null) }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    // The user's own categories (most used first), offered again as chips in the editor.
    val customCategories = remember(txns) {
        val builtIn = (Categories.expense + Categories.income).map { it.name.lowercase() }.toSet()
        txns.filter { it.category.lowercase() !in builtIn }
            .groupBy { it.type }
            .mapValues { (_, list) ->
                list.groupingBy { it.category }.eachCount().entries.sortedByDescending { it.value }.take(8).map { it.key }
            }
    }
    val month = YearMonth.now().minusMonths(monthOffset.toLong())
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val currentTab = MoneyTab.entries[tab]

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Money") },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (currentTab == MoneyTab.LENDING) newLoan = true else newTxnType = TxnType.EXPENSE },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(if (currentTab == MoneyTab.LENDING) "Add loan" else "Add") },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = screenPadding(padding, bottomExtra = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "tabs") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    MoneyTab.entries.forEachIndexed { i, t ->
                        SegmentedButton(
                            selected = tab == i,
                            onClick = { tab = i },
                            shape = SegmentedButtonDefaults.itemShape(i, MoneyTab.entries.size),
                        ) { Text(t.label) }
                    }
                }
            }
            when (currentTab) {
                MoneyTab.OVERVIEW -> overview(
                    month = month,
                    canGoNext = monthOffset > 0,
                    onPrev = { monthOffset++ },
                    onNext = { if (monthOffset > 0) monthOffset-- },
                    txns = txns,
                    loans = loans,
                    code = code,
                    budget = settings.monthlyBudgetMinor,
                    onAddIncome = { newTxnType = TxnType.INCOME },
                    onAddExpense = { newTxnType = TxnType.EXPENSE },
                    onOpenTxn = { editTxn = it },
                    loaded = txnsState != null,
                )
                MoneyTab.TRANSACTIONS -> transactions(
                    txns = txns,
                    code = code,
                    loaded = txnsState != null,
                    query = query,
                    onQuery = { query = it.take(60) },
                    onOpen = { editTxn = it },
                    onAdd = { newTxnType = TxnType.EXPENSE },
                )
                MoneyTab.LENDING -> lending(loans, code, loaded = loansState != null, onOpen = { editLoan = it }, onRepay = { repayLoan = it }, onAdd = { newLoan = true })
            }
        }
    }

    if (editTxn != null || newTxnType != null) {
        TxnEditorSheet(
            existing = editTxn,
            initialType = newTxnType ?: TxnType.EXPENSE,
            code = code,
            customCategories = customCategories,
            onDismiss = { editTxn = null; newTxnType = null },
            onSave = { vm.saveTxn(it); editTxn = null; newTxnType = null },
            onDelete = { vm.deleteTxn(it); editTxn = null; newTxnType = null },
        )
    }
    if (editLoan != null || newLoan) {
        LoanEditorSheet(
            existing = editLoan,
            onDismiss = { editLoan = null; newLoan = false },
            onSave = { vm.saveLoan(it); editLoan = null; newLoan = false },
            onDelete = { vm.deleteLoan(it); editLoan = null; newLoan = false },
        )
    }
    repayLoan?.let { loan ->
        RepayDialog(loan, code, onDismiss = { repayLoan = null }, onConfirm = { vm.repay(loan, it); repayLoan = null })
    }
}

// ------------------------------------------------------------------ Overview

private fun androidx.compose.foundation.lazy.LazyListScope.overview(
    month: YearMonth,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    txns: List<MoneyTxn>,
    loans: List<Loan>,
    code: String,
    budget: Long,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenTxn: (MoneyTxn) -> Unit,
    loaded: Boolean,
) {
    val from = month.atDay(1).toEpochDay()
    val to = month.atEndOfMonth().toEpochDay()
    val inMonth = txns.filter { it.epochDay in from..to }
    val income = inMonth.filter { it.type == TxnType.INCOME }.sumOf { it.amountMinor }
    val expense = inMonth.filter { it.type == TxnType.EXPENSE }.sumOf { it.amountMinor }

    item(key = "month") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) { Icon(Icons.Rounded.ChevronLeft, "Previous month") }
            Text(
                month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.year,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = onNext, enabled = canGoNext) { Icon(Icons.Rounded.ChevronRight, "Next month") }
        }
    }
    item(key = "hero") {
        HeroCard {
            Text("NET BALANCE", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
            Text(Money.format(income - expense, code), style = MaterialTheme.typography.displaySmall, maxLines = 1)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroFigure("Income", Money.format(income, code), Icons.Rounded.ArrowDownward, Modifier.weight(1f), onAddIncome)
                HeroFigure("Expenses", Money.format(expense, code), Icons.Rounded.ArrowUpward, Modifier.weight(1f), onAddExpense)
            }
            if (budget > 0) {
                Spacer(Modifier.height(14.dp))
                val frac = expense.toFloat() / budget
                Row {
                    Text("Budget", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.85f))
                    Text(
                        if (expense <= budget) "${Money.format(budget - expense, code)} left" else "${Money.format(expense - budget, code)} over",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearMeter(frac, if (frac > 1f) Color(0xFFFF8A84) else AppTheme.ext.gold)
            }
        }
    }
    if (loaded && txns.isEmpty()) {
        item(key = "empty") {
            EmptyState(
                Icons.Rounded.AccountBalanceWallet,
                "Track your money",
                "Add your expenses and income to see where your money goes each month.",
                actionLabel = "Add first entry",
                onAction = onAddExpense,
            )
        }
        return
    }
    val byCategory = inMonth.filter { it.type == TxnType.EXPENSE }.groupBy { it.category }
        .mapValues { e -> e.value.sumOf { it.amountMinor } }.entries.sortedByDescending { it.value }
    if (byCategory.isNotEmpty()) {
        item(key = "cats") {
            SectionHeader("Spending by category")
            AppCard {
                val top = byCategory.take(6)
                val rest = byCategory.drop(6).sumOf { it.value }
                val values = top.map { it.value.toFloat() } + if (rest > 0) listOf(rest.toFloat()) else emptyList()
                val labels = top.map { it.key } + if (rest > 0) listOf("Others") else emptyList()
                val colors = AppTheme.ext.chart
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(values, colors, size = 132.dp, thickness = 16.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.compact(expense, code), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        labels.forEachIndexed { i, l ->
                            LegendRow(colors[i % colors.size], l, "${((values[i] / expense.coerceAtLeast(1)) * 100).toInt()}%")
                        }
                    }
                }
            }
        }
    }
    item(key = "trend") {
        val months = (5 downTo 0).map { month.minusMonths(it.toLong()) }
        val values = months.map { m ->
            val f = m.atDay(1).toEpochDay()
            val t = m.atEndOfMonth().toEpochDay()
            txns.filter { it.type == TxnType.EXPENSE && it.epochDay in f..t }.sumOf { it.amountMinor }.toFloat()
        }
        SectionHeader("Expenses · last 6 months")
        AppCard {
            BarChart(
                values = values,
                labels = months.map { it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) },
                topLabels = values.map { if (it > 0f) Money.compact(it.toLong(), code) else "" },
                highlightIndex = 5,
                barColor = AppTheme.ext.expense,
                chartHeight = 110.dp,
            )
        }
    }
    item(key = "lend") {
        val owed = loans.filter { it.direction == LoanDirection.LENT }.sumOf { it.remainingMinor }
        val owe = loans.filter { it.direction == LoanDirection.BORROWED }.sumOf { it.remainingMinor }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Owed to you", Money.compact(owed, code), Icons.Rounded.ArrowDownward, AppTheme.ext.income, Modifier.weight(1f))
            StatTile("You owe", Money.compact(owe, code), Icons.Rounded.ArrowUpward, AppTheme.ext.expense, Modifier.weight(1f))
        }
    }
    val recent = inMonth.take(5)
    if (recent.isNotEmpty()) {
        item(key = "recentH") { SectionHeader("Recent") }
        items(recent, key = { "r" + it.id }) { TxnRow(it, code) { onOpenTxn(it) } }
    }
}

@Composable
private fun HeroFigure(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.White.copy(alpha = 0.12f),
        contentColor = Color.White,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ------------------------------------------------------------------ Transactions

private fun androidx.compose.foundation.lazy.LazyListScope.transactions(
    txns: List<MoneyTxn>,
    code: String,
    loaded: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onOpen: (MoneyTxn) -> Unit,
    onAdd: () -> Unit,
) {
    if (loaded && txns.isEmpty()) {
        item(key = "empty") {
            EmptyState(Icons.Rounded.Receipt, "No activity yet", "Your expenses and income will appear here.", actionLabel = "Add entry", onAction = onAdd)
        }
        return
    }
    item(key = "search") {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            placeholder = { Text("Search category or note") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, "Clear search") }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    val q = query.trim()
    val shown = if (q.isEmpty()) txns else txns.filter { it.category.contains(q, ignoreCase = true) || it.note.contains(q, ignoreCase = true) }
    if (q.isNotEmpty()) {
        if (shown.isEmpty()) {
            item(key = "noMatch") { EmptyState(Icons.Rounded.Search, "No matches", "No entries match \"$q\".") }
            return
        }
        item(key = "matchSummary") {
            val spent = shown.filter { it.type == TxnType.EXPENSE }.sumOf { it.amountMinor }
            val received = shown.filter { it.type == TxnType.INCOME }.sumOf { it.amountMinor }
            Text(
                buildString {
                    append(if (shown.size == 1) "1 entry" else "${shown.size} entries")
                    if (spent > 0) append(" · ${Money.format(spent, code)} spent")
                    if (received > 0) append(" · ${Money.format(received, code)} received")
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
    val today = LocalDate.now()
    shown.groupBy { it.epochDay }.forEach { (day, list) ->
        item(key = "d$day") {
            val net = list.sumOf { if (it.type == TxnType.INCOME) it.amountMinor else -it.amountMinor }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(Dates.relativeDay(day.toLocalDate(), today), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    (if (net >= 0) "+" else "") + Money.format(net, code),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(list, key = { "t" + it.id }) { TxnRow(it, code) { onOpen(it) } }
    }
}

@Composable
private fun TxnRow(t: MoneyTxn, code: String, onClick: () -> Unit) {
    val isIncome = t.type == TxnType.INCOME
    val color = if (isIncome) AppTheme.ext.income else AppTheme.ext.expense
    AppCard(onClick = onClick, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Categories.icon(t.category), color, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.category, style = MaterialTheme.typography.titleSmall)
                if (t.note.isNotBlank()) {
                    Text(t.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(
                (if (isIncome) "+" else "−") + Money.format(t.amountMinor, code),
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ------------------------------------------------------------------ Lending

private fun androidx.compose.foundation.lazy.LazyListScope.lending(
    loans: List<Loan>,
    code: String,
    loaded: Boolean,
    onOpen: (Loan) -> Unit,
    onRepay: (Loan) -> Unit,
    onAdd: () -> Unit,
) {
    val owed = loans.filter { it.direction == LoanDirection.LENT }.sumOf { it.remainingMinor }
    val owe = loans.filter { it.direction == LoanDirection.BORROWED }.sumOf { it.remainingMinor }
    item(key = "lsum") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Owed to you", Money.format(owed, code), Icons.Rounded.ArrowDownward, AppTheme.ext.income, Modifier.weight(1f))
            StatTile("You owe", Money.format(owe, code), Icons.Rounded.ArrowUpward, AppTheme.ext.expense, Modifier.weight(1f))
        }
    }
    if (loaded && loans.isEmpty()) {
        item(key = "empty") {
            EmptyState(
                Icons.Rounded.SwapHoriz,
                "No lendings",
                "Keep track of money you lent to or borrowed from people, with due dates and repayments.",
                actionLabel = "Add loan",
                onAction = onAdd,
            )
        }
        return
    }
    val open = loans.filter { !it.settled }
    val settled = loans.filter { it.settled }
    if (open.isNotEmpty()) {
        item(key = "openH") { SectionHeader("Open") }
        items(open, key = { "l" + it.id }) { LoanCard(it, code, onClick = { onOpen(it) }, onRepay = { onRepay(it) }) }
    }
    if (settled.isNotEmpty()) {
        item(key = "setH") { SectionHeader("Settled") }
        items(settled, key = { "s" + it.id }) { LoanCard(it, code, onClick = { onOpen(it) }, onRepay = null) }
    }
}

@Composable
private fun LoanCard(loan: Loan, code: String, onClick: () -> Unit, onRepay: (() -> Unit)?) {
    val lent = loan.direction == LoanDirection.LENT
    val color = if (lent) AppTheme.ext.income else AppTheme.ext.expense
    val today = LocalDate.now().toEpochDay()
    val overdue = !loan.settled && loan.dueEpochDay != null && loan.dueEpochDay < today
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(if (lent) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward, color, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(loan.person, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    (if (lent) "You lent" else "You borrowed") + " · " + Dates.day(loan.epochDay.toLocalDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(loan.remainingMinor, code), style = MaterialTheme.typography.titleMedium, color = color)
                Text("of " + Money.format(loan.amountMinor, code), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        LinearMeter(loan.repaidMinor.toFloat() / loan.amountMinor.coerceAtLeast(1), color)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                loan.settled -> Pill("Settled", AppTheme.ext.income)
                overdue -> Pill("Overdue · " + Dates.day(loan.dueEpochDay!!.toLocalDate()), AppTheme.ext.expense)
                loan.dueEpochDay != null -> Pill("Due " + Dates.relativeDay(loan.dueEpochDay.toLocalDate()), MaterialTheme.colorScheme.tertiary)
                else -> Pill("No due date", MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.weight(1f))
            if (onRepay != null) {
                FilledTonalButton(onClick = onRepay, contentPadding = PaddingValues(horizontal = 14.dp)) {
                    Text(if (lent) "Received" else "Paid back")
                }
            }
        }
        if (loan.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(loan.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ------------------------------------------------------------------ Editors

@Composable
private fun AmountField(value: String, onChange: (String) -> Unit, isError: Boolean, label: String = "Amount") {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' || it == ',' }.take(14)) },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        textStyle = MaterialTheme.typography.headlineSmall,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TxnEditorSheet(
    existing: MoneyTxn?,
    initialType: TxnType,
    code: String,
    customCategories: Map<TxnType, List<String>>,
    onDismiss: () -> Unit,
    onSave: (MoneyTxn) -> Unit,
    onDelete: (MoneyTxn) -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember { mutableStateOf(existing?.type ?: initialType) }
    var amount by remember { mutableStateOf(existing?.amountMinor?.let { Money.toInput(it) } ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: Categories.forType(initialType).first().name) }
    var customCategory by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var date by remember { mutableStateOf(existing?.epochDay?.toLocalDate() ?: LocalDate.now()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val parsed = Money.parse(amount)
    val cats = Categories.forType(type)
    val custom = customCategories[type].orEmpty()
    val finalCategory = (if (category == "Other" && customCategory.isNotBlank()) customCategory else category).trim().let { typed ->
        // Reuse the existing spelling ("pets" -> "Pets") so one category doesn't split into several.
        cats.firstOrNull { it.name.equals(typed, ignoreCase = true) }?.name
            ?: custom.firstOrNull { it.equals(typed, ignoreCase = true) }
            ?: typed
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetTitle(if (existing == null) "New entry" else "Edit entry") {
                if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TxnType.entries.forEachIndexed { i, t ->
                    SegmentedButton(
                        selected = type == t,
                        onClick = {
                            if (type != t) {
                                type = t
                                if (Categories.forType(t).none { it.name == category }) category = Categories.forType(t).first().name
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                    ) { Text(if (t == TxnType.EXPENSE) "Expense" else "Income") }
                }
            }
            AmountField(amount, { amount = it }, isError = amount.isNotEmpty() && parsed == null, label = "Amount ($code)")
            Text("Category", style = MaterialTheme.typography.labelLarge)
            val knownCategory = cats.any { it.name == category }
            val savedCustom = category in custom
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                cats.forEach { c ->
                    FilterChip(
                        selected = category == c.name || (!knownCategory && !savedCustom && c.name == "Other"),
                        onClick = { category = c.name },
                        label = { Text(c.name) },
                        leadingIcon = { Icon(c.icon, null, Modifier.size(FilterChipDefaults.IconSize)) },
                    )
                }
                // Categories the user created earlier.
                custom.forEach { name ->
                    FilterChip(
                        selected = category == name,
                        onClick = { category = name },
                        label = { Text(name) },
                        leadingIcon = { Icon(Categories.icon(name), null, Modifier.size(FilterChipDefaults.IconSize)) },
                    )
                }
            }
            if (category == "Other" || !knownCategory) {
                OutlinedTextField(
                    value = if (!knownCategory) category else customCategory,
                    onValueChange = { v -> if (!knownCategory) category = v.take(24) else customCategory = v.take(24) },
                    label = { Text("Custom category (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(120) },
                label = { Text("Note") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            DateField("Date", date, { if (it != null) date = it })
            Button(
                enabled = parsed != null && finalCategory.isNotBlank(),
                onClick = {
                    onSave(
                        MoneyTxn(
                            id = existing?.id ?: 0,
                            type = type,
                            amountMinor = parsed ?: 0,
                            category = finalCategory,
                            note = note.trim(),
                            epochDay = date.toEpochDay(),
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Save") }
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog("Delete entry?", "This entry will be permanently removed.", "Delete", onConfirm = { onDelete(existing) }, onDismiss = { confirmDelete = false }, destructive = true)
    }
}

@Composable
private fun LoanEditorSheet(
    existing: Loan?,
    onDismiss: () -> Unit,
    onSave: (Loan) -> Unit,
    onDelete: (Loan) -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var direction by remember { mutableStateOf(existing?.direction ?: LoanDirection.LENT) }
    var person by remember { mutableStateOf(existing?.person ?: "") }
    var amount by remember { mutableStateOf(existing?.amountMinor?.let { Money.toInput(it) } ?: "") }
    var repaid by remember { mutableStateOf(existing?.repaidMinor?.takeIf { it > 0 }?.let { Money.toInput(it) } ?: "") }
    var date by remember { mutableStateOf(existing?.epochDay?.toLocalDate() ?: LocalDate.now()) }
    var due by remember { mutableStateOf(existing?.dueEpochDay?.toLocalDate()) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    val parsed = Money.parse(amount)
    val parsedRepaid = if (repaid.isBlank()) 0L else Money.parse(repaid)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetTitle(if (existing == null) "New loan" else "Edit loan") {
                if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                LoanDirection.entries.forEachIndexed { i, d ->
                    SegmentedButton(
                        selected = direction == d,
                        onClick = { direction = d },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                    ) { Text(d.label) }
                }
            }
            OutlinedTextField(
                value = person,
                onValueChange = { person = it.take(40) },
                label = { Text(if (direction == LoanDirection.LENT) "Lent to" else "Borrowed from") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            AmountField(amount, { amount = it }, isError = amount.isNotEmpty() && parsed == null)
            if (existing != null) {
                OutlinedTextField(
                    value = repaid,
                    onValueChange = { v -> repaid = v.filter { it.isDigit() || it == '.' || it == ',' }.take(14) },
                    label = { Text("Already repaid") },
                    singleLine = true,
                    isError = parsedRepaid == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            DateField("Date", date, { if (it != null) date = it })
            DateField("Due date", due, { due = it }, allowClear = true, placeholder = "No due date")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(160) },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                enabled = parsed != null && person.isNotBlank() && parsedRepaid != null,
                onClick = {
                    val total = parsed ?: 0
                    onSave(
                        Loan(
                            id = existing?.id ?: 0,
                            person = person.trim(),
                            direction = direction,
                            amountMinor = total,
                            repaidMinor = (parsedRepaid ?: 0).coerceIn(0, total),
                            epochDay = date.toEpochDay(),
                            dueEpochDay = due?.toEpochDay(),
                            note = note.trim(),
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Save") }
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog("Delete loan?", "This record will be permanently removed.", "Delete", onConfirm = { onDelete(existing) }, onDismiss = { confirmDelete = false }, destructive = true)
    }
}

@Composable
private fun RepayDialog(loan: Loan, code: String, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var amount by remember { mutableStateOf(Money.toInput(loan.remainingMinor)) }
    val parsed = Money.parse(amount)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (loan.direction == LoanDirection.LENT) "Payment received" else "Payment made") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Remaining: ${Money.format(loan.remainingMinor, code)}", style = MaterialTheme.typography.bodyMedium)
                AmountField(amount, { amount = it }, isError = parsed == null)
                Box(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { amount = Money.toInput(loan.remainingMinor) }) { Text("Full amount") }
                }
            }
        },
        confirmButton = { Button(enabled = parsed != null, onClick = { onConfirm(parsed ?: 0) }) { Text("Record") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
