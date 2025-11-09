package com.example.lab3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lab3.ui.theme.Lab3Theme
import kotlin.math.cos

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab3Theme {
                Scaffold(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray)
                ) { innerPadding ->
                    Main(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Main(modifier: Modifier = Modifier) {
    var str_min by rememberSaveable { mutableStateOf("-10") } // Сначала min
    var str_max by rememberSaveable { mutableStateOf("10") }  // Потом max

    var x_min by rememberSaveable { mutableStateOf(-10f) }
    var x_max by rememberSaveable { mutableStateOf(10f) }

    if (x_max < x_min){
        val temp = x_min
        val temp_str = str_min

        x_min = x_max
        x_max = temp

        str_min = str_max
        str_max = temp_str
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.graph),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0, 0, 255)
        )
        Box(
            modifier = Modifier
                .size(400.dp)
                .background(color = Color(149, 149, 149))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Предварительно вычисляем основные параметры
                val graphStartX = width * 0.1f
                val graphEndX = width * 0.9f
                val graphStartY = height * 0.1f
                val graphEndY = height * 0.9f

                // Вычисляем диапазон Y
                var y_min = 0f
                var y_max = 2f // Для функции y = 1 - cos(x) диапазон всегда [0, 2]

                // Вычисляем коэффициенты масштабирования
                val scaleX = (graphEndX - graphStartX) / (x_max - x_min)
                val scaleY = (graphEndY - graphStartY) / (y_max - y_min)

                // Рисуем ось X
                val xAxisY = graphEndY + y_min * scaleY
                drawLine(
                    color = Color.Black,
                    start = Offset(x = graphStartX, y = xAxisY),
                    end = Offset(x = graphEndX, y = xAxisY),
                    strokeWidth = 2.0f
                )

                // Рисуем ось Y
                val yAxisX = graphStartX - x_min * scaleX
                drawLine(
                    color = Color.Black,
                    start = Offset(x = yAxisX, y = graphStartY),
                    end = Offset(x = yAxisX, y = graphEndY),
                    strokeWidth = 2.0f
                )

                // Рисуем деления на оси X
                val xIntMin = x_min.toInt()
                val xIntMax = x_max.toInt()
                for(xvalue in xIntMin..xIntMax) {
                    if (xvalue % 2 == 0) { // Только каждое второе деление
                        val canvasX = graphStartX + (xvalue - x_min) * scaleX
                        drawLine(
                            color = Color.Black,
                            start = Offset(x = canvasX, y = xAxisY - 5f),
                            end = Offset(x = canvasX, y = xAxisY + 5f),
                            strokeWidth = 2f
                        )

                        drawContext.canvas.nativeCanvas.drawText(
                            xvalue.toString(),
                            canvasX - 10f,
                            xAxisY + 25f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 20f
                            }
                        )
                    }
                }

                // Рисуем деления на оси Y
                for (i in 0..2) {
                    val yvalue = i * 1f
                    val canvasY = graphEndY - (yvalue - y_min) * scaleY

                    drawLine(
                        color = Color.Black,
                        start = Offset(x = yAxisX - 5f, y = canvasY),
                        end = Offset(x = yAxisX + 5f, y = canvasY),
                        strokeWidth = 2f
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        yvalue.toString(),
                        yAxisX - 30f,
                        canvasY + 8f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 15f
                        }
                    )
                }

                // Рисуем график функции y = 1 - cos(x) используя drawLine сегментами
                val numSegments = 100
                val segmentStep = (x_max - x_min) / numSegments

                for (segment in 0 until numSegments) {
                    val x1 = x_min + segment * segmentStep
                    val x2 = x_min + (segment + 1) * segmentStep

                    val y1 = 1f - cos(x1.toFloat()).toFloat()
                    val y2 = 1f - cos(x2.toFloat()).toFloat()

                    val canvasX1 = graphStartX + (x1 - x_min) * scaleX
                    val canvasY1 = graphEndY - (y1 - y_min) * scaleY
                    val canvasX2 = graphStartX + (x2 - x_min) * scaleX
                    val canvasY2 = graphEndY - (y2 - y_min) * scaleY

                    drawLine(
                        color = Color.Red,
                        start = Offset(x = canvasX1, y = canvasY1),
                        end = Offset(x = canvasX2, y = canvasY2),
                        strokeWidth = 3f
                    )
                }
            }
        }
        // Теперь сначала поле для min x
        OutlinedTextField(
            value = str_min,
            onValueChange = { str_min = it },
            label = { Text(text = stringResource(R.string.min_x)) }
        )
        // Потом поле для max x
        OutlinedTextField(
            value = str_max,
            onValueChange = { str_max = it },
            label = { Text(text = stringResource(R.string.max_x)) }
        )
        Button(
            onClick = {
                x_min = str_min.toFloatOrNull() ?: 0f
                x_max = str_max.toFloatOrNull() ?: 0f
            },
            enabled = str_min.isNotEmpty() && str_max.isNotEmpty()
        ) {
            Text(text = stringResource(R.string.draw))
        }
    }
}