#!/bin/bash
for DIR in $(ls -d **/src/main/res/drawable*); do
    for file in $(ls -l ${DIR} | /usr/bin/awk '{ print $9}' | /usr/bin/sed 's/\..\+//g'); do
        if [ ! -f ${DIR}/${file} ]; then
            echo "ignoring ${file}"
            continue
        fi;
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
