#!/usr/bin/env python3
import io, json, os, sys, urllib.request

try:
    from fontTools.ttLib import TTFont
    from fontTools.varLib import instancer
except ImportError:
    sys.exit('fonttools is needed: uv run --with fonttools generate_material_symbols.py')

COMMIT = '40a7a292a79d9394157e1ea24f83d52d5e17c556'
REPO = f'https://raw.githubusercontent.com/google/material-design-icons/{COMMIT}'
SYMBOLS = f'{REPO}/variablefont/MaterialSymbolsOutlined%5BFILL%2CGRAD%2Copsz%2Cwght%5D'
METADATA = 'https://fonts.google.com/metadata/icons?key=material_symbols&incomplete=1'
AXES = {'wght': 400, 'GRAD': 0, 'opsz': 24}
ALIASES = {
    'discount': 'sell',
    'fire_hydrant_alt': 'fire_hydrant',
    'miscellaneous_services': 'settings_suggest',
    'no_cell': 'mobile_off',
    'panorama_horizontal_select': 'panorama_horizontal',
    'panorama_photosphere_select': 'panorama_photosphere',
    'panorama_vertical_select': 'panorama_vertical',
    'panorama_wide_angle_select': 'panorama_wide_angle',
    'person_add_alt_1': 'person_add',
    'person_remove_alt_1': 'person_remove',
    'play_circle_filled': 'play_circle',
    'play_circle_outline': 'play_circle',
    'signal_wifi_statusbar_connected_no_internet_4': 'signal_wifi_statusbar_not_connected',
}
MIRRORED = [
    '360', 'accessible', 'accessible_forward', 'add_to_home_screen', 'airplane_ticket', 'align_horizontal_left',
    'align_horizontal_right', 'alt_route', 'announcement', 'arrow_back_ios', 'arrow_forward_ios', 'arrow_left',
    'arrow_right', 'arrow_right_alt', 'article', 'assignment', 'assignment_return', 'assistant_direction',
    'backspace', 'battery_unknown', 'bluetooth_searching', 'branding_watermark', 'call_made', 'call_merge',
    'call_missed', 'call_missed_outgoing', 'call_received', 'call_split', 'chat', 'chrome_reader_mode', 'comment',
    'compare_arrows', 'contact_support', 'directions_bike', 'directions_run', 'directions_walk',
    'drive_file_move', 'dvr', 'event_note', 'fact_check', 'featured_play_list', 'featured_video', 'feed',
    'follow_the_signs', 'format_align_left', 'format_align_right', 'format_indent_decrease',
    'format_indent_increase', 'format_list_bulleted', 'format_textdirection_l_to_r',
    'format_textdirection_r_to_l', 'forward', 'forward_to_inbox', 'grading', 'help', 'help_center',
    'help_outline', 'input', 'insert_comment', 'insert_drive_file', 'keyboard_backspace', 'keyboard_return',
    'keyboard_tab', 'label', 'label_important', 'label_off', 'last_page', 'launch', 'library_books', 'list_alt',
    'live_help', 'login', 'logout', 'manage_search', 'menu_book', 'menu_open', 'merge_type', 'message',
    'missed_video_call', 'mobile_screen_share', 'more', 'multiline_chart', 'navigate_before', 'navigate_next',
    'next_plan', 'next_week', 'not_listed_location', 'note', 'note_add', 'notes', 'offline_share', 'open_in_new',
    'outbound', 'phone_callback', 'phone_forwarded', 'phone_missed', 'playlist_add', 'playlist_add_check',
    'playlist_play', 'queue_music', 'read_more', 'receipt_long', 'redo', 'reply', 'reply_all', 'rotate_left',
    'rotate_right', 'rtt', 'rule', 'schedule_send', 'screen_share', 'segment', 'send_and_archive',
    'send_to_mobile', 'short_text', 'shortcut', 'show_chart', 'sort', 'speaker_notes', 'star_half',
    'sticky_note_2', 'stop_screen_share', 'subject', 'text_snippet', 'toc', 'trending_down', 'trending_flat',
    'trending_up', 'undo', 'view_list', 'view_quilt', 'view_sidebar', 'volume_down', 'volume_mute', 'volume_off',
    'volume_up', 'wrap_text', 'wysiwyg',
]
ROOT = os.path.dirname(os.path.abspath(__file__))
FONT = f'{ROOT}/kmp/src/commonMain/composeResources/font/material_symbols_outlined.ttf'
TABLE = f'{ROOT}/kmp/src/commonMain/kotlin/org/tasks/icons/MaterialSymbols.kt'
CATALOG = f'{ROOT}/kmp/src/commonMain/composeResources/files/icons.json'
CHUNK = 50_000

