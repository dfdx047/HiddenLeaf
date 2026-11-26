package com.dfdx047.hiddenleaf.data

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Phase 8 online features: Ranked by points (Elo/RP + Ninja tiers), Global Top 100 Leaderboard (cached 60s),
 * Custom Lobbies (2–8 players, King-of-the-Hill queue), and Spectator mode.
 */
object OnlineAdvanced {

    data class CharMastery(val xp: Int, val lvl: Int, val matches: Int)

    data class RankedProfile(
        val playerId: String,
        val name: String,
        val points: Int,
        val wins: Int,
        val losses: Int,
        val streak: Int,
        val rank: Int,
        val tier: String,
        val favChar: String = "naruto",
        val favCharLvl: Int = 1,
        val favCharXp: Int = 0,
        val mastery: Map<String, CharMastery> = emptyMap(),
    )

    data class LeaderboardEntry(
        val rank: Int,
        val playerId: String,
        val name: String,
        val points: Int,
        val wins: Int,
        val losses: Int,
        val streak: Int,
        val inputMode: String,
        val tier: String,
        val favChar: String = "naruto",
        val favCharLvl: Int = 1,
    )

    data class LobbyMember(val id: String, val name: String, val role: String)

    data class CustomLobby(
        val code: String,
        val playerId: String,
        val hostId: String,
        val maxPlayers: Int,
        val allowSpectators: Boolean,
        val inputMode: String,
        val activeRoom: String?,
        val members: List<LobbyMember>,
    )

    /** Stable local cryptographic identity for Ranked (cannot be spoofed without [secret]). */
    fun identity(context: Context): Pair<String, String> {
        val p = context.getSharedPreferences("online_ranked", Context.MODE_PRIVATE)
        var id = p.getString("player_id", null)
        var secret = p.getString("secret", null)
        if (id == null || secret == null) {
            val rnd = SecureRandom()
            val idBytes = ByteArray(12).also { rnd.nextBytes(it) }
            val secBytes = ByteArray(24).also { rnd.nextBytes(it) }
            id = "hl_" + idBytes.joinToString("") { "%02x".format(it) }
            secret = secBytes.joinToString("") { "%02x".format(it) }
            p.edit().putString("player_id", id).putString("secret", secret).apply()
        }
        return id to secret
    }

    fun savedFavChar(context: Context): String =
        context.getSharedPreferences("online_ranked", Context.MODE_PRIVATE).getString("fav_char", "naruto") ?: "naruto"

    fun saveFavChar(context: Context, charId: String) {
        context.getSharedPreferences("online_ranked", Context.MODE_PRIVATE).edit().putString("fav_char", charId).apply()
    }

    /** Maps UN5 internal character ID (fighter+0x68) to a stable slug; unknown/mod IDs map to "char_<id>". */
    fun charKeyForGameId(gameCharId: Int): String = when (gameCharId) {
        1 -> "naruto"
        2 -> "sasuke"
        3 -> "sakura"
        4 -> "kakashi"
        5 -> "lee"
        6 -> "neji"
        7 -> "gaara"
        8 -> "sai"
        9 -> "jiraiya"
        10 -> "itachi"
        11 -> "deidara"
        12 -> "minato"
        else -> "char_$gameCharId"
    }

    fun localMastery(context: Context): Map<String, CharMastery> {
        val raw = context.getSharedPreferences("online_ranked", Context.MODE_PRIVATE).getString("char_mastery", "{}") ?: "{}"
        return parseMasteryJson(runCatching { JSONObject(raw) }.getOrNull())
    }

    /** Records a fight played with [charKey] locally (per-character XP & level) and marks [charKey] as the active character. */
    fun recordLocalCharFight(context: Context, charKey: String, xpGain: Int = 60): CharMastery {
        val p = context.getSharedPreferences("online_ranked", Context.MODE_PRIVATE)
        val map = localMastery(context).toMutableMap()
        val cur = map[charKey] ?: CharMastery(0, 1, 0)
        val nextXp = cur.xp + xpGain
        val next = CharMastery(nextXp, (1 + nextXp / 200).coerceIn(1, 99), cur.matches + 1)
        map[charKey] = next
        val obj = JSONObject()
        for ((k, v) in map) obj.put(k, JSONObject().put("xp", v.xp).put("lvl", v.lvl).put("matches", v.matches))
        p.edit().putString("fav_char", charKey).putString("char_mastery", obj.toString()).apply()
        return next
    }

