package me.rpgz.treetools.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import me.rpgz.treetools.components.DashscopeApiKeySettings
import me.rpgz.treetools.components.PlanetInferSettings
import me.rpgz.treetools.components.ScreenTile
import me.rpgz.treetools.viewmodels.SettingPageViewModel

@Composable
fun SettingsPage(
    navController: NavHostController,
    viewModel: SettingPageViewModel = hiltViewModel(),
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        ScreenTile("设置")
        
        Spacer(modifier = Modifier.height(24.dp))
        
        DashscopeApiKeySettings(viewModel = viewModel)
        Spacer(modifier = Modifier.height(16.dp))
        PlanetInferSettings(viewModel=viewModel)
    }
}