package com.island.phone.app.home

const val HOME_COLUMNS = 4
const val HOME_ROWS = 6

enum class HomeWidgetKind { DATE, QUOTE, MUSIC, RECORD, PHOTOS }

sealed interface HomeItem {
    val column: Int
    val row: Int
    val columnSpan: Int
    val rowSpan: Int
    val label: String
}

class HomeAppItem(
    override val column: Int,
    override val row: Int,
    override val label: String,
    val iconName: String,
) : HomeItem {
    override val columnSpan: Int get() = 1
    override val rowSpan: Int get() = 1
}

class HomeWidgetItem(
    override val column: Int,
    override val row: Int,
    override val columnSpan: Int,
    override val rowSpan: Int,
    override val label: String,
    val kind: HomeWidgetKind,
    val square: Boolean = false,
) : HomeItem

object HomeFirstPageLayout {
    val items: List<HomeItem> = listOf(
        HomeWidgetItem(column = 0, row = 0, columnSpan = 4, rowSpan = 2, label = "日期", kind = HomeWidgetKind.DATE),
        HomeWidgetItem(column = 0, row = 2, columnSpan = 2, rowSpan = 2, label = "语录", kind = HomeWidgetKind.QUOTE, square = true),
        HomeAppItem(column = 2, row = 2, label = "外观", iconName = "Style"),
        HomeAppItem(column = 3, row = 2, label = "屿年", iconName = "Year"),
        HomeAppItem(column = 2, row = 3, label = "日历", iconName = "Cal"),
        HomeAppItem(column = 3, row = 3, label = "预设", iconName = "Preset"),
        HomeWidgetItem(column = 0, row = 4, columnSpan = 4, rowSpan = 2, label = "音乐", kind = HomeWidgetKind.MUSIC),
    )

    val dock: List<HomeAppItem> = listOf(
        HomeAppItem(column = 0, row = 0, label = "聊天", iconName = "Chat"),
        HomeAppItem(column = 1, row = 0, label = "相遇", iconName = "Meet"),
        HomeAppItem(column = 2, row = 0, label = "世界书", iconName = "Lore"),
        HomeAppItem(column = 3, row = 0, label = "设置", iconName = "Setup"),
    )
}

object HomeSecondPageLayout {
    val items: List<HomeItem> = listOf(
        HomeWidgetItem(column = 0, row = 0, columnSpan = 4, rowSpan = 2, label = "唱片", kind = HomeWidgetKind.RECORD),
        HomeWidgetItem(column = 0, row = 2, columnSpan = 4, rowSpan = 2, label = "相片", kind = HomeWidgetKind.PHOTOS),
        HomeAppItem(column = 0, row = 4, label = "相册", iconName = "Album"),
        HomeAppItem(column = 1, row = 4, label = "音乐", iconName = "Music"),
        HomeAppItem(column = 2, row = 4, label = "论坛", iconName = "Forum"),
        HomeAppItem(column = 3, row = 4, label = "查岗", iconName = "Watch"),
        HomeAppItem(column = 0, row = 5, label = "文学城", iconName = "Novel"),
        HomeAppItem(column = 1, row = 5, label = "情侣空间", iconName = "Love"),
        HomeAppItem(column = 2, row = 5, label = "美团", iconName = "Food"),
        HomeAppItem(column = 3, row = 5, label = "栖迟", iconName = "Dream"),
    )
}
