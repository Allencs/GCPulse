#!/bin/bash

echo "========================================="
echo "Starting GCPulse Backend..."
echo "========================================="

cd backend

export JAVA_HOME=$JAVA_21_HOME
export PATH=$JAVA_HOME/bin:$PATH

# 检查Java版本
java -version

echo ""
echo "Compiling and starting Spring Boot application..."
echo ""

mvn clean compile spring-boot:run

