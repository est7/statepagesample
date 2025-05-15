package com.vone.statepage.sample.presention

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vone.statepage.lib.extensions.Log
import com.vone.statepage.lib.extensions.d
import com.vone.statepage.lib.paginatedlist.PaginatedLazyList
import com.vone.statepage.lib.pulltorefresh.VoneRefreshLayout
import com.vone.statepage.lib.refreshablepaginatedlist.VoneRefreshablePaginatedList
import com.vone.statepage.lib.samplerefreshpaginateduistate.PaginatedListData
import com.vone.statepage.lib.samplerefreshpaginateduistate.SealedListState
import com.vone.statepage.lib.samplerefreshpaginateduistate.toPageState
import com.vone.statepage.lib.statepage.PageState
import com.vone.statepage.lib.statepage.StatePage
import com.vone.statepage.lib.statepage.StatePageActions
import com.vone.statepage.sample.Greeting
import com.vone.statepage.sample.data.SampleUserInfo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import statepagesample.composeapp.generated.resources.Res
import statepagesample.composeapp.generated.resources.ic_refresh

/**
 * 联系人列表项组件
 * 显示单个联系人信息
 */
@Composable
fun ContactListItem(contact: SampleUserInfo) {
    val greeting = remember { Greeting().greet() }
    ListItem(
        modifier = Modifier.height(200.dp),
        headlineContent = { Text(text = contact.name) },
        leadingContent = { Text("Compose: $greeting") },
        supportingContent = { Text(text = contact.email) })
}

