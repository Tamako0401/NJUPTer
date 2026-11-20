package com.example.njupter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.njupter.data.SettingsRepository
import com.example.njupter.data.campuspass.CampusPassRepository
import com.example.njupter.data.campuspass.CampusPassSnapshot
import com.example.njupter.data.campuspass.CampusPassToken
import com.example.njupter.data.campuspass.CampusPassTokenParser
import com.example.njupter.data.campuspass.CampusPassTokenRejectedException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface CampusPassUiState {
    data object Unbound : CampusPassUiState
    data object Loading : CampusPassUiState
    data class Error(val message: String, val tokenExpired: Boolean) : CampusPassUiState
    data class Ready(
        val token: CampusPassToken,
        val snapshot: CampusPassSnapshot
    ) : CampusPassUiState
}

class CampusPassViewModel(
    private val repository: CampusPassRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CampusPassUiState>(CampusPassUiState.Unbound)
    val uiState: StateFlow<CampusPassUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val token = settings.peekCampusPassToken()?.let(CampusPassTokenParser::parse)
        if (token == null) {
            _uiState.value = CampusPassUiState.Unbound
            return
        }
        if (token.expiresAtEpochSeconds * 1000 <= System.currentTimeMillis()) {
            _uiState.value = CampusPassUiState.Error(EXPIRED_MESSAGE, tokenExpired = true)
            return
        }
        _uiState.value = CampusPassUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                CampusPassUiState.Ready(token, repository.fetch(token))
            } catch (e: CampusPassTokenRejectedException) {
                CampusPassUiState.Error(e.message ?: EXPIRED_MESSAGE, tokenExpired = true)
            } catch (e: IOException) {
                CampusPassUiState.Error(e.message ?: NETWORK_ERROR_MESSAGE, tokenExpired = false)
            }
        }
    }

    /** Returns false when no valid token can be extracted from [input]. */
    fun bind(input: String): Boolean {
        val token = CampusPassTokenParser.parse(input) ?: return false
        viewModelScope.launch {
            settings.setCampusPassToken(token.raw)
            refresh()
        }
        return true
    }

    fun clearToken() {
        viewModelScope.launch {
            settings.setCampusPassToken(null)
            _uiState.value = CampusPassUiState.Unbound
        }
    }

    companion object {
        // Marker replaced by the localized campus_pass_expired copy when tokenExpired = true.
        const val EXPIRED_MESSAGE = "已过期"
        private const val NETWORK_ERROR_MESSAGE = "网络错误"

        fun provideFactory(
            repository: CampusPassRepository,
            settings: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CampusPassViewModel(repository, settings) as T
            }
        }
    }
}
