#!/bin/bash

# הדפס הודעת התחלה
echo "Running Notion update process..."

# הגדר את ה-Token של Notion
export NOTION_TOKEN="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"


# הרץ את התוכנית בקוטלין
java -jar your-tool.jar \
  --routes-category="" \
  --field-category=""

# הודעת סיום
echo "Notion update completed!"
