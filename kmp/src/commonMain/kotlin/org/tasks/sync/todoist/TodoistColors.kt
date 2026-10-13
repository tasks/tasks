package org.tasks.sync.todoist

object TodoistColors {
    private const val OPAQUE = 0xFF000000.toInt()

    private const val CHARCOAL = "charcoal"
    private const val GREY = "grey"
    private const val TAUPE = "taupe"

    const val DEFAULT_NAME = CHARCOAL

    private val PALETTE = linkedMapOf(
        "berry_red" to 0xb8256f,
        "red" to 0xdb4035,
        "orange" to 0xff9933,
        "yellow" to 0xfad000,
        "olive_green" to 0xafb83b,
        "lime_green" to 0x7ecc49,
        "green" to 0x299438,
        "mint_green" to 0x6accbc,
        "teal" to 0x158fad,
        "sky_blue" to 0x14aaf5,
        "light_blue" to 0x96c3eb,
        "blue" to 0x4073ff,
        "grape" to 0x884dff,
        "violet" to 0xaf38eb,
        "lavender" to 0xeb96eb,
        "magenta" to 0xe05194,
        "salmon" to 0xff8d85,
        CHARCOAL to 0x808080,
        GREY to 0xb8b8b8,
        TAUPE to 0xccac93,
    )

    fun toArgb(name: String): Int? = PALETTE[name]?.let { it or OPAQUE }

    private val ACHROMATIC = setOf(CHARCOAL, GREY, TAUPE)

    private const val CHROMA_THRESHOLD = 32

    fun toName(argb: Int): String {
        if (argb == 0) {
            return DEFAULT_NAME
        }
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val chroma = maxOf(r, g, b) - minOf(r, g, b)
        val candidates = if (chroma < CHROMA_THRESHOLD) {
            PALETTE.filterKeys { it in ACHROMATIC }
        } else {
            PALETTE
        }
        var best = DEFAULT_NAME
        var bestDistance = Int.MAX_VALUE
        candidates.forEach { (name, rgb) ->
            val dr = ((rgb shr 16) and 0xFF) - r
            val dg = ((rgb shr 8) and 0xFF) - g
            val db = (rgb and 0xFF) - b
            val distance = dr * dr + dg * dg + db * db
            if (distance < bestDistance) {
                bestDistance = distance
                best = name
            }
        }
        return best
    }
}
