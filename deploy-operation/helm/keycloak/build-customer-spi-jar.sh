#!/bin/bash

# Configuration
SPRING_PROJECT_DIR=$1
# Adjust path to your Spring Boot project
HELM_FILES_DIR="./keycloak/files"   # Path to Helm chart files directory

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
print_status "Checking prerequisites..."

if ! command_exists mvn && ! command_exists ./mvnw; then
    print_error "Gradle or Gradle wrapper not found. Please install Gradle or ensure mvnw is present."
    exit 1
fi

# Check if Spring Boot project directory exists
if [ ! -d "$SPRING_PROJECT_DIR" ]; then
    print_error "Spring Boot project directory not found: $SPRING_PROJECT_DIR"
    exit 1
fi

# Create files directory if it doesn't exist
mkdir -p "$HELM_FILES_DIR"

print_status "Starting build process..."

# Step 1: Build Spring Boot project
print_status "Building Spring Boot project..."
cd "$SPRING_PROJECT_DIR"

# Clean and build the project

print_status "Using system Gradle..."
./gradlew clean build --console=plain --no-daemon


# Check if build was successful
if [ $? -ne 0 ]; then
    print_error "Gradle build failed!"
    exit 1
fi

# Find the JAR file in target directory
JAR_FILE=$(find build/libs -name "*-APPOINTMENT-BOOKING-UNSET-VERSION.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -1)

if [ -z "$JAR_FILE" ]; then
    print_error "No JAR file found in target directory!"
    exit 1
fi

print_status "Found JAR file: $JAR_FILE"

# Step 2: Copy JAR file to Helm chart
print_status "Copying JAR file to Helm chart..."
cd - > /dev/null  # Go back to original directory

# Copy the JAR file
cp "$SPRING_PROJECT_DIR/$JAR_FILE" "$HELM_FILES_DIR/"

if [ $? -ne 0 ]; then
    print_error "Failed to copy JAR file to Helm chart!"
    exit 1
fi

# Get just the filename for the JAR
JAR_FILENAME=$(basename "$JAR_FILE")
print_status "JAR file copied to: $HELM_FILES_DIR/$JAR_FILENAME"

print_status "Build and copy completed successfully!"
print_status "JAR file location: $HELM_FILES_DIR/$JAR_FILENAME"
print_status "You can now run: helm upgrade keycloak-idp ./keycloak"