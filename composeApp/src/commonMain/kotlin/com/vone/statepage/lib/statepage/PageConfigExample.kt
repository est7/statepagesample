package com.vone.statepage.lib.statepage

// 示例全局配置：
/*
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val globalCfg = statePageConfig {
            loading = { /* 或许可以自定义骨架屏 */ SkeletonScreen() }
            empty = { retry -> EmptyIllustration(onClick = retry) }
            error = { throwable, retry ->
                ErrorIllustration(message = throwable.message ?: "Error", onClick = retry)
            }
            custom("maintenance") { _, _ -> MaintenancePage() }
        }

        setContent {
            ProvideStatePageConfig(globalCfg) {
                RootNavGraph()
            }
        }
    }
}
*/
/*
@Composable
fun ArticleListScreen(viewModel: ArticleVM = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    // 仅此页面需要定制 Empty 状态
    val pageSpecific = statePageConfig {
        empty = { retry ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.List, contentDescription = null)
                Text("No Articles Yet")
                Button(onClick = retry) { Text("Refresh") }
            }
        }
    }

    StatePage(
        state  = uiState,
        overrideConfig = pageSpecific,      // ← 局部覆盖
        onRetry = viewModel::refresh
    ) { list: List<Article> ->
        ArticleLazyColumn(list)
    }
}

@HiltViewModel
class ArticleVM @Inject constructor(
    private val repo: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ViewState<List<Article>>>(ViewState.Loading)
    val uiState: StateFlow<ViewState<List<Article>>> = _uiState

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _uiState.value = ViewState.Loading
        runCatching { repo.fetchArticles() }
            .onSuccess { list ->
                _uiState.value =
                    if (list.isEmpty()) ViewState.Empty else ViewState.Content(list)
            }
            .onFailure { e ->
                _uiState.value = ViewState.Error(e)
            }
    }
}
*/
