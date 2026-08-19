package cc.lanternmc.materiallauncher.core.util

/**
 * 存在则按条件替换，不存在则追加到末尾
 */
inline fun <T> MutableList<T>.upsert(item: T, predicate: (T) -> Boolean): MutableList<T> = apply {
    val idx = indexOfFirst(predicate)
    if (idx >= 0) set(idx, item) else add(item)
}