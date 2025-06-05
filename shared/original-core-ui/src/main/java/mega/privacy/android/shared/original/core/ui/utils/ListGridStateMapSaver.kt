package mega.privacy.android.shared.original.core.ui.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import mega.privacy.android.shared.original.core.ui.model.ListGridState
import mega.privacy.android.shared.original.core.ui.utils.ListGridStateMap.Companion.Saver

/**
 * The default [Saver] implementation for both LazyGridState and LazyListState.
 */
class ListGridStateMap {
    companion object {
        private const val LIST_GRID_MAP_ENTRY_SIZE = 5

        /**
         * Custom [Saver] for Pair of [LazyListState] and [LazyGridState].
         */
        val Saver: Saver<MutableState<Map<Long, ListGridState>>, *> = Saver(
            save = {
                mutableListOf<Any?>().apply {
                    it.value.forEach {
                        add(it.key)
                        add(it.value.lazyListState.firstVisibleItemIndex)
                        add(it.value.lazyListState.firstVisibleItemScrollOffset)
                        add(it.value.lazyGridState.firstVisibleItemIndex)
                        add(it.value.lazyGridState.firstVisibleItemScrollOffset)
                    }
                }
            },
            restore = { list ->
                val map = mutableMapOf<Long, ListGridState>()
                check(list.size.rem(LIST_GRID_MAP_ENTRY_SIZE) == 0)
                var index = 0
                while (index < list.size) {
                    val key = list[index] as Long
                    val listState = LazyListState(
                        firstVisibleItemIndex = list[index + 1] as Int,
                        firstVisibleItemScrollOffset = list[index + 2] as Int
                    )
                    val gridState = LazyGridState(
                        firstVisibleItemIndex = list[index + 3] as Int,
                        firstVisibleItemScrollOffset = list[index + 4] as Int
                    )
                    map[key] = ListGridState(listState, gridState)
                    index += LIST_GRID_MAP_ENTRY_SIZE
                }
                mutableStateOf(map.toMap())
            }
        )
    }
}

/**
 * Sync the [ListGridState] map with the opened folder node handles and the current node handle
 */
fun Map<Long, ListGridState>.sync(
    openedHandles: Set<Long>,
    currentHandle: Long,
) = filterKeys { openedHandles.contains(it) || it == currentHandle }
    .toMutableMap()
    .apply {
        if (!containsKey(currentHandle)) {
            this[currentHandle] = ListGridState()
        }
    }

/**
 * Sync the [ListGridState] map with the opened folder node handles array queue and the current node handle
 */
fun Map<Long, ListGridState>.sync(
    openedHandles: List<Long>,
    currentHandle: Long,
) = filterKeys { openedHandles.contains(it) || it == currentHandle }
    .toMutableMap()
    .apply {
        if (!containsKey(currentHandle)) {
            this[currentHandle] = ListGridState()
        }
    }

/**
 * Get the [ListGridState] for the given node handle
 */
fun Map<Long, ListGridState>.getState(
    handle: Long,
) = this[handle] ?: ListGridState()