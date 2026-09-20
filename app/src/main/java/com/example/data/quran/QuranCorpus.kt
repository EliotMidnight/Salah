package com.example.data.quran

import com.example.data.model.Ayah
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Verified offline Quran corpus: Tanzil Uthmani 1.1 Arabic text plus the
 * Saheeh International English translation. See bundled quran/SOURCES.md
 * for provenance, licensing and attribution.
 *
 * Files live under app/src/main/resources/quran and are packaged as Java
 * resources; each file's SHA-256 is verified at load and the corpus asserts
 * 114 surahs, 6,236 verses in canonical order, and the 604 page / 30 juz /
 * 240 hizb-quarter partitions before it is served.
 */
internal object QuranCorpus {
    const val TEXT_HASH = "6933e133dd56db778c801bf738848454e43648105a151e8d84d86a7cae39ec5f"
    const val METADATA_HASH = "8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a"
    const val EN_SAHIH_HASH = "f090ed258e647a393ddaa0b48f1e81ab7a5ccd5ad7d72d0da498e151bc0dbc18"

    private fun resource(name: String, hash: String): ByteArray {
        val bytes = requireNotNull(javaClass.getResourceAsStream("/quran/$name")) {
            "Missing Quran resource: $name"
        }.use { it.readBytes() }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        require(digest == hash) { "Quran resource integrity check failed: $name" }
        return bytes
    }

    private data class Partition(val index: Int, val reference: Int)
    private fun reference(surah: Int, ayah: Int) = surah * 1000 + ayah

    private val metadata by lazy {
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(resource("metadata.xml", METADATA_HASH).inputStream())
    }

    private fun partitions(tag: String): List<Partition> {
        val nodes = metadata.getElementsByTagName(tag)
        return (0 until nodes.length).map { index ->
            val node = nodes.item(index) as Element
            Partition(
                node.getAttribute("index").toInt(),
                reference(node.getAttribute("sura").toInt(), node.getAttribute("aya").toInt())
            )
        }
    }

    private val pages by lazy { partitions("page").also { require(it.size == 604) } }
    private val juzs by lazy { partitions("juz").also { require(it.size == 30) } }
    private val quarters by lazy { partitions("quarter").also { require(it.size == 240) } }

    fun quarterFor(surah: Int, ayah: Int): Int =
        quarters.last { it.reference <= reference(surah, ayah) }.index

    private val englishByRef: Map<Int, String> by lazy {
        val text = resource("en_sahihintl.txt", EN_SAHIH_HASH).toString(Charsets.UTF_8)
        val map = HashMap<Int, String>(7000)
        text.lineSequence().forEach { line ->
            if (line.isBlank()) return@forEach
            val fields = line.split('|', limit = 3)
            require(fields.size == 3 && fields[2].isNotBlank()) { "Bad translation line: $line" }
            map[reference(fields[0].toInt(), fields[1].toInt())] = fields[2]
        }
        require(map.size == 6236) { "Expected 6236 translated verses, got ${map.size}" }
        map
    }

    val ayahs: List<Ayah> by lazy {
        // Force translation load first so a corrupt translation fails fast with a clear error.
        val translations = englishByRef
        val text = resource("uthmani.txt", TEXT_HASH).toString(Charsets.UTF_8).removePrefix("﻿")
        val verses = text.lineSequence().filter { it.firstOrNull()?.isDigit() == true }.map { line ->
            val fields = line.split('|', limit = 3)
            require(fields.size == 3 && fields[2].isNotBlank())
            val surah = fields[0].toInt()
            val ayah = fields[1].toInt()
            val ref = reference(surah, ayah)
            Ayah(
                surahNumber = surah,
                ayahNumber = ayah,
                textArabic = fields[2],
                textEnglish = translations[ref] ?: "",
                pageNumber = pages.last { it.reference <= ref }.index,
                juzNumber = juzs.last { it.reference <= ref }.index,
                hizbQuarter = quarterFor(surah, ayah)
            )
        }.toList()
        require(verses.size == 6236)
        val suras = metadata.getElementsByTagName("sura")
        require(suras.length == 114)
        val expected = (0 until suras.length).flatMap { i ->
            val node = suras.item(i) as Element
            val surah = node.getAttribute("index").toInt()
            (1..node.getAttribute("ayas").toInt()).map { reference(surah, it) }
        }
        require(verses.map { reference(it.surahNumber, it.ayahNumber) } == expected)
        verses
    }
}
