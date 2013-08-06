#!/bin/bash
#PATH="../astrid/res/drawable"
for file in $(ls -l ../astrid/res/drawable | awk '{ print $9}' | sed 's/\..\+//g'); do 
    if [ ! -f ../astrid/res/drawable/${file} ]; then
        continue
    fi;
    filename=`basename ${file}`
    filename=${filename%.*}
    git grep -q ${filename} ..
    if [ $? -ne 0 ]; then
        echo -e "${file} not used"
    fi;
done;
        #let "count+=$(grep -c @drawable/$file $SRCFILE)"; 
        #let "count+=$(grep -c R.drawable.$file $SRCFILE)"; 
