#!/usr/bin/env python3
"""Derive app/src/main/resources/quran/mushaf_layout.txt from digitized print data.

The asset encodes where every word of the bundled corpus sits on the KFGQPC V2
(1421H) Madinah mushaf: 604 pages, each page's lines in print order, each
line's words as a range of corpus word ordinals. Word text itself is NOT stored
- it is looked up from uthmani.txt at load, so the layout stays a fact about
geometry and the verse text has exactly one source.

Sources
-------
corpus    app/src/main/resources/quran/uthmani.txt - the bundled Tanzil Uthmani
          text (Hafs 'an 'Asim): the words the asset positions.
layout    quran.com's digitization of the KFGQPC V2 print, as distributed by
          github.com/zonetecde/mushaf-layout (branch v2, mushaf/QPC2/page-NNN.json);
          --fetch downloads the 604 files into --zpages.
centring  the Quranic Universal Library's "KFGQPC V2 layout" resource, via the
          public mirror github.com/blueheron786/quranic-universal-library-mushaf-layouts
          (qpc-v2-15-lines.db). Only its is_centered flag per printed row is
          read; its word-level encoding is not used.

Corpus tokenisation (before ordinals)
-------------------------------------
The corpus keeps pause marks and prostration signs as separate whitespace
tokens; the print glues them onto the PRECEDING word's entry. A token that
holds no letter is therefore fused onto the token before it. What remains are
the 77,881 letter words whose ordinals the asset stores, 1-based in corpus
order. (In this corpus the basmalah is the first four WORDS of every surah's
first ayah except At-Tawbah's, which is why the print's basmalah rows point at
(f, 1, 0..3).)

What is checked (the script aborts on any violation)
----------------------------------------------------
  * every print entry folds to the corpus's next word; entries carrying two of
    our words are the print's four known word-joins and nothing else;
  * every verse-end marker is appended to the entry that ends its verse
    (5,662 of 6,236) or stands alone at the start of the next printed line
    (574); a standalone marker always leads its line, follows a line that ends
    a verse - across a page break, or past a surah header and its basmalah row -
    and its digits equal the verse it closes;
  * every verse closed by an entry's trailing digits carries those digits, and
    a verse closed with none carries its marker on the next line;
  * 114 surah-name lines, in surah order, followed by that surah's words;
  * 112 basmalah rows - every surah but 1 and 9 - each the opener's four
    basmalah words;
  * pages 1 and 2 have 8 lines, the other 602 pages 15 (8,820 text lines);
  * every slot is present and in order on every page;
  * the walk's last word is the corpus's last word;
  * the emitted file re-parses to the same records;
  * page starts versus metadata.xml: 579 of the 604 agree; the 25 exceptions
    are the documented print-versus-Tanzil boundary attributions (printed).
"""

import argparse
import hashlib
import json
import re
import sqlite3
import sys
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
QURAN_DIR = REPO / "app" / "src" / "main" / "resources" / "quran"
DEFAULT_ZPAGES = Path("/home/eliot/.opencode2/tmp/opencode/zpages")
DEFAULT_MIRROR = Path("/home/eliot/.opencode2/tmp/opencode/qpc-v2-15-lines.db")
ASSET = QURAN_DIR / "mushaf_layout.txt"

QPC2_URL = ("https://raw.githubusercontent.com/zonetecde/mushaf-layout/"
            "refs/heads/v2/mushaf/QPC2/page-{page:03d}.json")
MIRROR_URL = ("https://github.com/blueheron786/quranic-universal-library-mushaf-layouts")

PAGES = 604
WORD_COUNT = 77881

# The known two-word joins: the print writes this (surah, ayah, word) pair of
# adjacent same-verse words as one entry. Checked exact, so a future refetch
# whose tokenisation changed fails here instead of silently shifting lines.
EXPECTED_JOINS = {(2, 181, 2), (8, 6, 3), (13, 37, 7), (37, 130, 2)}

# Marks and separators that exist on the page but not in the corpus's letter
# stream: the small combiners, the tatweel a justified rendering inserts, the
# prostration sign, the rub el hizb sign and the bidi controls the data
# carries. Pause marks and prostration signs are stripped here because the
# corpus keeps them as separate tokens and the print keeps them fused onto
# words - both sides are compared as bare words.
MARKS = re.compile(
    "[\u064B-\u065F\u0670\u06D6-\u06ED\u08F0-\u08F3\u0640\u06DE\u06E9"
    "\u200E\u200F\u202A-\u202E\u2066-\u2069]"
)
DIGITS = re.compile("[٠-٩]")
DIGITS_RUN = re.compile("[٠-٩]+")
LETTER = re.compile("[\u0621-\u064A\u0671-\u06D3]")