/**
 * 联系人页面组件
 * 根据不同的UI状态显示不同的页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeContactsPage(
    id: String = "0", // 添加ID参数，默认值为"0"
    modifier: Modifier = Modifier, viewModel: SealedListStateViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Contacts") }, actions = {
                IconButton(onClick = { viewModel.reload() }) {
                    Icon(painterResource(Res.drawable.ic_refresh), "Refresh Contacts")
                }
            })
        }) { paddingValues ->
        Log.d(tag = "lilili", message = "Current uiState: $uiState") // 根据不同的UI状态显示不同的页面

        // 初始化数据加载
        LaunchedEffect(id) {
            Log.d("lilili", "初始化数据加载id = $id")
            viewModel.initDataById(id)
        }


        /*
                PaginatedContentListStatePage(
                    modifier = Modifier,
                    listState = uiState,
                    onRefresh = {
                        coroutineScope.launch {
                            viewModel.refresh()
                        }
                    },
                    onLoadMore = {
                        coroutineScope.launch {
                            viewModel.loadMore()
                        }
                    },
                ) {
                    ContactListItem(contact = it)
                }
        */
        /*
                PullToRefreshPaginatedContentListPage(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    listState = uiState,
                    onRefresh = {
                        coroutineScope.launch {
                            viewModel.refresh()
                        }
                    },
                    onLoadMore = {
                        coroutineScope.launch {
                            viewModel.loadMore()
                        }
                    },
                ) {
                    ContactListItem(contact = it)
                }
        */

        PullToRefreshPaginatedContentListStatePage(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            listState = uiState,
            onRefresh = {
                coroutineScope.launch {
                    viewModel.pullToRefresh()
                }
            },
            onReload = {
                coroutineScope.launch {
                    viewModel.reload()
                }
            },
            onLoadMore = {
                coroutineScope.launch {
                    viewModel.loadMore()
                }
            },
        ) {
            ContactListItem(contact = it)
        }

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PullToRefreshPaginatedContentListPage(
    modifier: Modifier = Modifier,
    listState: SealedListState<T>,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    itemContent: @Composable (T) -> Unit
) {
    val state = listState
    if (state is SealedListState.Success) {
        VoneRefreshablePaginatedList(
            items = state.items,
            loadState = state.paginationState,
            isRefreshing = state.isRefreshing,
            onRefresh = {
                onRefresh.invoke()
            },
            onLoadMore = {
                onLoadMore.invoke()
            },
            refreshEnabled = true,
            modifier = modifier,
            itemContent = itemContent
        )
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PaginatedContentListStatePage(
    modifier: Modifier = Modifier,
    listState: SealedListState<T>,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    itemContent: @Composable (T) -> Unit
) {
    StatePage<PaginatedListData<T>>(
        modifier = modifier,
        pageState = listState.toPageState(),
        actions = StatePageActions(
            onRetry = onRefresh,
        )
    ) { data ->
        PaginatedLazyList(
            items = data.items,
            loadState = data.paginationState,
            onLoadMore = onLoadMore,
            itemContent = itemContent
        )
    }
}

/**
 * 带有下拉刷新和分页加载功能的列表状态页面 Composable。
 *
 * @param T 列表项的数据类型。
 * @param modifier 应用于根 Composable 的 Modifier。
 * @param listState 当前的列表状态，类型为 [SealedListState]。
 * @param onRefresh 当触发下拉刷新时调用的回调。
 * @param onLoadMore 当列表滚动到底部，需要加载更多数据时调用的回调。
 * @param itemContent 用于渲染列表中每个单独项的 Composable 函数。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PullToRefreshPaginatedContentListStatePage(
    modifier: Modifier = Modifier,
    listState: SealedListState<T>,
    onRefresh: () -> Unit = {},
    onReload: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    itemContent: @Composable (item: T) -> Unit
) {
    // 使用独立的函数获取状态信息
    val (pageState, isRefreshing, immediateReset) = rememberPaginatedStateInfo(listState)

    // 记录日志（如果需要）
    if (true) {
        Log.d("lilili", listState.toString())
        Log.d("lilili", "isRefreshing = $isRefreshing")
    }

    VoneRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (pageState is PageState.Content) {
                onRefresh.invoke()
            } else {
                onReload.invoke()
            }
        },
        immediateReset = immediateReset,
        enabled = listState.canRefresh(),
        modifier = modifier
    ) {
        StatePage<PaginatedListData<T>>(
            pageState = pageState,
            actions = StatePageActions(
                onRetry = onReload
            )
        ) { paginatedData ->
            PaginatedLazyList(
                items = paginatedData.items,
                loadState = paginatedData.paginationState,
                onLoadMore = onLoadMore,
                itemContent = itemContent,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PullToRefreshPaginatedContentListStatePage1(
    modifier: Modifier = Modifier,
    listState: SealedListState<T>,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    itemContent: @Composable (T) -> Unit
) {

    val pageState = listState.toPageState()

    // 下拉刷新组件包装分页列表
    VoneRefreshLayout(
        isRefreshing = listState is SealedListState.Success && listState.isRefreshing,
        onRefresh = onRefresh,
        enabled = pageState.isContent || pageState.isError || pageState.isEmpty,
        modifier = modifier
    ) {

        StatePage<PaginatedListData<T>>(
            pageState = pageState,
            actions = StatePageActions(
                onRetry = onRefresh,
            )
        ) { data ->
            PaginatedLazyList(
                items = data.items,
                loadState = data.paginationState,
                onLoadMore = onLoadMore,
                itemContent = itemContent
            )
        }
    }
}


/**
 * 从 SealedListState 中提取所需的状态信息，优化重组性能。
 *
 * @param listState 密封列表状态
 * @return 三元组，包含 (PageState, isRefreshing, immediateReset)
 */
@Composable
fun <T> rememberPaginatedStateInfo(
    listState: SealedListState<T>
): Triple<PageState<PaginatedListData<T>>, Boolean, Boolean> {
    return remember(listState) {
        val pageState = when (listState) {
            is SealedListState.Loading -> PageState.Loading
            is SealedListState.Empty -> PageState.Empty(listState.message)
            is SealedListState.Error -> PageState.Error(listState.message, listState.throwable)
            is SealedListState.Success -> {
                val data = PaginatedListData(
                    items = listState.items,
                    isRefreshing = listState.isRefreshing,
                    paginationState = listState.paginationState,
                    isLastPage = listState.isLastPage
                )
                PageState.Content(data)
            }
        }

        val isRefreshing = (listState as? SealedListState.Success)?.isRefreshing == true
        val immediateReset = pageState is PageState.Empty || pageState is PageState.Error

        Triple(pageState, isRefreshing, immediateReset)
    }
}
