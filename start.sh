#!/bin/bash

# AWAD Email Backend - Quick Start Script

echo "=========================================="
echo "AWAD Email Backend - Quick Start"
echo "=========================================="
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 21 or higher."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Java version must be 21 or higher. Current version: $JAVA_VERSION"
    exit 1
fi

echo "✅ Java version: $(java -version 2>&1 | head -n 1)"
echo ""

# Check if MongoDB is running
if ! command -v mongosh &> /dev/null && ! command -v mongo &> /dev/null; then
    echo "⚠️  MongoDB client not found. Make sure MongoDB is running on localhost:27017"
else
    if mongosh --eval "db.version()" > /dev/null 2>&1 || mongo --eval "db.version()" > /dev/null 2>&1; then
        echo "✅ MongoDB is running"
    else
        echo "❌ MongoDB is not running. Please start MongoDB:"
        echo "   brew services start mongodb-community"
        echo "   OR"
        echo "   docker run -d -p 27017:27017 --name mongodb mongo:latest"
        exit 1
    fi
fi
echo ""

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo "⚠️  .env file not found. Creating from .env.example..."
    cp .env.example .env
    echo "✅ Created .env file"
    echo ""
    echo "⚠️  IMPORTANT: Please update the following in .env file:"
    echo "   - JWT_SECRET (use a strong random key - at least 32 characters)"
    echo "   - GOOGLE_CLIENT_ID (if using Google OAuth)"
    echo "   - MONGODB_URI (if not using localhost)"
    echo ""
    echo "💡 Generate a secure JWT secret with:"
    echo "   openssl rand -base64 32"
    echo ""
    read -p "Press Enter to continue or Ctrl+C to exit and configure..."
fi

# Build the project
echo "📦 Building the project..."
./mvnw clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the errors above."
    exit 1
fi

echo "✅ Build successful"
echo ""

# Run the application
echo "🚀 Starting the application..."
echo "   API will be available at: http://localhost:8080"
echo "   Health check: http://localhost:8080/api/health"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

./mvnw spring-boot:run

