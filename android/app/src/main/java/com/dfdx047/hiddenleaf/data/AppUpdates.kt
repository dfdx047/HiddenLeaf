package com.dfdx047.hiddenleaf.data

import android.content.Context
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

const val REPO = "dfdx047/HiddenLeaf"
const val REPO_URL = "https://github.com/$REPO"

data class UpdateInfo(val version: String, val notes: String, val apkUrl: String, val size: Long, val nightly: Boolean)

/** The update dialog's state. */
sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val done: Long, val total: Long) : UpdateState
    /** Downloaded, but Android has not yet let this app install packages. */
    data class NeedPermission(val info: UpdateInfo) : UpdateState
}

/**
 * Looks at the repository's GitHub releases. A test build (0.0.0-dev.N, version code N = the CI run) follows the
 * "android-nightly" pre-release, whose text carries the run number; a normal build follows the highest version among
 * the `vX.Y.Z[-beta.N]` (and the older `android-vX.Y.Z`) releases, pre-releases included while the app is in beta:
 * 0.1.0-beta < 0.1.0-beta.2 < 0.1.0.
 *
 * Automatically falls back to GitHub's Atom feed + expanded_assets HTML when the anonymous REST API hits HTTP 403.
 */
object AppUpdates {
    private data class Candidate(val version: String, val tag: String, val body: String, val apkUrl: String, val size: Long, val nightly: Boolean)

    suspend fun check(currentName: String, currentCode: Int, lang: String = java.util.Locale.getDefault().language): UpdateInfo? {
        val dev = currentName.contains("-dev")
        val cand = runCatching { bestFromApi(dev, currentCode) }.getOrNull()
            ?: (if (!dev) bestFromAtom() else null)
            ?: return null
        if (!cand.nightly && compareVersions(cand.version, currentName) <= 0) return null
        val notes = if (cand.nightly) cand.body else fetchChangelog(cand.tag, cand.version, lang, cand.body)
        return UpdateInfo(cand.version, notes, cand.apkUrl, cand.size, cand.nightly)
    }

    private suspend fun bestFromApi(dev: Boolean, currentCode: Int): Candidate? {
        val releases = JSONArray(Net.getText("https://api.github.com/repos/$REPO/releases?per_page=30"))
        var best: Candidate? = null
        for (i in 0 until releases.length()) {
            val r = releases.getJSONObject(i)
            if (r.optBoolean("draft")) continue
            val tag = r.optString("tag_name")
            val (apkUrl, size) = apkAsset(r) ?: continue
            if (dev) {
                if (tag != "android-nightly") continue
                val run = Regex("""dev\.(\d+)""").find(r.optString("body"))?.groupValues?.get(1)?.toIntOrNull() ?: return null
                return if (run > currentCode) Candidate("0.0.0-dev.$run", tag, "", apkUrl, size, true) else null
            }
            val name = versionOf(tag) ?: continue
            if (best == null || compareVersions(name, best.version) > 0) {
                best = Candidate(name, tag, r.optString("body").trim().take(12000), apkUrl, size, false)
            }
        }
        return best
    }

    private suspend fun bestFromAtom(): Candidate? {
        val xml = Net.getText("https://github.com/$REPO/releases.atom")
        val tags = parseAtomTags(xml)
        var bestPair: Pair<String, String>? = null
        for (tag in tags) {
            val name = versionOf(tag) ?: continue
            if (bestPair == null || compareVersions(name, bestPair.first) > 0) {
                bestPair = name to tag
            }
        }
        val (name, tag) = bestPair ?: return null
        val encodedTag = URLEncoder.encode(tag, "UTF-8").replace("+", "%20")
        val html = Net.getText("https://github.com/$REPO/releases/expanded_assets/$encodedTag")
        val href = Regex("""href="(/${Regex.escape(REPO)}/releases/download/[^"]+\.apk)"""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1) ?: return null
        return Candidate(name, tag, "", "https://github.com$href", -1L, false)
    }

