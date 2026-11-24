package com.dfdx047.hiddenleaf.data

import org.json.JSONArray
import java.net.URLEncoder

/** A driver package published on GitHub: one asset (.zip or .adpkg) of a release. */
data class DriverAsset(val name: String, val url: String, val size: Long)

data class DriverRelease(val title: String, val tag: String, val date: String, val assets: List<DriverAsset>)

/**
 * The open Mesa Vulkan drivers, one per GPU maker (the Drivers page's tabs): Turnip for Qualcomm Adreno, PanVK for
 * Arm Mali (MaliSX2 is ARMSX2's build of it), RADV for Samsung Xclipse (AMD RDNA).
 */
enum class DriverFamily(val label: String, val gpuMaker: String) {
    TURNIP("Turnip", "Adreno"),
    PANVK("PanVK", "Mali"),
    RADV("RADV", "Xclipse"),
    ;

    companion object {
        /** The family made for this GPU (null when unknown). */
        fun forGpu(gpu: String): DriverFamily? = when {
            isAdreno(gpu) -> TURNIP
            gpu.contains("Mali", true) || gpu.contains("Immortalis", true) -> PANVK
            gpu.contains("Xclipse", true) -> RADV
            else -> null
        }
    }
}

fun isAdreno(gpu: String) = gpu.contains("Adreno", ignoreCase = true)

/** Mali GPUs PanVK runs on (Bifrost, Valhall and newer: the G series and Immortalis). */
fun isPanvkGpu(gpu: String) = (gpu.contains("Mali-G", true) || gpu.contains("Mali G", true) || gpu.contains("Immortalis", true))

private val MALISX2_GPUS = Regex("""\bG(?:52|57|68|77|78|615|715)(?!\d)""", RegexOption.IGNORE_CASE)
fun isMaliSx2Gpu(gpu: String) =
    (gpu.contains("Mali", ignoreCase = true) || gpu.contains("Immortalis", ignoreCase = true)) && MALISX2_GPUS.containsMatchIn(gpu)

/** Where drivers come from: GitHub repositories filtered by GPU family. */
enum class DriverSource(
    val label: String,
    val repo: String,
    val family: DriverFamily,
    val gpus: (String) -> Boolean = { true },
    val experimental: Boolean = false,
    val androidZip: (String) -> Boolean = { true },
) {
    // Qualcomm Adreno (Turnip)
    ARMSX2_TURNIP("ARMSX2 · Turnip", "bmdhacks/armsx2-turnip", DriverFamily.TURNIP, ::isAdreno),
    MR_PURPLE("Mr. Purple", "MrPurple666/purple-turnip", DriverFamily.TURNIP, ::isAdreno),
    STEVEN("StevenMXZ", "StevenMXZ/Adreno-Tools-Drivers", DriverFamily.TURNIP, ::isAdreno),
    KIMCHI("K11MCH1", "K11MCH1/AdrenoToolsDrivers", DriverFamily.TURNIP, ::isAdreno),
    CRUETER("crueter (8 Elite)", "crueter/GameHub-8Elite-Drivers", DriverFamily.TURNIP, ::isAdreno),
    BANNER(
        "Banner's Turnip", "The412Banner/Banners-Turnip", DriverFamily.TURNIP, ::isAdreno,
        androidZip = { !it.contains("-Linux", true) && !it.contains("-Wayland", true) },
    ),
    // Arm Mali (MaliSX2 & PanVK)
    MALISX2("ARMSX2 · MaliSX2", "bmdhacks/malisx2", DriverFamily.PANVK, ::isMaliSx2Gpu, experimental = true),
    JIM_PANVK("JimVulkan · PanVK", "JimVulkan/mali-panvk", DriverFamily.PANVK, ::isPanvkGpu, experimental = true),
    ZENITH_PANVK(
        "PanVK kbase", "zenithblue-oss/panvk-kbase-android", DriverFamily.PANVK, ::isPanvkGpu, experimental = true,
        androidZip = { it.contains("adrenotools", true) },
    ),
    FRISTONE_PANVK("FristOneRR · PanVK", "FristOneRR/FristOneRR-Panvk-Driver", DriverFamily.PANVK, ::isPanvkGpu, experimental = true),
    // Samsung Xclipse / AMD RDNA (RADV)
    JIM_RADV(
        "JimVulkan · RADV", "JimVulkan/radv-xclipse", DriverFamily.RADV, { it.contains("Xclipse", true) }, experimental = true,
        androidZip = { !it.contains("glibc", true) && !it.contains("wayland", true) },
    ),
    PAULO_RADV(
        "Paulo Wesley · RADV", "paulowesll/mesa", DriverFamily.RADV, { it.contains("Xclipse", true) }, experimental = true,
        androidZip = { !it.contains("glibc", true) && !it.contains("wayland", true) },
    ),
    ;

    val page: String get() = "https://github.com/$repo/releases"

    companion object {
        fun forGpu(gpu: String): List<DriverSource> = if (gpu.isBlank()) entries.toList() else entries.filter { it.gpus(gpu) }
        fun of(family: DriverFamily): List<DriverSource> = entries.filter { it.family == family }
    }
}

