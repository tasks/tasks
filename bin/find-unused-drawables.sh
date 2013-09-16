#!/bin/bash
DIR="../astrid/src/main/res/drawable"
for file in $(ls -l ${DIR} | awk '{ print $9}' | sed 's/\..\+//g'); do
    if [ ! -f ${DIR}/${file} ]; then
        echo "ignoring ${file}"
        continue
    fi;
    filename=`basename ${file}`
    filename=${filename%%.*}
    git grep -q ${filename} ..
    if [ $? -ne 0 ]; then
        echo -e "${file} not used"
    fi;
done;
        #let "count+=$(grep -c @drawable/$file $SRCFILE)";
        #let "count+=$(grep -c R.drawable.$file $SRCFILE)";
