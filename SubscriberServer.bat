@ECHO OFF
if exist { "%JAVA_HOME%\bin\java" } (
    set "JAVA="%JAVA_HOME%\bin\java""
)
java -Xmx1024m -jar SubscriberServer-1.0.jar
pause