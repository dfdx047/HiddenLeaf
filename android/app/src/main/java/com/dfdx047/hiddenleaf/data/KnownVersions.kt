package com.dfdx047.hiddenleaf.data

/**
 * The releases and mods the launcher knows by the SHA-256 of their files (docs/analise-elf.md). Only hashes: nothing
 * of the game is in the app.
 *
 * The base is the European release (SLES-55605, Naruto Shippuden: Ultimate Ninja 5), the one the engine is
 * recompiled from. A mod is imported over it: its files that differ from the base are kept apart, and a mod that
 * changes the game's code needs its code pack (the changed and new functions, recompiled) to run.
 */
object KnownVersions {
    const val BASE_SERIAL = "SLES-55605"
    const val BASE_ELF = "SLES_556.05"
    const val BASE_ELF_SHA256 = "20a43677397731a2a20899336d1165ace5b436906b9b89be90fb10f4558dd19d"

    /** SHA-256 of the whole European ISO, Redump's dump (1926234112 bytes): a base that does not match is not imported. */
    const val BASE_ISO_SHA256 = "2e1b9a885f4e94e6b8c4204f139c53abd568fe49d6521d4d8921fe9460c07bff"

    /** The European overlays (PRG/, CodeWarrior MWo3 format): their code is in the engine too. */
    val BASE_OVERLAYS = mapOf(
        "PRG/BTL.BIN" to "7e8518da7bd4957af18cb0ababe67f0e9b37c42c6551375201b15997f0a3dfe3",
        "PRG/ADV.BIN" to "7e2af55362141bb1b055247cd7ef7edae290f3c0095701bc51467f096a2d00b8",
        "PRG/ETC.BIN" to "bdb6bda1f9d335047586a263e478486c8e7924b91fa972b6f3e58caec5ea0778",
        "PRG/TEXTENG.BIN" to "3e42d2ddffe770b05dd41e2c5937380133e255c9ce32ca2f037e34c65a8e571e",
        "PRG/TEXTFRN.BIN" to "485d44d2866e9e985d2d84b4796bd8daa573567d44b8913b9fbccffaafc836b0",
        "PRG/TEXTGER.BIN" to "84de40f3def485b80863370723e462a7b23ebb6a4b5e6a088b4897f78005222d",
        "PRG/TEXTITA.BIN" to "110a915c3043214a6c2a9bd2ca3cb33284629126f91b76db6e525a9fd7b20b03",
        "PRG/TEXTSPA.BIN" to "72ec1bb3aead2764bdc096a1a757c6969d1573c99954df6711e0a0495418e927",
    )

    /**
     * A mod the launcher recognises by its boot program. [isoSha256]: the SHA-256 of the whole images of it the launcher
     * accepts (a known mod from an image that matches none is not imported: damaged, or another build). [pack]: the engine has its code pack. [cardFolder]: the memory
     * card folder the mod loads and never makes itself (the A35: "no data of Ultimate Ninja 5 in slot 1", for New game too);
     * the launcher makes it from the base game's save ([ModSaves]).
     */
    data class KnownMod(val id: String, val name: String, val elf: String, val elfSha256: String, val changesCode: Boolean, val pack: Boolean,
                        val cardFolder: String = "", val isoSha256: List<String> = emptyList())

    val MODS = listOf(
        KnownMod("un6-a35", "Ultimate Ninja 6 (Alpha 3.5)", "SLUS_556.06",
            "47c40141a3e1aeb0c96bc28e8dc311938b284d54fd21f4d8ba953c2e16234809", changesCode = true, pack = false,
            cardFolder = "BASLUS-55606NARUTO6",
            isoSha256 = listOf("37e0117331f8a557349be5445d7871352f6d6ce631e8fb9dc9c3ebfd4025007d")),
        KnownMod("un6-a11x", "Ultimate Ninja 6 (Alpha 1.1X)", "SLES_556.05",
            "20872b06f4ef8b5ff49291b297a97941bc9131525b79e1cab2b1717156ca0056", changesCode = true, pack = false,
            isoSha256 = listOf("bd95521e21f01c6fd88bfc1fb4a77ede0042e916d9e4e9459c79ef9ab5fdd6d9")),
        // the Brazilian version: its ELF is packed (SjCRUNCH) but unpacked it is the European code; a data mod
        KnownMod("un5-br", "Versão Brasileira", "SLES_556.05",
            "7c3517aa09c0c3e34742b89ee19a2de5f71753380ee198e8861a8030cfcf8e51", changesCode = false, pack = true,
            isoSha256 = listOf("2c60b59142b74e5a2da5a0b93a721fcf1090f51c2dfb596b902279cfd842ed5e")),
    )

    fun modByElf(sha256: String): KnownMod? = MODS.firstOrNull { it.elfSha256 == sha256 }
}
