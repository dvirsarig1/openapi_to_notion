#!/bin/bash

echo "Running Notion update process..."

export NOTION_TOKEN=$(gcloud secrets versions access latest --secret=notion-token)

if [ ! -f "build/libs/app.jar" ]; then
  echo "Error: build/libs/app.jar not found!"
  exit 1
fi

java -jar build/libs/app.jar \
  --field-category="PII"

echo "Notion update completed!"
