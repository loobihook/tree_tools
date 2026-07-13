package me.rpgz.treetools.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import me.rpgz.treetools.components.ScreenTile

@Composable
fun ModelTestPage(navController: NavHostController) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScreenTile("模型调试")
    }
}