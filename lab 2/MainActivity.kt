package com.example.lab2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lab2.ui.theme.Lab2Theme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.saveable.rememberSaveable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AdaptiveLayout(
                        name = stringResource(R.string.hello_android),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

data class ListItem(
    val text: String,
    var isChecked: Boolean = false
)

@Composable
fun AdaptiveLayout(name: String, modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var items by rememberSaveable { mutableStateOf(listOf<ListItem>()) }

    if (isLandscape) {
        Gorizont(
            name = name,
            modifier = modifier,
            items = items,
            onItemsChange = { newItems -> items = newItems }
        )
    } else {
        Portret(
            name = name,
            modifier = modifier,
            items = items,
            onItemsChange = { newItems -> items = newItems }
        )
    }
}

@Composable
fun Gorizont(
    name: String,
    modifier: Modifier = Modifier,
    items: List<ListItem>,
    onItemsChange: (List<ListItem>) -> Unit
) {
    var number by remember { mutableStateOf("") }

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                placeholder = { Text(stringResource(R.string.number_hint)) },
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    if (number.isNotBlank()) {
                        onItemsChange(items + ListItem(text = number))
                        number = ""
                    }
                },
                modifier = Modifier
                    .width(150.dp)
                    .height(56.dp)
            ) {
                Text(stringResource(R.string.add_button_text))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    onItemsChange(items.map { it.copy(isChecked = true) })
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.add_button1))
            }

            Button(
                onClick = {
                    onItemsChange(items.map { it.copy(isChecked = false) })
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.add_button2))
            }

            Button(
                onClick = {
                    onItemsChange(items.mapIndexed { index, item ->
                        if (index % 2 != 0) {
                            item.copy(isChecked = true)
                        } else {
                            item.copy(isChecked = false)
                        }
                    })
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.add_button3))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    items(items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.text,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { isChecked ->
                                    onItemsChange(items.map { listItem ->
                                        if (listItem == item) {
                                            listItem.copy(isChecked = isChecked)
                                        } else {
                                            listItem
                                        }
                                    })
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Portret(
    name: String,
    modifier: Modifier = Modifier,
    items: List<ListItem>,
    onItemsChange: (List<ListItem>) -> Unit
) {
    var number by remember { mutableStateOf("") }

    Column(modifier = modifier.padding(16.dp)){
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                placeholder = { Text(stringResource(R.string.number_hint)) },
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    if (number.isNotBlank()) {
                        onItemsChange(items + ListItem(text = number))
                        number = ""
                    }
                },
                modifier = Modifier.width(60.dp)
            )
            {
                Text(stringResource(R.string.add_button))
            }
        }

        Row(
            modifier = modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Button(
                onClick = {
                    onItemsChange(items.map { it.copy(isChecked = true) })
                },
                modifier = Modifier
                    .width(170.dp)
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.add_button1))
            }

            Button(
                onClick = {
                    onItemsChange(items.map { it.copy(isChecked = false) })
                },
                modifier = Modifier
                    .width(170.dp)
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.add_button2))
            }
        }
        Button(
            onClick = {
                onItemsChange(items.mapIndexed { index, item ->
                    if (index % 2 != 0) {
                        item.copy(isChecked = true)
                    } else {
                        item.copy(isChecked = false)
                    }
                })
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .width(220.dp)
                .height(48.dp)
                .offset(x = 35.dp)
        ) {
            Text(stringResource(R.string.add_button3))
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color.Gray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
            ) {
                items(items) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.text,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = item.isChecked,
                            onCheckedChange = { isChecked ->
                                onItemsChange(items.map { listItem ->
                                    if (listItem == item) {
                                        listItem.copy(isChecked = isChecked)
                                    } else {
                                        listItem
                                    }
                                })
                            }
                        )
                    }
                }
            }
        }
    }
}