FOLD = str.maketrans({
    "أ": "ا", "إ": "ا", "آ": "ا", "ٱ": "ا", "ٲ": "ا", "ٳ": "ا",
    "ة": "ه", "ى": "ا", "ؤ": "ء", "ئ": "ء",
})

EAI = str.maketrans("٠١٢٣٤٥٦٧٨٩", "0123456789")

# The one mid-line standalone marker the print digitization carries, keyed by
# (page, slot, verse): page 589's line 13 holds ayah 84:21's marker after the
# verse's second word while the verse continues onto the next line. See
# consume_standalone for why this asset registers it at the verse's end.
EXPECTED_MIDLINE = [(589, 13, (84, 21))]
EXPECTED_MIDLINE_VERSES = {(84, 21)}


def folded(entry):
    """The corpus-side form of a print entry: marks, digits and gaps gone."""
    return MARKS.sub("", DIGITS.sub("", entry)).replace(" ", "").translate(FOLD)


def corpus_tokens():
    """{(s, a): [fused token, ...]} - the words the print renders, fused."""
    tokens = {}
    for line in (QURAN_DIR / "uthmani.txt").read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        surah, ayah, text = line.split("|", 2)
        fused = []
        for token in text.strip().split():
            if LETTER.search(token):
                fused.append(token)
            elif fused:
                # Pause marks and prostration signs ride on the word before
                # them, as the print sets them.
                fused[-1] += token
            else:
                raise AssertionError(f"{surah}:{ayah} opens with a bare mark {token!r}")
        tokens[(int(surah), int(ayah))] = fused
    return tokens


def word_stream(tokens):
    """[(s, a, w), ...]: the WORD_COUNT corpus words, in order."""
    return [(s, a, w) for (s, a), toks in sorted(tokens.items()) for w in range(len(toks))]


def page_starts_from_metadata():
    """metadata.xml's page-start verses, 1-indexed like the print's pages."""
    text = (QURAN_DIR / "metadata.xml").read_text(encoding="utf-8")
    starts = [(int(s), int(a))
              for s, a in re.findall(r'<page[^>]*?sura="(\d+)"[^>]*?aya="(\d+)"', text)]
    assert len(starts) == PAGES, f"metadata.xml lists {len(starts)} pages"
    return starts


def centre_flags(mirror_sqlite):
    """{(page, line): (kind, w)} for every row the QUL export lists."""
    with sqlite3.connect(mirror_sqlite) as con:
        rows = con.execute(
            "SELECT page_number, line_number, line_type, is_centered FROM pages"
        ).fetchall()
    assert len(rows) == 9046, f"mirror rows: {len(rows)}"
    flags = {}
    for page, line, kind, centered in rows:
        if kind not in ("ayah", "basmallah", "surah_name"):
            raise AssertionError(f"mirror row {page},{line}: {kind}")
        if kind != "ayah" and centered != 1:
            raise AssertionError(f"mirror row {page},{line}: {kind} not centred")
        flags[(page, line)] = centered
    return flags


