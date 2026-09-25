package com.example.ui.quran

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.data.model.Ayah
import com.example.data.model.RevelationType
import com.example.data.model.Surah
import com.example.data.quran.QuranDataSource
import com.example.ui.SalahUiState
import com.example.ui.components.CameraHoleSpacer
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocalStrings
import com.example.ui.theme.CardSurfaceShape
import com.example.ui.theme.ChipSurfaceShape
import com.example.ui.localization.ayahLabel
import com.example.ui.localization.backToSurahsLabel
import com.example.ui.localization.cardsModeLabel
import com.example.ui.localization.continuousModeLabel
import com.example.ui.localization.copyVerseLabel
import com.example.ui.localization.hideTranslationLabel
import com.example.ui.localization.juzWord
import com.example.ui.localization.nextSurahLabel
import com.example.ui.localization.pageWord
import com.example.ui.localization.previousSurahLabel
import com.example.ui.localization.readingSettingsLabel
import com.example.ui.localization.searchVersesHint
import com.example.ui.localization.shareChooserTitle
import com.example.ui.localization.shareVerseLabel
import com.example.ui.localization.showTranslationLabel
import com.example.ui.localization.tapVerseHint
import com.example.ui.localization.translationCreditLine
import com.example.ui.localization.translationSectionTitle
import com.example.ui.localization.verseCopiedToast
import com.example.ui.theme.ExpressiveMotion

/**
 * Filter mode matching Quran.json specification:
 * Surah, Page, Juz', Hizb, Bookmarks
 */
enum class QuranTab(val label: String) {
    SURAH("Surah"),
    PAGE("Page"),
    JUZ("Juz'"),
    HIZB("Hizb"),
    BOOKMARKS("Bookmarks")
}

/**
 * Reader layout inspired by the nour Quran experience:
 * - CARDS: each verse in its own card with actions and translation.
 * - CONTINUOUS: the whole surah as flowing text; tap a verse to inspect it.
 */
enum class QuranReadingMode {
    CARDS,
    CONTINUOUS
}

data class JuzItem(
    val number: Int,
    val arabicName: String,
    val englishName: String,
    val startSurahName: String,
    val startPage: Int
)

data class HizbItem(
    val number: Int,
    val arabicName: String,
    val englishName: String,
    val startPage: Int
)

val JUZ_LIST: List<JuzItem> = listOf(
    JuzItem(1, "الم", "Al-Fatihah", "Al-Fatihah 1:1", 1),
    JuzItem(2, "سيقول", "Sayaqool", "Al-Baqarah 2:142", 22),
    JuzItem(3, "تلك الرسل", "Tilka 'r-Rusul", "Al-Baqarah 2:253", 42),
    JuzItem(4, "لن تنالوا", "Lan Tanaaloo", "Aal-E-Imran 3:93", 62),
    JuzItem(5, "والمحصنات", "Wal-Muhsanat", "An-Nisa 4:24", 82),
    JuzItem(6, "لا يحب الله", "La Yuhibbullah", "An-Nisa 4:148", 102),
    JuzItem(7, "وإذا سمعوا", "Wa Iza Sami'oo", "Al-Ma'idah 5:82", 121),
    JuzItem(8, "ولو أننا", "Wa Law Annana", "Al-An'am 6:111", 142),
    JuzItem(9, "قال الملأ", "Qalal Mala'u", "Al-A'raf 7:88", 162),
    JuzItem(10, "واعلموا", "Wa'lamoo", "Al-Anfal 8:41", 182),
    JuzItem(11, "يعتذرون", "Ya'taziroon", "At-Tawbah 9:93", 201),
    JuzItem(12, "وما من دابة", "Wa Ma Min Dabbah", "Hud 11:6", 222),
    JuzItem(13, "وما أبرئ", "Wa Ma Ubarri'u", "Yusuf 12:53", 242),
    JuzItem(14, "ربما", "Rubama", "Al-Hijr 15:1", 262),
    JuzItem(15, "سبحان الذي", "Subhana 'l-Lazi", "Al-Isra 17:1", 282),
    JuzItem(16, "قال ألم", "Qala Alam", "Al-Kahf 18:75", 302),
    JuzItem(17, "اقترب", "Iqtaraba", "Al-Anbiya 21:1", 322),
    JuzItem(18, "قد أفلح", "Qad Aflaha", "Al-Mu'minun 23:1", 342),
    JuzItem(19, "وقال الذين", "Wa Qalallazina", "Al-Furqan 25:21", 362),
    JuzItem(20, "فما كان", "Fa Ma Kana", "An-Naml 27:56", 382),
    JuzItem(21, "ولا تجادلوا", "Wa La Tujadiloo", "Al-'Ankabut 29:46", 402),
    JuzItem(22, "ومن يقنت", "Wa Man Yaqnut", "Al-Ahzab 33:31", 422),
    JuzItem(23, "وما أنزلنا", "Wa Maliya", "Ya-Sin 36:28", 442),
    JuzItem(24, "فمن أظلم", "Faman Azlamu", "Az-Zumar 39:32", 462),
    JuzItem(25, "إليه يرد", "Ilayhi Yuraddu", "Fussilat 41:47", 482),
    JuzItem(26, "حم", "Ha-Meem", "Al-Ahqaf 46:1", 502),
    JuzItem(27, "قال فما خطبكم", "Qala Fama Khatbukum", "Adh-Dhariyat 51:31", 522),
    JuzItem(28, "قد سمع", "Qad Sami'a", "Al-Mujadila 58:1", 542),
    JuzItem(29, "تبارك الذي", "Tabaraka 'l-Lazi", "Al-Mulk 67:1", 562),
    JuzItem(30, "عمّ يتساءلون", "'Amma Yatasa'aloon", "An-Naba 78:1", 582)
)

