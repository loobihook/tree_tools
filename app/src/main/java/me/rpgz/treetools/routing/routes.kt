package me.rpgz.treetools.routing

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class Routes(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    TreeList("tree_list", "我的树木", Icons.Default.Park),
    CreateAnalysis("create_analysis", "新建分析", Icons.Default.AddCircle),
    DataOverview("data_overview", "数据总览", Icons.Default.BarChart),
    Toolbox("toolbox", "工具箱", Icons.Default.Build),
    
    CreateTreeAnalysisRecord("create_tree_analysis_record", "创建分析记录", Icons.Default.Create),
    ModelTest("model_test", "模型测试", Icons.Default.Science),
    RealSenseTest("realsense_test", "RealSense测试", Icons.Default.Camera),
    RealSenseMeasurement("realsense_measurement", "RealSense测量", Icons.Default.Star),
    Settings("settings", "设置", Icons.Default.Settings),
    Benchmark("benchmark", "模型Benchmark", Icons.Default.TrendingUp),
    ApiConfig("api_config", "API配置", Icons.Default.Wifi),
    SensorMode("sensor_mode", "传感器模式", Icons.Default.Bluetooth)
}