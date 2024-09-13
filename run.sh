#!/bin/bash

MONGO_CONTAINER_NAME="mongo"
MONGO_PORT="27017"

echo "Starting MongoDB with Docker Compose..."
docker run --name $MONGO_CONTAINER_NAME -d -p $MONGO_PORT:$MONGO_PORT -v mongo-data:/data/db mongo:8.0.0-rc11

echo "Building the application..."
mvn clean package -DskipTests

echo "Running the application..."
java -jar target/advertisement-0.0.1-SNAPSHOT.jar