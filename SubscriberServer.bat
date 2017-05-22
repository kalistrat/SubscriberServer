@ECHO OFF
if exist { "%JAVA_HOME%\bin\java" } (
    set "JAVA="%JAVA_HOME%\bin\java""
)
java -jar SubscriberServer-1.0.jar -Xmx256m
pause