package com.example.njupter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.njupter.data.SettingsRepository
import com.example.njupter.data.morningrun.MiniProgramToken
import com.example.njupter.data.morningrun.MiniProgramTokenParser
import com.example.njupter.data.morningrun.MorningRunRepository
import com.example.njupter.data.morningrun.MorningRunSnapshot
import com.example.njupter.data.morningrun.TokenRejectedException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface MorningRunUiState {
    data object Unbound : MorningRunUiState
    data object Loading : MorningRunUiState
    data class Error(val message: String, val tokenExpired: Boolean) : MorningRunUiState
    data class Ready(
        val token: MiniProgramToken,
        val snapshot: MorningRunSnapshot
    ) : MorningRunUiState
}

class MorningRunViewModel(
    private val repository: MorningRunRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MorningRunUiState>(MorningRunUiState.Unbound)
    val uiState: StateFlow<MorningRunUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val token = settings.peekMorningRunToken()?.let(MiniProgramTokenParser::parse)
        if (token == null) {
            _uiState.value = MorningRunUiState.Unbound
            return
        }
        if (token.expiresAtEpochSeconds * 1000 <= System.currentTimeMillis()) {
            _uiState.value = MorningRunUiState.Error(EXPIRED_MESSAGE, tokenExpired = true)
            return
        }
        _uiState.value = MorningRunUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                MorningRunUiState.Ready(token, repository.fetch(token.raw))
            } catch (e: TokenRejectedException) {
                MorningRunUiState.Error(e.message ?: EXPIRED_MESSAGE, tokenExpired = true)
            } catch (e: IOException) {
                MorningRunUiState.Error(e.message ?: NETWORK_ERROR_MESSAGE, tokenExpired = false)
            }
        }
    }

    /** Returns false when no valid token can be extracted from [input]. */
    fun bind(input: String): Boolean {
        val token = MiniProgramTokenParser.parse(input) ?: return false
        viewModelScope.launch {
            settings.setMorningRunToken(token.raw)
            refresh()
        }
        return true
    }

    fun clearToken() {
        viewModelScope.launch {
            settings.setMorningRunToken(null)
            _uiState.value = MorningRunUiState.Unbound
        }
    }

    companion object {
        // Marker shown when the stored token's exp has passed; the screen replaces it with
        // the localized morning_run_expired copy when tokenExpired = true.
        const val EXPIRED_MESSAGE = "已过期"
        private const val NETWORK_ERROR_MESSAGE = "网络错误"

        fun provideFactory(
            repository: MorningRunRepository,
            settings: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MorningRunViewModel(repository, settings) as T
            }
        }
    }
}