    private fun parseMasteryJson(o: JSONObject?): Map<String, CharMastery> {
        if (o == null) return emptyMap()
        val out = LinkedHashMap<String, CharMastery>()
        val keys = o.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = o.optJSONObject(k) ?: continue
            val xp = v.optInt("xp", 0).coerceAtLeast(0)
            val lvl = v.optInt("lvl", (1 + xp / 200).coerceIn(1, 99))
            val matches = v.optInt("matches", 0).coerceAtLeast(0)
            out[k] = CharMastery(xp, lvl, matches)
        }
        return out
    }

    suspend fun syncProfile(context: Context, server: String, name: String, inputMode: String, favChar: String = savedFavChar(context)): RankedProfile = withContext(Dispatchers.IO) {
        val (pid, secret) = identity(context)
        val local = localMastery(context)
        val localObj = JSONObject()
        for ((k, v) in local) localObj.put(k, JSONObject().put("xp", v.xp).put("lvl", v.lvl).put("matches", v.matches))
        val r = postJson(server, "/v1/profile", JSONObject()
            .put("player_id", pid)
            .put("secret", secret)
            .put("name", Online.cleanName(name).ifEmpty { "Ninja" })
            .put("input_mode", inputMode)
            .put("fav_char", favChar)
            .put("char_mastery", localObj))
        val merged = parseMasteryJson(r.optJSONObject("char_mastery"))
        if (merged.isNotEmpty()) {
            val saveObj = JSONObject()
            for ((k, v) in merged) saveObj.put(k, JSONObject().put("xp", v.xp).put("lvl", v.lvl).put("matches", v.matches))
            context.getSharedPreferences("online_ranked", Context.MODE_PRIVATE).edit().putString("char_mastery", saveObj.toString()).apply()
        }
        val activeChar = r.optString("fav_char", favChar)
        val activeM = merged[activeChar]
        RankedProfile(
            playerId = r.optString("player_id", pid),
            name = r.optString("name", name),
            points = r.optInt("points", 1000),
            wins = r.optInt("wins", 0),
            losses = r.optInt("losses", 0),
            streak = r.optInt("streak", 0),
            rank = r.optInt("rank", 1),
            tier = r.optString("tier", "Genin"),
            favChar = activeChar,
            favCharLvl = activeM?.lvl ?: r.optInt("fav_char_lvl", 1),
            favCharXp = activeM?.xp ?: r.optInt("fav_char_xp", 0),
            mastery = merged,
        )
    }

    suspend fun fetchLeaderboard(server: String, mode: String = "all", limit: Int = 100): List<LeaderboardEntry> = withContext(Dispatchers.IO) {
        if (!Online.isAllowedServer(server)) return@withContext emptyList()
        val url = "$server/v1/leaderboard?limit=$limit&mode=${URLEncoder.encode(mode, "UTF-8")}&t=${System.currentTimeMillis()}"
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "GET"
            c.useCaches = false
            c.connectTimeout = 6000
            c.readTimeout = 8000
            c.setRequestProperty("Cache-Control", "no-cache")
            c.setRequestProperty("User-Agent", "HiddenLeaf-Android")
            if (c.responseCode >= 400) return@withContext emptyList()
            val text = c.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONObject(text).optJSONArray("entries") ?: return@withContext emptyList()
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                LeaderboardEntry(
                    rank = o.optInt("rank", i + 1),
                    playerId = o.optString("player_id"),
                    name = o.optString("name"),
                    points = o.optInt("points", 1000),
                    wins = o.optInt("wins", 0),
                    losses = o.optInt("losses", 0),
                    streak = o.optInt("streak", 0),
                    inputMode = o.optString("input_mode", "any"),
                    tier = o.optString("tier", "Genin"),
                    favChar = o.optString("fav_char", "naruto"),
                    favCharLvl = o.optInt("fav_char_lvl", 1),
                )
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            c.disconnect()
        }
    }

    /** Curated public anime/fan-art portrait URLs for Naruto characters (cached locally in <root>/portraits/bank_<id>.png). */
    val CHARACTER_ART_URLS: Map<String, String> = mapOf(
        "naruto" to "https://cdn.myanimelist.net/images/characters/2/284121.jpg",
        "sasuke" to "https://cdn.myanimelist.net/images/characters/9/131317.jpg",
        "itachi" to "https://cdn.myanimelist.net/images/characters/9/284122.jpg",
        "minato" to "https://cdn.myanimelist.net/images/characters/4/49964.jpg",
        "kakashi" to "https://cdn.myanimelist.net/images/characters/7/284129.jpg",
        "gaara" to "https://cdn.myanimelist.net/images/characters/10/293375.jpg",
        "lee" to "https://cdn.myanimelist.net/images/characters/13/433353.jpg",
        "sakura" to "https://cdn.myanimelist.net/images/characters/9/69275.jpg",
        "neji" to "https://cdn.myanimelist.net/images/characters/2/105538.jpg",
        "jiraiya" to "https://cdn.myanimelist.net/images/characters/15/222159.jpg",
        "deidara" to "https://cdn.myanimelist.net/images/characters/9/47166.jpg",
        "pain" to "https://cdn.myanimelist.net/images/characters/12/421593.jpg",
        "obito" to "https://cdn.myanimelist.net/images/characters/15/284128.jpg",
        "orochimaru" to "https://cdn.myanimelist.net/images/characters/11/253973.jpg",
        "tsunade" to "https://cdn.myanimelist.net/images/characters/7/284130.jpg",
        "shikamaru" to "https://cdn.myanimelist.net/images/characters/3/131315.jpg",
        "hinata" to "https://cdn.myanimelist.net/images/characters/6/278736.jpg",
        "kisame" to "https://cdn.myanimelist.net/images/characters/14/49962.jpg",
        "sasori" to "https://cdn.myanimelist.net/images/characters/14/57544.jpg",
        "guy" to "https://cdn.myanimelist.net/images/characters/11/433351.jpg",
    )

    fun customAvatarFile(context: Context): java.io.File =
        java.io.File(java.io.File(GamePaths(context).root, "portraits"), "custom_avatar.png")

    fun hasCustomAvatar(context: Context): Boolean =
        customAvatarFile(context).let { it.exists() && it.length() > 512L }

    /** Imports any user-chosen gallery image, center-crops to square, scales to 192x192 PNG, and saves as custom_avatar.png. */
    fun saveCustomAvatarFromUri(context: Context, uri: android.net.Uri): Boolean = runCatching {
        val raw = context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) } ?: return false
        val side = minOf(raw.width, raw.height)
        val x = (raw.width - side) / 2
        val y = (raw.height - side) / 2
        val square = android.graphics.Bitmap.createBitmap(raw, x, y, side, side)
        val scaled = android.graphics.Bitmap.createScaledBitmap(square, 192, 192, true)
        val out = customAvatarFile(context)
        out.parentFile?.mkdirs()
        out.outputStream().use { scaled.compress(android.graphics.Bitmap.CompressFormat.PNG, 92, it) }
        if (scaled !== square) scaled.recycle()
        if (square !== raw) square.recycle()
        raw.recycle()
        true
    }.getOrDefault(false)

    fun clearCustomAvatar(context: Context) {
        runCatching { customAvatarFile(context).delete() }
    }

    /** Downloads and caches a character's portrait from the art bank into <root>/portraits/bank_<id>.png. */
    suspend fun fetchCharacterArt(context: Context, charId: String): java.io.File? = withContext(Dispatchers.IO) {
        val key = charId.lowercase()
        val url = CHARACTER_ART_URLS[key] ?: return@withContext null
        val out = java.io.File(java.io.File(GamePaths(context).root, "portraits"), "bank_$key.png")
        if (out.exists() && out.length() > 1024L) return@withContext out
        runCatching {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 5000
            c.readTimeout = 7000
            c.setRequestProperty("User-Agent", "HiddenLeaf-Android")
            try {
                if (c.responseCode in 200..299) {
                    val bmp = c.inputStream.use { android.graphics.BitmapFactory.decodeStream(it) } ?: return@runCatching null
                    val side = minOf(bmp.width, bmp.height)
                    val x = (bmp.width - side) / 2
                    val y = ((bmp.height - side) / 4).coerceAtLeast(0)
                    val sq = android.graphics.Bitmap.createBitmap(bmp, x, y, side, side)
                    val sc = android.graphics.Bitmap.createScaledBitmap(sq, 160, 160, true)
                    out.parentFile?.mkdirs()
                    out.outputStream().use { sc.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, it) }
                    if (sc !== sq) sc.recycle()
                    if (sq !== bmp) sq.recycle()
                    bmp.recycle()
                    out
                } else null
            } finally {
                c.disconnect()
            }
        }.getOrNull()
    }

    suspend fun reportRanked(
        context: Context, server: String, matchId: String, opponentId: String, role: String, winner: String, charId: String? = null,
    ): JSONObject? = withContext(Dispatchers.IO) {
        reportRankedBlocking(context, server, matchId, opponentId, role, winner, charId)
    }

    fun reportRankedBlocking(
        context: Context, server: String, matchId: String, opponentId: String, role: String, winner: String, charId: String? = null,
    ): JSONObject? {
        val (pid, secret) = identity(context)
        return runCatching {
            val body = JSONObject()
                .put("match_id", matchId)
                .put("player_id", pid)
                .put("opponent_id", opponentId)
                .put("secret", secret)
                .put("role", role)
                .put("winner", winner)
            if (!charId.isNullOrBlank()) body.put("char_id", charId)
            postJson(server, "/v1/ranked/report", body)
        }.getOrNull()
    }

    suspend fun createLobby(
        context: Context, server: String, name: String, fp: String, maxPlayers: Int, allowSpectators: Boolean, inputMode: String,
    ): CustomLobby = withContext(Dispatchers.IO) {
        val (pid, _) = identity(context)
        val r = postJson(server, "/v1/lobby/create", JSONObject()
            .put("v", Online.NET_VERSION)
            .put("game", Online.GAME)
            .put("fp", fp)
            .put("player_id", pid)
            .put("name", Online.cleanName(name).ifEmpty { "Host" })
            .put("max_players", maxPlayers.coerceIn(2, 8))
            .put("allow_spectators", allowSpectators)
            .put("input_mode", inputMode))
        parseLobby(r, pid)
    }

    suspend fun joinLobby(
        context: Context, server: String, code: String, name: String, fp: String, inputMode: String, spectator: Boolean = false,
    ): CustomLobby = withContext(Dispatchers.IO) {
        val (pid, _) = identity(context)
        val r = postJson(server, "/v1/lobby/join", JSONObject()
            .put("v", Online.NET_VERSION)
            .put("game", Online.GAME)
            .put("fp", fp)
            .put("code", code.filter { it.isDigit() })
            .put("player_id", pid)
            .put("name", Online.cleanName(name).ifEmpty { "Ninja" })
            .put("input_mode", inputMode)
            .put("spectator", spectator))
        parseLobby(r, pid)
    }

    suspend fun pollLobby(context: Context, server: String, code: String, activeRoom: String? = null): CustomLobby = withContext(Dispatchers.IO) {
        val (pid, _) = identity(context)
        val body = JSONObject().put("code", code).put("player_id", pid)
        if (!activeRoom.isNullOrEmpty()) body.put("active_room", activeRoom)
        val r = postJson(server, "/v1/lobby/state", body)
        parseLobby(r, pid)
    }

    suspend fun setLobbyRole(context: Context, server: String, code: String, targetId: String, role: String): CustomLobby = withContext(Dispatchers.IO) {
        val (pid, _) = identity(context)
        val r = postJson(server, "/v1/lobby/role", JSONObject()
            .put("code", code)
            .put("player_id", pid)
            .put("target_id", targetId)
            .put("role", role))
        parseLobby(r, pid)
    }

    suspend fun leaveLobby(context: Context, server: String, code: String) = withContext(Dispatchers.IO) {
        val (pid, _) = identity(context)
        runCatching { postJson(server, "/v1/lobby/leave", JSONObject().put("code", code).put("player_id", pid)) }
    }

    fun spectatorRelayUrl(server: String, code: String): String =
        "$server/v1/ws?code=${URLEncoder.encode(code.filter { it.isDigit() }, "UTF-8")}&role=spectator"

    private fun parseLobby(r: JSONObject, myId: String): CustomLobby {
        val arr = r.optJSONArray("members")
        val members = buildList {
            if (arr != null) for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                add(LobbyMember(m.optString("id"), m.optString("name"), m.optString("role", "queue")))
            }
        }
        return CustomLobby(
            code = r.optString("code"),
            playerId = r.optString("player_id", myId),
            hostId = r.optString("host_id", myId),
            maxPlayers = r.optInt("max_players", 4),
            allowSpectators = r.optBoolean("allow_spectators", true),
            inputMode = r.optString("input_mode", "any"),
            activeRoom = r.optString("active_room", "").takeIf { it.isNotEmpty() },
            members = members,
        )
    }

    private fun postJson(server: String, path: String, body: JSONObject): JSONObject {
        if (!Online.isAllowedServer(server)) throw Online.Failure("server")
        val c = URL(server + path).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"
            c.connectTimeout = 6000
            c.readTimeout = 10000
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.setRequestProperty("User-Agent", "HiddenLeaf-Android")
            c.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = c.responseCode
            val text = (if (code < 400) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code >= 400 && OnlineQuota.isQuotaAnswer(code, text)) {
                OnlineQuota.exhausted()
                throw Online.Failure("quota")
            }
            val json = JSONObject(text)
            json.optString("error").takeIf { it.isNotEmpty() }?.let { throw Online.Failure(it) }
            return json
        } catch (e: Online.Failure) {
            throw e
        } catch (_: Exception) {
            throw Online.Failure("network")
        } finally {
            c.disconnect()
        }
    }
}
