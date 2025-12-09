# Simple DSL Example with REST API

This example demonstrates how to create a simple DSL using Langium and expose the parser as a REST API service that can be consumed by external applications.

## Project Structure

- `src/language-server/` - Contains the Langium grammar and generated parser
- `src/rest-api/` - Contains the Express.js REST API server
- `kotlin-client/` - Contains a Kotlin/Gradle project that tests the DSL parser

## DSL Grammar

The Simple DSL is a basic greeting language with the following syntax:

```
Hello <name> [from <location>]!
```

Examples:
- `Hello World!`
- `Hello World from Earth!`

## REST API

The REST API exposes the following endpoints:

### `GET /health`
Health check endpoint that returns the status of the service.

### `POST /parse`
Parses DSL content and returns the AST as JSON.

**Request Body:**
```json
{
  "content": "Hello World!"
}
```

**Response:**
```json
{
  "success": true,
  "ast": {
    "name": "World"
  }
}
```

## Building and Running

### TypeScript REST API

```bash
# Install dependencies
npm install

# Generate Langium parser
npm run langium:generate

# Build the project
npm run build

# Start the REST API server
npm start
```

The server will start on port 3000.

### Kotlin Client

The Kotlin client project demonstrates how to:
1. Build the DSL parser using Gradle
2. Start the REST API server before running tests
3. Call the REST API from JUnit tests
4. Stop the server after tests complete

```bash
cd kotlin-client

# Run all tests (this will build the DSL parser, start the server, run tests, and stop the server)
./gradlew test

# Clean build
./gradlew clean test
```

## Gradle Integration

The Gradle build file (`kotlin-client/build.gradle.kts`) includes custom tasks:

- `npmInstall` - Installs npm dependencies
- `buildDslParser` - Builds the TypeScript DSL parser
- `startDslParser` - Starts the REST API server
- `stopDslParser` - Stops the REST API server
- `test` - Runs JUnit tests (depends on startDslParser, finalized by stopDslParser)

## Testing

The Kotlin client includes JUnit 5 tests that verify:
- Health check endpoint
- Parsing simple greetings
- Parsing greetings with location
- Error handling for invalid DSL content

All tests make HTTP requests to the REST API using OkHttp and parse JSON responses using Gson.