    internal fun parseAtomTags(xml: String): List<String> {
        val tagRegex = Regex("""/releases/tag/([^"<\s]+)""")
        val out = LinkedHashSet<String>()
        for (m in tagRegex.findAll(xml)) out += m.groupValues[1]
        return out.toList()
    }

    private suspend fun fetchChangelog(tag: String, version: String, lang: String, fallbackBody: String): String {
        suspend fun fetch(url: String) = runCatching { Net.getText(url) }.getOrDefault("")
        val fileNames = when (lang.lowercase()) {
            "pt" -> listOf("CHANGELOG.pt-BR.md", "CHANGELOG.md")
            "es" -> listOf("CHANGELOG.es.md", "CHANGELOG.md")
            else -> listOf("CHANGELOG.md")
        }
        // Try the release's exact tag first, then `main` (covers releases whose tag was moved or updated afterwards).
        for (ref in listOf(tag, "main").distinct()) {
            val raw = "https://raw.githubusercontent.com/$REPO/$ref"
            for (name in fileNames) {
                val content = fetch("$raw/$name")
                if (content.isBlank()) continue
                val notes = section(content, version)
                if (notes.isNotBlank()) return notes
            }
        }
        val fromBody = extractBodyForLanguage(fallbackBody, lang)
        return fromBody.ifBlank { fallbackBody.trim().take(8000) }
    }

