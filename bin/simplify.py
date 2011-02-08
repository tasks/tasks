#**********************************************************************************************
# 
# simplify.py - switch traditional Chinese text to simplified Chinese
#
# Author: Yue Zhang
# 
#**********************************************************************************************

import sys

g_sHint = "simplify.py \
Convert text from traditional Chinese to Simplified Chinese. \
Yue Zhang \
frcchang@gmail.com\
\
Usage - simplify.py traditional.txt >simplify.txt\
\
The input file must be encoded in UTF-8. \
You also need to put the char table utftable.txt in the same directory. \
\
"

if len(sys.argv) != 2:
   print g_sHint
   sys.exit(1)

table = {}
file = open("utftable.txt")
line = file.readline()
while line:
   line = line.strip()
   line = unicode(line, "utf-8")
   assert len(line)==3 and line[1]=="="
   table[line[2]] = line[0]
   line = file.readline()
file.close()

file = open(sys.argv[1])
line = file.readline()
while line:
   line = line[:-1]
   line = unicode(line, "utf-8")
   output = [table.get(char, char) for char in line]
   print "".join(output).encode("utf-8")
   line = file.readline()
file.close()
