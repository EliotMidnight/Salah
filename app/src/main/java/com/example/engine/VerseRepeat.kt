package com.example.engine

/**
 * How many times the verse the reader tapped is recited before the chain moves
 * on.
 *
 * One, three, five, or until told to stop. The count is about *this sitting*,
 * not about the reader: a habit of looping a verse is said-recitation-specific,
 * so unlike the reciter and the speed this is session state and no preference
 * is written. And one is the default, because the chain's whole point is
 * moving through the surah - a reader who never asked for repeats must never
 * receive one.
 */
enum class VerseRepeat(val count: Int) {
    ONCE(1),
    THRICE(3),
    FIVE_TIMES(5),

    /**
     * Until [QuranAudioPlayer.stop] or a given-away focus claim ends it. The
     * count is unbounded, which the state machine carries as "never satisfied":
     * no completion ever counts down to the next verse while the reader wants
     * this one to remain.
     */
    LOOP(-1);

    /**
     * More plays the verse the reader asked for still owes, after the play now
     * sounding ends. Zero is "one and done"; the loop's answer is a negative
     * that no completion can spend.
     *
     * The machine needs the *remaining* number, not the choice, because the
     * question at a verse's end is "again or on?" - a question about what is
     * left, not what was chosen.
     */
    internal val playsRemaining: Int
        get() = when (this) {
            ONCE -> 0
            THRICE -> 2
            FIVE_TIMES -> 4
            LOOP -> -1
        }

    /**
     * The reader's next choice, one step round the [count]s.
     *
     * Pure, like the speed's cycle: the pill asks, the session state answers.
     */
    fun next(): VerseRepeat {
        val index = entries.indexOf(this)
        return entries[(index + 1) % entries.size]
    }
}
