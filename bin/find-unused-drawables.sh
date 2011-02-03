#!/bin/bash
PROJECT=$(cd `dirname $0`/../astrid; pwd)
for file in $(ls $PROJECT/res/drawable -l | awk '{ print $8}' | sed 's/\..\+//g'); do 
    count=0; 
    for SRCFILE in `find $PROJECT -name "*.xml" -print 2> /dev/null`; do 
        let "count+=$(grep -c @drawable/$file $SRCFILE)"; 
    done
    for SRCFILE in `find $PROJECT -name "*.java" -print 2> /dev/null`; do 
        let "count+=$(grep -c R.drawable.$file $SRCFILE)"; 
    done; 
    if [ $count -lt 1 ]; then 
        echo -e "\e[0;31m$file\e[0m not used"; 
    else 
        echo -e "\e[0;32m$file\e[0m used"; 
    fi; 
done; 