def fetch(url):
    with urllib.request.urlopen(url) as response:
        return response.read()


def read_codepoints(url):
    return {name: int(code, 16) for name, code in (line.split() for line in fetch(url).decode().splitlines() if line.strip())}


codepoints = read_codepoints(f'{SYMBOLS}.codepoints')
for alias, target in ALIASES.items():
    codepoints[alias] = codepoints[target]

font = TTFont(io.BytesIO(fetch(f'{SYMBOLS}.ttf')), recalcTimestamp=False)
instancer.instantiateVariableFont(font, AXES, inplace=True, updateFontNames=True)

os.makedirs(os.path.dirname(FONT), exist_ok=True)
font.save(FONT)

entries = [f'{name}:{code:x}' for name, code in sorted(codepoints.items())]
chunks, current = [], ''
for entry in entries:
    if len(current) + len(entry) + 1 > CHUNK:
        chunks.append(current)
        current = ''
    current += (';' if current else '') + entry
chunks.append(current)

os.makedirs(os.path.dirname(TABLE), exist_ok=True)
with open(TABLE, 'w') as out:
    out.write(f'''package org.tasks.icons

object MaterialSymbols {{
    fun codepoint(name: String): Int? = codepoints[name]

    fun glyph(name: String): String? = codepoint(name)?.let {{ code ->
        if (code < 0x10000) {{
            code.toChar().toString()
        }} else {{
            val offset = code - 0x10000
            charArrayOf((0xD800 + (offset shr 10)).toChar(), (0xDC00 + (offset and 0x3FF)).toChar()).concatToString()
        }}
    }}

    fun isMirrored(name: String): Boolean = name in MIRRORED

    private val codepoints: Map<String, Int> by lazy {{
        buildMap({len(entries)}) {{
            for (chunk in CODEPOINTS) {{
                for (entry in chunk.splitToSequence(';')) {{
                    val separator = entry.indexOf(':')
                    put(entry.substring(0, separator), entry.substring(separator + 1).toInt(16))
                }}
            }}
        }}
    }}

    private val CODEPOINTS = arrayOf(
''')
    for chunk in chunks:
        out.write(f'        "{chunk}",\n')
    out.write('    )\n\n    private val MIRRORED = setOf(\n')
    for name in MIRRORED:
        out.write(f'        "{name}",\n')
    out.write('    )\n}\n')

metadata = fetch(METADATA).decode()
icons = json.loads(metadata[metadata.index('{'):])['icons']
catalog = [
    {'name': icon['name'], 'categories': icon['categories'], 'tags': icon['tags']}
    for icon in icons
    if 'Material Symbols Outlined' not in icon.get('unsupported_families', [])
]
missing = [icon['name'] for icon in catalog if icon['name'] not in codepoints]
if missing:
    sys.exit(f'icons missing from the font at COMMIT: {missing}')
with open(CATALOG, 'w') as out:
    json.dump({'icons': catalog}, out, separators=(',', ':'))
    out.write('\n')

print(f'{len(entries)} names ({len(ALIASES)} aliases) -> {os.path.relpath(TABLE, ROOT)}')
print(f'{os.path.getsize(FONT)} bytes -> {os.path.relpath(FONT, ROOT)}')
print(f'{len(catalog)} icons -> {os.path.relpath(CATALOG, ROOT)}')
