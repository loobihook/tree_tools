package me.rpgz.treetools.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.rpgz.treetools.models.PlanetInferModel
import me.rpgz.treetools.ui.theme.AppTypography
import me.rpgz.treetools.viewmodels.SettingPageViewModel

@Composable
fun PlanetInferSettings(
    viewModel: SettingPageViewModel,
    modifier: Modifier = Modifier
) {
    val selectedModel by viewModel.planetInferModel.collectAsState()
    
    Card(
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "模型设置",
                style = AppTypography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            val modelOptions = listOf(PlanetInferModel.THIRD_PARTY_API, PlanetInferModel.PRIVATE_MODEL)
            
            modelOptions.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (selectedModel == option),
                            onClick = {
                                viewModel.updatePlanetInferModel(option)
                            },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedModel == option),
                        onClick = null
                    )
                    Text(
                        text = option.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}