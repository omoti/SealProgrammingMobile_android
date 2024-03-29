package com.shirosoftware.sealprogrammingmobile.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shirosoftware.sealprogrammingmobile.R
import com.shirosoftware.sealprogrammingmobile.data.SettingsDataStore
import com.shirosoftware.sealprogrammingmobile.repository.SettingsRepository
import com.shirosoftware.sealprogrammingmobile.ui.theme.Primary
import com.shirosoftware.sealprogrammingmobile.ui.theme.PrimaryVariant
import com.shirosoftware.sealprogrammingmobile.ui.theme.SealProgrammingMobileTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val sliderPosition = viewModel.threshold.collectAsState(initial = 0.0f)
    val showScore = viewModel.showScore.collectAsState(false)

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        color = Color.White,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Primary),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            Column {
                ThresholdSetting(sliderPosition = sliderPosition.value, onValueChange = {
                    viewModel.updateThreshold(it)
                })
                Divider()
                ListItem(trailing = {
                    Switch(
                        checked = showScore.value,
                        onCheckedChange = { viewModel.updateShowScore(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Primary
                        )
                    )
                }) {
                    Text(text = stringResource(id = R.string.settings_show_score))
                }
                Divider()
            }
        }
    }
}

@Composable
private fun ThresholdSetting(sliderPosition: Float, onValueChange: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .padding(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.settings_threshold_title),
            fontSize = 15.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.settings_threshold_content),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "${(sliderPosition * 100).toInt()}%",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "0%", modifier = Modifier.weight(1.0f))
            Slider(
                modifier = Modifier.weight(8.0f),
                value = sliderPosition,
                steps = 9,
                valueRange = 0f..1f,
                onValueChange = onValueChange,
                colors = SliderDefaults.colors(
                    thumbColor = Primary,
                    activeTrackColor = PrimaryVariant,
                )
            )
            Text(text = "100%", modifier = Modifier.weight(1.0f))
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    SealProgrammingMobileTheme {
        SettingsScreen(
            viewModel = SettingsViewModel(
                SettingsRepository(
                    SettingsDataStore(
                        LocalContext.current
                    )
                )
            )
        )
    }
}
