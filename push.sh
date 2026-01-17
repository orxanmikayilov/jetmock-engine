#!/bin/bash

USER="orxanmikayilov"
IMAGE="jetmock-engine"
TAG="1.0.0"

# 1. Jar build
echo "🚀 JAR faylı yaradılır..."
./gradlew clean bootJar || { echo "❌ Build xətası!"; exit 1; }

# 2. Docker build (AMD64 platforması üçün - hamı işlədə bilsin deyə)
echo "📦 Docker image build olunur (linux/amd64)..."
docker build --platform linux/amd64 -t $IMAGE:latest . || { echo "❌ Docker build xətası!"; exit 1; }

# 3. Tag təyin olunur
echo "🏷️ Tag təyin olunur..."
docker tag $IMAGE:latest $USER/$IMAGE:$TAG
docker tag $IMAGE:latest $USER/$IMAGE:latest

# 4. Push
echo "☁️ Docker Hub-a push olunur..."
docker push $USER/$IMAGE:$TAG
docker push $USER/$IMAGE:latest

echo "✅ Proses uğurla başa çatdı!"