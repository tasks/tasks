#!/bin/bash
############################################################################
# Astrid pre-commit hook:
#
# 1. if translation .xml files have been changed, check for
#    change in .po files as well
#
############################################################################

XML_FILES="res/values/strings.xml res/values/arrays.xml"
TRANSLATION_PATH="translations/"

STATUS=$(bzr status)
for FILE in $XML_FILES; do
    echo $STATUS | grep $FILE &> /dev/null
    if [ "$?" == "0" ]; then
        PO_FILE=${FILE##*/}
        PO_FILE=${TRANSLATION_PATH}${PO_FILE%%.*}.po
        echo $STATUS | grep $PO_FILE &> /dev/null
        if [ "$?" != "0" ]; then
            echo "[pre-commit] Error: please update .po file: $PO_FILE"
            exit 1
        fi
    fi
done

exit 0
