#!/bin/bash
#
#Wrapper for xml2po for android and launchpad: Import .xml's from .po's, or export/update .po's from string.xml's. Provide a string with value "translator-credits" for Launchpad.
#   
# 	 Copyright (C) 2009 pjv
# 
# 	 This file is part of OpenIntents Androidxml2po.
#
#   OpenIntents Androidxml2po is free software: you can redistribute it and/or modify
#   it under the terms of the GNU General Public License as published by
#   the Free Software Foundation, either version 3 of the License, or
#   (at your option) any later version.
#
#   OpenIntents Androidxml2po is distributed in the hope that it will be useful,
#   but WITHOUT ANY WARRANTY; without even the implied warranty of
#   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#   GNU General Public License for more details.
#
#   You should have received a copy of the GNU General Public License
#   along with OpenIntents Androidxml2po.  If not, see <http://www.gnu.org/licenses/>.
#   
#

if [ ! -e /usr/bin/gettext ]; then
    echo "WARNING: gettext not detected - try apt-get install gettext"
fi    

#Set the languages here (long version is the android resource append string).
short_lang=("ca" "es" "fr" "de" "ja" "ru") #do not include template language ("en" usually).
long_lang=("ca" "es" "fr" "de" "ja" "ru")
#Change the dirs where the files are located.
launchpad_po_files_dir="translations"
launchpad_pot_file_dir="translations"
android_xml_files_res_dir="res/values"
#Change the typical filenames.
android_xml_filenames="strings arrays"
#Location of xml2po
xml2po="bin/xml2po.py"

function import_po2xml
{
    for resource_file in $android_xml_filenames; do
        for (( i=0 ; i<${#short_lang[*]} ; i=i+1 )); do
            echo "Importing .xml from .po for "${resource_file}-${short_lang[i]}""
            mkdir -p "${android_xml_files_res_dir}"-"${long_lang[i]}"
            ${xml2po} -a -l "${short_lang[i]}" -p "${launchpad_po_files_dir}"/"${resource_file}"-"${short_lang[i]}".po \
                "${android_xml_files_res_dir}"/"${resource_file}".xml > "${android_xml_files_res_dir}"-"${long_lang[i]}"/"${resource_file}".xml
        done
    done
}

function export_xml2po
{
    for resource_file in $android_xml_filenames; do
        echo "Exporting .xml to .pot: $resource_file"
        ${xml2po} -a -l "${short_lang[i]}" -o \
            "${launchpad_pot_file_dir}"/"${resource_file}".pot \
            "${android_xml_files_res_dir}"/"${resource_file}".xml

#        for (( i=0 ; i<${#short_lang[*]} ; i=i+1 )); do
#            if [ -e "${launchpad_po_files_dir}"/"${resource_file}"-"${short_lang[i]}".po ] ; then
#            	echo "Exporting .xml to updated .po for "${short_lang[i]}""
#            	echo "Making temporary folder: .tmp."${launchpad_po_files_dir}""
#            	mkdir -p .tmp."${launchpad_po_files_dir}"
#            	if [ -e "${android_xml_files_res_dir}"-"${long_lang[i]}"/"${resource_file}".xml ] ; then
#                	${xml2po} -a -u "${launchpad_po_files_dir}"/"${resource_file}"-"${short_lang[i]}".po \
#                	    "${android_xml_files_res_dir}"/"${resource_file}".xml
#                else
#                	${xml2po} -a -u "${launchpad_po_files_dir}"/"${resource_file}"-"${short_lang[i]}".po \
#                	    "${android_xml_files_res_dir}"/"${resource_file}".xml
#                fi
#            fi 
#        done
    done
}

function usage
{
    echo "Wrapper for xml2po for android and launchpad."
    echo "Usage: androidxml2po -i        Import .xml's from .po's. Updates the .xml's."
    echo "       androidxml2po -e        Export/update .po's from string.xml's. Overwrites the .pot and merges the .po's."
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
        -h | --help )           		usage
                                		exit
                                		;;
        * )                     		usage
                                		exit 1
    esac
    shift
done
usage
