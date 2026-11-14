package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Flare
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.UniverseVisualPalette
import kotlin.math.abs

enum class ItemVisualKind(
    val categoryTitle: String,
    val defaultHint: String
) {
    COFFEE_DRINK(
        categoryTitle = "Напиток / уют",
        defaultHint = "Тёплый напиток или термос — отличный повод сбавить напряжение, согреться или угостить собеседника."
    ),
    FOOD_PASTRY(
        categoryTitle = "Еда / угощение",
        defaultHint = "Подкрепит силы в дороге или станет душевным жестом внимания для нужного человека."
    ),
    BLADE_WEAPON(
        categoryTitle = "Клинок / оружие",
        defaultHint = "Поможет отбиться в опасной ситуации, перерезать преграду или показать серьёзность намерений."
    ),
    ENERGY_TECH_WEAPON(
        categoryTitle = "Стрелковое / импульс",
        defaultHint = "Серьёзный аргумент в перестрелке или напряжённом противостоянии."
    ),
    SHIELD_ARMOR(
        categoryTitle = "Защита / экипировка",
        defaultHint = "Снижает риск ранений и помогает выдержать удар в опасной сцене."
    ),
    POTION_ELIXIR(
        categoryTitle = "Зелье / эликсир",
        defaultHint = "Алхимический или особый состав: восстановит силы, обострит чувства или подействует на цель."
    ),
    MEDICINE_HERB(
        categoryTitle = "Медицина / травы",
        defaultHint = "Перевязка, лекарство или целебные травы — спасут от травмы и помогут раненому союзнику."
    ),
    KEY_ACCESS(
        categoryTitle = "Ключ / отмычка",
        defaultHint = "Открывает запертые двери, шкатулки, архивы или потайные ходы."
    ),
    PASS_CHIP(
        categoryTitle = "Пропуск / чип",
        defaultHint = "Даёт доступ в закрытые зоны, подтверждает статус или считывается терминалами."
    ),
    BOOK_SCROLL(
        categoryTitle = "Книга / свиток",
        defaultHint = "Хранит знания, схемы или записи — можно изучить, процитировать или обменять."
    ),
    LETTER_EVIDENCE(
        categoryTitle = "Письмо / улика",
        defaultHint = "Ценный документ, записка или доказательство — покажите его в подходящий момент."
    ),
    COIN_TREASURE(
        categoryTitle = "Ценность / средства",
        defaultHint = "Пригодится для оплаты, подкупа, торга или в качестве ценного залога."
    ),
    FLOWER_GIFT(
        categoryTitle = "Цветы / подарок",
        defaultHint = "Искренний знак внимания, который меняет тон диалога и запоминается собеседнику."
    ),
    UMBRELLA_CLOTHING(
        categoryTitle = "Одежда / укрытие",
        defaultHint = "Защитит от непогоды, поможет согреться или прикрыть спутника от дождя."
    ),
    MAGIC_ARTIFACT(
        categoryTitle = "Артефакт / кристалл",
        defaultHint = "Редкая вещь с особым откликом — пригодится в ритуале, расследовании или критический миг."
    ),
    GADGET_LIGHT(
        categoryTitle = "Связь / свет / прибор",
        defaultHint = "Осветит тёмный путь, поможет связаться с кем-то или просканировать обстановку."
    ),
    TOOL_GEAR(
        categoryTitle = "Инструмент / снаряжение",
        defaultHint = "Надёжный рабочий инструмент для починки, взлома, навигации или полевых задач."
    ),
    GENERIC_RELIC(
        categoryTitle = "Предмет инвентаря",
        defaultHint = "Личная вещь при вас: примените её в текущей сцене, передайте собеседнику или впишите в ход."
    )
}

@Immutable
data class ItemVisualSpec(
    val kind: ItemVisualKind,
    val icon: ImageVector,
    val bgStart: Color,
    val bgEnd: Color,
    val accent: Color
)

