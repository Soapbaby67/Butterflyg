package com.example.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.R
import kotlin.math.abs

enum class UniverseArchetype(val displayTag: String) {
    WITCHER_FANTASY("Тёмное фэнтези"),
    CYBERPUNK_SCIFI("Киберпанк"),
    SHINOBI_ANIME("Мир шиноби"),
    TITAN_SURVIVAL("Выживание"),
    ARCANE_MYSTERY("Мистика"),
    GENERIC_CHRONICLE("Хроника")
}

enum class WeatherMood(val russianLabel: String) {
    RAIN("Дождь"),
    STORM("Гроза"),
    FOG("Туман"),
    NIGHT("Ночь"),
    DAWN("Рассвет"),
    DAY("День"),
    SNOW("Снег"),
    INDOOR_CANDLE("Помещение"),
    NEON_NIGHT("Неон")
}

enum class NpcArchetype(val key: String, val roleLabelRu: String) {
    WARRIOR("warrior", "Воин"),
    ROGUE("rogue", "Тень"),
    MAGE("mage", "Мистик"),
    NOBLE("noble", "Знать"),
    CYBER("cyber", "Сетевик"),
    MERCHANT("merchant", "Торговец"),
    COMMONER("commoner", "Горожанин"),
    CREATURE("creature", "Существо");

    companion object {
        fun fromKey(raw: String?): NpcArchetype? {
            val k = raw?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.key == k }
        }
    }
}

sealed interface NpcPortraitSpec {
    data class PhotoPortrait(
        @DrawableRes val resId: Int,
        val tintOverlay: Color,
        val archetype: NpcArchetype
    ) : NpcPortraitSpec

    data class SilhouetteEmblem(
        val archetype: NpcArchetype,
        val backgroundStart: Color,
        val backgroundEnd: Color,
        val accentColor: Color
    ) : NpcPortraitSpec
}

data class UniverseVisualPalette(
    val archetype: UniverseArchetype,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val bannerScrimTop: Color,
    val bannerScrimBottom: Color,
    val atmosphericTint: Color,
    val healthHigh: Color,
    val healthMid: Color,
    val healthLow: Color,
    val trustPositive: Color,
    val trustNegative: Color,
    val choiceForceColor: Color,
    val choiceDiplomacyColor: Color,
    val choiceStealthColor: Color,
    val choiceWildcardColor: Color
)

@Composable
fun rememberUniverseVisualPalette(
    universe: String,
    isDark: Boolean = LocalIsDarkPaper.current
): UniverseVisualPalette {
    return remember(universe, isDark) {
        resolveUniverseVisualPalette(universe, isDark)
    }
}

fun resolveUniverseArchetype(universe: String): UniverseArchetype {
    val lower = universe.lowercase()
    return when {
        lower.contains("cyberpunk") || lower.contains("кибер") ||
            lower.contains("найт-сити") || lower.contains("sci-fi") ||
            lower.contains("космос") || lower.contains("неон") -> UniverseArchetype.CYBERPUNK_SCIFI

        lower.contains("наруто") || lower.contains("шиноби") ||
            lower.contains("конох") || lower.contains("аниме") && lower.contains("лист") -> UniverseArchetype.SHINOBI_ANIME

        lower.contains("титан") || lower.contains("парадиз") ||
            lower.contains("сигансин") || lower.contains("постапок") -> UniverseArchetype.TITAN_SURVIVAL

        lower.contains("поттер") || lower.contains("хогвартс") ||
            lower.contains("слизерин") || lower.contains("маги") && lower.contains("замок") -> UniverseArchetype.ARCANE_MYSTERY

        lower.contains("ведьмак") || lower.contains("новиград") ||
            lower.contains("королевств") || lower.contains("фэнтези") ||
            lower.contains("средневеков") -> UniverseArchetype.WITCHER_FANTASY

        else -> UniverseArchetype.GENERIC_CHRONICLE
    }
}

