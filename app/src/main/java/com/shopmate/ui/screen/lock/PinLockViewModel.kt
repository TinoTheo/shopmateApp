package com.shopmate.ui.screen.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.domain.repository.StaffRepository
import com.shopmate.domain.repository.TillStaff
import com.shopmate.domain.util.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PinLockUiState(
    val staff: List<TillStaff> = emptyList(),
    val selected: TillStaff? = null,
    val entered: String = "",
    val wrong: Boolean = false,
    val checking: Boolean = false,
    /** Seconds left before another try, after [MAX_TRIES] wrong ones in a row. */
    val waitSeconds: Int = 0,
)

private const val MAX_TRIES = 5
private const val WAIT_SECONDS = 30

/** "Who's selling?" — tap your name, enter your PIN (CLAUDE.md § Staff). Checked on the device. */
@HiltViewModel
class PinLockViewModel @Inject constructor(
    private val staffRepository: StaffRepository,
) : ViewModel() {

    private val local = MutableStateFlow(PinLockUiState())
    private var wrongInARow = 0

    val uiState: StateFlow<PinLockUiState> = combine(local, staffRepository.staff) { state, staff ->
        // One person on the till: skip the name step.
        state.copy(staff = staff, selected = state.selected ?: staff.singleOrNull())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PinLockUiState())

    fun select(person: TillStaff) = local.update { it.copy(selected = person, entered = "", wrong = false) }

    fun back() = local.update { it.copy(selected = null, entered = "", wrong = false) }

    fun digit(d: String) {
        val state = uiState.value
        if (d == "." || state.checking || state.waitSeconds > 0) return
        if (state.entered.length >= PinHasher.PIN_LENGTH) return
        val entered = state.entered + d
        local.update { it.copy(entered = entered, wrong = false) }
        if (entered.length == PinHasher.PIN_LENGTH) check(entered)
    }

    fun backspace() = local.update { it.copy(entered = it.entered.dropLast(1), wrong = false) }

    private fun check(pin: String) {
        val person = uiState.value.selected ?: return
        viewModelScope.launch {
            local.update { it.copy(checking = true) }
            val ok = staffRepository.unlock(person.id, pin)
            if (ok) {
                wrongInARow = 0
                local.value = PinLockUiState()
                return@launch
            }
            wrongInARow++
            local.update { it.copy(checking = false, entered = "", wrong = true) }
            if (wrongInARow >= MAX_TRIES) {
                wrongInARow = 0
                for (left in WAIT_SECONDS downTo 1) {
                    local.update { it.copy(waitSeconds = left) }
                    delay(1_000)
                }
                local.update { it.copy(waitSeconds = 0, wrong = false) }
            }
        }
    }
}
