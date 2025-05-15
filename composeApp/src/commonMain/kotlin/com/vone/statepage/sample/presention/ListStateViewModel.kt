package com.vone.statepage.sample.presention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vone.statepage.lib.samplerefreshpaginateduistate.ContentState
import com.vone.statepage.lib.samplerefreshpaginateduistate.ListState
import com.vone.statepage.lib.samplerefreshpaginateduistate.PaginationState
import com.vone.statepage.sample.data.ContactsRepository
import com.vone.statepage.sample.data.ContactsService
import com.vone.statepage.sample.data.SampleUserInfo
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 *
 * @author: est8
 * @date: 5/6/25
 */
class ListStateViewModel : ViewModel() {
    // Dependencies
    private val repository = ContactsRepository.getSingleInstance()

    // Pagination state
    private var currentPage = 1
    private var currentId = ""

    // UI state
    private val _uiState = MutableStateFlow(ListState<SampleUserInfo>())
    val uiState = _uiState.asStateFlow()

    // Public API

    /**
     * 初始化数据加载
     * @param id 数据ID
     */
    fun initDataById(id: String) {
        if (currentId != id) {
            this.currentId = id
            this.currentPage = 1
        }
        loadData(isRefresh = true)
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        if (!_uiState.value.canRefresh()) return

        currentPage = 1
        loadData(isRefresh = true)
    }

    /**
     * 加载更多数据
     */
    fun loadMore() {
        if (!_uiState.value.canLoadMore()) return

        loadData(isRefresh = false)
    }

    // Data loading

    /**
     * 加载数据
     * @param isRefresh 是否为刷新操作
     */
    private fun loadData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            updateLoadingState(isRefresh)

            repository.loadDataById(currentId, currentPage)
                .catch { exception ->
                    handleError(exception, isRefresh)
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { data -> handleSuccess(data, isRefresh) },
                        onFailure = { exception -> handleError(exception, isRefresh) }
                    )
                }
        }
    }

    // State management

    /**
     * 更新加载状态
     */
    private fun updateLoadingState(isRefresh: Boolean) {
        if (isRefresh) {
            val currentState = _uiState.value
            val contentState = currentState.contentState
            if (contentState is ContentState.Success) {
                _uiState.update { state ->
                    state.copy(
                        contentState = currentState.contentState.copy(
                            isRefreshing = true
                        ),
                    )
                }
            }

        } else {
            _uiState.update { state ->
                state.copy(
                    paginationState = PaginationState.Loading
                )
            }
        }
    }

    /**
     * 处理成功加载的数据
     */
    private fun handleSuccess(data: PersistentList<SampleUserInfo>, isRefresh: Boolean) {
        if (isRefresh) {
            handleRefreshSuccess(data)
        } else {
            handleLoadMoreSuccess(data)
        }
        currentPage++ // 下次加载下一页
    }

    /**
     * 处理刷新成功的数据
     */
    private fun handleRefreshSuccess(data: PersistentList<SampleUserInfo>) {
        if (data.isEmpty()) {
            _uiState.update { state ->
                state.copy(
                    contentState = ContentState.Empty(),
                    paginationState = PaginationState.Idle
                )
            }
            return
        }

        val isLastPage = data.size < ContactsService.pageSize

        _uiState.update { state ->
            state.copy(
                contentState = ContentState.Success(
                    data = data.toList(),
                    isLastPage = isLastPage
                ),
                paginationState = if (isLastPage)
                    PaginationState.Complete
                else
                    PaginationState.Idle
            )
        }
    }

    /**
     * 处理加载更多成功的数据
     */
    private fun handleLoadMoreSuccess(data: PersistentList<SampleUserInfo>) {
        val currentState = _uiState.value

        if (currentState.contentState is ContentState.Success) {
            val currentData = currentState.contentState.data
            val updatedData = currentData + data.toList()
            val isLastPage = data.size < ContactsService.pageSize

            _uiState.update { state ->
                state.copy(
                    contentState = ContentState.Success(
                        data = updatedData,
                        isLastPage = isLastPage
                    ),
                    paginationState = if (isLastPage)
                        PaginationState.Complete
                    else
                        PaginationState.Idle
                )
            }
        }
    }

    /**
     * 处理加载错误
     */
    private fun handleError(exception: Throwable, isRefresh: Boolean) {
        if (isRefresh) {
            _uiState.update { state ->
                state.copy(
                    contentState = ContentState.Error(
                        message = exception.message ?: "未知错误",
                        throwable = exception,
                    )
                )
            }
        } else {
            // 如果是加载更多错误，只更新分页状态
            _uiState.update { state ->
                state.copy(
                    paginationState = PaginationState.Error(
                        message = exception.message ?: "加载更多失败",
                        throwable = exception
                    )
                )
            }
        }
    }
}

