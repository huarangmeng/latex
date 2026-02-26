/*
 * Copyright (c) 2026 huarangmeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.hrm.latex.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * LaTeX 预览主入口
 * 提供三大类别的预览:
 * 1. 基础 LaTeX - 传统 LaTeX 数学公式
 * 2. 增量 LaTeX - 增量解析和渲染
 * 3. 化学公式 - 使用 mhchem 的化学公式
 */

// ========== 数据模型 ==========

data class PreviewGroup(
    val id: String,
    val title: String,
    val description: String,
    val items: List<PreviewItem>
)

data class PreviewItem(
    val id: String,
    val title: String,
    val latex: String,
    val content: @Composable () -> Unit = {
        Latex(
            latex = latex,
            isDarkTheme = false,
            config = LatexConfig(accessibilityEnabled = true)
        )
    }
)

data class PreviewCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: String = "📚",
    val groups: List<PreviewGroup>
)

// ========== 预览分类 ==========

val previewCategories = listOf(
    PreviewCategory(
        id = "basic",
        title = "基础 LaTeX",
        description = "传统 LaTeX 数学公式渲染",
        icon = "📐",
        groups = basicLatexPreviewGroups
    ),
    PreviewCategory(
        id = "incremental",
        title = "增量 LaTeX",
        description = "增量解析和渲染演示",
        icon = "⚡",
        groups = incrementalLatexPreviewGroups
    ),
    PreviewCategory(
        id = "chemical",
        title = "化学公式",
        description = "化学方程式和分子式",
        icon = "⚗️",
        groups = chemicalPreviewGroups
    ),
    PreviewCategory(
        id = "linebreaking",
        title = "Line Breaking",
        description = "automatic line breaking and edge case fixes",
        icon = "↩️",
        groups = lineBreakingPreviewGroups
    ),
)

// ========== 主界面 ==========

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LatexPreview() {
    var selectedCategory by remember { mutableStateOf<PreviewCategory?>(null) }

    BackHandler(selectedCategory != null) { selectedCategory = null }

    if (selectedCategory == null) {
        CategoryListScreen(
            categories = previewCategories,
            onCategoryClick = { selectedCategory = it }
        )
    } else {
        PreviewCategoryScreen(
            title = selectedCategory!!.title,
            groups = selectedCategory!!.groups,
            onBack = { selectedCategory = null }
        )
    }
}

// ========== 分类列表页 ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    categories: List<PreviewCategory>,
    onCategoryClick: (PreviewCategory) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LaTeX 预览") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { category ->
                Card(
                    onClick = { onCategoryClick(category) },
                    modifier = Modifier.fillMaxSize(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${category.icon} ${category.title}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = category.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${category.groups.size} 个分组",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ========== 分组预览页 ==========

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun PreviewCategoryScreen(
    title: String,
    groups: List<PreviewGroup>,
    onBack: () -> Unit
) {
    var selectedGroup by remember { mutableStateOf<PreviewGroup?>(null) }

    BackHandler(selectedGroup != null) { selectedGroup = null }

    if (selectedGroup == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Text("返回")
                        }
                    }
                )
            }
        ) { paddingValues ->
            PreviewGroupList(
                groups = groups,
                onGroupClick = { selectedGroup = it },
                modifier = Modifier.padding(paddingValues)
            )
        }
    } else {
        PreviewItemList(
            group = selectedGroup!!,
            onBack = { selectedGroup = null }
        )
    }
}

// ========== 示例列表页 ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewItemList(
    group: PreviewGroup,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(group.items) { item ->
                PreviewCard(item.title) {
                    item.content()
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewAll() {
    LatexPreview()
}
