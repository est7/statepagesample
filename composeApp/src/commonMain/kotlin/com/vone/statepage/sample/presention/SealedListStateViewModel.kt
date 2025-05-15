package com.vone.statepage.sample.presention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vone.statepage.lib.samplerefreshpaginateduistate.PaginationState
import com.vone.statepage.lib.samplerefreshpaginateduistate.SealedListState
import com.vone.statepage.sample.data.ContactsRepository
import com.vone.statepage.sample.data.ContactsService
import com.vone.statepage.sample.data.SampleUserInfo
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.plus as immutablePlus

class SealedListStateViewModel : ViewModel() {
    // Dependencies
    private val repository = ContactsRepository.getSingleInstance()

    // Pagination state
    private var currentPage = 1
    private var currentId = ""

    // UI state
    private val _uiState = MutableStateFlow<SealedListState<SampleUserInfo>>(
        SealedListState.Loading
    )
    val uiState = _uiState.asStateFlow()

    /**
     * 定义不同的数据加载类型
     */
    enum class LoadingType {
        /** 初始加载或重新加载（显示全屏加载状态） */
        INITIAL_LOAD,

        /** 下拉刷新（保留当前内容，显示刷新指示器） */
        PULL_TO_REFRESH,

        /** 加载更多（保留当前内容，在底部显示加载指示器） */
        PAGINATION
    }

    // Public API

    /**
     * 初始化数据加载
     * @param id 数据ID
     */
    fun initDataById(id: String) {
        val isNewId = currentId != id
        if (isNewId) {
            this.currentId = id
            this.currentPage = 1
            _uiState.value = SealedListState.Loading
        }

        fetchData(
            loadingType = if (isNewId) LoadingType.INITIAL_LOAD else LoadingType.PULL_TO_REFRESH
        )
    }

    /**
     * 重新加载数据 - 用于从 Empty 或 Error 状态恢复
     * 会先将状态设置为 Loading，显示加载页面
     */
    fun reload() {
        currentPage = 1
        fetchData(loadingType = LoadingType.INITIAL_LOAD)
    }

    /**
     * 刷新数据 - 用于已有内容时的下拉刷新
     * 保持当前内容显示，同时显示刷新指示器
     */
    fun pullToRefresh() {
        if (!_uiState.value.canRefresh()) return
        currentPage = 1
        fetchData(loadingType = LoadingType.PULL_TO_REFRESH)
    }

    /**
     * 加载更多数据 - 用于分页加载
     * 保持当前内容显示，在底部显示加载指示器
     */
    fun loadMore() {
        val currentState = _uiState.value as? SealedListState.Success<SampleUserInfo>
        if (currentState?.canLoadMore() != true) return

        fetchData(loadingType = LoadingType.PAGINATION)
    }

    // Data loading

    /**
     * 获取数据
     * @param loadingType 加载类型，决定了UI状态的变化方式
     */
    private fun fetchData(loadingType: LoadingType) {
        viewModelScope.launch {
            // 更新UI状态以反映正在进行的加载操作
            applyLoadingState(loadingType)

            // 执行数据加载
            repository.loadDataById(currentId, currentPage)
                .collect { result ->
                    result.fold(
                        onSuccess = { data ->
                            handleSuccessResult(data, loadingType)
                        },
                        onFailure = { exception ->
                            handleErrorResult(exception, loadingType)
                        }
                    )
                }
        }
    }

    // State management

    /**
     * 根据加载类型应用相应的加载状态
     * @param loadingType 加载类型
     */
    private fun applyLoadingState(loadingType: LoadingType) {
        when (loadingType) {
            LoadingType.INITIAL_LOAD -> {
                // 设置 Loading 状态
                _uiState.value = SealedListState.Loading
            }

            LoadingType.PULL_TO_REFRESH -> {
                // 下拉刷新：保持当前内容，显示刷新指示器
                val currentState = _uiState.value as? SealedListState.Success<SampleUserInfo>
                currentState?.let { state ->
                    _uiState.value = state.copy(isRefreshing = true)
                }
            }

            LoadingType.PAGINATION -> {
                // 加载更多：保持当前内容，更新分页状态为加载中
                val currentState = _uiState.value as? SealedListState.Success<SampleUserInfo>
                currentState?.let { state ->
                    _uiState.value = state.copy(
                        paginationState = PaginationState.Loading
                    )
                }
            }
        }
    }

