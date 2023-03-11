package com.simplemobiletools.draw.pro.extensions

fun <K, V> LinkedHashMap<K, V>.removeFirst(): Pair<K, V> {
    val key = keys.first()
    val value = values.first()
    remove(key)
    return key to value
}

fun <K, V> LinkedHashMap<K, V>.removeLast(): Pair<K, V> {
    val key = keys.last()
    val value = values.last()
    remove(key)
    return key to value
}

fun <K, V> LinkedHashMap<K, V>.removeLastOrNull(): Pair<K?, V?> {
    val key = keys.lastOrNull()
    val value = values.lastOrNull()
    remove(key)
    return key to value
}
