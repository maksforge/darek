package app.darek.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.darek.MainApplication
import app.darek.data.AppPreferences
import app.darek.tip.TipJar
import app.darek.tip.tipAskDue
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class HomeUiState(
    /** False wherever Play will not sell a tip: nothing is asked for that cannot be bought (ADR-0007). */
    val showTipAsk: Boolean = false,
)

class HomeViewModel(
    private val preferences: AppPreferences,
    private val tipJar: TipJar,
    private val installedAt: Instant,
) : ViewModel() {
    val state: StateFlow<HomeUiState> =
        combine(preferences.tipAnsweredUnder, tipJar.offers) { answeredUnder, offers ->
            HomeUiState(
                showTipAsk = offers.isNotEmpty() && tipAskDue(answeredUnder, installedAt, Instant.now()),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // Prices are asked for only once the ask could be due, which saves a product query on every
        // other visit. The binding itself happens on every foreground anyway, to settle tips.
        viewModelScope.launch {
            if (tipAskDue(preferences.tipAnsweredUnder.first(), installedAt, Instant.now())) tipJar.refresh()
        }
    }

    /** Either answer, *Leave a tip* or *No thanks*, ends the ask on this install. */
    fun answerTipAsk() {
        viewModelScope.launch { preferences.markTipAnswered(installedAt) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] as MainApplication
                    HomeViewModel(app.container.preferences, app.container.tipJar, app.container.installedAt)
                }
            }
    }
}
