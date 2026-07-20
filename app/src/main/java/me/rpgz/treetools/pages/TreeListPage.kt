package me.rpgz.treetools.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import me.rpgz.treetools.components.ScreenTile
import me.rpgz.treetools.db.entities.TreeAnalysisRecordEntity
import me.rpgz.treetools.models.TreeAnalysisRecordExtra
import me.rpgz.treetools.routing.Routes
import me.rpgz.treetools.viewmodels.HomePageViewModel
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun TreeListPage(navController: NavHostController, viewModel: HomePageViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchMode by remember { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisibleIndex ->
            if (lastVisibleIndex != null &&
                lastVisibleIndex >= uiState.records.size - 3 &&
                uiState.hasMorePages &&
                !uiState.isLoading
            ) {
                viewModel.loadNextPage()
            }
        }
    }

    LaunchedEffect(searchQuery) {
        delay(300)
        viewModel.searchRecords(searchQuery)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                AnimatedContent(
                    targetState = isSearchMode,
                    transitionSpec = {
                        if (targetState) {
                            slideInHorizontally(initialOffsetX = { -it }) + fadeIn() with
                                    slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        } else {
                            slideInHorizontally(initialOffsetX = { it }) + fadeIn() with
                                    slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                        }
                    }
                ) { searchMode ->
                    if (searchMode) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索树木名称、树种...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                Row {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "清除")
                                        }
                                    }
                                    IconButton(onClick = {
                                        isSearchMode = false
                                        searchQuery = ""
                                    }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                    } else {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("我的树木", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                                    Text("记录每一棵树木的生长历程", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = { isSearchMode = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "搜索",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            StatisticsCard(
                                recordCount = uiState.records.size,
                                todayCount = uiState.todayCount,
                                reportCount = uiState.reportCount
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.records.isEmpty() && uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "正在加载树木档案...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                uiState.records.isEmpty() && !uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Park,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.searchQuery.isBlank()) "暂无树木档案" else "未找到匹配的树木",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.searchQuery.isBlank()) "开始记录您的第一棵树木" else "试试其他搜索词",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (uiState.searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { navController.navigate(Routes.CreateAnalysis.route) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("新建分析", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.records,
                            key = { _, it -> it.id ?: 0 }
                        ) { index, record ->
                            AnimatedTreeRecordCard(
                                record = record,
                                index = index,
                                onItemClick = { recordId ->
                                    navController.navigate("${Routes.CreateTreeAnalysisRecord.route}?recordId=$recordId")
                                },
                                onDeleteClick = { recordId ->
                                    viewModel.deleteRecord(recordId)
                                }
                            )
                        }

                        if (uiState.isLoading && uiState.records.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                }
                            }
                        }

                        if (!uiState.hasMorePages && uiState.records.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "已显示全部树木",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AnimatedTreeRecordCard(
    record: TreeAnalysisRecordEntity,
    index: Int,
    onItemClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    val enterAnimation = remember {
        fadeIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + slideInVertically(
            initialOffsetY = { 20 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    AnimatedVisibility(
        visible = true,
        enter = enterAnimation
    ) {
        val animatedOffset = remember { Animatable(0f) }
        var isSwiping by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
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

        val hasReport = record.report?.isNotBlank() == true
        val hasHeight = extraData?.treeHeight != null

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.error
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(88.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("删除", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset(x = animatedOffset.value.dp),
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
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Park,
                                contentDescription = "树木图标",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (hasReport) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("报告已生成", style = MaterialTheme.typography.labelSmall) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else if (hasHeight) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("生长中", style = MaterialTheme.typography.labelSmall) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        extraData?.treeSpecies?.let { species ->
                            Text(
                                text = species,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            extraData?.treeHeight?.let { height ->
                                Text(
                                    text = "${height}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            extraData?.treeDiameter?.let { diameter ->
                                Text(
                                    text = "${diameter}cm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

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
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("确认删除", style = MaterialTheme.typography.headlineSmall) },
                text = { Text("确定要删除这棵树木的记录吗？此操作无法撤销。", style = MaterialTheme.typography.bodyMedium) },
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
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("取消", style = MaterialTheme.typography.labelLarge)
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun StatisticsCard(recordCount: Int, todayCount: Int, reportCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedNumber(value = recordCount)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "树木总数",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            VerticalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.height(48.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedNumber(value = todayCount)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "今日新增",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            VerticalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.height(48.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedNumber(value = reportCount)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "已生成报告",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun AnimatedNumber(value: Int) {
    val animatedValue = animateIntAsState(
        targetValue = value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Text(
        text = animatedValue.value.toString(),
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}