package app.darek.listen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Runs [block] for the latest value asked for, one run at a time, so whatever burst of [run],
 * [change] and [stop] calls arrives from whichever threads, the last one is what ends up running. A
 * new value waits for the old run to finish cancelling. [ended] follows every run; a run that
 * returns on its own is forgotten before it, so a [run] of the same value starts it again.
 */
class LatestRun<T : Any>(
    scope: CoroutineScope,
    private val block: suspend (T) -> Unit,
    private val ended: () -> Unit,
) {
    // Compared by identity, so a request for a value that was superseded and asked for again is new.
    private class Request<T>(
        val value: T,
    )

    private val wanted = MutableStateFlow<Request<T>?>(null)

    init {
        scope.launch {
            wanted.collectLatest { request ->
                // Superseded while the old run was still finishing: never started, so nothing ends.
                if (request == null || wanted.value !== request) return@collectLatest
                try {
                    block(request.value)
                    wanted.compareAndSet(request, null)
                } finally {
                    ended()
                }
            }
        }
    }

    /** Asked for while it is already running, a value keeps its run. */
    fun run(value: T) = wanted.update { if (it?.value == value) it else Request(value) }

    /** Restarts a run with the changed value; while nothing runs, nothing starts. */
    fun change(transform: (T) -> T) =
        wanted.update { request ->
            request?.let { transform(it.value) }?.let { if (it == request.value) request else Request(it) }
        }

    fun stop() {
        wanted.value = null
    }
}
