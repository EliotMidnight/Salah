# Quran text sources

## Quran text (Uthmani)
- File: `uthmani.txt` — Tanzil Quran Text (Uthmani), Version 1.1, downloaded verbatim from
  https://tanzil.net/pub/download/index.php?quranType=uthmani&outType=txt-2&marks=true&sajdah=true&alef=true&tatweel=true&agree=true
- SHA-256: `6933e133dd56db778c801bf738848454e43648105a151e8d84d86a7cae39ec5f`
- Copyright (C) 2007-2026 Tanzil Project. License: Creative Commons Attribution 3.0.
- The complete Tanzil notice is preserved at the top of `uthmani.txt`. The text is
  distributed unchanged; changing it is not allowed. A link to https://tanzil.net/ is
  provided in the app's About/content-source screen so readers can track updates.
- Check updates at: http://tanzil.net/updates/

## Partition metadata
- File: `metadata.xml` — Tanzil Quran Metadata, Version 1.0, downloaded verbatim from
  https://tanzil.net/res/text/metadata/quran-data.xml
- SHA-256: `8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a`
- Copyright (C) 2008-2009 Tanzil.info, license attribute `cc-by` (Creative Commons
  Attribution). Supplied unchanged and credited here with the source link above.

## Translations and tafsir
- File: `en_sahihintl.txt` — Saheeh International English translation (6236 verses,
  `surah|ayah|text` per line), text extracted verbatim from the Tanzil translation page
  https://tanzil.net/trans/en.sahih
- SHA-256: `f090ed258e647a393ddaa0b48f1e81ab7a5ccd5ad7d72d0da498e151bc0dbc18`
- Saheeh International (Jeddah, Saudi Arabia). Translation by three American women
  converts; published by Dar Abul-Qasim. Displayed in-app with the credit line
  "English — Saheeh International". No tafsir text is bundled yet.

## Mushaf line layout
- File: `mushaf_layout.txt` — where every word of the bundled Uthmani text sits on the
  604 pages of the **King Fahd Complex (KFGQPC) V2 mushaf, the 1421H Madinah print**
  (the same print quran.com serves in its page view).
- SHA-256: `b8816389c4d5d4d9a1951690f30a4bbdc0b7029f7e1e86ab6814ebcb2bef96dc`
- Generated in-house by `tools/madinah_layout.py` from the print's line/word structure:
  quran.com's published digitization of the V2 print (mirrored at the
  `zonetecde/mushaf-layout` repo) supplies which words print on which line, and the
  Quranic Universal Library export of the same print supplies the line-shape cross-check
  and which rows the print centres. Words are not stored here — the file points at the
  verified Tanzil corpus above, so the verse text still has one source. The mushaf layout
  is positional data; the text it positions belongs to the Uthmani file's copyright.
  Provenance, invariants and the derivation are documented in the top-level
  `README.md`, "The Madinah mushaf".

## Integrity
`QuranCorpus` verifies each file's SHA-256 at load and asserts 114 suras, 6,236
verses in canonical order, and 604 page / 30 juz / 240 hizb-quarter partitions before
the corpus is served. `mushaf_layout.txt` rides the same gate, and its own loader
refuses to tile the corpus if the layout does not place every word exactly once.
