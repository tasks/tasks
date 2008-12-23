#!/bin/bash
#  BzrToGit - SCM migration tool for going from bzr to git
#  Copyright (C) 2009  Henrik Nilsson
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License, version 3, as 
#  published by the Free Software Foundation.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program.  If not, see <http:#www.gnu.org/licenses/>.

keepbzr="no"
for x in $@; do
  if [ "$x" = "--keepbzr" ]; then keepbzr="yes"
  elif [ "$x" = "--help" ]; then
    echo "--keepbzr = Keep the .bzr directory so that it can still be used by bazaar"
    echo "--help    = Display this list"
    exit
  else echo "${x}: unknown argument, type --help to see available arguments"; exit 1; fi
done

rev=1
git init
while bzr revert -r revno:$rev 2> /dev/null; do
  logentry="`bzr log -r $rev`"
  committer="`echo "$logentry" | sed -n -e "/^committer:/{s/^committer: //;p;}"`"
  timestamp="`echo "$logentry" | sed -n -e "/^timestamp:/{s/^timestamp: //;p;}"`"
  export GIT_AUTHOR_DATE="$timestamp"
  msg=`echo "$logentry" | sed -e "1,/^message:/d"`
  committer_name=`echo "$committer" | sed -e "s/ *<[^<]*\$//;"`
  committer_email=`echo "$committer" | sed -e "s/.*<//;s/>//;"`

  if [ "$committer_email" == "tim@todoroo.com" -o \
       "$committer_email" == "dev.astrid" -o \
       "$committer_email" == "timsu@global" -o \
       "$committer_email" == "timsu@ayumi" -o \
       "$committer_email" == "timsu@betaful.com" ]; then
     committer_name="Tim Su"
     committer_email="tim@todoroo.com"
     echo ts hack
  fi

  git config user.name "$committer_name"
  git config user.email "$committer_email"
  ls -a1 | while read x; do
    if [ "$x" != ".bzr" ] && [ "$x" != "." ] && [ "$x" != ".." ]; then git add "$x"; fi
  done
  git commit -a -m "$msg" 
# --author="$committer"
  let rev+=1
done

[ "$keepbzr" != "yes" ] && rm -r .bzr
