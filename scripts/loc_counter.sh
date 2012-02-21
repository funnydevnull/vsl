#!/bin/sh

#
# Count java LOC
#

if [ -z "$VSLBASE" ];
then
	echo "Use 'source setenv.sh' before calling this script."
	exit 1
fi


cd $VSLBASE/src
LOC=`cat \`find *|grep java\`|grep -v '[^[:space:]]*[/\*]'|grep  '[^[:space:]]'|wc|awk '{print $1}'`

echo "$LOC lines of code (not counting comments and space)."
