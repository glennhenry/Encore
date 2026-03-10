#!/bin/bash
set -e

echo " Building server JAR..."
./gradlew shadowJar

echo "Build finished"
echo

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

  mkdir -p deploy
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
