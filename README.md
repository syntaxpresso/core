<div align="center">
  <img width="500" alt="syntaxpresso" src="https://github.com/user-attachments/assets/be0749b2-1e53-469c-8d99-012024622ade" />
</div>

<div align="center">
  <img alt="rust" src="https://img.shields.io/badge/built%20with-Rust-orange?logo=rust" />
  <img alt="CI Status" src="https://img.shields.io/github/actions/workflow/status/syntaxpresso/core/develop.yml?branch=develop&label=CI&logo=github">
  <img alt="GitHub Downloads (all assets, latest release)" src="https://img.shields.io/github/downloads/syntaxpresso/core/latest/total">
</div>

# Syntaxpresso Core

A high-performance Rust-based CLI backend for IDE plugins that provides advanced code generation and manipulation capabilities using Tree-Sitter for precise AST manipulation.

It is a stateless, language-agnostic AST manipulation engine built on Tree-Sitter and developed in Rust to ensure high performance in syntax processing.

Unlike traditional tools that treat code as plain text, Syntaxpresso Core understands code as a logical structure (tree). Thanks to Tree-Sitter, this logical understanding can be extended to any programming language (Java, Python, TypeScript, etc.) simply by swapping the grammar.

The primary goal is to serve as a universal backend for IDE plugins and programmatic code automation. Syntaxpresso Core centralizes complex refactoring logic into a single portable binary, eliminating the need to reimplement business rules for each text editor.

> **Developer-Focused Documentation**: This README contains basic technical architecture, implementation details, and integration guides for developers building IDE plugins or extending Core's functionalities. If you want to use the plugin on your IDE, refer to these repositories:

