#!/bin/bash
###############################################################################
#
#   Wrapper for xml2po for android and launchpad: Import .xml's from .po's,
#   or export/update .po's from string.xml's. Provide a string with value
#   "translator-credits" for Launchpad.
#   
# 	 Copyright (C) 2009 pjv, modified by Tim Su <tim@todoroo.com>
# 
# 	 This file is part of OpenIntents Androidxml2po.
#
#   OpenIntents Androidxml2po is free software: you can redistribute it
#   and/or modify it under the terms of the GNU General Public License as
#   published by the Free Software Foundation, either version 3 of the
#   License, or (at your option) any later version.
#
#   OpenIntents Androidxml2po is distributed in the hope that it will be
#   useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
#   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
#   General Public License for more details.
#
#   You should have received a copy of the GNU General Public License along
#   with OpenIntents Androidxml2po.  If not, see
#   <http://www.gnu.org/licenses/>.
#   
###############################################################################
# Using this file:
#
# 1. check out android supported locales:
#    http://developer.android.com/sdk/android-2.2.html#locs
# 2. update your translations folder
# 3. update the po_lang and res_lang strings

if [ ! -e /usr/bin/gettext ]; then
    echo "WARNING: gettext not detected - try apt-get install gettext"
fi    

# Set the languages here (po -> name of .po file. res -> name of res folder)
po_lang=( "ca" "cs" "da" "de" "es" "fr" "he" "it" "ja" "ko" "nb" "nl" "pl" "pt" "pt_BR" "ru" "sv" "th" "tr" "zh_CN"  "zh_TW")
res_lang=("ca" "cs" "da" "de" "es" "fr" "he" "it" "ja" "ko" "nb" "nl" "pl" "pt" "pt-rBR" "ru" "sv" "th" "tr" "zh-rCN" "zh-rTW")

#Change the dirs where the files are located.
launchpad_po_files_dir="translations"
launchpad_pot_file_dir="translations"

folder="astrid"
if [ "$2" != "" ]; then
    echo "Operating on folder $2"
    folder="$2"
fi
android_xml_files_res_dir="${folder}/res/values"

#Change the typical filenames.
android_xml_filenames="strings"
#Location of xml2po
xml2po="`dirname $0`/xml2po.py"
catxml="`dirname $0`/catxml"

function cat_all_xml() {
    ${catxml} "${android_xml_files_res_dir}"/"${resource_file}"-*.xml api/res/values/${resource_file}*.xml \
        ../astrid-plugins/astrid-power-pack/res/values/${resource_file}*.xml \
        > "${launchpad_pot_file_dir}/${resource_file}".xml
}

function import_po2xml
{
    for resource_file in $android_xml_filenames; do
        echo "Concatenating strings into single XML"
        ${catxml} "${android_xml_files_res_dir}"/"${resource_file}"*.xml > "${launchpad_pot_file_dir}/${resource_file}".xml
        echo "Importing .xml from .pot: $resource_file"
        for (( i=0 ; i<${#po_lang[*]} ; i=i+1 )); do
            echo " Importing .xml from .po for "${resource_file}-${po_lang[i]}""
            mkdir -p "${android_xml_files_res_dir}"-"${res_lang[i]}"
            ${xml2po} -a -l "${po_lang[i]}" -p "${launchpad_po_files_dir}/${resource_file}"-"${po_lang[i]}".po \
                "${launchpad_pot_file_dir}"/"${resource_file}".xml > "${android_xml_files_res_dir}"-"${res_lang[i]}"/"${resource_file}".xml
        done
    done
    rm -f .xml2po.mo
    sed -i 's/\\\\/\\/g' ${android_xml_files_res_dir}-*/* 
}

function export_xml2po
{
    for resource_file in $android_xml_filenames; do
        echo "Concatenating strings into single XML"
        cat_all_xml
        echo "Exporting .xml to .pot: $resource_file"
        ${xml2po} -a -l en -o \
            "${launchpad_pot_file_dir}/${resource_file}".pot \
            "${launchpad_pot_file_dir}/${resource_file}.xml"

        for (( i=0 ; i<${#po_lang[*]} ; i=i+1 )); do
            echo " Exporting .xml to updated .po for "${resource_file}-${po_lang[i]}
            ${xml2po} -a \
                -r "${android_xml_files_res_dir}"-"${res_lang[i]}"/"${resource_file}".xml \
                "${launchpad_pot_file_dir}"/"${resource_file}".xml > \
                "${launchpad_po_files_dir}/${resource_file}"-"${po_lang[i]}".po
            msguniq "${launchpad_po_files_dir}/${resource_file}"-"${po_lang[i]}".po -o "${launchpad_po_files_dir}/${resource_file}"-"${po_lang[i]}".po
        done
    done

}

function export_pot
{
    for resource_file in $android_xml_filenames; do
        echo "Concatenating strings into single XML"
        cat_all_xml
        echo "Exporting .xml to .pot: $resource_file"
        ${xml2po} -a -l en -o \
            "${launchpad_pot_file_dir}/${resource_file}".pot \
            "${launchpad_pot_file_dir}/${resource_file}.xml"
    done

}
function usage
{
    echo "Wrapper for xml2po for android and launchpad."
    echo "Usage: androidxml2po -i        Import .xml's from .po's. Updates the .xml's."
    echo "       androidxml2po -e        Export/update .po's from string.xml's. Overwrites the .pot and merges the .po's. (NOTE: SUPER BROKEN)"
    echo "       androidxml2po -t        Export/update .pot file"
    echo "Set variables correctly inside. Provide a string with value "translator-credits" for Launchpad."
    echo ""
    echo "Copyright 2009 by pjv. Licensed under GPLv3."
}

###Main
while [ "$1" != "" ]; do
    case $1 in
        -i | --po2xml | --import )         	shift
        					import_po2xml
        					exit
                                		;;
        -e | --xml2po | --export )    		export_xml2po
        					exit
                                		;;
        -t | --pot )    			export_pot
        					exit
                                		;;
        -h | --help )           		usage
                                		exit
                                		;;
        * )                     		usage
                                		exit 1
    esac
    shift
done
usage

