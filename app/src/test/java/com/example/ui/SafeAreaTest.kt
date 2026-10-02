package com.example.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.components.SafeArea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

/**
 * The reader keeps its text out of the hardware.
 *
 * ### The two defects this file exists for
 *
 * Neither is visible in a screenshot, because both only appear in **landscape** on a
 * device with a display cutout - a configuration nobody tests and that a suite of
 * 25 portrait baselines cannot represent.
 *
 * **1. Four copies of the top inset, two of them wrong.** `ScreenScaffold` and
 * `ScreenTopBar` asked `WindowInsets.statusBars` for the top and nothing else, so on
 * a device whose camera hole reaches the top edge, every title in the app was
 * underneath it. The Today screen, the Quran reader and `PageInsets` each carried
 * their own copy of the *correct* `max(statusBars, cutout)` computation - so the two
 * screens that were supposed to agree about where content starts had two different
 * answers, and the right one was the one whose name never mentioned the cutout.
 *
 * **2. No sides at all, anywhere.** In portrait a cutout is at the top, so a
 * top-only answer is complete and nobody notices. Rotated, it moves to the left or
 * right edge and is 30-40dp deep against a 16dp design gutter - so the first column
 * of every page of Arabic was under the notch, and in right-to-left text the first
 * column is the *right* one, which is where a page **begins**. A reader lost the start
 * of the page: a wrong reading, not a crowded one.
 *
 * ### Why this tests arithmetic and not composition
 *
 * A Robolectric window reports no display cutout, so composing the real thing would
 * measure zero, pass, and prove nothing about a defect that only exists on hardware
 * with a hole in it. So the arithmetic is tested against [WindowInsets] doubles -
 * which is possible because `WindowInsets` is an interface - and the *choice of which
 * insets to ask for* is tested by reading the source.
 *
 * That split is not a convenience. The two halves fail differently: the first is a
 * bug in one function, and the second is a habit that spreads. Only the second is
 * catchable by a test, and only because it is a mechanical property of the tree.
 */
class SafeAreaTest {

    // --- The fixture, and its own guard ------------------------------------

    private val density = Density(3f)

    /**
     * A cutout that reaches the top edge and the left edge, and nothing on the right.
     *
     * Deeper than the status bar, which is the case `max` exists for and the one
     * `statusBars` alone gets wrong. Deliberately **asymmetric** left-to-right: a
     * symmetric fixture would let a broken implementation - one that reserved only
     * `getLeft`, say - pass.
     */
    private val cutout = FakeInsets(top = 96, left = 108, right = 0)
    private val statusBars = FakeInsets(top = 72)
    private val noHardware = FakeInsets(top = 0, left = 0, right = 0)

    @Test
    fun `the fixture reports what it says it reports`() {
        // A guard on the double itself. If `FakeInsets` ever stopped behaving like a
        // `WindowInsets`, every test below would be asserting against a fiction -
        // which is precisely the failure mode this class exists to catch, so it is
        // worth catching here too.
        assertEquals(
            "the fixture does not report the cutout it was built with",
            96,
            cutout.getTop(density)
        )
        assertEquals(
            "the fixture does not report the left edge it was built with",
            108,
            cutout.getLeft(density, LayoutDirection.Ltr)
        )
        assertEquals(
            "the fixture ignores the layout direction, so a direction-sensitive bug " +
                "would be hidden",
            0,
            cutout.getLeft(density, LayoutDirection.Rtl)
        )
    }

    // --- The top ----------------------------------------------------------

    @Test
    fun `the top reserve is the deeper of the status bar and the cutout`() {
        assertEquals(
            "a cutout deeper than the status bar was not reserved. Asking " +
                "`statusBars` alone gives 72 and puts the title under a 96px hole - " +
                "which is what ScreenScaffold and ScreenTopBar both did.",
            96,
            SafeArea.topPx(statusBars, cutout, density)
        )
    }

    @Test
    fun `the top reserve is the status bar when the cutout is shallower`() {
        val shallowCutout = FakeInsets(top = 12)
        assertEquals(
            "a cutout that does not reach the top must not shrink the reserve below " +
                "the status bar - that puts the title under the clock",
            72,
            SafeArea.topPx(statusBars, shallowCutout, density)
        )
    }

    @Test
    fun `the top reserve never shrinks to nothing`() {
        assertEquals(
            "the top reserve must be at least the status bar whatever the cutout does",
            72,
            SafeArea.topPx(statusBars, noHardware, density)
        )
    }

    @Test
    fun `the top reserve exceeds the status bar alone`() {
        // Stated as its own test because it is the shape of mistake that comes back:
        // the platform offers a direct, obvious `statusBars` getter, and it answers a
        // *smaller* question than the one being asked - which is not obvious at the
        // call site and is invisible until a title is clipped.
        val statusOnly = statusBars.getTop(density)
        val safe = SafeArea.topPx(statusBars, cutout, density)

        assertTrue(
            "SafeArea.topPx must exceed the status bar when the cutout is deeper; " +
                "if they are equal the cutout is not being reserved ($safe vs " +
                "$statusOnly)",
            safe > statusOnly
        )
    }

    // --- The sides, which had no implementation at all --------------------

    @Test
    fun `the side reserve is the deeper cutout edge, and it is non-zero`() {
        val sides = SafeArea.sidesPx(cutout, density, LayoutDirection.Ltr)

        assertEquals(
            "a cutout on the left edge was not reserved. Rotated, the outer column of " +
                "every page of Arabic sits under it - and in RTL that column is where " +
                "a page begins.",
            108,
            sides
        )
        assertTrue("the side reserve must be positive on a device with a cutout", sides > 0)
    }