- [Neovim](https://github.com/syntaxpresso/syntaxpresso.nvim)

## Core Capabilities

- **AST-Based Code Manipulation**: Uses Tree-Sitter for precise, syntax-aware code parsing and manipulation
- **Incremental Parsing**: Leverages Tree-Sitter's incremental parsing for fast, efficient code updates
- **Dual Interface**: Supports both programmatic CLI usage (for IDE integration) and interactive TUI (for standalone use)

## Core Technologies

- **Tree-Sitter**: Powers the AST parsing and manipulation engine
  - Incremental parsing for fast updates
  - Precise byte-level node positioning
  - Error-resilient parsing (can work with incomplete code)
- **Rust**: Ensures memory safety and high performance
- **Clap**: Provides type-safe CLI argument parsing
- **Serde**: Handles JSON serialization/deserialization

## Design Philosophy

- **AST-First Approach**: All code modifications are performed through Tree-Sitter AST manipulation, not string concatenation or regex
- **Incremental Updates**: Uses Tree-Sitter's incremental parsing to efficiently update only affected portions of the syntax tree
- **Path Containment**: All file operations validate that paths stay within the working directory using canonicalization and containment checks
- **Type Safety**: Strongly-typed commands and responses prevent runtime errors
- **Separation of Concerns**: Clear layering between CLI, commands, services, and Tree-Sitter operations

# Quick Reference

- [Architecture](#architecture) - Understand the design and data flow
- [Communication Model](#communication-model-stateless-request-response) - How Core interacts with clients
- [Core Components](#core-components) - TSFile, Query Builder, DirectoryValidator, Commands Router
- [Binary Variants](#binary-variants) - CLI-only vs UI-enabled binaries
- [Features & Capabilities](#features--capabilities) - Available commands and language support
- [Installation](#installation-for-developers) - Download or build from source
- [Plugin Integration Guide](#plugin-integration-guide) - Integrate with Neovim, VSCode, etc.
- [Running Tests](#running-tests) - Test the codebase
- [Performance Benchmarks](#performance-benchmarks) - Typical operation timings
- [Contributing](#contributing) - Guidelines and process

# Architecture

## Overview (big picture)

Syntaxpresso follows a **client-backend architecture** with strict separation of concerns:

**Backend** (Core):

- Self-contained binary with complete AST manipulation logic
- Language and IDE-agnostic - unaware of what invokes it
- Accepts standardized CLI arguments, returns structured JSON
- Can run standalone (terminal, CI/CD) or via IDE plugins

**Frontend** (Client):

- Lightweight interface layer (Neovim, VSCode, scripts, etc.)
- Collects user intent and delegates processing to Core
- No business logic - purely handles I/O and presentation

**Communication Model:**

- **Unidirectional**: Client → Core (never Core → Client)
- **Stateless**: One process per request
- **Synchronous**: Request → Process → JSON Response → Exit

<div align="center">
  <img width="600" alt="syntaxpresso-architecture" src="https://private-user-images.githubusercontent.com/32070796/508597349-ddd3cd2d-3f03-4bbf-b855-8fc17248b3c2.png?jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3NjM3NjY1MzcsIm5iZiI6MTc2Mzc2NjIzNywicGF0aCI6Ii8zMjA3MDc5Ni81MDg1OTczNDktZGRkM2NkMmQtM2YwMy00YmJmLWI4NTUtOGZjMTcyNDhiM2MyLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNTExMjElMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjUxMTIxVDIzMDM1N1omWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWNhMzQwZTRlOWQwZDliYmRhMGM5NDMxNzY3YjI3ZjA0NDNjNmFhMmI5NWZmYmIyMTNmNzEwYTBmYTU0MTM3OTgmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0In0.d1-NEqBU8uHMvDr1fF8qhcJYQOf4b7_E3ois2XCAGVY" />
</div>

## Project's Structure

```
src/
├── commands/                          # Top-level command routing
│   ├── mod.rs                         # Commands enum (language router)
│   └── java/                          # Java-specific commands
│       ├── mod.rs
│       ├── commands.rs                # JavaCommands enum (CLI + UI)
│       ├── *_command.rs               # Command executors (14 files)
│       ├── services/                  # Business logic services
│       │   ├── create_java_file_service.rs
│       │   ├── create_jpa_entity_service.rs
│       │   └── ... (15 files total)
│       ├── validators/                # Java-specific input validation
│       │   ├── java_class_name_validator.rs
│       │   ├── package_name_validator.rs
│       │   └── mod.rs
│       ├── responses/                 # Java-specific response types
│       │   ├── file_response.rs
│       │   ├── package_response.rs
│       │   └── ... (10 files total)
│       ├── treesitter/                # Java AST manipulation
│       │   ├── mod.rs
│       │   ├── types/                 # Java type definitions
│       │   │   ├── java_basic_types.rs
│       │   │   ├── java_field_modifier.rs
│       │   │   └── ... (20+ type files)
│       │   └── services/              # Tree-Sitter AST services
│       │       ├── annotation_service.rs
│       │       ├── class_declaration_service.rs
│       │       └── ... (10+ service files)
│       └── ui/                        # Java UI forms (feature: ui)
│           ├── create_java_file.rs
│           ├── create_jpa_entity.rs
│           └── ... (10 files total)
├── common/                            # Language-agnostic utilities
│   ├── response.rs                    # Generic Response<T>
│   ├── error_response.rs
│   ├── query.rs
│   ├── ts_file.rs                     # Core Tree-Sitter abstraction
│   ├── utils/                         # Generic utilities
│   │   ├── case_util.rs
│   │   ├── path_security_util.rs
│   │   └── path_util.rs
│   ├── validators/                    # Generic validators
│   │   ├── directory_validator.rs
│   │   └── mod.rs
│   └── ui/                            # Generic UI components (feature: ui)
│       ├── form_trait.rs
│       ├── runner.rs
│       └── widgets.rs
├── lib.rs                             # Library entry point
└── main.rs                            # CLI entry point

tests/                                 # Integration tests
```

The structure is designed to **isolate development**, reduce the learning curve, and ensure safe maintenance by preventing cross-language business logic contamination.

**The Global Router** (`src/commands/mod.rs`):

- Acts as a high-level dispatcher - doesn't execute commands, only routes them
- Flow: "Java request? → Route to `java/`. Python request? → Route to `python/`."

**Language Modules** (`src/commands/java/`, `src/commands/python/`, etc.):

- Each directory functions as a **bounded context** (DDD pattern)
- Complete encapsulation: language-specific validators, services, and Tree-Sitter types live in isolation
- Benefit: A bug in the Python module cannot affect Java - they share no memory or language-specific logic

**Shared infrastructure** (`src/common/`):

- Base infrastructure used by all language modules
- Reusable capabilities: file I/O, incremental parsing, JSON responses, path validation
- Eliminates duplicate code across language implementations

## The execution flow and the responsability of each layer

```
┌─────────────────────────────────────────────────────────────┐
│ Frontend (IDE Plugin / Script / Terminal)                   │
│  - Spawns binary with arguments                             │
│  - Captures JSON from stdout                                │
│  - Parses and consumes response                             │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ Clap CLI Parser (main.rs)                                   │
│  - Validates arguments                                      │
│  - Routes to language subcommand                            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │ Commands (mod.rs)    │
              │  - Language router   │
              └──────────┬───────────┘
                         │
                         ▼
              ┌──────────────────────────────────┐
              │ JavaCommands (java/commands.rs)  │
              │  - Java-specific routing         │
              └──────────┬───────────────────────┘
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
┌──────────────────────┐  ┌──────────────────────┐
│ Interactive UI Path  │  │ Programmatic Path    │
│ (#[cfg(feature="ui)])│  │ (Default)            │
│                      │  │                      │
│ - TUI forms          │  │ - Direct execution   │
│ - User interaction   │  │ - Calls commands     │
│ - Calls commands     │  │                      │
└──────────┬───────────┘  └──────────┬───────────┘
           │                         │
           └──────────┬──────────────┘
                      ▼
           ┌────────────────────────┐
           │ Command Layer          │
           │  java/*_command.rs     │
           │                        │
           │  - Owns command names  │
           │  - Validates inputs    │
           │  - Calls services      │
           │  - Builds Response<T>  │
           └──────────┬─────────────┘
                      ▼
            ┌────────────────────────┐
            │ Service Layer          │
            │  java/services/        │
            │  java/treesitter/      │
            │      services/         │
            │                        │
            │  - Business logic      │
            │  - Tree-Sitter ops     │
            │  - File I/O            │
            │  - Returns domain objs │
            └──────────┬─────────────┘
                       ▼
            ┌────────────────────────┐
            │ Tree-Sitter Layer      │
            │  common/ts_file.rs     │
            │                        │
            │  - AST parsing         │
            │  - Incremental updates │
            │  - Query execution     │
            │  - Node manipulation   │
            └────────────────────────┘

            ┌────────────────────────┐
            │ Validation Layer       │
            │  common/validators/    │
            │  java/validators/      │
            │                        │
            │  - Generic validators  │
            │  - Language-specific   │
            │    validators          │
            └────────────────────────┘
```

**Entry Layer** (Frontend & Parser):

- **Frontend (Client)**: Acts as the trigger only - doesn't process logic, just invokes the binary with arguments and awaits JSON on stdout. IDE-agnostic (Neovim, VSCode, or shell).
- **Clap CLI Parser** (`main.rs`): The gatekeeper - validates argument syntax before any domain memory is allocated, rejecting invalid commands immediately.

**Routing Layer**:

- **Commands Router** (`commands/mod.rs`): Global dispatcher - identifies language context ("Is this a Java command?") and forwards to the correct module, without knowing which specific command will execute.
- **Language Dispatcher** (`java/commands.rs`): Language-specific router - knows available commands (e.g., `create-entity`, `create-repository`) and directs to the correct executor.

**Dual Interface Layer**:

- **UI Path (TUI)**: When enabled, takes control of stdin to render interactive forms, guiding users in building arguments.
- **Programmatic Path (CLI)**: Direct execution path ("headless"), optimized for speed and automation scripts or third-party interfaces.

**Command Layer** (`java/*_command.rs`):

- Acts as the controller - orchestrates specific operations
- Triggers semantic validators (e.g., "Is the package name valid?")
- Calls necessary services
- Builds standardized `Response<T>` objects

**Service Layer** (`java/services/`, `java/treesitter/services/`):

- The heart of business logic
- Holds domain knowledge (e.g., structure of `@Entity` annotations, how to inject fields into classes)
- Translates command intent ("create a field") into syntax tree operations

**Infrastructure Layer** (`common/ts_file.rs`):

- The only layer permitted to touch code bytes
- Performs incremental parsing
- Executes queries to locate nodes (e.g., find where class begins)
- Applies surgical text edits to AST without corrupting the rest of the file

**Validation Layer** (`common/validators/`, `java/validators/`):

- Ensures integrity and safety
- **Generic**: Prevents path traversal attacks, validates directories
- **Language-Specific**: Enforces naming conventions (e.g., Java class and package naming rules)

# Communication Model (Stateless Request-Response)

**Ephemeral Process Model:**

- **One Process Per Command**: Each execution (e.g., creating an entity) spawns a new OS process, executes the task, and terminates immediately after returning the response
- **Zero Memory Persistence**: No background server or daemon consuming idle RAM - each execution starts with a clean slate, eliminating bugs from previous runs (memory leaks or corrupted state)

**The Protocol: JSON over Standard Streams**

Communication is standardized to ensure language-agnostic integration (the "Universal Backend" principle):

- **Input**: CLI arguments (or stdin for TUI)
- **Output**: JSON emitted to stdout
- **Error Signaling**:
  - Exit Code `0`: Success (JSON contains data)
  - Exit Code `1`: Failure (JSON contains `errorReason`)

**Architecture Benefits:**

- **Reliability**: Process isolation prevents state corruption
- **Simplicity**: No complex IPC or socket communication required
- **Language Agnostic**: Any language that can spawn processes and parse JSON can integrate
- **Resource Efficiency**: No background processes consuming memory when idle

# Core Components

The following components form the foundation of Syntaxpresso's architecture, handling everything from AST manipulation to security validation.

## TSFile (`src/common/ts_file.rs`)

Every operation in Syntaxpresso begins and ends with `TSFile`. It abstracts Tree-Sitter functionality and file system operations into a cohesive API.

**Responsibility:** Maintain the consistency triangle: **Source Code (String) ↔ Syntax Tree (Tree) ↔ Parser (Language)**. When one changes, TSFile ensures the others update instantly.

### Lifecycle

**1. Initialization:**

```rust
// Load from disk (standard for existing files)
let ts_file = TSFile::from_file(Path::new("User.java"), tree_sitter_java::language())?;

// Load from string (essential for unit tests or in-memory generation)
let ts_file = TSFile::from_source_code("public class User {}", tree_sitter_java::language())?;

// Decode from Base64 (critical for CLI arguments - avoids terminal escaping issues)
let ts_file = TSFile::from_base64_source_code(base64_str, tree_sitter_java::language())?;
```

**2. Query:** TSFile abstracts Tree-Sitter's cursor and iterator complexity. Think of it as "database search" for code.

```rust
// Most powerful method: LISP-style query returns exact nodes
let nodes = ts_file.query("(class_declaration name: (identifier) @name)")?;

for node in nodes {
    println!("Found class: {}", ts_file.node_text(&node));
}
```

**3. Mutation:** All modifications use incremental parsing for performance.

```rust
// Replace entire source code (forces complete reparse)
ts_file.update_source_code("public class UpdatedUser {}");

// Replace text by byte range (start and end indices)
ts_file.replace_text_by_range(0, 10, "private");

// Replace content of specific syntax element (most common)
// Automatically calculates start/end bytes from node and applies incremental edit
let class_node = ts_file.query("(class_declaration) @class")?.first().unwrap();
ts_file.replace_text_by_node(&class_node, "public class NewName { }");

// Insert text at position (pushes existing content forward)
// Zero-width replacement: calls apply_incremental_edit(position, position, text)
// Useful for: adding methods to classes, imports at file top, or injecting annotations
ts_file.insert_text(50, "\n    private String name;");
```

**4. Persistence:** Save with path security validation.

```rust
// Validates path stays within base_path before writing
ts_file.save_as(
    Path::new("src/main/java/User.java"),
    Path::new("/project/root") // Base path for security check
)?;
```

## Query Builder (`src/common/query.rs`)

A fluent API for constructing and executing Tree-Sitter queries with powerful result filtering and extraction.

**Objective:** Abstract Tree-Sitter's query complexity into an ergonomic, chainable API that handles common query patterns.

### Core Features

**1. Fluent Query Construction:**

```rust
// Basic query - returns all matched nodes
let nodes = ts_file
    .query("(class_declaration name: (identifier) @name)")
    .nodes()?;

// Query within a specific scope (search only inside a node)
let methods = ts_file
    .query("(method_declaration) @method")
    .within(class_node)
    .nodes()?;

// Return only a specific capture
let class_names = ts_file
    .query("(class_declaration name: (identifier) @name)")
    .returning("name")
    .nodes()?;

// Return multiple specific captures
let nodes = ts_file
    .query("(method_declaration name: (identifier) @name parameters: (formal_parameters) @params)")
    .returning_captures(&["name", "params"])
    .nodes()?;
```

**2. Result Extraction Modes:**

```rust
// Get first node (Option<Node>)
let first_class = ts_file
    .query("(class_declaration) @class")
    .first_node()?;

// Expect exactly one result (fails if 0 or >1)
let single_class = ts_file
    .query("(class_declaration) @class")
    .single_node()?;  // Returns Result<Node, QueryError::NotSingleResult>

// Get all captures with metadata
let result = ts_file
    .query("(method_declaration name: (identifier) @name body: (block) @body)")
    .execute()?;

for capture in result.captures() {
    if let Some(name_node) = capture.get("name") {
        println!("Method: {}", ts_file.node_text(name_node));
    }
}
```

**3. Advanced Filtering:**

```rust
// Filter results with predicate
let public_methods = ts_file
    .query("(method_declaration modifiers: (modifiers) @mods) @method")
    .execute()?
    .filter(|capture| {
        capture.get("mods")
            .map(|n| ts_file.node_text(n).contains("public"))
            .unwrap_or(false)
    });

// Map results to custom types
let method_names: Vec<String> = ts_file
    .query("(method_declaration name: (identifier) @name)")
    .execute()?
    .map(|capture| {
        capture.get("name")
            .map(|n| ts_file.node_text(n).to_string())
            .unwrap_or_default()
    });
```

**4. Error Handling:**

```rust
use crate::common::query::QueryError;

match ts_file.query("(invalid syntax").execute() {
    Err(QueryError::CompilationError(e)) => println!("Query syntax error: {}", e),
    Err(QueryError::NoTree) => println!("File not parsed"),
    Err(QueryError::NotSingleResult { found }) => println!("Expected 1, found {}", found),
    Ok(result) => { /* process */ }
}
```

**Key Benefits:**

- **Type-safe**: Compile-time guarantees for query structure
- **Ergonomic**: Chainable API reduces boilerplate
- **Flexible**: Multiple return modes (nodes, captures, filtered results)
- **Error-resilient**: Clear error types for debugging

## DirectoryValidator (`src/common/validators/`)

Ensures Syntaxpresso only writes to permitted locations.

**Objective:** Prevent commands from overwriting files outside the user-specified working directory (CWD).

**Solution:** Enforces **Path Containment** - all write operations must resolve to a child path of the working directory.

```rust
// Used in command execution to protect file creation
pub fn validate_file_path_within_base(
    file_path: &Path,
    base_path: &Path,
) -> Result<PathBuf, String> {
    let canonical_file = file_path.canonicalize()
        .map_err(|_| "Invalid file path")?;
    let canonical_base = base_path.canonicalize()
        .map_err(|_| "Invalid base path")?;

    if !canonical_file.starts_with(&canonical_base) {
        return Err("Path escapes working directory".to_string());
    }

    Ok(canonical_file)
}

// Example usage in create_jpa_entity command
let safe_path = DirectoryValidator::validate_file_path_within_base(
    &entity_file_path,
    &cwd
)?;
```

**Methods:**

- `validate_file_path_within_base`: Protects during command execution - rejects paths with `..` that escape the root. Used in all creation commands (`create_java_file`, `create_jpa_entity`, etc.) to ensure generated files stay within the project.
- `validate_directory_unrestricted`: Used by Clap for `--cwd` validation - only checks if directory exists

## Commands Router (`src/commands/mod.rs`)

Receives user intent (e.g., "create an entity") and routes to the correct command handler.

**Responsibilities:**

- **Routing & Delegation**: Doesn't execute logic, only decides who should execute
- **Language Dispatch**: `java` argument → Java module, `python` argument → Python module
- **Open/Closed Principle**: New languages can be added without modifying existing code

```rust
#[derive(Subcommand)]
pub enum Commands {
    /// Java language commands
    Java(JavaCommands),
    
    /// Python language commands (future)
    // Python(PythonCommands),
}

impl Commands {
    pub fn execute(&self) -> Result<String, Box<dyn std::error::Error>> {
        match self {
            Commands::Java(cmd) => cmd.execute(),
            // Commands::Python(cmd) => cmd.execute(),
        }
    }
}
```

**Flow:** `main.rs` → `cli.command.execute()` → descends enum tree → reaches leaf command (e.g., `create_jpa_entity_command.rs`)

## Command Implementation (`src/commands/java/*_command.rs`)

Each command file acts as a **Controller**:

```rust
pub fn execute_create_jpa_entity(
    cwd: &Path,
    package_name: &str,
    file_name: &str,
) -> Response<FileResponse> {
    // 1. Validate inputs
    if let Err(e) = PackageNameValidator::validate(package_name) {
        return Response::error("create-jpa-entity", cwd, e);
    }

    // 2. Call service layer
    match CreateJpaEntityService::create(cwd, package_name, file_name) {
        Ok(file_info) => Response::success("create-jpa-entity", cwd, file_info),
        Err(e) => Response::error("create-jpa-entity", cwd, e),
    }
}
```

**Isolation:** This pattern keeps `commands.rs` clean with only routing logic ("where do I send this?"), while business logic lives in services.

# Binary Variants

The core is compiled in two variants to support different integration approaches:

## CLI-only (Default) - `syntaxpresso-core-{platform}-{arch}`

**Target Use Case**: Build IDE plugins with custom UI or programmatic code manipulation

- **Size**: ~3.4MB (optimized for minimal footprint)
- **Dependencies**: Core Rust libraries + Tree-Sitter only
- **Interface**: Pure CLI with JSON output
- **Performance**: No UI overhead

**Use when:**

- Building IDE plugins with your own custom interface (Neovim with custom Lua UI, VSCode with custom webviews, etc.)
- Programmatically manipulating Java code in scripts or automation tools
- Integrating into build pipelines or CI/CD systems
- Need only JSON-based programmatic access

## UI-enabled - `syntaxpresso-core-ui-{platform}-{arch}`

**Target Use Case**: Build IDE plugins using the built-in TUI or standalone terminal usage

- **Size**: ~4.0MB (includes Ratatui + Crossterm)
- **Dependencies**: Core libraries + TUI framework
- **Interface**: Interactive terminal forms + CLI fallback (supports all CLI commands)
- **Performance**: Minimal UI overhead (< 100ms startup)

**Use when:**

- Building IDE plugins that leverage the provided Terminal UI (e.g., Neovim plugin that spawns TUI forms)
- Using as a standalone terminal application without any IDE
- Need both programmatic CLI access AND interactive forms
- Want ready-to-use TUI without implementing your own interface

# Features & Capabilities

Syntaxpresso Core provides language-specific tooling for code generation and manipulation. Each language module implements capabilities tailored to its ecosystem.

## Core Capabilities

All language modules provide:

- **Discovery**: Scan projects for language-specific constructs (classes, entities, modules, types)
- **File Generation**: Create files (classes, interfaces, enums, types, etc.)
- **Code Manipulation**: Add/modify fields, methods, and properties in existing code
- **Interactive UI**: Terminal forms for guided generation (UI-enabled binary)

## Currently Supported: Java

The Java module provides comprehensive JPA/Spring tooling including:

- **Discovery**: JPA entities, mapped superclasses, packages, and file scanning
- **File Generation**: JPA entities, repositories, classes, interfaces, enums, records
- **Code Manipulation**: Basic fields, ID fields, enum fields, entity relationships
- **Interactive UI**: Terminal forms for all generation commands (UI-enabled binary)

**View all available commands:**

```bash
syntaxpresso-core java --help
```

For standalone terminal usage, the UI-enabled binary provides interactive forms. Just add `-ui` suffix for UI commands:

**Example command:**

```bash
syntaxpresso-core java create-jpa-entity-ui \
  --cwd /path/to/project
```

## Response

### Response Types (`src/responses/`)

All commands return a standardized `Response<T>` structure:

```rust
pub struct Response<T> {
    pub command: String,
    pub cwd: String,
    pub succeed: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub data: Option<T>,
    #[serde(skip_serializing_if = "Option::is_none", rename = "errorReason")]
    pub error_reason: Option<String>,
}
```

Success response:

```json
{
  "command": "create-jpa-entity",
  "cwd": "/path/to/project",
  "succeed": true,
  "data": {
    "fileType": "User",
    "filePackageName": "com.example.entities",
    "filePath": "/path/to/User.java"
  }
}
```

Error response:

```json
{
  "command": "create-jpa-entity",
  "cwd": "/path/to/project",
  "succeed": false,
  "errorReason": "Package name is invalid"
}
```

# Installation for Developers

## System Requirements

- **Rust Toolchain**: Rust 2024 Edition or later
- **Cargo**: Latest stable version
- **Platform**: Linux, macOS, or Windows

## Option 1: Download Pre-built Binaries

Download from [GitHub Releases](https://github.com/syntaxpresso/core/releases):

**CLI-only binaries** (recommended for IDE integration):

- `syntaxpresso-core-linux-amd64` - Linux x86_64
- `syntaxpresso-core-macos-amd64` - macOS Intel
- `syntaxpresso-core-macos-arm64` - macOS Apple Silicon
- `syntaxpresso-core-windows-amd64.exe` - Windows x86_64

**UI-enabled binaries** (for standalone use):

- `syntaxpresso-core-ui-linux-amd64` - Linux x86_64
- `syntaxpresso-core-ui-macos-amd64` - macOS Intel
- `syntaxpresso-core-ui-macos-arm64` - macOS Apple Silicon
- `syntaxpresso-core-ui-windows-amd64.exe` - Windows x86_64

## Option 2: Build from Source

Requires Rust 2024 Edition toolchain.

**CLI-only:**

```bash
cargo build --release
```

**UI-enabled:**

```bash
cargo build --release --features ui
```

Binary location: `target/release/syntaxpresso-core`

## Building from Source

**CLI-only variant:**

```bash
cargo build --release
```

**UI-enabled variant:**

```bash
cargo build --release --features ui
```

Output: `target/release/syntaxpresso-core`

## Running Tests

```bash
# Run all tests
cargo test

# Run specific test file
cargo test --test class_declaration_service_tests

# Run with output
cargo test -- --nocapture
```

# Plugin Integration Guide

## Neovim Plugin Development

To develop or integrate with the Neovim plugin:

1. **Build the core:**

   ```bash
   cargo build --release --features ui
   ```

2. **Configure plugin to use local build:**

   ```lua
   {
       "syntaxpresso/syntaxpresso.nvim",
       dir = "/path/to/syntaxpresso.nvim/",
       opts = {
           executable_path = "/path/to/target/release/syntaxpresso-core",
           auto_update = {
               enabled = true, -- Auto-update enabled
               frequency = "always", -- Check on every Neovim start
               prompt = true, -- Prompt before installing updates
           },
       },
   }
   ```

## VSCode / Other IDE Integration

The CLI interface is language-agnostic. Example in TypeScript:

```typescript
import { spawn } from "child_process";

function executeCommand(args: string[]): Promise<any> {
  return new Promise((resolve, reject) => {
    const proc = spawn("./syntaxpresso-core", args);
    let stdout = "";

    proc.stdout.on("data", (data) => (stdout += data));
    proc.on("close", (code) => {
      if (code === 0) {
        resolve(JSON.parse(stdout));
      } else {
        reject(new Error(stdout));
      }
    });
  });
}

// Usage - note the 'java' namespace
const response = await executeCommand([
  "java", // Language namespace
  "create-jpa-entity",
  "--cwd",
  projectPath,
  "--package-name",
  "com.example.domain",
  "--file-name",
  "User",
]);
```

# Performance Benchmarks

Typical operation timings on modern hardware:

| Operation                  | Time      | Notes                             |
| -------------------------- | --------- | --------------------------------- |
| Process spawn              | ~10-20ms  | Cold start overhead               |
| Parse 1000 LOC Java file   | ~5-10ms   | Initial parse                     |
| Incremental field addition | ~1-3ms    | Uses incremental parsing          |
| Full file reparse          | ~50-200ms | Avoided by incremental updates    |
| JSON serialization         | <1ms      | Negligible overhead               |
| Total command execution    | ~20-50ms  | End-to-end for typical operations |

# Contributing

Contributions are welcome!

This project follows standard Rust development practices.

Always code for interfaces.

## Pull Request Process

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Add tests and ensure they pass
5. Run code quality checks (`cargo fmt`, `cargo clippy`)
6. Commit your changes
7. Push to your fork
8. Open a Pull Request with a clear description

## Technical Support & Resources

- **Issues & Bug Reports**: [GitHub Issues](https://github.com/syntaxpresso/core/issues)
- **Discussions**: [GitHub Discussions](https://github.com/syntaxpresso/core/discussions)
- **Source Code**: [GitHub Repository](https://github.com/syntaxpresso/core)
- **Release Notes**: [GitHub Releases](https://github.com/syntaxpresso/core/releases)
