#!/bin/bash

# הדפס הודעת התחלה
echo "Running Notion update process..."

# הגדר את ה-Token של Notion
export NOTION_TOKEN="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"

# בדוק אם הקובץ קיים
if [ ! -f "build/libs/app.jar" ]; then
  echo "Error: build/libs/app.jar not found!"
  exit 1
fi

# הרץ את התוכנית בקוטלין
java -jar build/libs/app.jar \
  --routes-category="" \
  --field-category=""

# הודעת סיום
echo "Notion update completed!"
