/*
 * Copyright 2019 Mike Penz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tasks.icons

import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.ITypeface
import java.util.LinkedList

@Suppress("EnumEntryName")
object OutlinedGoogleMaterial2 : ITypeface {

    override val fontRes: Int
        get() = R.font.google_material_font_40811b1

    override val characters: Map<String, Char> by lazy {
        Icon.values().associate { it.name to it.character }
    }
    
    override val mappingPrefix: String
        get() = "gmo"

    override val fontName: String
        get() = "Google Material"

    override val version: String
        get() = "1"

    override val iconCount: Int
        get() = characters.size

    override val icons: List<String>
        get() = characters.keys.toCollection(LinkedList())

    override val author: String
        get() = "Google"

    override val url: String
        get() = "https://github.com/google/material-design-icons/tree/master/symbols/web"

    override val description: String
        get() = "Google Material Icons"

    override val license: String
        get() = "Apache 2.0"

    override val licenseUrl: String
        get() = "https://github.com/google/material-design-icons/blob/master/LICENSE"

    override fun getIcon(key: String): IIcon = Icon.valueOf(key)

    enum class Icon constructor(override val character: Char) : IIcon {
		gmo_vertical_align_center('\uf1c1'),
		gmo_vertical_align_top('\uf1c0'),
		gmo_vertical_distribute('\uf1bf'),
		gmo_vertical_shades('\uf1be'),
		gmo_vertical_shades_closed('\uf1bd'),
		gmo_vertical_split('\uf1bc'),
		gmo_vibration('\uf1bb'),
		gmo_video_call('\uf1ba'),
		gmo_video_camera_back('\uf1b9'),
		gmo_video_camera_front('\uf1b8'),
		gmo_video_camera_front_off('\uf1b7'),
		gmo_video_chat('\uf1b6'),
		gmo_video_file('\uf1b5'),
		gmo_video_label('\uf1b4'),
		gmo_video_library('\uf1b3'),
		gmo_video_search('\uf1b2'),
		gmo_video_settings('\uf1b1'),
		gmo_video_stable('\uf1b0'),
		gmo_videocam('\uf1af'),
		gmo_videocam_off('\uf1ae'),
		gmo_videogame_asset('\uf1ad'),
		gmo_videogame_asset_off('\uf1ac'),
		gmo_view_agenda('\uf1ab'),
		gmo_view_array('\uf1aa'),
		gmo_view_carousel('\uf1a9'),
		gmo_view_column('\uf1a8'),
		gmo_view_column_2('\uf1a7'),
		gmo_view_comfy('\uf1a6'),
		gmo_view_comfy_alt('\uf1a5'),
		gmo_view_compact('\uf1a4'),
		gmo_view_compact_alt('\uf1a3'),
		gmo_view_cozy('\uf1a2'),
		gmo_view_day('\uf1a1'),
		gmo_view_headline('\uf1a0'),
		gmo_view_in_ar('\uf19f'),
		gmo_view_in_ar_new('\uf19e'),
		gmo_view_in_ar_off('\uf19d'),
		gmo_view_kanban('\uf19c'),
		gmo_view_list('\uf19b'),
		gmo_view_module('\uf19a'),
		gmo_view_quilt('\uf199'),
		gmo_view_real_size('\uf198'),
		gmo_view_sidebar('\uf197'),
		gmo_view_stream('\uf196'),
		gmo_view_timeline('\uf195'),
		gmo_view_week('\uf194'),
		gmo_vignette('\uf193'),
		gmo_villa('\uf192'),
		gmo_visibility('\uf191'),
		gmo_visibility_lock('\uf190'),
		gmo_visibility_off('\uf18f'),
		gmo_vital_signs('\uf18e'),
		gmo_vitals('\uf18d'),
		gmo_vo2_max('\uf18c'),
		gmo_voice_chat('\uf18b'),
		gmo_voice_over_off('\uf18a'),
		gmo_voice_selection('\uf189'),
		gmo_voicemail('\uf188'),
		gmo_volcano('\uf187'),
		gmo_volume_down('\uf186'),
		gmo_volume_down_alt('\uf185'),
		gmo_volume_mute('\uf184'),
		gmo_volume_off('\uf183'),
		gmo_volume_up('\uf182'),
		gmo_volunteer_activism('\uf181'),
		gmo_voting_chip('\uf180'),
		gmo_vpn_key('\uf17f'),
		gmo_vpn_key_alert('\uf17e'),
		gmo_vpn_key_off('\uf17d'),
		gmo_vpn_lock('\uf17c'),
		gmo_vr180_create2d('\uf17b'),
		gmo_vr180_create2d_off('\uf17a'),
		gmo_vrpano('\uf179'),
		gmo_wall_art('\uf178'),
		gmo_wall_lamp('\uf177'),
		gmo_wallet('\uf176'),
		gmo_wallpaper('\uf175'),
		gmo_wallpaper_slideshow('\uf174'),
		gmo_ward('\uf173'),
		gmo_warehouse('\uf172'),
		gmo_warning('\uf171'),
		gmo_warning_off('\uf170'),
		gmo_wash('\uf16f'),
		gmo_watch('\uf16e'),
		gmo_watch_button_press('\uf16d'),
		gmo_watch_check('\uf16c'),
		gmo_watch_off('\uf16b'),
		gmo_watch_screentime('\uf16a'),
		gmo_watch_vibration('\uf169'),
		gmo_watch_wake('\uf168'),
		gmo_water('\uf167'),
		gmo_water_bottle('\uf166'),
		gmo_water_bottle_large('\uf165'),
		gmo_water_damage('\uf164'),
		gmo_water_do('\uf163'),
		gmo_water_drop('\uf162'),
		gmo_water_ec('\uf161'),
		gmo_water_full('\uf160'),
		gmo_water_heater('\uf15f'),
		gmo_water_lock('\uf15e'),
		gmo_water_loss('\uf15d'),
		gmo_water_lux('\uf15c'),
		gmo_water_medium('\uf15b'),
		gmo_water_orp('\uf15a'),
		gmo_water_ph('\uf159'),
		gmo_water_pump('\uf158'),
		gmo_water_voc('\uf157'),
		gmo_waterfall_chart('\uf156'),
		gmo_waves('\uf155'),
		gmo_waving_hand('\uf154'),
		gmo_wb_auto('\uf153'),
		gmo_wb_incandescent('\uf152'),
		gmo_wb_iridescent('\uf151'),
		gmo_wb_shade('\uf150'),
		gmo_wb_sunny('\uf14f'),
		gmo_wb_twilight('\uf14e'),
		gmo_wc('\uf14d'),
		gmo_weather_hail('\uf14c'),
		gmo_weather_mix('\uf14b'),
		gmo_weather_snowy('\uf14a'),
		gmo_web('\uf149'),
		gmo_web_asset('\uf148'),
		gmo_web_asset_off('\uf147'),
		gmo_web_stories('\uf146'),
		gmo_web_traffic('\uf145'),
		gmo_webhook('\uf144'),
		gmo_weekend('\uf143'),
		gmo_weight('\uf142'),
		gmo_west('\uf141'),
		gmo_whatshot('\uf140'),
		gmo_wheelchair_pickup('\uf13f'),
		gmo_where_to_vote('\uf13e'),
		gmo_widgets('\uf13d'),
		gmo_width('\uf13c'),
		gmo_width_full('\uf13b'),
		gmo_width_normal('\uf13a'),
		gmo_width_wide('\uf139'),
		gmo_wifi('\uf138'),
		gmo_wifi_1_bar('\uf137'),
		gmo_wifi_2_bar('\uf136'),
		gmo_wifi_add('\uf135'),
		gmo_wifi_calling('\uf134'),
		gmo_wifi_calling_1('\uf133'),
		gmo_wifi_calling_2('\uf132'),
		gmo_wifi_calling_3('\uf131'),
		gmo_wifi_calling_bar_1('\uf130'),
		gmo_wifi_calling_bar_2('\uf12f'),
		gmo_wifi_calling_bar_3('\uf12e'),
		gmo_wifi_channel('\uf12d'),
		gmo_wifi_find('\uf12c'),
		gmo_wifi_home('\uf12b'),
		gmo_wifi_lock('\uf12a'),
		gmo_wifi_notification('\uf129'),
		gmo_wifi_off('\uf128'),
		gmo_wifi_password('\uf127'),
		gmo_wifi_protected_setup('\uf126'),
		gmo_wifi_proxy('\uf125'),
		gmo_wifi_tethering('\uf124'),
		gmo_wifi_tethering_error('\uf123'),
		gmo_wifi_tethering_off('\uf122'),
		gmo_wind_power('\uf121'),
		gmo_window('\uf120'),
		gmo_window_closed('\uf11f'),
		gmo_window_open('\uf11e'),
		gmo_window_sensor('\uf11d'),
		gmo_wine_bar('\uf11c'),
		gmo_woman('\uf11b'),
		gmo_woman_2('\uf11a'),
		gmo_work('\uf119'),
		gmo_work_alert('\uf118'),
		gmo_work_history('\uf117'),
		gmo_work_update('\uf116'),
		gmo_workspace_premium('\uf115'),
		gmo_workspaces('\uf114'),
		gmo_workspaces_outline('\uf113'),
		gmo_wounds_injuries('\uf112'),
		gmo_wrap_text('\uf111'),
		gmo_wrist('\uf110'),
		gmo_wrong_location('\uf10f'),
		gmo_wysiwyg('\uf10e'),
		gmo_yard('\uf10d'),
		gmo_your_trips('\uf10c'),
		gmo_youtube_activity('\uf10b'),
		gmo_youtube_searched_for('\uf10a'),
		gmo_zone_person_alert('\uf109'),
		gmo_zone_person_idle('\uf108'),
		gmo_zone_person_urgent('\uf107'),
		gmo_zoom_in('\uf106'),
		gmo_zoom_in_map('\uf105'),
		gmo_zoom_out('\uf104'),
		gmo_zoom_out_map('\uf103'),
		gmo_local_grocery_store('\uf3e2'), // mapped to 'gmo_shopping_cart'
		;

		override val typeface: ITypeface by lazy { OutlinedGoogleMaterial2 }
	}
}