fun resolveItemVisualSpec(itemName: String): ItemVisualSpec {
    val lower = itemName.trim().lowercase()
    val hash = abs(lower.hashCode())

    val (kind, icon, baseStart, baseEnd, baseAccent) = when {
        containsAny(lower, "кофе", "чай", "эспрессо", "латте", "капуч", "круж", "термос", "вино", "эль", "фляг", "напит") ->
            Quint(
                ItemVisualKind.COFFEE_DRINK,
                Icons.Outlined.LocalCafe,
                Color(0xFF4A2C22),
                Color(0xFF8C5336),
                Color(0xFFF6C68E)
            )
        containsAny(lower, "пирог", "булоч", "круассан", "хлеб", "торт", "печен", "шоколад", "еда", "яблок", "завтрак", "рацион") ->
            Quint(
                ItemVisualKind.FOOD_PASTRY,
                Icons.Outlined.Restaurant,
                Color(0xFF4F341D),
                Color(0xFF9C6A2E),
                Color(0xFFFFD88A)
            )
        containsAny(lower, "пистол", "револьвер", "бластер", "винтов", "патрон", "шокер", "импульс") ->
            Quint(
                ItemVisualKind.ENERGY_TECH_WEAPON,
                Icons.Outlined.Bolt,
                Color(0xFF1A2838),
                Color(0xFF2D4C6E),
                Color(0xFF7AD6FF)
            )
        containsAny(lower, "клин", "стилет", "меч", "нож", "кинжал", "катана", "куна", "топор", "копь") ->
            Quint(
                ItemVisualKind.BLADE_WEAPON,
                Icons.Outlined.Security,
                Color(0xFF331B22),
                Color(0xFF692B36),
                Color(0xFFF29D96)
            )
        containsAny(lower, "брон", "щит", "жилет", "шлем", "наруч", "доспех") ->
            Quint(
                ItemVisualKind.SHIELD_ARMOR,
                Icons.Outlined.Shield,
                Color(0xFF212B38),
                Color(0xFF435970),
                Color(0xFFA7CCF0)
            )
        containsAny(lower, "зель", "эликсир", "мутаген", "алхим", "склянк", "флакон", "настой", "яд") ->
            Quint(
                ItemVisualKind.POTION_ELIXIR,
                Icons.Outlined.Science,
                Color(0xFF1E362E),
                Color(0xFF316E5A),
                Color(0xFF8EF0C8)
            )
        containsAny(lower, "аптеч", "лекар", "бинт", "мазь", "трав", "антидот", "пластыр", "ромшк") ->
            Quint(
                ItemVisualKind.MEDICINE_HERB,
                Icons.Outlined.LocalHospital,
                Color(0xFF2A3B28),
                Color(0xFF4F7848),
                Color(0xFFBDF2A2)
            )
        containsAny(lower, "ключ", "отмыч", "брелок") ->
            Quint(
                ItemVisualKind.KEY_ACCESS,
                Icons.Outlined.Key,
                Color(0xFF3A2D18),
                Color(0xFF7D5F2B),
                Color(0xFFFFDC88)
            )
        containsAny(lower, "пропуск", "чип", "карт", "жетон", "бейдж", "удостовер", "билет") ->
            Quint(
                ItemVisualKind.PASS_CHIP,
                Icons.Outlined.CreditCard,
                Color(0xFF142A3B),
                Color(0xFF1F5A78),
                Color(0xFF6FE7F5)
            )
        containsAny(lower, "книг", "дневник", "свиток", "конспект", "гримуар", "тетрад", "том", "альбом", "блокнот") ->
            Quint(
                ItemVisualKind.BOOK_SCROLL,
                Icons.AutoMirrored.Outlined.MenuBook,
                Color(0xFF382438),
                Color(0xFF664066),
                Color(0xFFE7B8EC)
            )
        containsAny(lower, "письм", "запис", "документ", "улик", "фото", "чек", "расписк", "газет", "досье") ->
            Quint(
                ItemVisualKind.LETTER_EVIDENCE,
                Icons.Outlined.Mail,
                Color(0xFF383024),
                Color(0xFF6D5D43),
                Color(0xFFF3DEC0)
            )
        containsAny(lower, "монет", "крон", "эдди", "золот", "кошел", "деньг", "самоцвет", "кольц", "ожерел", "рубин") ->
            Quint(
                ItemVisualKind.COIN_TREASURE,
                Icons.Outlined.Paid,
                Color(0xFF3E2D12),
                Color(0xFF8B6720),
                Color(0xFFFFDF76)
            )
        containsAny(lower, "цвет", "роз", "букет", "тюльпан", "подар", "лент", "сувенир", "венок") ->
            Quint(
                ItemVisualKind.FLOWER_GIFT,
                Icons.Outlined.LocalFlorist,
                Color(0xFF421E2C),
                Color(0xFF883956),
                Color(0xFFFFA8C8)
            )
        containsAny(lower, "зонт", "плащ", "плед", "шарф", "пальт", "куртк", "перчат", "очк", "маск") ->
            Quint(
                ItemVisualKind.UMBRELLA_CLOTHING,
                Icons.Outlined.BeachAccess,
                Color(0xFF212D45),
                Color(0xFF3B517C),
                Color(0xFFAEC8FF)
            )
        containsAny(lower, "амулет", "медальон", "кристалл", "палоч", "рун", "талисман", "артефакт", "печать") ->
            Quint(
                ItemVisualKind.MAGIC_ARTIFACT,
                Icons.Outlined.Diamond,
                Color(0xFF2B1C45),
                Color(0xFF5A3791),
                Color(0xFFD2A8FF)
            )
        containsAny(lower, "фонар", "факел", "свеч", "телефон", "комм", "рация", "сканер", "смартфон") ->
            Quint(
                ItemVisualKind.GADGET_LIGHT,
                if (lower.contains("телеф") || lower.contains("смарт") || lower.contains("комм")) {
                    Icons.Outlined.Smartphone
                } else if (lower.contains("скан")) {
                    Icons.Outlined.Memory
                } else {
                    Icons.Outlined.Flare
                },
                Color(0xFF1F2B36),
                Color(0xFF38536B),
                Color(0xFFFFD180)
            )
        containsAny(lower, "компас", "верёв", "верев", "крюк", "инструм", "мультитул", "камера", "сумк", "рюкзак") ->
            Quint(
                ItemVisualKind.TOOL_GEAR,
                if (lower.contains("компас")) Icons.Outlined.Explore else Icons.Outlined.Build,
                Color(0xFF2A2922),
                Color(0xFF565341),
                Color(0xFFDCD39E)
            )
        else -> {
            val fallbackAccents = listOf(
                Color(0xFFE0B472),
                Color(0xFF80D0E8),
                Color(0xFFE595A8),
                Color(0xFF9CD49F),
                Color(0xFFC1A3F0)
            )
            val chosen = fallbackAccents[hash % fallbackAccents.size]
            Quint(
                ItemVisualKind.GENERIC_RELIC,
                when (hash % 4) {
                    0 -> Icons.Outlined.AutoAwesome
                    1 -> Icons.Outlined.Description
                    2 -> Icons.Outlined.CardGiftcard
                    else -> Icons.Outlined.Backpack
                },
                lerp(Color(0xFF1E1D24), chosen, 0.14f),
                lerp(Color(0xFF2F2E3B), chosen, 0.28f),
                chosen
            )
        }
    }

    // Subtle per-item hue variation so two items of the same kind still feel distinct
    val tintVariant = when (hash % 3) {
        0 -> baseAccent
        1 -> lerp(baseAccent, Color(0xFFFFF1D0), 0.16f)
        else -> lerp(baseAccent, Color(0xFFA6E3FF), 0.14f)
    }
    val shiftedEnd = when (hash % 3) {
        0 -> baseEnd
        1 -> lerp(baseEnd, Color(0xFF4C334D), 0.18f)
        else -> lerp(baseEnd, Color(0xFF24495C), 0.18f)
    }

    return ItemVisualSpec(
        kind = kind,
        icon = icon,
        bgStart = baseStart,
        bgEnd = shiftedEnd,
        accent = tintVariant
    )
}

