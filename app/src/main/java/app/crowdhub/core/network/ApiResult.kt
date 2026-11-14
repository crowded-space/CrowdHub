package app.crowdhub.core.network

sealed class ApiResult<out T> {
    data class Success<T>(
        val data: T,
        val fetchedAtMs: Long,
        val fromCache: Boolean = false,
        val isStale: Boolean = false
    ) : ApiResult<T>()

    data class Failure<T>(
        val error: ApiError,
        val staleData: T? = null,
        val staleFetchedAtMs: Long? = null
    ) : ApiResult<T>()

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> staleData
    }
}
