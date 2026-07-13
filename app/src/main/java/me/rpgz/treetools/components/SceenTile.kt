package me.rpgz.treetools.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import me.rpgz.treetools.ui.theme.AppTypography

/**
 * only when navController is not null and backBtn is true, the back button work properly
 */
@Composable
fun ScreenTile(title: String, navController: NavController?=null, backBtn: Boolean = false) {
    Column(modifier = Modifier.padding(bottom = 12.dp, top = if(backBtn) 0.dp else 12.dp), horizontalAlignment = Alignment.Start) {
        if(backBtn) {
            TextButton(onClick = { navController?.popBackStack() }, contentPadding = PaddingValues(all=0.dp), modifier = Modifier.height(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "go back")
                Text("返回", style = AppTypography.titleMedium)
            }
        }
        Text(title, style = AppTypography.headlineLarge)
    }
}

@Preview
@Composable
fun ScreenTitlePreview() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScreenTile("预览", backBtn = true)
    }
}