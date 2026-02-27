# Change me

Kotlin Ktor game server template with ready to write docs website.

```
Server emulator for X, developed by X, closed on X.

*This project tries to reconstruct the server-side behavior. Client files and assets are not included; you must supply your own data. This project is not official and is not associated with the original creators.*
```

## Editlist

Server files to edit:
- ...

Docs files to edit:
- `docs/package.json` + `npm install`
- `docs/astro.config.mjs`
- `docs/src/components/Giscus.astro`
- `docs/src/content/docs/intro.md`
- `docs/src/content/docs/flow.md`
- `docs/src/assets/` (for image in `intro.md`)
- `docs/public/favicon.ico` (for docs' favicon)

# Server Manual

## Requirements

- **Java 25+**
- **MongoDB v8.0+**
- **Node v18.20.8 or v20.3.0, v22.0.0+** (only for docs)

## Setup

To run the server, ensure MongoDB is running on (default `mongodb://localhost:27017`). Then, run the following command:

```bash
.\gradlew run
```

- File and API server runs on `127.0.0.1:8080`
- Socket server runs on `127.0.0.1:7777`

You can also run the server from IntelliJ IDE run plugin.

## Build

To build the server, simply run the `build.bat/sh` script. Output will be in `deploy/`. Run the deployment server using `java -jar changeme.jar`.

For manual build:

```bash
.\gradlew shadowJar
```

Optionally, you can build the documentation website:

```bash
cd docs
npm install
npm run build
```

Then, move the `dist` directory to `deploy/`.

Server will be available on the same port as development mode. The documentation website, if built, will be available on `127.0.0.1:8080/docs`.

## Configuration

Various server settings can be set from `src/main/resources/application.yaml`.

Some configuration can be set via environment variables, those typically have `$` within the config.

For example, in PowerShell (Windows):

```ps1
$env:DEV_MODE = "false"
$env:ADMIN = "true"
java -jar changeme.jar
```

## Docs

Empty documentation template ([built with Starlight](https://starlight.astro.build/), based on [sl-obsidian-starter](https://github.com/glennhenry/sl-obsidian-starter)) is available on `docs/`

To run the website locally:

```bash
cd docs
npm install
npm run dev
```

Docs running on `http://localhost:4321/docs`

For more info on setup and configuration, please see
the [official Starlight documentation](https://starlight.astro.build/getting-started/).

### How to add new page:

1. A page must be `.md` file and is enforced to have this on top of them (frontmatter):

```
---
title: Subfolder Example
slug: folderA/folderB/example
description: example
---
```

2. Replace the title appropriately. The description is optional; you can set it to be the same as the title. Any images
   or videos should be placed in `src/assets/`.
3. The slug is produced from the directory structure. For instance, this page is named `example.md` and is under the
   `folderB` within the `folderA`.
4. Next, add the page to the sidebar.
   1. Begin by editing the `astro.config.mjs`.
   2. Follow the existing sidebar link
      format. [More details on official documentation](https://starlight.astro.build/guides/sidebar/).

## DevTools

An external web-based developer toolkit that provides a user interface for monitoring and interacting with the server.

See `DevTools.md` for details.

## Structure

<details>
<summary>Open</summary>

```
.
├── src/main/kotlin/
│   ├── annotation/             # Custom annotations
│   ├── api/                    # REST API endpoints
│   │   ├── models/             # API request/response models
│   │   └── routes/             # API routes
│   ├── context/                # States model (server, player) and tracker
│   ├── core/                   # Core game logic (domain repository and service)
│   │   ├── data/               # Global game data, game definitions, and parser
│   │   └── model/              # Game data models
│   ├── data/                   # Database implementation
│   │   └── collection/         # Database collection models
│   ├── devtools/               # Developer toolkits
│   │   └── command/            # Server command system
│   │       ├── core/           # Core implementation
│   │       └── impl/           # Command implementation
│   ├── security                # Security functionality
│   │   └── validation/         # Validation system
│   ├── server/                 # Game servers and implementation
│   │   ├── core/               # Core server components
│   │   ├── handler/            # Message handlers
│   │   ├── messaging/          # Server messaging components
│   │   │   ├── codec/          # Message codec components
│   │   │   └── format/         # Message format definitions
│   │   └── tasks/              # Server task system
│   │       └── impl/           # Server task implementation
│   ├── user/                   # User management
│   │   ├── auth/               # Authentication and session system
│   │   └── model/              # User models
│   ├── utils/                  # Utility package
│   │   ├── constants/          # Global constants
│   │   ├── functions/          # Utility functions
│   │   └── logging/            # Logger system
│   └── Application.kt          # Application entry point
├── src/main/resources/
│   ├── application.yaml        # Server configuration
│   └── logback.xml             # Logging configuration (not much used)
├── src/main/test/              # Code tests
├── static/                     # Game assets
├── docs/                       # Documentation
│   └── src/content/docs/       # Markdown documentation
├── deploy/                     # Build output
├── .logs/                      # Logs file
├── .telemetry/                 # Telemetry file
└── dev.bat/dev.sh              # Script to run development server
└── build.bat/build.sh          # Script to build the server
└── autorun.bat/autorun.sh      # Script to run deployment server (in deploy)
```

</details>

### TODO

- dev tools (from route /devtools)
  - basic authentication with ephemeral token; token refresh from local command line
  - advanced logger display
  - server monitoring
  - ability to execute command from API
- rate limiter, flood anticipator

# DevTools

There are console, monitor, and command tool available.

## Console

TBA

## Monitor

TBA

## Commands

### Implementation

All command implementations are located under the package `src/main/kotlin/devtools/cmd`.

In short, dev can create command by implementing the `Command<T>` interface. They have to define `commandId`, description, required arguments, and the execution logic inside the `execute` method. They should also register the command implementation with `CommandDispatcher.register` from `ServerContext` (typically in `Application.kt`).

Consult the code directly for implementation details. Help and usage details are available in the DevTools itself.
