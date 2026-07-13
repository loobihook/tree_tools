package me.rpgz.treetools.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import me.rpgz.treetools.viewmodels.SettingPageViewModel

@Composable
fun DashscopeApiKeySettings(
    viewModel: SettingPageViewModel,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val dashscopeApiKey by viewModel.dashscopeApiKey.collectAsState()
    val isApiKeyEditable by viewModel.isApiKeyEditable.collectAsState()
    var isPasswordVisible by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "API 配置",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = dashscopeApiKey,
                onValueChange= {v: String -> if (isApiKeyEditable) viewModel.updateDashscopeApiKey(v)},
                label = { Text("DashScope API Key") },
                placeholder = { Text("请输入 API Key") } ,
                modifier = Modifier.fillMaxWidth(),
                enabled = isApiKeyEditable,
                readOnly = !isApiKeyEditable,
                visualTransformation = if (isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                suffix = {
                    TextButton(
                        onClick = { isPasswordVisible = !isPasswordVisible }
                    ) {
                        Text(if (isPasswordVisible) "隐藏" else "显示")
                    }
                } ,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (isApiKeyEditable) {
                            viewModel.saveDashscopeApiKey()
                            keyboardController?.hide()
                        }
                    }
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isApiKeyEditable) {
                    TextButton(
                        onClick = {
                            viewModel.cancelApiKeyEdit()
                            keyboardController?.hide()
                        }
                    ) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            viewModel.saveDashscopeApiKey()
                            keyboardController?.hide()
                        }
                    ) {
                        Text("保存")
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.toggleApiKeyEditable()
                        }
                    ) {
                        Text("编辑")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}