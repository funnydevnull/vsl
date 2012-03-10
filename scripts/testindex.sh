#!/bin/sh

if [ -z "$VSLBASE" ];
then
	echo "Use 'source setenv.sh' before calling this script."
	exit 1
fi

CMD=$1

case $CMD in
create)
	java -cp $CLASSPATH vsl.test.TestCoreIndex create $VSLBASE/config/vsl.cfg $2 
	;;
add)
	java -cp $CLASSPATH vsl.test.TestCoreIndex add $VSLBASE/config/vsl.cfg $2 $3
	;;
show)
	# arg 2 should be an entry id
	java -cp $CLASSPATH vsl.test.TestCoreIndex show $VSLBASE/config/vsl.cfg $2 
	;;
mod)
	# arg $2 should be an index key --  will now be prompted for other data
	# arg 2 should be an entry id, arg 3 a version id
	java -cp $CLASSPATH vsl.test.TestCoreIndex mod $VSLBASE/config/vsl.cfg $2 
	;;
oldupdate)
	# arg 2 should be an entry id, arg 3 a version id
	java -cp $CLASSPATH vsl.test.TestCoreIndex update $VSLBASE/config/vsl.cfg $2 $3 $4 $5
	;;
print)
	java -cp $CLASSPATH vsl.test.TestCoreIndex printMap $VSLBASE/test/db_test/vsl.db
	;;
help)
	java -cp $CLASSPATH vsl.test.TestCoreIndex help
	;;
*)
	echo "This script must be called with: store, show, update, print, help."
	;;
esac