    /**
     * 处理成功加载的数据
     * @param data 加载的数据
     * @param loadingType 加载类型
     */
    private fun handleSuccessResult(data: PersistentList<SampleUserInfo>, loadingType: LoadingType) {
        when (loadingType) {
            LoadingType.INITIAL_LOAD, LoadingType.PULL_TO_REFRESH -> {
                // 初始加载或下拉刷新：替换现有数据
                handleFirstPageResult(data)
            }

            LoadingType.PAGINATION -> {
                // 加载更多：追加数据
                handlePaginationResult(data)
            }
        }

        // 更新页码，为下次加载做准备
        currentPage++
    }

    /**
     * 处理第一页数据结果（初始加载或刷新）
     * @param data 加载的数据
     */
    private fun handleFirstPageResult(data: PersistentList<SampleUserInfo>) {
        if (data.isEmpty()) {
            // 数据为空，显示空状态
            _uiState.value = SealedListState.Empty()
            return
        }

        val isLastPage = data.size < ContactsService.pageSize

        // 更新为成功状态，包含新数据
        _uiState.value = SealedListState.Success(
            items = data,
            paginationState = if (isLastPage) PaginationState.Complete else PaginationState.Idle,
            isLastPage = isLastPage,
            isRefreshing = false // 确保刷新指示器关闭
        )
    }

    /**
     * 处理分页加载结果（加载更多）
     * @param data 加载的数据
     */
    private fun handlePaginationResult(data: PersistentList<SampleUserInfo>) {
        val currentState = _uiState.value as? SealedListState.Success<SampleUserInfo>

        currentState?.let { state ->
            // 将新数据追加到现有数据
            val updatedItems = state.items.immutablePlus(data)
            val isLastPage = data.size < ContactsService.pageSize

            // 更新状态，保留现有数据并追加新数据
            _uiState.value = state.copy(
                items = updatedItems,
                paginationState = if (isLastPage) PaginationState.Complete else PaginationState.Idle,
                isLastPage = isLastPage
            )
        }
    }

    /**
     * 处理加载错误
     * @param exception 发生的异常
     * @param loadingType 加载类型
     */
    private fun handleErrorResult(exception: Throwable, loadingType: LoadingType) {
        when (loadingType) {
            LoadingType.INITIAL_LOAD -> {
                // 初始加载错误：显示错误页面
                _uiState.value = SealedListState.Error(
                    message = exception.message ?: "加载失败",
                    throwable = exception
                )
            }

            LoadingType.PULL_TO_REFRESH -> {
                // 下拉刷新错误：保留现有数据，关闭刷新指示器，显示错误提示
                val currentState = _uiState.value as? SealedListState.Success<SampleUserInfo>
                if (currentState != null) {
                    // 有现有数据，保留并更新状态
                    _uiState.value = currentState.copy(
                        isRefreshing = false,
                        paginationState = PaginationState.Error(exception.message ?: "刷新失败")
                    )
                } else {
                    // 没有现有数据，显示错误页面
                    _uiState.value = SealedListState.Error(
                        message = exception.message ?: "刷新失败",
                        throwable = exception
                    )
                }
            }

            LoadingType.PAGINATION -> {
                // 加载更多错误：保留现有数据，更新分页状态为错误
                val currentState = _uiState.value as? SealedListState.Success<SampleUserInfo>
                currentState?.let { state ->
                    _uiState.value = state.copy(
                        paginationState = PaginationState.Error(exception.message ?: "加载更多失败")
                    )
                }
            }
        }
    }
}
