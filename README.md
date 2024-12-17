# Notion OpenAPI Converter

A utility to convert OpenAPI YAML definitions into organized Notion pages using the Notion API.

## **Overview**
This tool reads OpenAPI definitions, processes endpoints, schemas, and authentication details, and generates structured pages in **Notion**.

---

## **Requirements**
1. **Java 11+**  
   Ensure you have Java Development Kit (JDK) version 11 or higher installed.

2. **Notion Integration**
    - Create an integration via [Notion Integrations](https://www.notion.so/my-integrations).
    - Obtain the `NOTION_TOKEN` (API token for Notion).
    - Share the **target Notion page** with the integration for editing access.

3. **Configuration File**: A YAML file that maps OpenAPI files to Notion pages (explained below).

---

## **Setup Instructions**

### 1. Create the Configuration File (`config.yaml`)
Create a `config.yaml` file with the following structure:

```yaml
pages:
  - notionPageId: "<NOTION_PAGE_ID>"  # The ID of the Notion page
    apiFolder: "path/to/api-docs"     # Folder containing OpenAPI YAML files

# Optional: Generate a unified collection file
generateCollection: "path/to/collection.yaml"
```
- **`notionPageId`**: The Notion page ID where the content will be created.
- **`apiFolder`**: Path to the folder containing OpenAPI definitions.
- **`generateCollection`** (optional): Combines multiple OpenAPI files into one collection.

---

### 2. Build the Application

Run the following command to build the project:
```bash
./gradlew shadowJar
```
This generates a runnable JAR file at `build/libs/app.jar`.

---

### 3. Set Environment Variables

Set the Notion API token:
```bash
export NOTION_TOKEN=<your_notion_token>
```

---

### 4. Run the Application

Use the following syntax to run the app:
```bash
java -jar build/libs/app.jar [OPTIONS]
```

#### **Available Arguments**:
| Argument               | Description                              | Example                                |
|------------------------|------------------------------------------|----------------------------------------|
| `--routes-category`    | Filter endpoints by tags (categories)    | `--routes-category=PAYMENTS,GENERAL..` |
| `--field-category`     | Filter fields by custom x-category       | `--field-category=PII..`               |

#### **Example Run Command**:
```bash
java -jar build/libs/app.jar --routes-category=PAYMENTS,GENERAL,Legal --field-category=PII
```

## **Running Example**
1. Ensure `config.yaml` is set up.
2. Export the Notion token:
   ```bash
   export NOTION_TOKEN=secret_your_notion_token_here
   ```
3. Run the app:
   ```bash
   java -jar build/libs/app.jar --routes-category=PII --field-category=Public
   ```

---

## **Expected Result**
- Structured Notion pages will be created/updated based on the OpenAPI files.
- Pages will include endpoints, schemas, parameters, and authentication details.

---

## **Troubleshooting**
- **Missing Token**: Ensure `NOTION_TOKEN` is set correctly.
- **Config Errors**: Verify `config.yaml` follows the correct structure.
- **Invalid OpenAPI Files**: Validate your YAML files using [Swagger Editor](https://editor.swagger.io/).

---