fun resolveUniverseVisualPalette(universe: String, isDark: Boolean): UniverseVisualPalette {
    val archetype = resolveUniverseArchetype(universe)

    val (primary, secondary, atmTint) = when (archetype) {
        UniverseArchetype.WITCHER_FANTASY -> Triple(
            if (isDark) Color(0xFFD68D4A) else Color(0xFF7D3E1C),
            if (isDark) Color(0xFF8AA8BE) else Color(0xFF3E5A70),
            Color(0xFF1D242B)
        )
        UniverseArchetype.CYBERPUNK_SCIFI -> Triple(
            if (isDark) Color(0xFF3FD2E6) else Color(0xFF0E7484),
            if (isDark) Color(0xFFE8648C) else Color(0xFFA62952),
            Color(0xFF0E1B2B)
        )
        UniverseArchetype.SHINOBI_ANIME -> Triple(
            if (isDark) Color(0xFFE07256) else Color(0xFFA2381E),
            if (isDark) Color(0xFFD6B05B) else Color(0xFF7B5B18),
            Color(0xFF192622)
        )
        UniverseArchetype.TITAN_SURVIVAL -> Triple(
            if (isDark) Color(0xFFC59B5E) else Color(0xFF6B4F24),
            if (isDark) Color(0xFF8CA090) else Color(0xFF425949),
            Color(0xFF232624)
        )
        UniverseArchetype.ARCANE_MYSTERY -> Triple(
            if (isDark) Color(0xFFD5A95E) else Color(0xFF6E4E1B),
            if (isDark) Color(0xFF65B89A) else Color(0xFF246951),
            Color(0xFF1F1929)
        )
        UniverseArchetype.GENERIC_CHRONICLE -> Triple(
            if (isDark) TerracottaRuby else LiteraryBurgundy,
            if (isDark) MutedBrass else AgedBrass,
            Color(0xFF211C18)
        )
    }

    return UniverseVisualPalette(
        archetype = archetype,
        accentPrimary = primary,
        accentSecondary = secondary,
        bannerScrimTop = atmTint.copy(alpha = 0.28f),
        bannerScrimBottom = atmTint.copy(alpha = 0.88f),
        atmosphericTint = atmTint,
        healthHigh = if (isDark) Color(0xFF66BB7A) else Color(0xFF2E6F3E),
        healthMid = if (isDark) Color(0xFFE2AD4E) else Color(0xFF9C660C),
        healthLow = if (isDark) Color(0xFFE56A6A) else Color(0xFF9E2A2B),
        trustPositive = if (isDark) Color(0xFF6DC08B) else Color(0xFF2D6A43),
        trustNegative = if (isDark) Color(0xFFE57373) else Color(0xFF9E2A2B),
        choiceForceColor = if (isDark) Color(0xFFE06D65) else Color(0xFF962D27),
        choiceDiplomacyColor = if (isDark) Color(0xFFD8AE5A) else Color(0xFF8A6016),
        choiceStealthColor = if (isDark) Color(0xFF69AEE0) else Color(0xFF2B5F88),
        choiceWildcardColor = if (isDark) Color(0xFFB388E6) else Color(0xFF623B94)
    )
}

@DrawableRes
fun resolveSceneBackdropRes(
    universe: String,
    location: String,
    weatherMood: WeatherMood
): Int {
    val arch = resolveUniverseArchetype(universe)
    val locLower = location.lowercase()

    if (arch == UniverseArchetype.CYBERPUNK_SCIFI ||
        weatherMood == WeatherMood.NEON_NIGHT ||
        locLower.contains("бар") && arch == UniverseArchetype.CYBERPUNK_SCIFI ||
        locLower.contains("неон") || locLower.contains("кибер") || locLower.contains("сектор")
    ) {
        return R.drawable.img_scene_neon_district
    }

    if (arch == UniverseArchetype.SHINOBI_ANIME ||
        arch == UniverseArchetype.ARCANE_MYSTERY ||
        locLower.contains("библиотек") || locLower.contains("храм") ||
        locLower.contains("лес") || locLower.contains("академ") ||
        locLower.contains("замок") || locLower.contains("башн")
    ) {
        return R.drawable.img_scene_ancient_sanctum
    }

    if (arch == UniverseArchetype.WITCHER_FANTASY ||
        arch == UniverseArchetype.TITAN_SURVIVAL ||
        locLower.contains("док") || locLower.contains("порт") ||
        locLower.contains("стен") || locLower.contains("город") ||
        locLower.contains("таверн") || locLower.contains("площад")
    ) {
        return R.drawable.img_scene_fantasy_harbor
    }

    return when (weatherMood) {
        WeatherMood.NEON_NIGHT -> R.drawable.img_scene_neon_district
        WeatherMood.INDOOR_CANDLE, WeatherMood.FOG, WeatherMood.DAWN -> R.drawable.img_scene_ancient_sanctum
        else -> R.drawable.img_scene_fantasy_harbor
    }
}