    /** Reads the localized changelog bundled in APK assets (`assets/changelog/CHANGELOG*.md`). */
    fun loadBundledNotes(context: Context, version: String, lang: String = java.util.Locale.getDefault().language): String {
        val fileNames = when (lang.lowercase()) {
            "pt" -> listOf("changelog/CHANGELOG.pt-BR.md", "changelog/CHANGELOG.md")
            "es" -> listOf("changelog/CHANGELOG.es.md", "changelog/CHANGELOG.md")
            else -> listOf("changelog/CHANGELOG.md")
        }
        for (path in fileNames) {
            val text = runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }.getOrDefault("")
            if (text.isBlank()) continue
            val sec = section(text, version)
            if (sec.isNotBlank()) return sec
        }
        return ""
    }

    /** "v0.1.0-beta" or "android-v0.2.0" -> "0.1.0-beta" / "0.2.0"; anything else (the nightly) -> null. */
    internal fun versionOf(tag: String): String? {
        val v = tag.removePrefix("android-").removePrefix("v")
        return v.takeIf { parse(it) != null && v != tag }
    }

    /**
     * Extracts [version]'s section (or the newest `## ` section when [version] is null / not explicitly found)
     * from a CHANGELOG file, without the heading line itself.
     */
    internal fun section(changelog: String, version: String? = null): String {
        val lines = changelog.replace("\r", "").lines()
        val from = if (!version.isNullOrBlank()) {
            val clean = version.removePrefix("v")
            lines.indexOfFirst { it.startsWith("## ") && (it.contains("v$clean") || it.contains(clean)) }
                .takeIf { it >= 0 } ?: lines.indexOfFirst { it.startsWith("## ") }
        } else {
            lines.indexOfFirst { it.startsWith("## ") }
        }
        if (from < 0) return ""
        val to = (from + 1 until lines.size).firstOrNull { lines[it].startsWith("## ") } ?: lines.size
        return lines.subList(from + 1, to).joinToString("\n").trim().take(8000)
    }

    /**
     * When falling back to a trilingual GitHub Release body (`## 🇧🇷 Português` / `## 🇺🇸 English` / `## 🇪🇸 Español`),
     * returns only the user's language block instead of showing all three languages concatenated.
     */
    internal fun extractBodyForLanguage(body: String, lang: String): String {
        if (body.isBlank()) return ""
        val lines = body.replace("\r", "").lines()
        val markers = when (lang.lowercase()) {
            "pt" -> listOf("Português", "🇧🇷")
            "es" -> listOf("Español", "🇪🇸")
            else -> listOf("English", "🇺🇸")
        }
        val from = lines.indexOfFirst { line ->
            line.startsWith("## ") && markers.any { line.contains(it, ignoreCase = true) }
        }
        if (from < 0) return body.trim().take(8000)
        val to = (from + 1 until lines.size).firstOrNull { lines[it].startsWith("## ") } ?: lines.size
        return lines.subList(from + 1, to).joinToString("\n").trim().take(8000)
    }

    /** Mirrors app/build.gradle.kts versionCode formula so releases always increase monotonically and install in place. */
    internal fun expectedVersionCode(vName: String): Int {
        val vParts = vName.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val vSuffix = vName.substringAfter('-', "")
        val vPre = if (vSuffix.isEmpty()) 99 else (Regex("""(\d+)$""").find(vSuffix)?.value?.toIntOrNull() ?: 1).coerceIn(1, 98)
        return maxOf(1, (vParts.getOrElse(0) { 0 } * 10000 + vParts.getOrElse(1) { 0 } * 100 + vParts.getOrElse(2) { 0 }) * 100 + vPre)
    }

    /**
     * Compares "1.2.3", "1.2.3-beta", "1.2.3-beta.2": numbers first; for the same numbers a release without a suffix
     * is newer than one with it, and suffixes compare piece by piece (numbers as numbers, a longer one is newer).
     */
    internal fun compareVersions(a: String, b: String): Int {
        val c = compare(parse(a) ?: listOf(0), parse(b) ?: listOf(0))
        if (c != 0) return c
        val sa = a.substringAfter('-', ""); val sb = b.substringAfter('-', "")
        if (sa.isEmpty() || sb.isEmpty()) return (if (sa.isEmpty()) 1 else 0) - (if (sb.isEmpty()) 1 else 0)
        val pa = sa.split('.'); val pb = sb.split('.')
        for (i in 0 until minOf(pa.size, pb.size)) {
            val na = pa[i].toIntOrNull(); val nb = pb[i].toIntOrNull()
            val d = if (na != null && nb != null) na.compareTo(nb) else pa[i].compareTo(pb[i])
            if (d != 0) return d
        }
        return pa.size - pb.size
    }

    private fun apkAsset(r: JSONObject): Pair<String, Long>? {
        val assets = r.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name").endsWith(".apk")) return a.getString("browser_download_url") to a.optLong("size", -1)
        }
        return null
    }

    private fun parse(v: String): List<Int>? =
        v.substringBefore('-').split('.').map { it.toIntOrNull() ?: return null }.takeIf { it.isNotEmpty() }

    private fun compare(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val d = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
            if (d != 0) return d
        }
        return 0
    }
}

data class CatalogPack(val name: String, val description: String, val url: String, val size: Long)

sealed interface CatalogState {
    object Loading : CatalogState
    object Failed : CatalogState
    data class Loaded(val packs: List<CatalogPack>) : CatalogState
}

/**
 * The list of texture packs the app offers to download: android/texture-packs.json in the repository, read from
 * `main`, so packs can be added or fixed without a new app version.
 * Format: {"packs":[{"name":"…","description":"…","url":"https://…/pack.zip","size":123456}]}
 */
object TextureCatalog {
    const val URL = "https://raw.githubusercontent.com/$REPO/main/android/texture-packs.json"

    suspend fun load(): List<CatalogPack> {
        val arr = JSONObject(Net.getText(URL)).optJSONArray("packs") ?: return emptyList()
        return (0 until arr.length()).mapNotNull {
            val o = arr.getJSONObject(it)
            val url = o.optString("url")
            if (!url.startsWith("https://")) null
            else CatalogPack(o.optString("name", "Texture pack"), o.optString("description"), url, o.optLong("size", -1))
        }
    }
}