class Walker:
    """Places every print entry on the corpus word stream, checking as it goes."""

    def __init__(self, tokens, stream, centre):
        self.tokens = tokens
        self.stream = stream
        self.centre = centre
        self.gi = 0
        self.pages = []
        self.joins = []
        self.standalone = []
        self.midline_markers = []
        # The most recent surah the print named. A header can print at the end
        # of a page whose basmalah row prints on the next one (page 76 names
        # Surah 4; page 77 opens with the row).
        self.surahNamed = None
        # A verse that ended without its printed marker: the marker must lead
        # the next printed text line. A surah header and its basmalah row can
        # sit between, so the pending marker survives them.
        self.pending = None

    def last_word(self):
        return self.stream[self.gi - 1]

    def consume_entry(self, page, slot, text):
        tokens, stream = self.tokens, self.stream
        digits = DIGITS_RUN.findall(text)
        assert len(digits) <= 1, f"p{page} L{slot}: several digit runs in {text!r}"
        s2, a2, w2 = stream[self.gi]
        ours = folded(tokens[(s2, a2)][w2])
        have = folded(text)
        if have == ours:
            self.gi += 1
        else:
            # The print writes a few of our word pairs as one entry: two
            # adjacent words of the SAME verse.
            s3, a3, w3 = stream[self.gi + 1]
            assert a3 == a2, f"p{page} L{slot}: a join crosses a verse"
            ours2 = folded(tokens[(s3, a2)][w3])
            assert have == ours + ours2, \
                f"p{page} L{slot}: {text!r} vs {s2}:{a2}#{w2}+{w3} ('{ours}', '{ours2}')"
            self.joins.append((s2, a2, w2))
            self.gi += 2

        if digits:
            closed = self.last_word()
            assert closed[2] == len(tokens[(closed[0], closed[1])]) - 1, \
                f"p{page} L{slot}: digits on {text!r} but the verse continues"
            value = int(digits[0].translate(EAI))
            assert value == closed[1], f"p{page} L{slot}: marker {value} closes verse {closed[1]}"
        elif self.last_word()[2] == len(tokens[(self.last_word()[0], self.last_word()[1])]) - 1:
            # The verse ended with no printed marker: it must lead the next
            # text line.
            self.pending = self.last_word()[:2]

    def consume_standalone(self, page, slot, text, index, location):
        if index == 0:
            # A marker on the following line keeps its own entry, and always
            # leads the line: the previous line ended a verse.
            closed = self.last_word()
            assert closed[2] == len(self.tokens[(closed[0], closed[1])]) - 1, \
                f"p{page} L{slot}: marker follows a word that continues a verse"
            digits = "".join(DIGITS_RUN.findall(text))
            value = int(digits.translate(EAI))
            assert value == closed[1], f"p{page} L{slot}: marker {value} closes verse {closed[1]}"
            self.standalone.append((page, slot))
            if self.pending is not None:
                assert self.pending == closed[:2], \
                    f"p{page} L{slot}: expected the marker of verse {self.pending}"
                self.pending = None
            return

        # One digitization artifact in the whole data: page 589's line 13
        # carries ayah 84:21's marker after the verse's second word, while the
        # verse itself continues onto the next line. The QUL export places the
        # marker where it belongs, at the verse's end, and a mid-verse marker
        # is not a print convention, so this asset registers the marker at the
        # verse's end - which the renderer derives alone from the verse's word
        # range on its line. The entry itself is not line content.
        surah, ayah = map(int, location.split(":")[:2])
        running = self.stream[self.gi]
        assert running[:2] == (surah, ayah) and running[2] > 0, \
            f"p{page} L{slot}: mid-line marker of {surah}:{ayah} not in a running verse"
        self.midline_markers.append((page, slot, (surah, ayah)))
        assert (page, slot) == EXPECTED_MIDLINE[0][:2], \
            f"p{page} L{slot}: unexpected mid-line marker of {surah}:{ayah}"

    def text_line(self, page, slot, entries, centered):
        assert entries, f"p{page} L{slot}: a text line with no words"
        start = self.gi
        for index, entry in enumerate(entries):
            text = entry["word"]
            if not LETTER.search(text):
                self.consume_standalone(page, slot, text, index, entry["location"])
                continue
            self.consume_entry(page, slot, text)
        assert self.gi > start, f"p{page} L{slot}: a text line with only a marker"
        flags = ""
        if DIGITS_RUN.fullmatch(entries[0]["word"]):
            flags += "w"
        if centered:
            flags += "c"
        return start, self.gi - 1, flags

    def run(self, zpages):
        for page_number in range(1, PAGES + 1):
            page = json.loads((zpages / f"page-{page_number:03d}.json").read_text("utf-8"))
            records = []
            for line in page["lines"]:
                kind = line["type"]
                slot = line["line"]
                assert slot == len(records) + 1, f"p{page_number}: slot {slot} out of order"
                if kind == "surah-header":
                    surah = int(line["surah"])
                    assert surah == self.stream[self.gi][0], \
                        f"p{page_number} L{slot}: header {surah} vs corpus {self.stream[self.gi][0]}"
                    assert self.surahNamed is None or surah == self.surahNamed + 1, \
                        f"p{page_number} L{slot}: header {surah} after header {self.surahNamed}"
                    self.surahNamed = surah
                    records.append(("H", slot, surah, None, None, ""))
                elif kind == "basmala":
                    surah = self.stream[self.gi][0]
                    # Byte order varies inside the corpus (بِّسْمِ vs بِسْمِ):
                    # the shadda and the kasra swap. Folding is the comparison.
                    assert folded(self.tokens[(surah, 1)][0]) == "بسم", \
                        f"p{page_number} L{slot}: basmalah row for surah {surah}"
                    assert self.stream[self.gi][1:] == (1, 0), \
                        f"p{page_number} L{slot}: basmalah row mid-verse"
                    assert self.stream[self.gi + 3] == (surah, 1, 3), \
                        f"p{page_number} L{slot}: basmalah row short"
                    assert surah == self.surahNamed, \
                        f"p{page_number} L{slot}: basmalah row under header {self.surahNamed}"
                    assert surah not in (1, 9), f"p{page_number}: basmalah row for {surah}"
                    self.gi += 4
                    records.append(("B", slot, surah, self.gi - 4, self.gi - 1, ""))
                else:
                    assert kind == "text", f"p{page_number} L{slot}: {kind}"
                    centered = self.centre[(page_number, slot)] == 1
                    start, end, flags = self.text_line(
                        page_number, slot, line.get("words", []), centered)
                    records.append(("A", slot, None, start, end, flags))
            self.pages.append((page_number, records))
        assert self.gi == WORD_COUNT, f"walked {self.gi} of {WORD_COUNT} words"
        assert self.pending is None, f"an end marker never printed: {self.pending}"
        assert len(self.standalone) == 573, f"standalone markers: {len(self.standalone)}"
        assert self.midline_markers == EXPECTED_MIDLINE, \
            f"mid-line standalone markers: {self.midline_markers}"
        assert len(self.joins) == 4 and set(self.joins) == EXPECTED_JOINS, \
            f"joins: {sorted(self.joins)}"
        sizes = {}
        for page_number, records in self.pages:
            sizes[len(records)] = sizes.get(len(records), 0) + 1
        expected_sizes = {p: (8 if p in (1, 2) else 15) for p in range(1, PAGES + 1)}
        for page_number, records in self.pages:
            assert len(records) == expected_sizes[page_number], \
                f"p{page_number}: {len(records)} lines"
        assert sizes == {8: 2, 15: 602}, sizes


