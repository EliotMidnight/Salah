package com.example.data.model

import androidx.compose.runtime.Immutable

/**
 * One verse, identified the way a reader identifies one.
 *
 * ### Why this exists at all
 *
 * The reader used to hold a bare `selectedAyah: Int` and ask
 * `ayah.ayahNumber == selected`. That is ambiguous, and not in the abstract - it
 * is ambiguous on a real page. Page 604 holds Al-Ikhlas, Al-Falaq and An-Nas, so
 * ayah 1 exists three times on it; page 285 crosses from Al-Isra into Al-Kahf.
 * Selecting "verse 3" on either highlighted *two* verses, and the copy and
 * bookmark actions then fired on whichever one happened to be first in the
 * list. A verse is only a verse when the surah is part of the identity.
 *
 * ### Why the page is carried along
 *
 * A page number is not derivable from a reference without a corpus lookup, and
 * the two readers of this value - the chrome that names where you are, and the
 * persistence that puts you back there - both need it on every frame. Carrying
 * it means the page shown in the reader, the page written to storage and the page
 * a bookmark resolves to are the same number by construction, with no lookup
 * that could disagree.
 */
@Immutable
data class QuranRef(
    val surah: Int,
    val ayah: Int,
    val page: Int
) {
    /** The same verse, re-pointed at another page. Used when a page is turned. */
    fun onPage(page: Int): QuranRef = copy(page = page)

    // There was an `atAyah` here too, `copy(ayah = ...)`. It documented a distinction
    // that turned out not to be one: every caller that moved within a surah was going
    // through the reader, which owns the position, so nobody was calling it and nothing
    // needed it.

    /** `2:255` - how a reference is written when there is no room for more. */
    override fun toString(): String = "$surah:$ayah"

    companion object {
        /** Al-Fatihah, verse 1, on page 1: the first frame of a first run. */
        val Start = QuranRef(surah = 1, ayah = 1, page = 1)
    }
}

/**
 * A place in the mushaf that is not a single verse.
 *
 * Used by the browse surfaces, which answer "where do I go" for a page, a juz'
 * or a hizb just as much as for a surah, and by the reader's own chrome, which
 * has to be able to say "page 285" without inventing a verse that is not there.
 *
 * [verse] is always the *first* verse of the place, which is what a reader
 * expects when they arrive: the opening line of the page, the opening verse of
 * the juz', the start of the surah.
 */
@Immutable
data class QuranPlace(
    val kind: Kind,
    val number: Int,
    val verse: QuranRef
) {
    enum class Kind { PAGE, JUZ, HIZB, SURAH }
}
