package com.example.maincalculate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.maincalculate.ui.theme.MainCalculateTheme
import java.math.BigDecimal

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainCalculateTheme {
                Scaffold( modifier = Modifier.fillMaxSize() ) { innerPadding ->
                    AppScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun AppScreen(modifier: Modifier = Modifier) {
    var showResultScreen by remember { mutableStateOf(false) }
    var calculationResult by remember { mutableStateOf("") }

    if (showResultScreen) {
        ResultScreen(
            result = calculationResult,
            onBackClick = { showResultScreen = false },
            modifier = modifier
        )
    } else {
        MainCalc(
            onCalculate = { result ->
                calculationResult = result
                showResultScreen = true
            },
            modifier = modifier
        )
    }
}

@Composable
fun ResultScreen(
    result: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(id =R.string.resq),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = result,
            modifier = Modifier.padding(bottom = 32.dp)
        )
10
        Button(
            onClick = onBackClick,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = stringResource(id =R.string.back))
        }
    }
}

@Composable
fun MainCalc(onCalculate: (String) -> Unit, modifier: Modifier = Modifier) {
    var op1 by remember { mutableStateOf(value = "") }
    var op2 by remember { mutableStateOf(value = "") }
    var result by remember { mutableStateOf(value = "") }
    var focusedTextField by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = modifier.fillMaxSize()
    ) {

        Text(text = stringResource(id =R.string.app_name))
        Text(text = stringResource(id =R.string.ch1))
        TextField(
            value = op1, onValueChange = { op1 = it },
            modifier = Modifier.onFocusChanged {
                if (it.isFocused) focusedTextField = "op1"
            }
        )
        Text(text = stringResource(id =R.string.ch2))
        TextField(
            value = op2,
            onValueChange = { newVal -> op2 = newVal },
            modifier = Modifier.onFocusChanged {
                if (it.isFocused) focusedTextField = "op2"
            }
        )
//1 ряд
        Row(
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                Alignment.CenterHorizontally
            ),
            modifier = Modifier.fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Button(
                onClick = {
                    when (focusedTextField) {
                        "op1" -> op1 += "1"
                        "op2" -> op2 += "1"
                        "result" -> result += "1"
                    }
                }, shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.num1))
            }

            Button(
                onClick = {
                    when (focusedTextField) {
                        "op1" -> op1 += "2"
                        "op2" -> op2 += "2"
                        "result" -> result += "2"
                    }
                }, shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.num2))
            }

            Button(
                onClick = {
                    when (focusedTextField) {
                        "op1" -> op1 += "3"
                        "op2" -> op2 += "3"
                        "result" -> result += "3"
                    }
                }, shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.num3))
            }

            Button(
                onClick = {
                    val num1 = op1.toDoubleOrNull() ?: 0.0
                    val num2 = op2.toDoubleOrNull() ?: 0.0
                    result = (num1 + num2).toString()
                    onCalculate(result)
                }, shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.plus))
            }
        }
//2 ряд
        Row(
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                Alignment.CenterHorizontally
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    when (focusedTextField) {
                        "op1" -> op1 += "4"
                        "op2" -> op2 += "4"
                        "result" -> result += "4"
                    }
                }, shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.num4))
            }

            Button(
                onClick = {
                    when (focusedTextField) {
                        "op1" -> op1 += "5"
                        "op2" -> op2 += "5"
                        "result" -> result += "5"
                    }
                }, shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.num5))
            }

            Button(
                onClick = {
                    when (focusedTextField) {
                        "op1" -> op1 += "6"
                        "op2" -> op2 += "6"
                        "result" -> result += "6"
                    }
                }, shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.num6))
            }

            Button(
                onClick = {
                    val num1 = BigDecimal(op1 ?: "0")
                    val num2 = BigDecimal(op2 ?: "0")
                    result = (num1 - num2).toString()
                    onCalculate(result)
                }, shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.minus))
            }
        }
// 3 ряд
        Row(
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                Alignment.CenterHorizontally
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    when (focusedTextField) {
                        "op1" -> op1 += "7"
                        "op2" -> op2 += "7"
                        "result" -> result += "7"
                    }
                }, shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.num7))
            }

            Button(
                onClick = {
                    when (focusedTextField) {
                        "op1" -> op1 += "8"
                        "op2" -> op2 += "8"
                        "result" -> result += "8"
                    }
                }, shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.num8))
            }

            Button(
                onClick = {
                    when (focusedTextField) {
                        "op1" -> op1 += "9"
                        "op2" -> op2 += "9"
                        "result" -> result += "9"
                    }
                }, shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.num9))
            }

            Button(
                onClick = {
                    val num1 = op1.toDoubleOrNull() ?: 0.0
                    val num2 = op2.toDoubleOrNull() ?: 0.0
                    if (num2 == 0.0) {

                        result = context.getString(R.string.del0)
                    } else {
                        result = (num1 / num2).toString()
                    }
                    onCalculate(result)
                }, shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.del))
            }
        }
        // 4 ряд
        Row(
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                Alignment.CenterHorizontally
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    when (focusedTextField) {
                        "op1" -> op1 += "."
                        "op2" -> op2 += "."
                        "result" -> result += "."
                    }
                }, shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.dot))
            }

            Button(
                onClick = {
                    when (focusedTextField) {
                        "op1" -> op1 += "0"
                        "op2" -> op2 += "0"
                        "result" -> result += "0"
                    }
                }, shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.num0))
            }

            Button(
                onClick = {
                    op1 = ""
                    op2 = ""
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.clear))
            }

            Button(
                onClick = {
                    val num1 = op1.toDoubleOrNull() ?: 0.0
                    val num2 = op2.toDoubleOrNull() ?: 0.0
                    result = (num1 * num2).toString()
                    onCalculate(result)
                }, shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = stringResource(id =R.string.proz))
            }
        }
    }
}