#!/bin/bash
for DIR in $(ls -d **/src/main/res/layout*); do
    for file in $(ls -l ${DIR} | awk '{ print $9}' | sed 's/\..\+//g'); do
        if [ ! -f ${DIR}/${file} ]; then
            continue
        fi;
        filename=`basename ${file}`
        filename=${filename%.*}
        git grep -q ${filename} .
        if [ $? -ne 0 ]; then
            echo -e "${DIR}/${file} not used"
        fi;
    done;
done;
