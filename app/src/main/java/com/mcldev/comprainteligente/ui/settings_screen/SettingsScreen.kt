package com.mcldev.comprainteligente.ui.settings_screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import com.mcldev.comprainteligente.R

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsScreenVM
) {
    val selectedItem by viewModel.dataRetentionPeriod.collectAsState()
    val deleteAllData by viewModel.deleteAllData.collectAsState()
    Scaffold { padding ->
        Column (
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.general),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Card (
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(stringResource(R.string.time_keep_data))
                    Spacer(Modifier.width(16.dp))
                    CustomDropDown(
                        selectedItem,
                        { newSelection -> viewModel.updateDataRetentionPeriod(newSelection) }
                    )
                }
            }

            Card (
                modifier = Modifier.padding(top = 8.dp)
            ) {
                /*
                TODO: Improve the UX of this config as it can be ambigous. Disabling the swtich only deletes db contents but keep images
                 */
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(stringResource(R.string.delete_all_data))
                    Spacer(Modifier.width(16.dp))

                    Switch(
                        checked = deleteAllData,
                        onCheckedChange = {
                            viewModel.updateDeleteAllData(it)
                        }
                    )
                }
            }
            Spacer(Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.credits),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.credits_icon))
                    withLink(
                        LinkAnnotation.Url(
                            "https://www.flaticon.com/br/icone-gratis/carrinho-de-supermercado_2203239?term=compras&page=1&position=74&origin=search&related_id=2203239",
                            TextLinkStyles(style = SpanStyle(color = Color.Blue))
                        )
                    ) {
                        append(" flaticon")
                    }
                }
            )

        }
    }
}


@Composable
fun CustomDropDown(
    defaultSelection: Int,
    updateDataRetentionPeriod: (Int) -> Unit
){
    val isDropDownExpanded = remember {
        mutableStateOf(false)
    }

    val itemPosition = remember {
        mutableStateOf(defaultSelection)
    }

    val timeToDelte = listOf(
        stringResource(R.string.month1),
        stringResource(R.string.month3),
        stringResource(R.string.year1),
        stringResource(R.string.year2),
        stringResource(R.string.never)
        )
    Box() {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
                isDropDownExpanded.value = true
            }
        ) {
            Text(text = timeToDelte[itemPosition.value])
            Image(
                painter = painterResource(id = R.drawable.drop_down_ic),
                contentDescription = "DropDown Icon"
            )
        }
        DropdownMenu(
            expanded = isDropDownExpanded.value,
            onDismissRequest = {
                isDropDownExpanded.value = false
            }) {
            timeToDelte.forEachIndexed { index, username ->
                DropdownMenuItem(text = {
                    Text(text = username)
                },
                    onClick = {
                        isDropDownExpanded.value = false
                        itemPosition.value = index
                        updateDataRetentionPeriod(index)
                    })
            }
        }
    }
}