@echo off
@title Dump
set CLASSPATH=.;dist\Lithium.jar;dist\mina-core.jar;dist\slf4j-api.jar;dist\slf4j-jdk14.jar;dist\mysql-connector-java-bin.jar
java -server -Dnet.sf.odinms.wzpath=D:\XMLWZs\GMSv111\ tools.wztosql.DumpItems
pause