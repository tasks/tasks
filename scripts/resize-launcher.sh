#!/bin/bash

INPUT="$1"
FILENAME="$2"
RESOURCES=src/main/res

convert -resize 144x144 ${INPUT} ${RESOURCES}/drawable-xxhdpi/${FILENAME}
convert -resize 96x96 ${INPUT} ${RESOURCES}/drawable-xhdpi/${FILENAME}
convert -resize 72x72 ${INPUT} ${RESOURCES}/drawable-hdpi/${FILENAME}
convert -resize 48x48 ${INPUT} ${RESOURCES}/drawable/${FILENAME}
