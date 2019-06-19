SRC_DIR= src
BIN_DIR= bin
LIBS=lib/mysql-connector-java-5.1.12-bin.jar:lib/jrdf-0.5.6.1.jar:lib/commons-lang-2.4.jar:lib/ezmorph-1.0.jar:lib/json-lib-0.9.jar:lib/commons-beanutils-1.7.0.jar:lib/commons-logging-1.1.jar:lib/junit-3.8.1.jar:lib/log4j-1.2.13.jar:lib/oro-2.0.8.jar:lib/xmlunit-1.0.jar:lib/xom-1.1.jar

all: compile

# To compile/run in an IDE you will need to add the following to the classpath
# lib/jrdf-0.5.6.jar
# lib/mysql-connector-java-5.1.12-bin.jar
# lib/commons-lang-2.4.jar
# lib/ezmorph-1.0.jar
# lib/json-lib-0.9.jar
# lib/commons-beanutils-1.7.0.jar
# lib/commons-logging-1.1.jar
# lib/junit-3.8.1.jar
# lib/log4j-1.2.13.jar
# lib/oro-2.0.8.jar
# lib/xmlunit-1.0.jar 
# lib/xom-1.1.jar
# 
# To resolve errors in the testcase package (Not nessecary for running or compiling the bot)
# you will need:
# JUnit: junit.jar 
# Java version 6 or greater

compile:
	javac -cp $(LIBS) -sourcepath $(SRC_DIR) -d $(BIN_DIR) $(SRC_DIR)/Bot.java $(SRC_DIR)/org/jrdf/parser/rdfxml/RdfXmlParser.java

clean:
	rm -rf $(BIN_DIR)
	mkdir $(BIN_DIR)
	
run:
	java -cp bin:$(LIBS) Bot

debug:
	java -cp bin:$(LIBS) -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=33867 Bot