val HIZB_LIST: List<HizbItem> = (1..60).map { hizb ->
    val juzNum = (hizb - 1) / 2 + 1
    val part = if (hizb % 2 == 1) "1st Half" else "2nd Half"
    val page = ((hizb - 1) * 10 + 1).coerceIn(1, 604)
    HizbItem(
        number = hizb,
        arabicName = "الحزب $hizb",
        englishName = "Hizb $hizb · Juz $juzNum ($part)",
        startPage = page
    )
}

@Composable
fun QuranScreen(
    state: SalahUiState,
    onSurahSelected: (Int) -> Unit,
    onAyahViewed: (Ayah) -> Unit,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    onStopAudio: () -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onPageSelected: (Int) -> Unit = {},
    onJuzSelected: (Int) -> Unit = {},
    onHizbSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inReaderMode by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(QuranTab.SURAH) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showFontSlider by remember { mutableStateOf(false) }
    var readingMode by remember { mutableStateOf(QuranReadingMode.CARDS) }
    // Verse to resume at when the reader opens (continue-reading pill / bookmark jump).
    var resumeAyahNumber by remember { mutableStateOf(1) }

    val strings = LocalStrings.current
    val arabicUi = AppLanguage.fromNameOrCode(state.language) == AppLanguage.ARABIC
    val showSearchField = isSearchOpen || searchQuery.isNotEmpty()
    val searchPlaceholder = when (selectedTab) {
        QuranTab.SURAH -> strings.searchSurahPlaceholder
        QuranTab.PAGE -> "Search page number (1–604)..."
        QuranTab.JUZ -> "Search Juz' number or name..."
        QuranTab.HIZB -> "Search Hizb number..."
        QuranTab.BOOKMARKS -> "Search bookmarks..."
    }
    val listState = rememberLazyListState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
        if (!inReaderMode) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    CameraHoleSpacer(extra = 2.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.navQuran,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!showSearchField) {
                            Text(
                                text = "114 ${strings.surahTab} · 30 ${strings.juzTab} · 6,236 ${strings.versesCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (showSearchField) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = searchPlaceholder,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (searchQuery.isNotEmpty()) searchQuery = ""
                                        else isSearchOpen = false
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (searchQuery.isNotEmpty()) {
                                            Icons.Default.Clear
                                        } else {
                                            Icons.Default.Close
                                        },
                                        contentDescription = if (searchQuery.isNotEmpty()) "Clear" else "Close search",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp, max = 56.dp)
                                .padding(start = 8.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        FilledTonalIconButton(
                            onClick = { isSearchOpen = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("quran_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

            // Library tabs: Surah, Page, Juz', Hizb, Bookmarks
            val tabs = QuranTab.values()
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(selectedTab),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 4.dp,
                divider = {}
            ) {
                tabs.forEach { tab ->
                    val tabLabel = when (tab) {
                        QuranTab.SURAH -> strings.surahTab
                        QuranTab.PAGE -> strings.pageTab
                        QuranTab.JUZ -> strings.juzTab
                        QuranTab.HIZB -> strings.hizbTab
                        QuranTab.BOOKMARKS -> "${strings.bookmarksTab} (${state.bookmarks.size})"
                    }
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tabLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("quran_tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Continue-reading card (search now lives in the header above)
            ContinueReadingCard(
                surahNumber = state.continueReading.surahNumber,
                ayahNumber = state.continueReading.ayahNumber,
                arabicUi = arabicUi,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("quran_continue_reading_pill"),
                onClick = {
                    resumeAyahNumber = state.continueReading.ayahNumber.coerceAtLeast(1)
                    onSurahSelected(state.continueReading.surahNumber)
                    inReaderMode = true
                }
            )

            // 6. LIBRARY LISTS (animated across tabs)
            // weight(1f): give the list the remaining viewport height so LazyColumn
            // is bounded and can scroll (without it, only ~one row fits).
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .fillMaxHeight(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = ExpressiveMotion.SHORT)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = ExpressiveMotion.SHORT))
                },
                label = "quran_tab_swap"
            ) { tab ->
            when (tab) {
                QuranTab.SURAH -> {
                    SurahLibraryList(
                        searchQuery = searchQuery,
                        arabicUi = arabicUi,
                        onSurahClick = { surah, resumeAyah ->
                            resumeAyahNumber = resumeAyah
                            onSurahSelected(surah.number)
                            inReaderMode = true
                        }
                    )
                }

                QuranTab.PAGE -> {
                    // Canonical page starts from the verified corpus (not startPage heuristics).
                    val pageStarts = remember {
                        (1..604).associateWith { p ->
                            QuranDataSource.firstAyahOnPage(p)
                        }
                    }
                    val pages = (1..604).filter {
                        if (searchQuery.isBlank()) true
                        else it.toString().contains(searchQuery.trim())
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(pages, key = { it }) { page ->
                            val start = pageStarts[page]
                            val surah = start?.let { QuranDataSource.getSurahByNumber(it.surahNumber) }
                                ?: QuranDataSource.SURAHS[0]
                            val startAyah = start?.ayahNumber ?: 1
                            QuranPillItem(
                                badgeText = "P.$page",
                                title = "${strings.pageTab} $page",
                                subtitle = "${surah.englishName} · ${strings.ayahLabel} $startAyah",
                                trailingText = "ص $page",
                                onClick = {
                                    resumeAyahNumber = startAyah
                                    onPageSelected(page)
                                    inReaderMode = true
                                },
                                testTag = "page_item_$page"
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }

                QuranTab.JUZ -> {
                    val filteredJuz = JUZ_LIST.filter {
                        if (searchQuery.isBlank()) true
                        else {
                            it.number.toString() == searchQuery.trim() ||
                                    it.englishName.contains(searchQuery, ignoreCase = true) ||
                                    it.arabicName.contains(searchQuery)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredJuz, key = { it.number }) { juz ->
                            val juzStart = remember(juz.number) {
                                QuranDataSource.firstAyahForJuz(juz.number)
                            }
                            QuranPillItem(
                                badgeText = "J.${juz.number}",
                                title = "${strings.juzTab} ${juz.number} · ${juz.englishName}",
                                subtitle = juzStart?.let {
                                    val s = QuranDataSource.getSurahByNumber(it.surahNumber)
                                    "${s?.englishName ?: juz.startSurahName} · ${strings.ayahLabel} ${it.ayahNumber}"
                                } ?: "${juz.startSurahName} · ${strings.pageTab} ${juz.startPage}",
                                trailingText = juz.arabicName,
                                onClick = {
                                    resumeAyahNumber = juzStart?.ayahNumber ?: 1
                                    onJuzSelected(juz.number)
                                    inReaderMode = true
                                },
                                testTag = "juz_item_${juz.number}"
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }

                QuranTab.HIZB -> {
                    val filteredHizb = HIZB_LIST.filter {
                        if (searchQuery.isBlank()) true
                        else it.number.toString() == searchQuery.trim() || it.englishName.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredHizb, key = { it.number }) { hizb ->
                            val hizbStart = remember(hizb.number) {
                                QuranDataSource.firstAyahForHizb(hizb.number)
                            }
                            QuranPillItem(
                                badgeText = "H.${hizb.number}",
                                title = hizb.englishName,
                                subtitle = hizbStart?.let {
                                    val s = QuranDataSource.getSurahByNumber(it.surahNumber)
                                    "${s?.englishName ?: ""} · ${strings.ayahLabel} ${it.ayahNumber}"
                                } ?: "${strings.pageTab} ${hizb.startPage}",
                                trailingText = hizb.arabicName,
                                onClick = {
                                    resumeAyahNumber = hizbStart?.ayahNumber ?: 1
                                    onHizbSelected(hizb.number)
                                    inReaderMode = true
                                },
                                testTag = "hizb_item_${hizb.number}"
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }

                QuranTab.BOOKMARKS -> {
                    val filteredBookmarks = state.bookmarks.filter {
                        if (searchQuery.isBlank()) true
                        else it.surahName.contains(searchQuery, ignoreCase = true) ||
                                it.ayahSnippet.contains(searchQuery)
                    }

                    if (filteredBookmarks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No bookmarks found for \"$searchQuery\""
                                    else "No bookmarks saved yet.\nTap the bookmark icon beside any verse in reader mode to save it.",
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredBookmarks, key = { "${it.surahNumber}_${it.ayahNumber}" }) { bm ->
                                // Resolve live text from the verified corpus; fall back to the
                                // stored snippet for references that no longer resolve.
                                val liveAyah = remember(bm.surahNumber, bm.ayahNumber) {
                                    QuranDataSource.resolveAyah(bm.surahNumber, bm.ayahNumber)
                                }
                                QuranPillItem(
                                    badgeText = "${bm.surahNumber}:${bm.ayahNumber}",
                                    title = "${bm.surahName} (${bm.surahNumber}:${bm.ayahNumber})",
                                    subtitle = (liveAyah?.textArabic ?: bm.ayahSnippet).take(40) +
                                        if ((liveAyah?.textArabic ?: bm.ayahSnippet).length > 40) "..." else "",
                                    trailingText = "",
                                    onClick = {
                                        resumeAyahNumber = bm.ayahNumber.coerceAtLeast(1)
                                        onSurahSelected(bm.surahNumber)
                                        inReaderMode = true
                                    },
                                    testTag = "bookmark_${bm.surahNumber}_${bm.ayahNumber}"
                                )
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
            }
        } else {
            // MUSHAF READER MODE (nour-style)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ReaderView(
                surah = state.selectedSurah,
                ayahs = state.currentSurahAyahs,
                bookmarks = state.bookmarks,
                fontScale = state.quranFontScale,
                isAudioPlaying = state.isAudioPlaying,
                currentAudioAyah = state.currentAudioAyah,
                showFontSlider = showFontSlider,
                readingMode = readingMode,
                resumeAyahNumber = resumeAyahNumber,
                arabicUi = arabicUi,
                listState = listState,
                onBack = { inReaderMode = false },
                onToggleFontSlider = { showFontSlider = !showFontSlider },
                onFontScaleChange = onFontScaleChange,
                onModeChange = { readingMode = it },
                onSelectSurah = {
                    resumeAyahNumber = 1
                    onSurahSelected(it.number)
                },
                onAyahClick = { onAyahViewed(it) },
                onAyahVisible = { onAyahViewed(it) },
                onToggleBookmark = onToggleBookmark,
                onTogglePlayAyah = onTogglePlayAyah,
                onStopAudio = onStopAudio
            )
            }
        }
        }
    }
}

/**
 * Continue-reading card with live progress (nour-style ContinueReadingSurahCard,
 * adapted to Salah's pill geometry).
 */
@Composable
private fun ContinueReadingCard(
    surahNumber: Int,
    ayahNumber: Int,
    arabicUi: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val surah = remember(surahNumber) {
        QuranDataSource.getSurahByNumber(surahNumber) ?: QuranDataSource.SURAHS[0]
    }
    val totalVerses = surah.totalVerses
    val lastAyah = ayahNumber.coerceIn(1, totalVerses)
    val fraction = lastAyah.toFloat() / totalVerses.toFloat()
    val readingText = if (arabicUi) {
        "${surah.arabicName} · ${strings.ayahLabel} $lastAyah / $totalVerses"
    } else {
        "${strings.continueReading} · ${surah.englishName} · ${strings.ayahLabel} $lastAyah / $totalVerses"
    }
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(ChipSurfaceShape)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = ChipSurfaceShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = strings.continueReading,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = readingText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Surah library: popular surahs when idle, directory rows for surah matches
 * plus verse-level results (Arabic + English) when searching — nour-style.
 */
@Composable
private fun SurahLibraryList(
    searchQuery: String,
    arabicUi: Boolean,
    onSurahClick: (Surah, Int) -> Unit
) {
    val strings = LocalStrings.current
    if (searchQuery.isBlank()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(QuranDataSource.SURAHS, key = { it.number }) { surah ->
                SurahDirectoryRow(
                    surah = surah,
                    arabicUi = arabicUi,
                    onClick = { onSurahClick(surah, 1) },
                    testTag = "surah_item_${surah.number}"
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    } else {
        val normalized = remember(searchQuery) { QuranDataSource.normalizeArabic(searchQuery) }
        val filteredSurahs = remember(searchQuery, normalized) {
            QuranDataSource.SURAHS.filter {
                it.englishName.contains(searchQuery, ignoreCase = true) ||
                    it.englishTranslation.contains(searchQuery, ignoreCase = true) ||
                    it.arabicName.contains(searchQuery) ||
                    QuranDataSource.normalizeArabic(it.arabicName).contains(normalized) ||
                    it.number.toString() == searchQuery.trim()
            }
        }
        // Verse-level matches across the full corpus (capped for smooth scrolling).
        val allMatchingAyahs = remember(searchQuery) {
            QuranDataSource.searchAyahs(searchQuery)
        }
        val matchingAyahs = remember(allMatchingAyahs) { allMatchingAyahs.take(50) }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredSurahs.isNotEmpty()) {
                item {
                    Text(
                        text = "${filteredSurahs.size} ${strings.surahTab}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                items(filteredSurahs, key = { it.number }) { surah ->
                    SurahDirectoryRow(
                        surah = surah,
                        arabicUi = arabicUi,
                        onClick = { onSurahClick(surah, 1) },
                        testTag = "surah_item_${surah.number}"
                    )
                }
            }
            if (matchingAyahs.isNotEmpty()) {
                item {
                    Text(
                        text = "${allMatchingAyahs.size} ${strings.versesCount}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                items(matchingAyahs, key = { "${it.surahNumber}:${it.ayahNumber}" }) { ayah ->
                    val surah = QuranDataSource.getSurahByNumber(ayah.surahNumber)
                    SearchAyahCard(
                        ayah = ayah,
                        surahName = if (arabicUi) surah?.arabicName else surah?.englishName,
                        onClick = {
                            val target = surah ?: QuranDataSource.SURAHS[0]
                            onSurahClick(target, ayah.ayahNumber)
                        }
                    )
                }
            }
            if (filteredSurahs.isEmpty() && matchingAyahs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * Compact directory row (nour SurahListItem, adapted): number badge,
 * names + revelation meta, Arabic title trailing.
 */
@Composable
private fun SurahDirectoryRow(
    surah: Surah,
    arabicUi: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val strings = LocalStrings.current
    val revelation = when (surah.revelationType) {
        RevelationType.MECCAN -> strings.meccan
        RevelationType.MEDINAN -> strings.medinan
    }
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "${surah.number}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (arabicUi) surah.arabicName else surah.englishName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${surah.totalVerses} ${strings.versesCount} · $revelation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!arabicUi) {
                    Text(
                        surah.englishTranslation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!arabicUi) {
                Text(
                    surah.arabicName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * Verse-level search result card showing live Arabic plus its English
 * translation (nour-style search results).
 */
@Composable
private fun SearchAyahCard(
    ayah: Ayah,
    surahName: String?,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${surahName ?: ""} (${ayah.surahNumber}:${ayah.ayahNumber})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${strings.pageWord} ${ayah.pageNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${ayah.textArabic} \u06DD${QuranDataSource.toArabicDigits(ayah.ayahNumber)}",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            if (ayah.textEnglish.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ayah.textEnglish,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Standard 56dp tall pill box specified in Quran.json:
 * size2: 56, radiusTop: 48, radiusBottom: 48, fill: "surfaceContainerHigh"
 */
@Composable
private fun QuranPillItem(
    badgeText: String,
    title: String,
    subtitle: String,
    trailingText: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle Badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        fontSize = if (badgeText.length > 3) 10.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (trailingText.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trailingText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Serif
                )
            }
        }
    }
}

/**
 * nour-style reader: minimalist header with surah switcher, Cards/Continuous
 * modes, translation inspector, prev/next navigation and the audio bar.
 */
@Composable
private fun ReaderView(
    surah: Surah,
    ayahs: List<Ayah>,
    bookmarks: List<com.example.data.local.BookmarkEntity>,
    fontScale: Float,
    isAudioPlaying: Boolean,
    currentAudioAyah: Int,
    showFontSlider: Boolean,
    readingMode: QuranReadingMode,
    resumeAyahNumber: Int,
    arabicUi: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onBack: () -> Unit,
    onToggleFontSlider: () -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onModeChange: (QuranReadingMode) -> Unit,
    onSelectSurah: (Surah) -> Unit,
    onAyahClick: (Ayah) -> Unit,
    onAyahVisible: (Ayah) -> Unit,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    onStopAudio: () -> Unit
) {
    val strings = LocalStrings.current
    var showSurahDropdown by remember { mutableStateOf(false) }
    var inspectedAyahNumber by remember(surah.number) { mutableStateOf<Int?>(null) }
    var showTranslationsInContinuous by remember(surah.number) { mutableStateOf(false) }

    // Jump to the resume verse when the reader opens / surah changes (cards mode).
    // Prefer the ViewModel cursor when it points at a different ayah in this surah
    // (page/juz/hizb jumps set activeReadingAyahNumber via selectPage/selectJuz).
    LaunchedEffect(surah.number, resumeAyahNumber, readingMode) {
        if (readingMode == QuranReadingMode.CARDS && ayahs.isNotEmpty()) {
            val index = (resumeAyahNumber - 1).coerceIn(0, ayahs.lastIndex)
            // Item 0 is the banner header, verses start at item 1.
            listState.scrollToItem(index + 1)
        } else {
            listState.scrollToItem(0)
        }
    }

    // Track the visible verse for continue-reading (cards mode only, where
    // item 0 is the banner header and each following item is one verse).
    if (readingMode == QuranReadingMode.CARDS && ayahs.isNotEmpty()) {
        LaunchedEffect(surah.number) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collect { index ->
                    ayahs.getOrNull(index - 1)?.let { onAyahVisible(it) }
                }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Reader Header with camera-hole + status-bar safe insets
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("reader_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backToSurahsLabel)
                        }
                        Box {
                            TextButton(
                                onClick = { showSurahDropdown = true },
                                shape = CardSurfaceShape,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "${surah.number}. ${if (arabicUi) surah.arabicName else surah.englishName} ▾",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${surah.arabicName} · ${strings.pageWord} ${surah.startPage}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Serif
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showSurahDropdown,
                                onDismissRequest = { showSurahDropdown = false }
                            ) {
                                QuranDataSource.SURAHS.forEach { s ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (arabicUi) "${s.number}. ${s.arabicName}"
                                                else "${s.number}. ${s.englishName} (${s.arabicName})",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        onClick = {
                                            inspectedAyahNumber = null
                                            showSurahDropdown = false
                                            onSelectSurah(s)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            onModeChange(
                                if (readingMode == QuranReadingMode.CONTINUOUS) QuranReadingMode.CARDS
                                else QuranReadingMode.CONTINUOUS
                            )
                        }) {
                            Icon(
                                imageVector = if (readingMode == QuranReadingMode.CONTINUOUS) Icons.Default.ViewAgenda
                                else Icons.Default.AutoStories,
                                contentDescription = if (readingMode == QuranReadingMode.CONTINUOUS) strings.cardsModeLabel
                                else strings.continuousModeLabel,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onToggleFontSlider) {
                            Icon(Icons.Default.FormatSize, contentDescription = strings.readingSettingsLabel)
                        }
                    }
                }

                if (showFontSlider) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("A", fontSize = 12.sp)
                        Slider(
                            value = fontScale,
                            onValueChange = onFontScaleChange,
                            valueRange = 0.8f..1.6f,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text("A", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Reading content
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Surah header banner with Bismillah (except Surahs 1 and 9)
            item {
                Spacer(modifier = Modifier.height(2.dp))
                SurahHeaderBanner(surah = surah, arabicUi = arabicUi)
            }

            if (readingMode == QuranReadingMode.CARDS) {
                items(ayahs, key = { it.ayahNumber }) { ayah ->
                    val isPlaying = isAudioPlaying && currentAudioAyah == ayah.ayahNumber
                    val isBookmarked =
                        bookmarks.any { it.surahNumber == ayah.surahNumber && it.ayahNumber == ayah.ayahNumber }
                    VerseCard(
                        ayah = ayah,
                        surah = surah,
                        fontScale = fontScale,
                        isPlaying = isPlaying,
                        isBookmarked = isBookmarked,
                        onClick = { onAyahClick(ayah) },
                        onToggleBookmark = { onToggleBookmark(ayah) },
                        onPlayClick = { onTogglePlayAyah(ayah) }
                    )
                }
            } else {
                item {
                    ContinuousQuranTextCard(
                        ayahs = ayahs,
                        fontScale = fontScale,
                        selectedAyahNumber = inspectedAyahNumber,
                        onSelectAyahNumber = { tapped ->
                            inspectedAyahNumber = if (inspectedAyahNumber == tapped) null else tapped
                        }
                    )
                }

                inspectedAyahNumber?.let { number ->
                    ayahs.find { it.ayahNumber == number }?.let { inspected ->
                        item {
                            AyahInspectorCard(
                                ayah = inspected,
                                surah = surah,
                                fontScale = fontScale,
                                isBookmarked = bookmarks.any {
                                    it.surahNumber == inspected.surahNumber && it.ayahNumber == inspected.ayahNumber
                                },
                                onDismiss = { inspectedAyahNumber = null },
                                onToggleBookmark = { onToggleBookmark(inspected) }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.translationSectionTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(onClick = { showTranslationsInContinuous = !showTranslationsInContinuous }) {
                            Text(
                                if (showTranslationsInContinuous) strings.hideTranslationLabel
                                else "${strings.showTranslationLabel} (${surah.totalVerses})"
                            )
                        }
                    }
                }

                if (showTranslationsInContinuous) {
                    items(ayahs, key = { "t${it.ayahNumber}" }) { ayah ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ChipSurfaceShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "${strings.ayahLabel} ${ayah.ayahNumber}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ayah.textEnglish,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // End-of-surah prev/next navigation (nour-style)
            item {
                SurahNavCard(
                    surah = surah,
                    onSelectSurah = {
                        inspectedAyahNumber = null
                        onSelectSurah(it)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Audio Player Bar if playing
        if (isAudioPlaying) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Reciting ${surah.englishName} : Verse $currentAudioAyah",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Mishary Alafasy · Offline & stream capable",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Row {
                        IconButton(onClick = onStopAudio) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop audio",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Spiritual surah header banner with Bismillah ornament (nour-style).
 */
@Composable
private fun SurahHeaderBanner(surah: Surah, arabicUi: Boolean) {
    val strings = LocalStrings.current
    val revelation = when (surah.revelationType) {
        RevelationType.MECCAN -> strings.meccan
        RevelationType.MEDINAN -> strings.medinan
    }
    // Juz of the surah's opening verse from the verified corpus partitions.
    val juzNumber = remember(surah.number) {
        QuranDataSource.getAyahsForSurah(surah.number).firstOrNull()?.juzNumber ?: 1
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardSurfaceShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "سُورَةُ ${surah.arabicName}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (!arabicUi) {
                Text(
                    text = "${strings.surahTab} ${surah.englishName} — ${surah.englishTranslation}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "$revelation · ${surah.totalVerses} ${strings.versesCount} · ${strings.juzWord} $juzNumber",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (surah.number != 9 && surah.number != 1) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Verse card (nour-style): surah:ayah badge, play/bookmark/copy/share
 * actions, Uthmani Arabic with end marker, divider, English translation.
 */
@Composable
private fun VerseCard(
    ayah: Ayah,
    surah: Surah,
    fontScale: Float,
    isPlaying: Boolean,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onPlayClick: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("ayah_card_${ayah.ayahNumber}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row with verse badge & actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CardSurfaceShape
                ) {
                    Text(
                        text = "${ayah.surahNumber}:${ayah.ayahNumber}",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlayClick, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onToggleBookmark, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(ayahShareText(ayah, surah)))
                            Toast.makeText(context, strings.verseCopiedToast, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = strings.copyVerseLabel,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, ayahShareText(ayah, surah) + " — via SALAH")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, strings.shareChooserTitle))
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = strings.shareVerseLabel,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic text (RTL, large, high aesthetic)
            Text(
                text = "${ayah.textArabic} \u06DD${QuranDataSource.toArabicDigits(ayah.ayahNumber)}",
                fontSize = (22 * fontScale).sp,
                fontFamily = FontFamily.Serif,
                lineHeight = (36 * fontScale).sp,
                textAlign = TextAlign.Right,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // English translation (Saheeh International, bundled offline)
            Text(
                text = ayah.textEnglish,
                fontSize = (13 * fontScale).sp,
                lineHeight = (18 * fontScale).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = strings.translationCreditLine,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

private fun ayahShareText(ayah: Ayah, surah: Surah): String {
    return "${ayah.textArabic}\n\n\"${ayah.textEnglish}\"\n[${surah.englishName} ${ayah.surahNumber}:${ayah.ayahNumber}]"
}

/**
 * Flowing continuous text (nour ContinuousQuranTextCard): the whole surah as
 * one RTL block with traditional ۝ markers; tap a verse to select it.
 */
@Composable
private fun ContinuousQuranTextCard(
    ayahs: List<Ayah>,
    fontScale: Float,
    selectedAyahNumber: Int?,
    onSelectAyahNumber: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val markerColor = MaterialTheme.colorScheme.primary
    val highlightFill = MaterialTheme.colorScheme.primaryContainer
    val highlightText = MaterialTheme.colorScheme.onPrimaryContainer

    val continuousText = remember(ayahs, selectedAyahNumber, fontScale) {
        buildAnnotatedString {
            ayahs.forEach { ayah ->
                val isSelected = ayah.ayahNumber == selectedAyahNumber
                pushStringAnnotation(tag = "AYAH", annotation = "${ayah.ayahNumber}")
                if (isSelected) {
                    pushStyle(
                        SpanStyle(
                            background = highlightFill.copy(alpha = 0.5f),
                            color = highlightText,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                append(ayah.textArabic)
                append(" ")
                pushStyle(
                    SpanStyle(
                        color = markerColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = (22 * fontScale * 0.9f).sp
                    )
                )
                append("\u06DD${QuranDataSource.toArabicDigits(ayah.ayahNumber)} ")
                pop()
                if (isSelected) pop()
                pop()
            }
        }
    }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("continuous_text_surface"),
        shape = CardSurfaceShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = continuousText,
                fontSize = (22 * fontScale).sp,
                lineHeight = (22 * fontScale * 2.1f).sp,
                textAlign = TextAlign.Right,
                style = TextStyle(textDirection = TextDirection.Rtl),
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(continuousText) {
                        detectTapGestures { offset ->
                            textLayoutResult?.let { layout ->
                                val charOffset = layout.getOffsetForPosition(offset)
                                val tapped = continuousText.getStringAnnotations(
                                    tag = "AYAH",
                                    start = charOffset,
                                    end = charOffset
                                ).firstOrNull()?.item?.toIntOrNull()
                                if (tapped != null) onSelectAyahNumber(tapped)
                            }
                        }
                    },
                onTextLayout = { textLayoutResult = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.tapVerseHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Inspector for the verse tapped in continuous mode (nour AyahInspectorCard):
 * translation plus bookmark/copy/share actions.
 */
@Composable
private fun AyahInspectorCard(
    ayah: Ayah,
    surah: Surah,
    fontScale: Float,
    isBookmarked: Boolean,
    onDismiss: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ayah_inspector_card"),
        shape = CardSurfaceShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CardSurfaceShape
                ) {
                    Text(
                        text = "${surah.englishName} (${ayah.surahNumber}:${ayah.ayahNumber})",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = strings.hideTranslationLabel)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${ayah.textArabic} \u06DD${QuranDataSource.toArabicDigits(ayah.ayahNumber)}",
                fontSize = (18 * fontScale).sp,
                lineHeight = (30 * fontScale).sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = ayah.textEnglish,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onToggleBookmark,
                    shape = CardSurfaceShape,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isBookmarked) "Saved" else "Save", style = MaterialTheme.typography.labelMedium)
                }
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(ayahShareText(ayah, surah)))
                        Toast.makeText(context, strings.verseCopiedToast, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = strings.copyVerseLabel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, ayahShareText(ayah, surah) + " — via SALAH")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, strings.shareChooserTitle))
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = strings.shareVerseLabel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * End-of-surah previous/next navigation (nour-style).
 */
@Composable
private fun SurahNavCard(
    surah: Surah,
    onSelectSurah: (Surah) -> Unit
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardSurfaceShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    if (surah.number > 1) {
                        QuranDataSource.getSurahByNumber(surah.number - 1)?.let { onSelectSurah(it) }
                    }
                },
                enabled = surah.number > 1,
                shape = CardSurfaceShape
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.previousSurahLabel)
            }
            TextButton(
                onClick = {
                    if (surah.number < 114) {
                        QuranDataSource.getSurahByNumber(surah.number + 1)?.let { onSelectSurah(it) }
                    }
                },
                enabled = surah.number < 114,
                shape = CardSurfaceShape
            ) {
                Text(strings.nextSurahLabel)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}
