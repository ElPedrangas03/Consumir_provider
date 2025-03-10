package com.example.consumirprovider

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.ContentResolver
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.consumirprovider.ui.theme.ConsumirProviderTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConsumirProviderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CurrencyExchangeScreen()
                }
            }
        }
    }
}

@Composable
fun CurrencyExchangeScreen() {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val scope = rememberCoroutineScope()

    // Estados para la selección de moneda
    var selectedCurrency by remember { mutableStateOf("USD") }
    var expandedCurrency by remember { mutableStateOf(false) }
    val currencies = listOf("USD", "EUR", "MXN", "GBP", "JPY")

    // Estados para las fechas
    var startDate by remember { mutableStateOf("2025-01-01 00:00:00.000") }
    var endDate by remember { mutableStateOf("2025-12-31 23:59:59.000") }

    // Estados para los resultados y la carga
    var exchangeRates by remember { mutableStateOf<List<Pair<String, Double>>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        // Selector de moneda
        CurrencyDropdown(
            currencies = currencies,
            selectedCurrency = selectedCurrency,
            onCurrencySelected = { selectedCurrency = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de fecha y hora de inicio
        DateTimePicker(
            label = "Fecha de inicio",
            currentDateTime = startDate,
            onDateTimeSelected = { startDate = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de fecha y hora de fin
        DateTimePicker(
            label = "Fecha de fin",
            currentDateTime = endDate,
            onDateTimeSelected = { endDate = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para ejecutar la consulta
        Button(
            onClick = {
                if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                    loading = true
                    scope.launch {
                        try {
                            val rates = withContext(Dispatchers.IO) {
                                fetchExchangeRate(
                                    contentResolver = contentResolver,
                                    currency = "MXN",
                                    change = selectedCurrency,
                                    startDate = startDate,
                                    endDate = endDate
                                )
                            }
                            exchangeRates = rates
                            Log.d("ExchangeRate", "Rates: $rates")
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Error al obtener datos: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            loading = false
                        }
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Selecciona ambas fechas",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Consultar datos")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar resultados o indicador de carga
        when {
            loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            exchangeRates.isNotEmpty() -> {
                ExchangeRateChart(
                    exchangeRates = exchangeRates,
                    currency = selectedCurrency,
                    change = "MXN_to_$selectedCurrency",
                    onValueSelected = { e, h ->
                        if (e != null && h != null) {
                            val index = h.x.toInt()
                            if (index >= 0 && index < exchangeRates.size) {
                                val (date, rate) = exchangeRates[index]
                                Toast.makeText(
                                    context,
                                    "Fecha: $date, Valor: $rate",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Índice inválido: $index",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Nada seleccionado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onNothingSelected = {
                        Toast.makeText(
                            context,
                            "Nada seleccionado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(exchangeRates) { (date, rate) ->
                        ExchangeRateItem(date = date, rate = rate)
                    }
                }
            }
            else -> Text("No hay datos disponibles", modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun ExchangeRateItem(date: String, rate: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Fecha: $date", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Tasa: ${"%.4f".format(rate)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@SuppressLint("Range")
fun fetchExchangeRate(
    contentResolver: ContentResolver,
    currency: String,
    change: String,
    startDate: String,
    endDate: String
): List<Pair<String, Double>> {
    val uri = Uri.parse("content://com.example.proyectodivisa.provider/divisas_by_currency_and_change_and_date/$currency/$change/$startDate/$endDate")

    return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val results = mutableListOf<Pair<String, Double>>()
        while (cursor.moveToNext()) {
            val date = cursor.getString(cursor.getColumnIndex("date"))
            val rate = cursor.getDouble(cursor.getColumnIndex("rate"))
            results.add(Pair(date, rate))
            Log.d("Divisas", "Fecha: $date, Tasa de cambio: $rate")
        }
        results
    } ?: emptyList()
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar")
            }
        },
        text = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(
    label: String,
    currentDateTime: String,
    onDateTimeSelected: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()) }

    // Estado para fecha y hora seleccionada
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }

    Column {
        // Botón para abrir el DatePicker
        Button(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Seleccionar $label")
        }

        // Mostrar la fecha y hora seleccionada
        Text(
            text = "$label: $currentDateTime",
            modifier = Modifier.padding(8.dp)
        )

        // DatePicker
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(onClick = {
                        datePickerState.selectedDateMillis?.let { timestamp ->
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = timestamp
                            }
                            selectedDate = calendar
                            showDatePicker = false
                            showTimePicker = true // Abrir el TimePicker después de seleccionar la fecha
                        }
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDatePicker = false }) {
                        Text("Cancelar")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showTimePicker) {
            val timePickerState = rememberTimePickerState()
            TimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                onConfirm = {
                    selectedDate?.let { calendar ->
                        calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        calendar.set(Calendar.MINUTE, timePickerState.minute)
                        onDateTimeSelected(dateFormatter.format(calendar.time))
                        showTimePicker = false
                    }
                }
            ) {
                TimePicker(state = timePickerState)
            }
        }
    }
}

@Composable
fun ExchangeRateChart(
    exchangeRates: List<Pair<String, Double>>,
    currency: String,
    change: String,
    onValueSelected: (Entry?, Highlight?) -> Unit,
    onNothingSelected: () -> Unit
) {
    val entries = exchangeRates.mapIndexed { index, (_, rate) ->
        Entry(index.toFloat(), rate.toFloat())
    }

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                configureChart()
                data = LineData(
                    LineDataSet(entries, "Tipo de Cambio $change/$currency").apply {
                        color = Color.BLUE
                        lineWidth = 2.5f
                        setDrawCircles(true)
                        setCircleColor(Color.BLUE)
                        valueTextSize = 12f
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                        cubicIntensity = 0.2f
                    }
                )
                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        onValueSelected(e, h)
                    }

                    override fun onNothingSelected() {
                        onNothingSelected()
                    }
                })
                invalidate()
                animateX(1000)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    )
}

fun LineChart.configureChart() {
    description.isEnabled = false
    setTouchEnabled(true)
    isDragEnabled = true
    setScaleEnabled(true)
    setPinchZoom(true)

    xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        granularity = 1f
        setDrawGridLines(false)
    }

    axisLeft.apply {
        setDrawGridLines(true)
        axisMinimum = 0f
    }

    axisRight.isEnabled = false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDropdown(
    currencies: List<String>,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedCurrency,
            onValueChange = {},
            readOnly = true,
            label = { Text("Moneda destino") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}