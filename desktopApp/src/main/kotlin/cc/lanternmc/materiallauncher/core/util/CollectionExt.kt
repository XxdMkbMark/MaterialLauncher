package cc.lanternmc.materiallauncher.core.util

/**
 * 存在则按条件替换，不存在则追加到末尾
 */
inline fun <T> MutableList<T>.upsert(item: T, predicate: (T) -> Boolean): MutableList<T> = apply {
    val idx = indexOfFirst(predicate)
    if (idx >= 0) set(idx, item) else add(item)
}

fun List<Int>.compareWithPaddedZero(other: List<Int>): Int {
    val count = maxOf(size, other.size)
    for (i in 0 until count) {
        val va = getOrElse(i) { 0 }
        val vb = other.getOrElse(i) { 0 }
        if (va != vb) return va - vb
    }
    return 0
}