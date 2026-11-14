package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.rememberRpgHaptics
import com.example.ui.theme.ChroniclesDynamicBackdrop
import kotlinx.coroutines.launch

private const val TOTAL_SLIDES = 9

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationScreen(
    hapticsEnabled: Boolean,
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { TOTAL_SLIDES })
    val scope = rememberCoroutineScope()
    val haptics = rememberRpgHaptics(hapticsEnabled)

    ChroniclesDynamicBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("presentation_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Презентация приложения",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Слайд ${pagerState.currentPage + 1} из $TOTAL_SLIDES",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(TOTAL_SLIDES) { idx ->
                            val selected = pagerState.currentPage == idx
                            Box(
                                modifier = Modifier
                                    .height(7.dp)
                                    .width(if (selected) 20.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptics.tap()
                                scope.launch {
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("presentation_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Назад")
                        }

                        Button(
                            onClick = {
                                haptics.tap()
                                if (pagerState.currentPage < TOTAL_SLIDES - 1) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                } else {
                                    onBack()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("presentation_next_button")
                        ) {
                            Text(
                                if (pagerState.currentPage < TOTAL_SLIDES - 1) "Далее" else "Готово"
                            )
                            if (pagerState.currentPage < TOTAL_SLIDES - 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .testTag("presentation_pager")
            ) { page ->
                SlideContainer {
                    when (page) {
                        0 -> SlideOverviewAndPrinciples()
                        1 -> SlideShelfAndScenarios()
                        2 -> SlideSetupWizard()
                        3 -> SlideGameplayWireframe()
                        4 -> SlideNpcResistance()
                        5 -> SlideInteractiveTools()
                        6 -> SlideCustomizationAndModels()
                        7 -> SlideArchitecture()
                        else -> SlideRoadmap()
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun SlideContainer(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SlideHeader(badge: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SlideBulletCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SlideOverviewAndPrinciples() {
    SlideHeader(
        badge = "Слайд 1 • Концепция",
        title = "«Хроники Бабочки»",
        subtitle = "Книжная текстовая RPG нового поколения с живым ИИ-мастером, где мир запоминает каждое решение."
    )
    Image(
        painter = painterResource(id = R.drawable.img_chronicle_bookplate),
        contentDescription = "Обложка Хроник Бабочки",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(MaterialTheme.shapes.medium)
    )
    SlideBulletCard(
        icon = Icons.Outlined.AutoStories,
        title = "Anti-Godmodding",
        body = "ИИ никогда не решает и не думает за вас — мастер описывает только мир и реакции NPC."
    )
    SlideBulletCard(
        icon = Icons.Outlined.AutoAwesome,
        title = "Эффект бабочки",
        body = "Поворотные события фиксируются в хронологии, а отклонение от канона измеряется в процентах."
    )
    SlideBulletCard(
        icon = Icons.Outlined.Shield,
        title = "Живые характеры и границы",
        body = "Персонажи помнят обиды, держат дистанцию и не влюбляются со второго сообщения."
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlideShelfAndScenarios() {
    SlideHeader(
        badge = "Слайд 2 • Библиотека миров",
        title = "Полка хроник и 10 сценариев",
        subtitle = "Быстрый старт в любой атмосфере — от дождливой кофейни и общежития до орбиты газового гиганта."
    )
    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Схема полки хроник",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Все", "Романтика", "Рутина и уют", "Фэнтези", "Аниме", "Мистика и Sci-Fi").forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = "• Карточки шаблонов с обложками миров\n• Карточки ваших кампаний: арт локации, счётчик хода, мини-аватар активного NPC, здоровье и % канона",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    SlideBulletCard(
        icon = Icons.Outlined.FavoriteBorder,
        title = "Романтика и повседневность",
        body = "«Кофейня на углу», «Отдел расследований», «Лавка травника», «Общежитие №4» — упор на диалоги, бюджет и химию отношений."
    )
    SlideBulletCard(
        icon = Icons.Outlined.Explore,
        title = "Эпик, мистика и космос",
        body = "Ведьмак, Наруто, Атака титанов, Хогвартс, Киберпанк 2077 и космический челнок «Икар-7»."
    )
}

@Composable
private fun SlideSetupWizard() {
    SlideHeader(
        badge = "Слайд 3 • Создание истории",
        title = "Мастер старта за 4 вопроса",
        subtitle = "Любой шаблон можно редактировать или создать собственный авторский мир с нуля."
    )
    val steps = listOf(
        "1. Вселенная" to "Каноничный фэндом, повседневный город или полностью свой мир.",
        "2. Персонаж" to "Архетип (Каноничный герой / Свой персонаж OC / Попаданец) и слабости.",
        "3. Точка старта" to "Конкретный час, локация и событие, с которого начинается первая глава.",
        "4. Тон и правила" to "6 пресетов: Романтика, Рутина и уют, Мрачный реализм, Сёнэн, Интриги, Детектив."
    )
    steps.forEach { (step, desc) ->
        SlideBulletCard(
            icon = Icons.Outlined.AutoAwesome,
            title = step,
            body = desc
        )
    }
}

@Composable
private fun SlideGameplayWireframe() {
    SlideHeader(
        badge = "Слайд 4 • Главный экран",
        title = "Игровая сессия без «сухих скобок»",
        subtitle = "Вся важная информация оформлена в визуальные блоки с тематической палитрой вселенной."
    )
    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Схема экрана игры (сверху вниз)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            WireframeRow("1. Живой фон сцены", "Полноэкранный арт локации + частицы погоды на фоне")
            WireframeRow("2. Верхняя панель", "Кнопки вызова [Статус], [Факты], [Кодекс], [Настройки]")
            WireframeRow("3. Окно повествования", "Отдельная стеклянная карточка с буквицей и стримингом")
            WireframeRow("4. Окно NPC", "Аватар персонажа прямо внутри окна с репликой и доверием")
            WireframeRow("5. Выборы и предметы", "Карточки подходов + лента предметов со своей картинкой")
        }
    }
}

@Composable
private fun WireframeRow(label: String, detail: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(126.dp)
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SlideNpcResistance() {
    SlideHeader(
        badge = "Слайд 5 • Психология персонажей",
        title = "Характер и границы NPC",
        subtitle = "Трёхуровневая защита от мгновенного угождения и искусственной влюблённости."
    )
    SlideBulletCard(
        icon = Icons.Outlined.Psychology,
        title = "Уровень 1 — Системный запрет на уступчивость",
        body = "NPC учитывают усталость, занятость на смене, гордость и скепсис. При давлении или бестактности они отказывают и дистанцируются."
    )
    SlideBulletCard(
        icon = Icons.Outlined.Security,
        title = "Уровень 2 — Память характера и красных линий",
        body = "Для каждого ключевого собеседника фиксируются «Характер» и «Личные границы», которые сохраняются и видны в карточке и Кодексе."
    )
    SlideBulletCard(
        icon = Icons.Outlined.Shield,
        title = "Уровень 3 — Программный ограничитель слоубёрна",
        body = "Незнакомцы стартуют не выше +25 доверия, а за один ход доверие растёт максимум на +12 — отношения нужно выстраивать поступками."
    )
}

@Composable
private fun SlideInteractiveTools() {
    SlideHeader(
        badge = "Слайд 6 • Интерактив",
        title = "Предметы, Кодекс и Блокнот фактов",
        subtitle = "Всё под рукой в один тап прямо во время сцены."
    )
    SlideBulletCard(
        icon = Icons.Outlined.Backpack,
        title = "Быстрый инвентарь в реальном времени",
        body = "Нажмите на любой предмет над полем ввода, в HUD или в Кодексе: можно сразу применить его в сцене, передать NPC или вставить в черновик."
    )
    SlideBulletCard(
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        title = "Кодекс с 4 вкладками",
        body = "Анкета героя, досье NPC со шкалой доверия, лента Эффекта бабочки и ваши личные заметки."
    )
    SlideBulletCard(
        icon = Icons.Outlined.EditNote,
        title = "Блокнот закреплённых фактов",
        body = "Добавьте важные обещания, пароли или детали внешности — закреплённые заметки передаются мастеру на каждом ходе."
    )
}

@Composable
private fun SlideCustomizationAndModels() {
    SlideHeader(
        badge = "Слайд 7 • Настройки и ИИ",
        title = "Шрифты, модели Gemini и комфорт",
        subtitle = "Гибкая настройка чтения на телефоне и бесплатные быстрые модели без подписок."
    )
    SlideBulletCard(
        icon = Icons.Outlined.FormatSize,
        title = "5 шрифтов с кириллицей + 4 размера",
        body = "Книжный (Literata), Роман (Playfair), Уютный (Nunito), Чистый (Inter) и Терминал (JetBrains Mono) с живым предпросмотром текста."
    )
    SlideBulletCard(
        icon = Icons.Outlined.Speed,
        title = "Бесплатные модели + живой стриминг",
        body = "По умолчанию работает Gemini 3.5 Flash-Lite, в меню можно включить Gemini 3.8 Flash с авто-переходом на Flash-Lite при лимитах."
    )
    SlideBulletCard(
        icon = Icons.Outlined.Settings,
        title = "5 тем оформления и тактильная отдача",
        body = "Динамический градиент по умолчанию (меняется от экрана, мира и эмоций сцены), Светлая/Тёмная бумага, Ночной неон и виброотклик."
    )
}

@Composable
private fun SlideArchitecture() {
    SlideHeader(
        badge = "Слайд 8 • Архитектура",
        title = "Устройство под капотом",
        subtitle = "Чистое разделение облачной синхронизации хроник и быстрых локальных настроек."
    )
    SlideBulletCard(
        icon = Icons.Outlined.Layers,
        title = "Экранный слой (Jetpack Compose Material 3)",
        body = "Адаптивные цветовые схемы вселенных, потоковый декодер JSON-текста на лету и компонентная навигация."
    )
    SlideBulletCard(
        icon = Icons.Outlined.CloudSync,
        title = "Облако (Google Sign-In + Cloud Firestore)",
        body = "Все кампании, ходы, инвентарь, NPC и Эффект бабочки привязаны к аккаунту и защищены строгими правилами доступа."
    )
    SlideBulletCard(
        icon = Icons.Outlined.AutoAwesome,
        title = "ИИ и локальная память (AI Logic + DataStore)",
        body = "Запросы к Gemini защищены Firebase App Check, а шрифты, модель ИИ и Блокнот фактов хранятся локально в DataStore."
    )
}

@Composable
private fun SlideRoadmap() {
    SlideHeader(
        badge = "Слайд 9 • План развития",
        title = "Дорожная карта будущих версий",
        subtitle = "Идеи следующих этапов развития, которые можно подключать по приоритету."
    )
    SlideBulletCard(
        icon = Icons.Outlined.RocketLaunch,
        title = "Этап 1 — Глубина ролевого отыгрыша",
        body = "Ветвление и откат последнего хода для проверки альтернатив, групповые сцены с 2–3 NPC одновременно и редактор своих шаблонов."
    )
    SlideBulletCard(
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        title = "Этап 2 — Атмосфера и экспорт",
        body = "Озвучка глав (TTS), мягкий фоновый амбиент под погоду сцены и экспорт завершённой хроники в файл книги."
    )
    SlideBulletCard(
        icon = Icons.Outlined.AutoAwesome,
        title = "Этап 3 — Умный QoL",
        body = "Автоподсказки для закрепления важных улик в Блокнот фактов и помощь при сюжетном тупике."
    )
}
