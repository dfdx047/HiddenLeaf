package com.dfdx047.hiddenleaf.data

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
 */
object AppUpdates {
    suspend fun check(currentName: String, currentCode: Int): UpdateInfo? {
        val releases = JSONArray(Net.getText("https://api.github.com/repos/$REPO/releases?per_page=30"))
        val dev = currentName.contains("-dev")
        var best: UpdateInfo? = null
        var bestTag = ""
        for (i in 0 until releases.length()) {
            val r = releases.getJSONObject(i)
            if (r.optBoolean("draft")) continue
            val tag = r.optString("tag_name")
            val (apkUrl, size) = apkAsset(r) ?: continue
            if (dev) {
                if (tag != "android-nightly") continue
                val run = Regex("""dev\.(\d+)""").find(r.optString("body"))?.groupValues?.get(1)?.toIntOrNull() ?: return null
                return if (run > currentCode) UpdateInfo("0.0.0-dev.$run", "", apkUrl, size, true) else null
            }
            val name = versionOf(tag) ?: continue
            if (best == null || compareVersions(name, best.version) > 0) {
                best = UpdateInfo(name, r.optString("body").trim().take(8000), apkUrl, size, false)
                bestTag = tag
            }
        }
        val found = best?.takeIf { compareVersions(it.version, currentName) > 0 } ?: return null
        // The notes: that version's section of the changelog in the repository at its tag (CHANGELOG.pt-BR.md,
        // CHANGELOG.es.md or CHANGELOG.md), else the release's own text.
        val lang = java.util.Locale.getDefault().language
        val raw = "https://raw.githubusercontent.com/$REPO/$bestTag"
        suspend fun fetch(url: String) = runCatching { Net.getText(url) }.getOrDefault("")
        val file = when (lang) {
            "pt" -> fetch("$raw/CHANGELOG.pt-BR.md")
            "es" -> fetch("$raw/CHANGELOG.es.md")
            else -> ""
        }.ifBlank { fetch("$raw/CHANGELOG.md") }
        val notes = section(file)
        return if (notes.isNotBlank()) found.copy(notes = notes) else found
    }

    /** "v0.1.0-beta" or "android-v0.2.0" -> "0.1.0-beta" / "0.2.0"; anything else (the nightly) -> null. */
    internal fun versionOf(tag: String): String? {
        val v = tag.removePrefix("android-").removePrefix("v")
        return v.takeIf { parse(it) != null && v != tag }
    }

    /** The newest version's part of a changelog: from its first "## " heading to the next one, without the heading. */
    internal fun section(changelog: String): String {
        val lines = changelog.lines()
        val from = lines.indexOfFirst { it.startsWith("## ") }
        if (from < 0) return ""
        val to = (from + 1 until lines.size).firstOrNull { lines[it].startsWith("## ") } ?: lines.size
        return lines.subList(from + 1, to).joinToString("\n").trim().take(8000)
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
