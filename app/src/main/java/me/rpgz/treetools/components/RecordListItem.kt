package me.rpgz.treetools.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import me.rpgz.treetools.db.entities.TreeAnalysisRecordEntity
import me.rpgz.treetools.models.TreeAnalysisRecordExtra
import java.text.SimpleDateFormat
import java.util.*
import coil3.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordListItem(
    record: TreeAnalysisRecordEntity,
    onItemClick: (Long) -> Unit = {},
    onDeleteClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val formattedDate = remember(record.createdAt) {
        dateFormat.format(Date(record.createdAt))
    }

    val extraData = remember(record.extra) {
        try {
            if (record.extra != null) {
                Gson().fromJson(record.extra, TreeAnalysisRecordExtra::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    val healthStatus = remember(extraData) {
        calculateHealthStatus(extraData)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = { record.id?.let { onItemClick(it) } },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getTreeIcon(extraData?.treeSpecies),
                        contentDescription = "树种图标",
                        modifier = Modifier.size(36.dp),
                        tint = getTreeIconColor(extraData?.treeSpecies)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = healthStatus.first,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = healthStatus.second,
                            labelColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(24.dp)
                    )
                }

                extraData?.treeSpecies?.let { species ->
                    Text(
                        text = species,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    extraData?.treeHeight?.let { height ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = "树高",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${height}m",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    extraData?.treeDiameter?.let { diameter ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Circle,
                                contentDescription = "直径",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${diameter}cm",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("确定要删除这条记录吗？此操作无法撤销。", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        record.id?.let { onDeleteClick(it) }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("取消", style = MaterialTheme.typography.labelLarge)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

fun calculateHealthStatus(extraData: TreeAnalysisRecordExtra?): Pair<String, Color> {
    return if (extraData != null) {
        val height = extraData.treeHeight ?: 0f
        val diameter = extraData.treeDiameter ?: 0f

        when {
            height > 5f && diameter > 20f -> Pair("健康", Color.Green)
            height > 3f && diameter > 10f -> Pair("良好", Color(0xFF2196F3))
            height > 1f && diameter > 5f -> Pair("正常", Color(0xFFFFC107))
            else -> Pair("需关注", Color(0xFFFF5722))
        }
    } else {
        Pair("未知", Color.Gray)
    }
}

fun getTreeIcon(species: String?): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        species.isNullOrBlank() -> Icons.Default.Park
        species.contains("松", ignoreCase = true) -> Icons.Default.Park
        species.contains("杉", ignoreCase = true) -> Icons.Default.Park
        species.contains("柏", ignoreCase = true) -> Icons.Default.Park
        species.contains("梧桐", ignoreCase = true) -> Icons.Default.Grass
        species.contains("柳", ignoreCase = true) -> Icons.Default.Grass
        species.contains("杨", ignoreCase = true) -> Icons.Default.Grass
        species.contains("槐", ignoreCase = true) -> Icons.Default.Grass
        species.contains("樟", ignoreCase = true) -> Icons.Default.Grass
        else -> Icons.Default.Park
    }
}

fun getTreeIconColor(species: String?): Color {
    return when {
        species.isNullOrBlank() -> Color.Gray
        species.contains("松", ignoreCase = true) -> Color(0xFF2E7D32)
        species.contains("杉", ignoreCase = true) -> Color(0xFF388E3C)
        species.contains("柏", ignoreCase = true) -> Color(0xFF43A047)
        species.contains("梧桐", ignoreCase = true) -> Color(0xFF66BB6A)
        species.contains("柳", ignoreCase = true) -> Color(0xFF81C784)
        species.contains("杨", ignoreCase = true) -> Color(0xFFA5D6A7)
        species.contains("槐", ignoreCase = true) -> Color(0xFFC8E6C9)
        species.contains("樟", ignoreCase = true) -> Color(0xFFE8F5E9)
        else -> Color(0xFF4CAF50)
    }
}