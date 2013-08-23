#!/bin/bash
PROJECT=$(cd `dirname $0`/..; pwd)
pushd ${PROJECT}
echo ${PROJECT}
path=astrid/res/layout
for file in $(ls -l ${path} | awk '{ print $9}' | sed 's/\..\+//g'); do 
    if [ ! -f ${path}/${file} ]; then
        continue
    fi;
    filename=`basename ${file}`
    filename=${filename%.*}
    git grep -q ${filename}
    if [ $? -ne 0 ]; then
        echo -e "${file} not used"
    fi;
done; 
popd
