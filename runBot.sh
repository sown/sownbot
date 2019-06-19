#!/bin/bash
if [ $# -ne 1 ]; then
	echo "usage $0 pidfile"
	exit
fi

if [ ! `ps -p $PPID -o comm=` = "sownbot" ]; then
	echo "This script should be invoked from an init.d script."
	echo "Try something like: invoke-rc.d sownbot start|stop|restart"
	exit
fi

nohup java -cp bin:lib/mysql-connector-java-5.1.12-bin.jar:lib/jrdf-0.5.6.1.jar:lib/commons-lang-2.4.jar:lib/ezmorph-1.0.jar:lib/json-lib-0.9.jar:lib/commons-beanutils-1.7.0.jar:lib/commons-logging-1.1.jar:lib/junit-3.8.1.jar:lib/log4j-1.2.13.jar:lib/oro-2.0.8.jar:lib/xmlunit-1.0.jar:lib/xom-1.1.jar -Djava.net.preferIPv4Stack=true Bot >> sownbot.log 2>> sownbot.err &
echo $! > $1