    @Test
    fun `the side reserve takes the larger of the two edges`() {
        // The other edge is the deeper one this time, so a `getLeft`-only
        // implementation - which passes the test above - fails here.
        val rightSide = FakeInsets(top = 96, left = 0, right = 140)
        assertEquals(
            "only the left edge was read; a notch on the right is not reserved",
            140,
            SafeArea.sidesPx(rightSide, density, LayoutDirection.Ltr)
        )
    }

    @Test
    fun `the side reserve is the same in RTL as in LTR`() {
        // The mushaf lays its page out RTL *regardless of the interface language*, so
        // an implementation that resolved only one edge would give a different page
        // margin to an Arabic reader than to an English one - on the same device.
        val ltr = SafeArea.sidesPx(cutout, density, LayoutDirection.Ltr)
        val rtl = SafeArea.sidesPx(cutout, density, LayoutDirection.Rtl)

        assertEquals(
            "the side reserve changed with the layout direction ($ltr vs $rtl), so an " +
                "RTL reader gets a different page margin from the same device",
            ltr,
            rtl
        )
    }

    @Test
    fun `a device with no cutout reserves nothing at the sides`() {
        assertEquals(
            "a device with no cutout must be unchanged",
            0,
            SafeArea.sidesPx(noHardware, density, LayoutDirection.Ltr)
        )
    }

    // --- One implementation, and one caller of the platform ---------------

    @Test
    fun `only SafeArea asks the platform where the hardware is`() {
        // The half that a unit test of the arithmetic cannot see, and the half that
        // actually caused the defect: four files each asking the platform for the top
        // inset, two of them asking it the wrong question.
        //
        // It is a source test, and honestly so - this is a *habit*, not a behaviour,
        // and habits are only visible in the tree. The cost is that it breaks when
        // something legitimately reads an inset for another reason, which is why the
        // allowlist exists and is empty rather than populated in advance.
        val offenders = kotlinSources()
            .filterNot { it.endsWith(CANONICAL) }
            .filterNot { it.endsWith("SafeAreaTest.kt") }
            .filter { source ->
                val code = codeOnly(File(source).readText())
                INSET_READERS.any { reader ->
                    Regex("""\bWindowInsets\s*\.\s*$reader\b""").containsMatchIn(code)
                }
            }
            .map { File(it).name }

        assertTrue(
            "Files reading the platform's window insets directly:\n" +
                offenders.joinToString("\n") { "  $it" } +
                "\n\nSafeArea is the single answer to \"where is the hardware\". A second " +
                "copy is a second chance to ask a smaller question than the one being " +
                "asked - which is exactly how the top inset came to have two answers.",
            offenders.isEmpty()
        )
    }

    // --- The double -------------------------------------------------------

    /**
     * A `WindowInsets` with values chosen by the test.
     *
     * Resolves `getLeft`/`getRight` *against the layout direction*, because the real
     * implementation does and a double that ignored the argument would hide the
     * direction-sensitivity that the RTL/LTR test is written to catch.
     */
    private class FakeInsets(
        private val top: Int,
        private val left: Int = 0,
        private val right: Int = 0
    ) : WindowInsets {
        override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int =
            if (layoutDirection == LayoutDirection.Ltr) left else right

        override fun getTop(density: Density): Int = top

        override fun getRight(density: Density, layoutDirection: LayoutDirection): Int =
            if (layoutDirection == LayoutDirection.Ltr) right else left

        override fun getBottom(density: Density): Int = 0
    }

    // --- Source access ----------------------------------------------------

    /**
     * [source] with its comments removed.
     *
     * Necessary, and not as a nicety: this class *documents* the two accessors it
     * forbids, so a naive text search matches its own prose - as it first did, on
     * `Scaffold.kt` explaining where its insets now come from. A check that fires
     * on the sentence describing the rule is a check nobody can keep enabled.
     *
     * Line comments and the `*`-led lines of a KDoc block are all that Kotlin uses
     * here, so those are what comes out. A `//` inside a string literal would also
     * be removed, which is harmless: the only thing being looked for afterwards is
     * a qualified name, and no string in this app spells one.
     */
    private fun codeOnly(source: String): String = source
        .lines()
        .filterNot { line ->
            val trimmed = line.trimStart()
            trimmed.startsWith("*") || trimmed.startsWith("//")
        }
        .joinToString("\n") { line -> line.substringBefore("//") }

    private fun kotlinSources(): List<String> = listOf("src/main/java", "src/test/java")
        .flatMap { dir ->
            File(moduleRoot(), dir).walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .map { it.path }
                .toList()
        }

    /** Walks up from the working directory to the `app` module. */
    private fun moduleRoot(): String {
        var dir = File("").absoluteFile
        while (dir.parentFile != null) {
            if (File(dir, "build.gradle.kts").isFile) return dir.path
            dir = dir.parentFile
        }
        return File("").absolutePath
    }

    private companion object {
        const val CANONICAL = "SafeArea.kt"

        /**
         * The platform accessors that mean "where is the hardware".
         *
         * `ime` and `navigationBars` are deliberately absent: a caller that wants the
         * keyboard or the navigation bar is asking a *different* question, and
         * `SafeArea` is not the answer to it.
         */
        val INSET_READERS = listOf("statusBars", "displayCutout")
    }
}