def page_start_of(records, stream):
    """The first verse the print STARTS on a page: its first opener word."""
    for kind, slot, surah, from_word, to_word, flags in records:
        if kind not in ("B", "A"):
            continue
        s, a, w = stream[from_word]
        if w == 0:
            return (s, a)
    return None


def parse_asset_text(text):
    """Round-trip parser: the same records back from the emitted bytes."""
    import re as _re
    pages = []
    covering = 1
    for raw in text.splitlines():
        if raw.startswith("#") or not raw.strip():
            continue
        fields = raw.split()
        page = int(fields[0])
        assert page == len(pages) + 1, f"page out of order: {page}"
        records = []
        expect_slot = 1
        for field in fields[1:]:
            m = _re.fullmatch(r"(\d+)([HBA])(\d+)?(?:-(\d+))?([wc]*)", field)
            assert m, f"unparsable record: {field!r}"
            slot, kind, lo, hi, flags = m.groups()
            assert int(slot) == expect_slot, f"p{page}: slot {slot} after {expect_slot - 1}"
            expect_slot += 1
            if kind == "H":
                records.append(("H", int(slot), int(lo), None, None, ""))
            else:
                assert flags in ("", "w", "c", "wc"), field
                lo, hi = int(lo), int(hi)
                assert lo == covering, f"p{page} L{slot}: range starts at {lo}, expected {covering}"
                covering = hi + 1
                records.append((kind, int(slot), None, lo - 1, hi - 1, flags))
        pages.append((page, records))
    assert covering == WORD_COUNT + 1, f"asset covers {covering - 1} of {WORD_COUNT} words"
    return pages


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--fetch", action="store_true", help="download the 604 print pages")
    parser.add_argument("--zpages", type=Path, default=DEFAULT_ZPAGES)
    parser.add_argument("--mirror-sqlite", type=Path, default=DEFAULT_MIRROR)
    args = parser.parse_args()

    if args.fetch:
        args.zpages.mkdir(parents=True, exist_ok=True)
        for page in range(1, PAGES + 1):
            target = args.zpages / f"page-{page:03d}.json"
            if not target.exists():
                with urllib.request.urlopen(QPC2_URL.format(page=page), timeout=60) as src:
                    target.write_bytes(src.read())
        print(f"fetched {PAGES} print pages into {args.zpages}")

    if not args.zpages.exists():
        sys.exit(f"print pages not found: {args.zpages} (see --fetch)")
    if not args.mirror_sqlite.exists():
        sys.exit(f"QUL mirror db not found: {args.mirror_sqlite}\n({MIRROR_URL})")

    tokens = corpus_tokens()
    stream = word_stream(tokens)
    assert len(stream) == WORD_COUNT, f"corpus words: {len(stream)}"
    centre = centre_flags(args.mirror_sqlite)
    meta_starts = page_starts_from_metadata()

    walker = Walker(tokens, stream, centre)
    walker.run(args.zpages)

    header = """\
# Mushaf line layout of the KFGQPC V2 (1421H) Madinah mushaf: where every word
# of the bundled Uthmani corpus prints, per printed line of the 604 pages.
#
# One data line per page; fields space-separated.
#   First field: the page number (1..604).
#   Then one record per printed line, in print order:
#     <slot>H<surah>             surah name line, always centred
#     <slot>B<from>-<to>         basmalah row, always centred: the four corpus
#                                words that open the surah
#     <slot>A<from>-<to>[w][c]   text line; <from>..<to> is an inclusive range
#                                of the corpus's 77,881 word ordinals (1-based
#                                in corpus order); w = the verse marker of the
#                                ayah that ended on the previous line prints at
#                                this line's start; c = the print centres it
#
# Where the corpus keeps a pause or prostration sign as its own token, the print
# renders it on the preceding word; that fused text is this word's rendering.
# The corpus carries the basmalah as the first four words of every surah's
# opener (all but At-Tawbah's), which is what the B rows point at.
# Provenance and the validated invariants: README, "Madinah mushaf layout".
"""
    data_lines = []
    differences = []
    for page_number, records in walker.pages:
        fields = [str(page_number)]
        for kind, slot, surah, from_word, to_word, flags in records:
            if kind == "H":
                fields.append(f"{slot}H{surah}")
            else:
                fields.append(f"{slot}{kind}{from_word + 1}-{to_word + 1}{flags}")
        data_lines.append(" ".join(fields))

        if page_start_of(records, stream) != meta_starts[page_number - 1]:
            differences.append(
                (page_number, meta_starts[page_number - 1], page_start_of(records, stream)))
    assert len(differences) == 25, \
        f"metadata page-start differences: {len(differences)}\n" + "\n".join(map(str, differences))

    body = "\n".join(data_lines) + "\n"
    ASSET.write_text(header + body, encoding="utf-8")

    # Round-trip: the emitted bytes must parse back to the same records. A
    # record's surah field is only known to the walk (B rows point at the
    # surah's own words), so the comparison projects to what the file stores.
    pages = parse_asset_text(ASSET.read_text(encoding="utf-8"))

    def shape(iterable):
        return [(page,
                 [(kind, slot, from_word, to_word, flags)
                  for kind, slot, _surah, from_word, to_word, flags in records])
                for page, records in iterable]

    assert shape(pages) == shape(walker.pages), "round-trip differs"

    text_bytes = ASSET.read_text(encoding="utf-8").encode("utf-8")
    digest = hashlib.sha256(text_bytes).hexdigest()

    print(f"wrote {ASSET.relative_to(REPO)}  ({len(text_bytes)} bytes)")
    print(f"LAYOUT_HASH = {digest}")
    kinds = {}
    for _, records in walker.pages:
        for record in records:
            kinds[record[0]] = kinds.get(record[0], 0) + 1
    print("records:", kinds)
    print("joins:", sorted(walker.joins))
    centred_a = [(page_number, record[1], record[5])
                 for page_number, records in walker.pages
                 for record in records if record[0] == "A" and "c" in record[5]]
    print(f"centred text lines: {len(centred_a)}")
    print("metadata page-start differences (25):")
    for page_number, meta, actual in differences:
        print(f"  p{page_number}: metadata {meta} vs print {actual}")


if __name__ == "__main__":
    main()
