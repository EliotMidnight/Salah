package com.example.ui.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ayah
import com.example.data.model.RevelationType
import com.example.data.model.Surah
import com.example.data.quran.QuranDataSource
import com.example.ui.SalahUiState
import com.example.ui.localization.LocalStrings

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

    val strings = LocalStrings.current
    val listState = rememberLazyListState()

    // Auto scroll when jumping to active ayah
    LaunchedEffect(state.activeReadingAyahNumber, inReaderMode) {
        if (inReaderMode && state.currentSurahAyahs.isNotEmpty()) {
            val index = (state.activeReadingAyahNumber - 1).coerceIn(0, state.currentSurahAyahs.lastIndex)
            listState.animateScrollToItem(index)
        }
    }

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
            // Safe insets padding for top camera cutout
            Spacer(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                    .height(6.dp)
            )

            // 1. TOP CHIPS (Quran.json layout: Surah, Page, Juz', Hizb, Bookmarks)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuranTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val tabLabel = when (tab) {
                        QuranTab.SURAH -> strings.surahTab
                        QuranTab.PAGE -> strings.pageTab
                        QuranTab.JUZ -> strings.juzTab
                        QuranTab.HIZB -> strings.hizbTab
                        QuranTab.BOOKMARKS -> "${strings.bookmarksTab} (${state.bookmarks.size})"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTab = tab
                        },
                        label = {
                            Text(
                                text = tabLabel,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("quran_tab_${tab.name.lowercase()}")
                    )
                }
            }

            // 2. DIVIDER (Quran.json: x: 34, y: 40)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )

            // 3. ROW: CONTINUE READING PILL + 56DP SEARCH BUTTON (Quran.json: x: 34, y: 56)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Continue reading bar (fill: surfaceContainerHigh, radius: 48, height: 56)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .clickable {
                            onSurahSelected(state.continueReading.surahNumber)
                            inReaderMode = true
                        }
                        .testTag("quran_continue_reading_pill"),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = strings.continueReading,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.continueReading.surahName,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${strings.continueReading} · Ayah ${state.continueReading.ayahNumber}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Search Icon Button (Quran.json: kind: iconButton, variant: filled, size: 56)
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable { isSearchOpen = !isSearchOpen }
                        .testTag("quran_search_button"),
                    color = if (isSearchOpen || searchQuery.isNotEmpty()) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchOpen || searchQuery.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 4. DIVIDER (Quran.json: x: 34, y: 128)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )

            // 5. ANIMATED SEARCH INPUT (Toggled by Search button)
            AnimatedVisibility(
                visible = isSearchOpen || searchQuery.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            when (selectedTab) {
                                QuranTab.SURAH -> "Search surah name, Arabic, or number..."
                                QuranTab.PAGE -> "Search page number (1–604)..."
                                QuranTab.JUZ -> "Search Juz' number or name..."
                                QuranTab.HIZB -> "Search Hizb number..."
                                QuranTab.BOOKMARKS -> "Search bookmarks..."
                            }
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
            }

            // 6. LIST OF 56DP PILL BOXES (Quran.json: size2: 56, radius: 48, fill: surfaceContainerHigh)
            when (selectedTab) {
                QuranTab.SURAH -> {
                    val filteredSurahs = QuranDataSource.SURAHS.filter {
                        if (searchQuery.isBlank()) true
                        else {
                            it.englishName.contains(searchQuery, ignoreCase = true) ||
                                    it.arabicName.contains(searchQuery) ||
                                    it.englishTranslation.contains(searchQuery, ignoreCase = true) ||
                                    it.number.toString() == searchQuery.trim()
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredSurahs, key = { it.number }) { surah ->
                            QuranPillItem(
                                badgeText = "${surah.number}",
                                title = surah.englishName,
                                subtitle = "${surah.totalVerses} verses · ${surah.revelationType.labelEn}",
                                trailingText = surah.arabicName,
                                onClick = {
                                    onSurahSelected(surah.number)
                                    inReaderMode = true
                                },
                                testTag = "surah_item_${surah.number}"
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }

                QuranTab.PAGE -> {
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
                            val surah = QuranDataSource.SURAHS.lastOrNull { it.startPage <= page }
                                ?: QuranDataSource.SURAHS[0]
                            QuranPillItem(
                                badgeText = "P.$page",
                                title = "Page $page",
                                subtitle = "Surah ${surah.englishName}",
                                trailingText = "ص $page",
                                onClick = {
                                    onPageSelected(page)
                                    onSurahSelected(surah.number)
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
                            QuranPillItem(
                                badgeText = "J.${juz.number}",
                                title = "Juz' ${juz.number} · ${juz.englishName}",
                                subtitle = "${juz.startSurahName} · Page ${juz.startPage}",
                                trailingText = juz.arabicName,
                                onClick = {
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
                            QuranPillItem(
                                badgeText = "H.${hizb.number}",
                                title = hizb.englishName,
                                subtitle = "Starting from Page ${hizb.startPage}",
                                trailingText = hizb.arabicName,
                                onClick = {
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
                                    else "No bookmarks saved yet.\nTap the bookmark icon beside any Ayah in reader mode to save it.",
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
                                QuranPillItem(
                                    badgeText = "${bm.surahNumber}:${bm.ayahNumber}",
                                    title = "${bm.surahName} (${bm.surahNumber}:${bm.ayahNumber})",
                                    subtitle = bm.ayahSnippet.take(40) + if (bm.ayahSnippet.length > 40) "..." else "",
                                    trailingText = "",
                                    onClick = {
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
        } else {
            // MUSHAF READER MODE
            ReaderView(
                surah = state.selectedSurah,
                ayahs = state.currentSurahAyahs,
                bookmarks = state.bookmarks,
                fontScale = state.quranFontScale,
                isAudioPlaying = state.isAudioPlaying,
                currentAudioAyah = state.currentAudioAyah,
                showFontSlider = showFontSlider,
                listState = listState,
                onBack = { inReaderMode = false },
                onToggleFontSlider = { showFontSlider = !showFontSlider },
                onFontScaleChange = onFontScaleChange,
                onAyahClick = { onAyahViewed(it) },
                onToggleBookmark = onToggleBookmark,
                onTogglePlayAyah = onTogglePlayAyah,
                onStopAudio = onStopAudio
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
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp)
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

@Composable
private fun ReaderView(
    surah: Surah,
    ayahs: List<Ayah>,
    bookmarks: List<com.example.data.local.BookmarkEntity>,
    fontScale: Float,
    isAudioPlaying: Boolean,
    currentAudioAyah: Int,
    showFontSlider: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onBack: () -> Unit,
    onToggleFontSlider: () -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onAyahClick: (Ayah) -> Unit,
    onToggleBookmark: (Ayah) -> Unit,
    onTogglePlayAyah: (Ayah) -> Unit,
    onStopAudio: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Reader Header with safe cutout insets
        Surface(
            modifier = Modifier
                .fillMaxWidth()
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Column {
                            Text(
                                text = surah.englishName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${surah.arabicName} · Page ${surah.startPage}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Serif
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = onToggleFontSlider) {
                            Icon(Icons.Default.FormatSize, contentDescription = "Font size")
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

        // Bismillah Banner (except Surah 9)
        if (surah.number != 9 && surah.number != 1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    fontSize = (22 * fontScale).sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Ayah List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            items(ayahs) { ayah ->
                val isPlaying = isAudioPlaying && currentAudioAyah == ayah.ayahNumber
                val isBookmarked = bookmarks.any { it.surahNumber == ayah.surahNumber && it.ayahNumber == ayah.ayahNumber }

                AyahCard(
                    ayah = ayah,
                    fontScale = fontScale,
                    isPlaying = isPlaying,
                    isBookmarked = isBookmarked,
                    onClick = { onAyahClick(ayah) },
                    onToggleBookmark = { onToggleBookmark(ayah) },
                    onPlayClick = { onTogglePlayAyah(ayah) }
                )
                Spacer(modifier = Modifier.height(10.dp))
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

@Composable
private fun AyahCard(
    ayah: Ayah,
    fontScale: Float,
    isPlaying: Boolean,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onPlayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("ayah_card_${ayah.ayahNumber}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Action bar per Ayah
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${ayah.ayahNumber}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlayClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onToggleBookmark, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic text (RTL, large, high aesthetic)
            Text(
                text = ayah.textArabic,
                fontSize = (22 * fontScale).sp,
                fontFamily = FontFamily.Serif,
                lineHeight = (36 * fontScale).sp,
                textAlign = TextAlign.Right,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // English translation
            Text(
                text = ayah.textEnglish,
                fontSize = (13 * fontScale).sp,
                lineHeight = (18 * fontScale).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
