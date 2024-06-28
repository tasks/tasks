@file:Suppress("ClassName")

package com.todoroo.astrid.service

import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksIcons.ALL_INBOX
import org.tasks.themes.TasksIcons.CLEAR
import org.tasks.themes.TasksIcons.CLOUD
import org.tasks.themes.TasksIcons.DELETE
import org.tasks.themes.TasksIcons.EDIT
import org.tasks.themes.TasksIcons.FILTER_LIST
import org.tasks.themes.TasksIcons.HISTORY
import org.tasks.themes.TasksIcons.LABEL
import org.tasks.themes.TasksIcons.LABEL_OFF
import org.tasks.themes.TasksIcons.NOTIFICATIONS
import org.tasks.themes.TasksIcons.PENDING_ACTIONS
import org.tasks.themes.TasksIcons.PLACE
import org.tasks.themes.TasksIcons.TIMER
import org.tasks.themes.TasksIcons.TODAY
import javax.inject.Inject

class Upgrade_13_11 @Inject constructor(
    private val locationDao: LocationDao,
    private val filterDao: FilterDao,
    private val tagDataDao: TagDataDao,
    private val caldavDao: CaldavDao,
) {
    internal suspend fun migrateIcons() {
        locationDao.getPlaces().forEach {
            locationDao.update(it.copy(icon = it.icon.migrateLegacyIcon()))
        }
        filterDao.getFilters().forEach {
            filterDao.update(it.copy(icon = it.icon.migrateLegacyIcon()))
        }
        tagDataDao.getAll().forEach {
            tagDataDao.update(it.copy(icon = it.icon.migrateLegacyIcon()))
        }
        caldavDao.getCalendars().forEach {
            caldavDao.update(it.copy(icon = it.icon.migrateLegacyIcon()))
        }
    }

    companion object {
        const val VERSION = 131100

        fun String?.migrateLegacyIcon(): String? {
            val index = this?.toIntOrNull()
            return if (index == null) this else LEGACY_ICONS[index]
        }

        private val LEGACY_ICONS by lazy {
            mapOf(
                -1 to null,
                0 to null,
                1 to LABEL,
                2 to FILTER_LIST,
                3 to CLOUD,
                4 to ALL_INBOX,
                5 to LABEL_OFF,
                6 to HISTORY,
                7 to TODAY,
                8 to TasksIcons.LIST,
                1000 to "flag",
                1062 to "home",
                1041 to "work_outline",
                1001 to "pets",
                1002 to "payment",
                1003 to "attach_money",
                1059 to "euro_symbol",
                1042 to "store",
                1043 to "shopping_cart",
                1004 to "hourglass_empty",
                1005 to "favorite_border",
                1006 to "school",
                1007 to "drive_eta",
                1008 to "whatshot",
                1009 to "star_border",
                1010 to "account_balance",
                1011 to "location_city",
                1012 to "cake",
                1013 to "kitchen",
                1014 to "fitness_center",
                1015 to "child_friendly",
                1016 to "free_breakfast",
                1017 to "golf_course",
                1018 to "beach_access",
                1019 to "restaurant_menu",
                1020 to "local_pharmacy",
                1021 to "fastfood",
                1022 to "hotel",
                1023 to "flight",
                1057 to "flight_takeoff",
                1058 to "flight_land",
                1024 to "directions_run",
                1025 to "wb_sunny",
                1026 to "desktop_mac",
                1027 to "computer",
                1028 to "format_paint",
                1029 to "storage",
                1030 to "send",
                1031 to "weekend",
                1032 to "link",
                1033 to "business",
                1034 to "chat_bubble_outline",
                1035 to "voicemail",
                1036 to "email",
                1037 to "call",
                1039 to "movie",
                1040 to "equalizer",
                1071 to "pie_chart",
                1072 to "show_chart",
                1044 to "schedule",
                1045 to "photo_camera",
                1046 to DELETE,
                1047 to "attachment",
                1048 to "vpn_key",
                1049 to "event",
                1050 to PLACE,
                1051 to "markunread_mailbox",
                1052 to "label_important",
                1053 to "android",
                1054 to "build",
                1055 to "bug_report",
                1056 to "book",
                1060 to "explore",
                1061 to "gavel",
                1063 to "print",
                1064 to "receipt",
                1038 to "new_releases",
                1065 to "report_problem",
                1068 to "error_outline",
                1069 to "not_interested",
                1070 to "report",
                1066 to "turned_in",
                1067 to "turned_in_not",
                1073 to "headset",
                1074 to "mic_none",
                1075 to TIMER,
                1076 to CLEAR,
                1077 to "search",
                1078 to "repeat",
                1079 to NOTIFICATIONS,
                1080 to "star_half",
                1081 to "share",
                1082 to "sentiment_very_satisfied",
                1083 to "sentiment_very_dissatisfied",
                1084 to "sentiment_satisfied",
                1085 to "sentiment_dissatisfied",
                1086 to "mood_bad",
                1087 to "mood",
                1088 to "spa",
                1089 to "room_service",
                1090 to "meeting_room",
                1091 to "hot_tub",
                1092 to "business_center",
                1093 to "priority_high",
                1094 to "power",
                1095 to "power_off",
                1096 to "directions_bike",
                1097 to "local_florist",
                1098 to "local_pizza",
                1099 to "navigation",
                1100 to "local_play",
                1101 to "local_bar",
                1102 to "local_laundry_service",
                1103 to "local_offer",
                1104 to "local_shipping",
                1105 to "local_hospital",
                1106 to "directions_boat",
                1107 to "directions_walk",
                1108 to "wb_incandescent",
                1109 to "landscape",
                1110 to "music_note",
                1111 to "healing",
                1112 to "brush",
                1113 to "brightness_2",
                1114 to "security",
                1115 to "scanner",
                1116 to "router",
                1117 to "watch",
                1118 to "videogame_asset",
                1119 to "cached",
//        1120 to "ic_octocat",
                1121 to "perm_identity",
                1122 to "track_changes",
                1123 to "open_in_new",
                1124 to EDIT,
                1125 to "info",
                1126 to "palette",
                1127 to "sd_storage",
                1128 to "baseline_lens",
                1129 to "map",
                1130 to "check_blackdp",
                1131 to "undo",
                1132 to "next_week",
                1133 to "local_cafe",
                1134 to "nights_stay",
                1135 to "single_bed",
                1136 to "weather_sunset",
                1137 to "calendar_today",
                1138 to "select_all",
                1139 to "widgets",
                1140 to "call_split",
                1141 to "ac_unit",
                1142 to "airport_shuttle",
                1143 to "apartment",
                1144 to "bathtub",
                1145 to "casino",
                1146 to "child_care",
                1147 to "pool",
                1148 to "house",
                1149 to "storefront",
                1150 to "poll",
                1151 to "emoji_transportation",
                1152 to "emoji_objects",
                1153 to "emoji_nature",
                1154 to "emoji_food_beverage",
                1155 to "emoji_events",
                1156 to "deck",
                1157 to "fireplace",
                1158 to "outdoor_grill",
                1159 to "thumb_up",
                1160 to "thumb_down",
                1161 to "vertical_align_top",
                1162 to "keyboard_arrow_left",
                1163 to "keyboard_arrow_right",
                1164 to "content_paste",
                1165 to "content_copy",
                1166 to "play_arrow",
                1167 to "play_circle_outline",
                1168 to "not_started",
                1169 to "date_range",
                1170 to PENDING_ACTIONS,
                1171 to "visibility_off",
                1172 to "nature",
                1173 to "eco",
                1174 to "bedtime",
                1175 to "auto_stories",
                1176 to "flash_on",
                1177 to "wb_twilight",
                1178 to "local_atm",
                1179 to "cleaning_services",
                1180 to "plumbing",
                1181 to "pest_control_rodent",
                1182 to "people_outline",
                1183 to "forum",
//        1184 to "twitter_logo_black",
                1185 to "person_add",
                1186 to "block",
            )
        }
    }
}