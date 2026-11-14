package app.crowdhub.core.browser

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WebSessionState {
    UNKNOWN,
    LOGGED_IN,
    LOGGED_OUT
}

class SessionMonitor(
    private val isLoginUrlPredicate: (String) -> Boolean
) {
    private val _state = MutableStateFlow(WebSessionState.UNKNOWN)
    val state: StateFlow<WebSessionState> = _state.asStateFlow()

    fun onPageVisited(url: String?) {
        if (url.isNullOrBlank()) return
        _state.value = if (isLoginUrlPredicate(url)) {
            WebSessionState.LOGGED_OUT
        } else {
            WebSessionState.LOGGED_IN
        }
    }

    fun reset() {
        _state.value = WebSessionState.UNKNOWN
    }
}
