#!/bin/sh

if [ -z "$VSLBASE" ];
then
	echo "Use 'source setenv.sh' before calling this script."
	exit 1
fi

CMD=$1

case $CMD in
store)
	java -cp $CLASSPATH vsl.test.TestCore1 store $VSLBASE/config/vsl.cfg $2 $3
	;;
show)
	# arg 2 should be an entry id
	java -cp $CLASSPATH vsl.test.TestCore1 show $VSLBASE/config/vsl.cfg $2 
	;;
update)
	# arg 2 should be an entry id, arg 3 a version id
	java -cp $CLASSPATH vsl.test.TestCore1 update $VSLBASE/config/vsl.cfg $2 $3 $4 $5
	;;
print)
	java -cp $CLASSPATH vsl.test.TestCore1 printMap $VSLBASE/test/db_test/vsl.db
	;;
help)
	java -cp $CLASSPATH vsl.test.TestCore1 help
	;;
*)
	echo "This script must be called with: store, show, update, print, help."
	;;
esac
