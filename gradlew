#!/bin/sh

# Resolve the project directory
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)

# Default to JDK 21 if JAVA_HOME is not already set
if [ -z "$JAVA_HOME" ] && [ -d "/usr/lib/jvm/java-21-temurin" ]; then
    JAVA_HOME="/usr/lib/jvm/java-21-temurin"
fi

if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVACMD" -Xmx64m -Xms64m -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