@Composable
fun ItemPictureTile(
    itemName: String,
    palette: UniverseVisualPalette? = null,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val spec = remember(itemName) { resolveItemVisualSpec(itemName) }
    val corner = (size * 0.28f).coerceAtLeast(8.dp)
    val borderAccent = palette?.accentPrimary?.let { lerp(spec.accent, it, 0.24f) } ?: spec.accent

    Surface(
        shape = RoundedCornerShape(corner),
        border = BorderStroke(1.dp, borderAccent.copy(alpha = 0.55f)),
        shadowElevation = 2.dp,
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(spec.bgStart, spec.bgEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Custom decorative motif per item category
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Soft inner glow behind the item emblem
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(spec.accent.copy(alpha = 0.34f), Color.Transparent),
                        center = center,
                        radius = this.size.minDimension * 0.48f
                    ),
                    center = center,
                    radius = this.size.minDimension * 0.48f
                )

                when (spec.kind) {
                    ItemVisualKind.COFFEE_DRINK -> {
                        // Rising aromatic steam lines
                        val sx = this.size.width * 0.38f
                        drawLine(
                            color = spec.accent.copy(alpha = 0.45f),
                            start = Offset(sx, this.size.height * 0.28f),
                            end = Offset(sx + 3f, this.size.height * 0.14f),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = spec.accent.copy(alpha = 0.35f),
                            start = Offset(sx + 8f, this.size.height * 0.25f),
                            end = Offset(sx + 10f, this.size.height * 0.12f),
                            strokeWidth = 1.6f,
                            cap = StrokeCap.Round
                        )
                    }
                    ItemVisualKind.POTION_ELIXIR, ItemVisualKind.MAGIC_ARTIFACT -> {
                        // Alchemical / arcane sparkles
                        drawCircle(
                            color = spec.accent.copy(alpha = 0.65f),
                            radius = 2.2f,
                            center = Offset(this.size.width * 0.22f, this.size.height * 0.24f)
                        )
                        drawCircle(
                            color = spec.accent.copy(alpha = 0.55f),
                            radius = 1.7f,
                            center = Offset(this.size.width * 0.78f, this.size.height * 0.22f)
                        )
                    }
                    ItemVisualKind.BLADE_WEAPON, ItemVisualKind.ENERGY_TECH_WEAPON -> {
                        // Diagonal slash / glint line
                        drawLine(
                            color = spec.accent.copy(alpha = 0.28f),
                            start = Offset(this.size.width * 0.14f, this.size.height * 0.84f),
                            end = Offset(this.size.width * 0.86f, this.size.height * 0.16f),
                            strokeWidth = 2.2f,
                            cap = StrokeCap.Round
                        )
                    }
                    ItemVisualKind.PASS_CHIP, ItemVisualKind.GADGET_LIGHT -> {
                        // Subtle cyber corner ticks
                        drawLine(
                            color = spec.accent.copy(alpha = 0.50f),
                            start = Offset(this.size.width * 0.14f, this.size.height * 0.20f),
                            end = Offset(this.size.width * 0.32f, this.size.height * 0.20f),
                            strokeWidth = 1.8f,
                            cap = StrokeCap.Round
                        )
                    }
                    else -> {
                        // Top corner specular gleam
                        drawCircle(
                            color = Color.White.copy(alpha = 0.28f),
                            radius = 1.8f,
                            center = Offset(this.size.width * 0.78f, this.size.height * 0.22f)
                        )
                    }
                }
            }

            Icon(
                imageVector = spec.icon,
                contentDescription = itemName,
                tint = spec.accent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(size * 0.22f)
            )
        }
    }
}

fun itemCategoryIcon(item: String): ImageVector = resolveItemVisualSpec(item).icon

private fun containsAny(text: String, vararg tokens: String): Boolean =
    tokens.any { text.contains(it) }

private data class Quint<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
