/*
 * The table below is copied unchanged from ical4j 3.2.19
 * (src/main/resources/net/fortuna/ical4j/transform/rfc5545/msTimezones, read by TzHelper).
 * https://github.com/ical4j/ical4j
 *
 * BSD 3-Clause License
 *
 * Copyright (c) 2012, Ben Fortuna
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.tasks.icalendar

import kotlinx.datetime.TimeZone

// Maps a Windows TZID (e.g. Exchange's "W. Europe Standard Time") to an Olson ID, like ical4j's TzHelper on JVM.
internal fun olsonId(tzId: String): String =
    if (isTimeZone(tzId)) tzId else msTimeZones[tzId]?.takeIf(::isTimeZone) ?: tzId

private fun isTimeZone(id: String) = runCatching { TimeZone.of(id) }.isSuccess

// Each line is "<Windows ID>;<display name>=<Olson ID>"; both names map to the Olson ID.
private val msTimeZones: Map<String, String> by lazy {
    MS_TIMEZONES.trim().lines().flatMap { line ->
        val (names, olson) = line.split('=')
        names.split(';').map { it to olson }
    }.toMap()
}

private const val MS_TIMEZONES = """
Dateline Standard Time;(UTC-12:00) International Date Line West=Etc/GMT+12
UTC-11;(UTC-11:00) Coordinated Universal Time -11=Etc/GMT+12
Samoa Standard Time;(UTC-11:00)Samoa=US/Samoa
Hawaiian Standard Time;(UTC-10:00) Hawaii=US/Hwaii
Alaskan Standard Time;(UTC-09:00) Alaska=US/Alaska
Pacific Standard Time;(UTC-08:00) Pacific Time (US and Canada)=Canada/Pacific
Pacific Standard Time (Mexico);(UTC-08:00)Baja California=Mexico/BajaNorte
Mountain Standard Time;(UTC-07:00) Mountain Time (US and Canada)=America/Denver
Mountain Standard Time (Mexico);(UTC-07:00) Chihuahua, La Paz, Mazatlan=America/Chihuahua
US Mountain Standard Time;(UTC-07:00) Arizona=US/Arizona
Canada Central Standard Time;(UTC-06:00) Saskatchewan=Canada/Saskatchewan
Central America Standard Time;(UTC-06:00) Central America=US/Central
Central Standard Time;(UTC-06:00) Central Time (US and Canada)=America/Winnipeg
Central Standard Time (Mexico);(UTC-06:00) Guadalajara, Mexico City, Monterrey=Mexico/General
Eastern Standard Time;(UTC-05:00) Eastern Time (US and Canada)=US/Eastern
SA Pacific Standard Time;(UTC-05:00) Bogota, Lima, Quito=America/Bogota
US Eastern Standard Time;(UTC-05:00) Indiana (East)=US/Indiana-Starke
Venezuela Standard Time;(UTC-04:30) Caracas=America/Caracas
Atlantic Standard Time;(UTC-04:00) Atlantic Time (Canada)=Atlantic/Bermuda
Central Brazilian Standard Time;(UTC-04:00) Cuiaba=America/Cuiaba
Pacific SA Standard Time;(UTC-04:00) Santiago=America/Santiago
SA Western Standard Time;(UTC-04:00) Georgetown, La Paz, Manaus, San Juan=America/Manaus
Paraguay Standard Time;(UTC-04:00) Asuncion=America/Asuncion
Newfoundland Standard Time;(UTC-03:30) Newfoundland=Canada/Newfoundland
E. South America Standard Time;(UTC-03:00) Brasilia=Brazil/East
Greenland Standard Time;(UTC-03:00) Greenland=Atlantic/Stanley
Montevideo Standard Time;(UTC-03:00) Montevideo=America/Montevideo
SA Eastern Standard Time;(UTC-03:00) Cayenne, Fortaleza=America/Fortaleza
Argentina Standard Time;(UTC-03:00) Buenos Aires=America/Buenos_Aires
Mid-Atlantic Standard Time;(UTC-02:00) Mid-Atlantic=Atlantic/South_Georgia
UTC-2;(UTC-02:00) Coordinated Universal Time -02=Etc/GMT+2
Azores Standard Time;(UTC-01:00) Azores=America/Azores
Cape Verde Standard Time;(UTC-01:00) Cape Verde Is.=Atlantic/Cape_Verde
GMT Standard Time;(UTC) Dublin, Edinburgh, Lisbon, London=Europe/Dublin
Greenwich Standard Time;(UTC) Monrovia, Reykjavik=UTC
Morocco Standard Time;(UTC) Casablanca=UTC
UTC;(UTC) Coordinated Universal Time=UTC
Central Europe Standard Time;(UTC+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague=Europe/Budapest
Central European Standard Time;(UTC+01:00) Sarajevo, Skopje, Warsaw, Zagreb=Europe/Sarajevo
Mitteleuropäische Zeit;(UTC+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna=Europe/Vienna
Romance Standard Time;(UTC+01:00) Brussels, Copenhagen, Madrid, Paris=Europe/Paris
W. Central Africa Standard Time;(UTC+01:00) West Central Africa=Africa/Algiers
W. Europe Standard Time;(UTC+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna=Europe/Vienna
Namibia Standard Time;(UTC+01:00) Windhoek=Africa/Windhoek
E. Europe Standard Time;(UTC+02:00) Minsk=Europe/Kiev
Egypt Standard Time;(UTC+02:00) Cairo=Africa/Cairo
FLE Standard Time;(UTC+02:00) Helsinki, Kyiv, Riga, Sofia, Tallinn, Vilnius=Europe/Sofia
GTB Standard Time;(UTC+02:00) Athens, Bucharest=Europe/Bucharest
GTB Standard Time;(UTC+02:00) Athens, Bucharest, Istanbul=Europe/Bucharest
Israel Standard Time;(UTC+02:00) Jerusalem=Asia/Jerusalem
Jordan Standard Time;(UTC+02:00) Amman=Asia/Amman
Middle East Standard Time;(UTC+02:00) Beirut=Asia/Beirut
South Africa Standard Time;(UTC+02:00) Harare, Pretoria=Africa/Harare
Syria Standard Time;(UTC+02:00) Damascus=Asia/Damascus
Turkey Standard Time;(UTC+02:00) Istanbul=Asia/Istanbul
Arab Standard Time;(UTC+03:00) Kuwait, Riyadh=Asia/Kuwait
Arabic Standard Time;(UTC+03:00) Baghdad=Asia/Bagdad
E. Africa Standard Time;(UTC+03:00) Nairobi=Africa/Nairobi
Kaliningrad Standard Time;(UTC+03:00) Kaliningrad=Europe/Kaliningrad
Iran Standard Time;(UTC+03:30) Tehran=Asia/Tehran
Russian Standard Time;(UTC+04:00) Moscow, St. Petersburg, Volgograd=Europe/Moscow
Arabian Standard Time;(UTC+04:00) Abu Dhabi, Muscat=Asia/Muscat
Azerbaijan Standard Time;(UTC+04:00) Baku=Asia/Baku
Caucasus Standard Time;(UTC+04:00) Yerevan=Asia/Yerevan
Georgian Standard Time;(UTC+04:00) Tbilisi=Asia/Tbilisi
Mauritius Standard Time;(UTC+04:00) Port Louis=Asia/Tbilisi
Afghanistan Standard Time;(UTC+04:30) Kabul=Asia/Kabul
West Asia Standard Time;(UTC+05:00) Tashkent=Asia/Tashkent
Pakistan Standard Time;(UTC+05:00) Islamabad, Karachi=Asia/Karachi
India Standard Time;(UTC+05:30) Chennai, Kolkata, Mumbai, New Delhi=Asia/Kolkata
Sri Lanka Standard Time;(UTC+05:30) Sri Jayawardenepura=Asia/Kolkata
Nepal Standard Time;(UTC+05:45) Kathmandu=Asia/Kathmandu
Ekaterinburg Standard Time;(UTC+06:00) Ekaterinburg=Asia/Yekaterinburg
Central Asia Standard Time;(UTC+06:00) Astana=Asia/Astana
Bangladesh Standard Time;(UTC+06:00) Dhaka=Asia/Astana
Myanmar Standard Time;(UTC+06:30) Yangon (Rangoon)=Asia/Rangoon
N. Central Asia Standard Time;(UTC+07:00) Novosibirsk=Asia/Novosibirsk
SE Asia Standard Time;(UTC+07:00) Bangkok, Hanoi, Jakarta=Asia/Jakarta
North Asia Standard Time;(UTC+08:00) Krasnoyarsk=Asia/Krasnoyarsk
China Standard Time;(UTC+08:00) Beijing, Chongqing, Hong Kong, Urumqi=Hongkong
Singapore Standard Time;(UTC+08:00) Kuala Lumpur, Singapore=Asia/Kuala_Lumpur
Taipei Standard Time;(UTC+08:00) Taipei=Asia/Taipei
W. Australia Standard Time;(UTC+08:00) Perth=Austraila/Perth
Ulaanbaatar Standard Time;(UTC+08:00) Ulaanbaatar=Asia/Ulaanbaatar
North Asia East Standard Time;(UTC+09:00) Irkutsk=Asia/Irkutsk
Korea Standard Time;(UTC+09:00) Seoul=Asia/Seoul
Tokyo Standard Time;(UTC+09:00) Osaka, Sapporo, Tokyo=Asia/Tokyo
AUS Central Standard Time;(UTC+09:30) Darwin=Australia/Darwin
Cen. Australia Standard Time;(UTC+09:30) Adelaide=Australia/Adelaide
Yakutsk Standard Time;(UTC+10:00) Yakutsk=Asia/Yakutsk
AUS Eastern Standard Time;(UTC+10:00) Canberra, Melbourne, Sydney=Australia/Melbourne
E. Australia Standard Time;(UTC+10:00) Brisbane=Australia/Brisbane
Tasmania Standard Time;(UTC+10:00) Hobart=Australia/Hobart
West Pacific Standard Time;(UTC+10:00) Guam, Port Moresby=Pacific/Guam
Vladivostok Standard Time;(UTC+11:00) Vladivostok=Asia/Vladivostok
Central Pacific Standard Time;(UTC+11:00) Solomon Is., New Caledonia=Pacific/Efate
Magadan Standard Time;(UTC+12:00) Magadan=Asia/Magadan
Fiji Standard Time;(UTC+12:00) Fiji=Pacific/Fiji
New Zealand Standard Time;(UTC+12:00) Auckland, Wellington=Pacific/Auckland
UTC+12;(UTC+12:00) Coordinated Universal Time +12=NZ
Tonga Standard Time;(UTC+13:00) Nuku'alofa=Pacific/Alpia
"""