fun resolveNpcPortraitSpec(
    name: String,
    role: String,
    universe: String,
    explicitArchetype: String,
    palette: UniverseVisualPalette
): NpcPortraitSpec {
    val combined = "${name.lowercase()} ${role.lowercase()}"
    val universeArch = palette.archetype

    val inferred = NpcArchetype.fromKey(explicitArchetype) ?: when {
        combined.contains("фиксер") || combined.contains("нетраннер") ||
            combined.contains("хакер") || combined.contains("киборг") ||
            combined.contains("корпорат") -> NpcArchetype.CYBER

        combined.contains("титан") || combined.contains("чудовищ") ||
            combined.contains("монстр") || combined.contains("звер") ||
            combined.contains("призрак") || combined.contains("демон") -> NpcArchetype.CREATURE

        combined.contains("контрабанд") || combined.contains("вор") ||
            combined.contains("шпион") || combined.contains("убийц") ||
            combined.contains("развед") || combined.contains("агент") ||
            combined.contains("тень") || combined.contains("плут") -> NpcArchetype.ROGUE

        combined.contains("торгов") || combined.contains("купец") ||
            combined.contains("трактир") || combined.contains("корчмар") ||
            combined.contains("ростов") || combined.contains("лавк") -> NpcArchetype.MERCHANT

        combined.contains("леди") || combined.contains("графин") ||
            combined.contains("княж") || combined.contains("королев") ||
            combined.contains("чародейк") || combined.contains("ведьм") ||
            combined.contains("госпож") || combined.contains("дипломат") ||
            isLikelyFemaleRussianName(name) -> NpcArchetype.NOBLE

        combined.contains("страж") || combined.contains("рыцар") ||
            combined.contains("ведьмак") || combined.contains("солдат") ||
            combined.contains("наёмн") || combined.contains("капитан") ||
            combined.contains("охран") || combined.contains("воин") ||
            combined.contains("шиноби") || combined.contains("гарнизон") -> NpcArchetype.WARRIOR

        combined.contains("маг") || combined.contains("наставник") ||
            combined.contains("учён") || combined.contains("жрец") ||
            combined.contains("профессор") || combined.contains("инквизитор") ||
            combined.contains("алхимик") || combined.contains("лекар") -> NpcArchetype.MAGE

        universeArch == UniverseArchetype.CYBERPUNK_SCIFI -> NpcArchetype.CYBER

        else -> {
            val hash = abs(name.trim().lowercase().hashCode())
            when (hash % 4) {
                0 -> NpcArchetype.ROGUE
                1 -> NpcArchetype.WARRIOR
                2 -> NpcArchetype.MAGE
                else -> NpcArchetype.COMMONER
            }
        }
    }

    return when (inferred) {
        NpcArchetype.WARRIOR -> NpcPortraitSpec.PhotoPortrait(
            resId = R.drawable.img_npc_warrior,
            tintOverlay = palette.atmosphericTint.copy(alpha = 0.14f),
            archetype = inferred
        )
        NpcArchetype.ROGUE -> NpcPortraitSpec.PhotoPortrait(
            resId = R.drawable.img_npc_rogue,
            tintOverlay = palette.atmosphericTint.copy(alpha = 0.16f),
            archetype = inferred
        )
        NpcArchetype.MAGE -> NpcPortraitSpec.PhotoPortrait(
            resId = R.drawable.img_npc_mystic,
            tintOverlay = palette.atmosphericTint.copy(alpha = 0.14f),
            archetype = inferred
        )
        NpcArchetype.NOBLE -> NpcPortraitSpec.PhotoPortrait(
            resId = R.drawable.img_npc_lady,
            tintOverlay = palette.atmosphericTint.copy(alpha = 0.12f),
            archetype = inferred
        )
        NpcArchetype.CYBER -> NpcPortraitSpec.PhotoPortrait(
            resId = R.drawable.img_npc_cyber,
            tintOverlay = palette.accentPrimary.copy(alpha = 0.12f),
            archetype = inferred
        )
        NpcArchetype.MERCHANT, NpcArchetype.COMMONER, NpcArchetype.CREATURE -> {
            NpcPortraitSpec.SilhouetteEmblem(
                archetype = inferred,
                backgroundStart = palette.atmosphericTint,
                backgroundEnd = palette.accentSecondary.copy(alpha = 0.55f),
                accentColor = palette.accentPrimary
            )
        }
    }
}

private fun isLikelyFemaleRussianName(name: String): Boolean {
    val firstWord = name.trim().substringBefore(' ').lowercase()
    if (firstWord.length < 3) return false
    val femaleKnown = setOf(
        "трисс", "йеннифэр", "цири", "микаса", "энни", "хистория", "сакура",
        "цунаде", "хината", "гермиона", "джинни", "полумна", "беллатриса",
        "джуди", "панам", "роуг", "кейра", "шани", "эвелин"
    )
    if (firstWord in femaleKnown) return true
    val maleExceptions = setOf("никита", "илья", "лука", "фома", "кузьма", "савва", "данила")
    if (firstWord in maleExceptions) return false
    return firstWord.endsWith("а") || firstWord.endsWith("я")
}