object DriverSources {
    /**
     * Fetches the newest releases of [source]. Tries GitHub's REST API first and automatically falls back to
     * GitHub's Atom feed + expanded_assets HTML when the unauthenticated REST API hits its 60 req/hr rate limit (HTTP 403).
     */
    suspend fun releases(source: DriverSource, count: Int = 12): List<DriverRelease> {
        val fromApi = runCatching { releasesFromApi(source, count) }.getOrNull()
        if (fromApi != null) return fromApi
        return releasesFromAtom(source, count.coerceAtMost(8))
    }

    private suspend fun releasesFromApi(source: DriverSource, count: Int): List<DriverRelease> {
        val a = JSONArray(Net.getText("https://api.github.com/repos/${source.repo}/releases?per_page=$count"))
        val out = mutableListOf<DriverRelease>()
        for (i in 0 until a.length()) {
            val r = a.optJSONObject(i) ?: continue
            if (r.optBoolean("draft")) continue
            val assets = mutableListOf<DriverAsset>()
            r.optJSONArray("assets")?.let { list ->
                for (j in 0 until list.length()) {
                    val x = list.optJSONObject(j) ?: continue
                    val name = x.optString("name")
                    if (!name.endsWith(".zip", true) && !name.endsWith(".adpkg", true)) continue
                    if (!source.androidZip(name)) continue
                    assets += DriverAsset(name, x.optString("browser_download_url"), x.optLong("size"))
                }
            }
            if (assets.isEmpty()) continue
            out += DriverRelease(
                title = r.optString("name").ifBlank { r.optString("tag_name") },
                tag = r.optString("tag_name"),
                date = r.optString("published_at").take(10),
                assets = assets,
            )
        }
        return out
    }

    private suspend fun releasesFromAtom(source: DriverSource, count: Int): List<DriverRelease> {
        val xml = Net.getText("https://github.com/${source.repo}/releases.atom")
        val entryRegex = Regex("<entry>(.*?)</entry>", RegexOption.DOT_MATCHES_ALL)
        val tagRegex = Regex("""/releases/tag/([^"<\s]+)""")
        val titleRegex = Regex("<title[^>]*>(.*?)</title>", RegexOption.DOT_MATCHES_ALL)
        val updatedRegex = Regex("<updated>([0-9]{4}-[0-9]{2}-[0-9]{2})")
        val hrefRegex = Regex("""href="(/${Regex.escape(source.repo)}/releases/download/[^"]+\.(?:zip|adpkg))"""", RegexOption.IGNORE_CASE)
        val out = mutableListOf<DriverRelease>()
        for (m in entryRegex.findAll(xml)) {
            if (out.size >= count) break
            val block = m.groupValues[1]
            val tag = tagRegex.find(block)?.groupValues?.get(1) ?: continue
            val title = titleRegex.find(block)?.groupValues?.get(1)?.trim()?.ifBlank { tag } ?: tag
            val date = updatedRegex.find(block)?.groupValues?.get(1) ?: ""
            val encodedTag = URLEncoder.encode(tag, "UTF-8").replace("+", "%20")
            val html = runCatching { Net.getText("https://github.com/${source.repo}/releases/expanded_assets/$encodedTag") }.getOrNull() ?: continue
            val seen = HashSet<String>()
            val assets = mutableListOf<DriverAsset>()
            for (hm in hrefRegex.findAll(html)) {
                val rel = hm.groupValues[1]
                if (!seen.add(rel)) continue
                val name = rel.substringAfterLast('/')
                if (!source.androidZip(name)) continue
                assets += DriverAsset(name, "https://github.com$rel", 0L)
            }
            if (assets.isNotEmpty()) {
                out += DriverRelease(title = title, tag = tag, date = date, assets = assets)
            }
        }
        return out
    }
}
