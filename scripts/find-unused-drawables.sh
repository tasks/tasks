#!/bin/bash
for DIR in $(ls -d src/main/res/drawable*); do
    for file in $(ls -l ${DIR} | awk '{ print $9}' | sed 's/\..\+//g'); do
        filename=`basename ${file}`
        filename=${filename%%.*}
        git grep -q ${filename} .
        if [ $? -ne 0 ]; then
            echo -e "${DIR}/${file} not used"
        fi;
    done;
done;
        #let "count+=$(grep -c @drawable/$file $SRCFILE)";
        #let "count+=$(grep -c R.drawable.$file $SRCFILE)";
