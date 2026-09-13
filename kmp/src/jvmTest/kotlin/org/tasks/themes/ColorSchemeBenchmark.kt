package org.tasks.themes

import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme
import com.materialkolor.palettes.TonalPalette
import org.junit.Ignore
import org.junit.Test
import org.tasks.kmp.org.tasks.themes.ColorProvider

/**
 * Not a correctness test and not part of CI - it measures wall clock time, which is exactly the
 * kind of thing that makes a build flaky. Run it explicitly:
 *
 *     ./gradlew :kmp:jvmTest --tests '*ColorSchemeBenchmark*' -i
 *
 * These are the pure-Kotlin colour costs the task list pays while scrolling. Every row hosts its
 * own composition for the chip row and gets a fresh setContent lambda, so TasksTheme cannot skip
 * recomposition and calls dynamicColorScheme once per row; every chip resolves a tone through HCT.
 *
 * Device frame timing is a separate exercise - this only shows how expensive the underlying calls
 * are, so the claim that they are worth avoiding can be checked rather than assumed.
 */
@Ignore("Timing benchmark - run explicitly, see class doc")
class ColorSchemeBenchmark {

    @Test
    fun colorCosts() {
        val seed = ColorProvider.BLUE_500
        val chipColor = 0xFF3F51B5.toInt()

        report("dynamicColorScheme (once per row)") {
            dynamicColorScheme(seedColor = Color(seed), isDark = false)
        }
        report("TonalPalette.fromInt().tone() (once per chip)") {
            TonalPalette.fromInt(chipColor).tone(30)
        }
        report("contentColor (once per chip)") { contentColor(chipColor) }
    }

    private fun report(label: String, block: () -> Any?) {
        repeat(WARMUP) { block() }
        val start = System.nanoTime()
        repeat(ITERATIONS) { block() }
        val perCallMicros = (System.nanoTime() - start).toDouble() / ITERATIONS / 1_000.0
        println("BENCH | $label: ${"%.2f".format(perCallMicros)} us/call")
    }

    private companion object {
        const val WARMUP = 2_000
        const val ITERATIONS = 20_000
    }
}
