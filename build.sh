#!/bin/bash
set -e

APP_NAME="encore"
JAR_SOURCE="build/tasks/_${APP_NAME}_executableJarJvm/${APP_NAME}-jvm-executable.jar"
JAR_TARGET="deploy/${APP_NAME}.jar"

echo "Building server JAR..."
./amper package --format=executable-jar

mkdir -p deploy

if command -v rsync >/dev/null 2>&1; then
    rsync -a "$JAR_SOURCE" "$JAR_TARGET"
    rsync -a assets deploy/
    rsync -a backstage deploy/
else
    cp "$JAR_SOURCE" "$JAR_TARGET"
    cp -r assets deploy/
    cp -r backstage deploy/
fi

echo
echo "Build success."

# shellcheck disable=SC2162
read -p "Build documentation? (y/n): " BUILDDOCS

if [[ "$BUILDDOCS" == "y" || "$BUILDDOCS" == "Y" ]]; then
  echo
  echo " Building documentation..."
  
  cd docs

  if [[ -f "package.json" ]]; then
    echo "Installing dependencies..."
    npm install
  fi

  echo "Running build..."
  npm run build

  cd ..

  echo
  echo " Moving built docs to deploy/docs/..."

  rm -rf deploy/docs
  mkdir -p deploy/docs

  if command -v rsync &> /dev/null; then
    rsync -a docs/dist/ deploy/docs/
  else
    cp -r docs/dist/* deploy/docs/
  fi

  echo "Documentation successfully moved to deploy/docs/"
else
  echo "Skipping documentation build."
fi

echo
echo " Build finished successfully!"
# shellcheck disable=SC2162
read -p "Press Enter to exit..."
