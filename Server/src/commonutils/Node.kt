package commonutils

class Node(
    var id: Int,
    var flags: MutableSet<String>,
    var longitude: Double,
    var latitude: Double,
) {
    fun getFlag(key: String): Boolean = key in flags

    fun setFlags(key: String) {
        flags.add(key)
    }

    fun removeFlag(key: String) {
        flags.remove(key)
    }
}
