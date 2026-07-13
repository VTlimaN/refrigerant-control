# Project Identity

- Display name: Controle de Gases Refrigerantes.
- Repository and artifact: `refrigerant-control`.
- Group: `dev.sasser`.
- Base package: `dev.sasser.refrigerantcontrol`.

# Fixed Technical Baseline

- Use Java 25 without preview features or `--enable-preview`.
- Use Spring Boot 4.1.0.
- Use Maven Wrapper pinned to Maven 3.9.16.
- Package the application as an executable JAR.
- Use Spring MVC and Thymeleaf in a modular monolith.
- Keep compatibility with Windows and Debian Linux.

# Product Principle

Follow “Complete internally, simple externally.” Internal rules, storage, validation, traceability, backup, and integrity must eventually be complete, while daily use must remain simple and fast.

# Language Rules

- Use English for packages, classes, interfaces, methods, variables, constants, tests, database identifiers, technical identifiers, source comments, and Git commit messages.
- Use Brazilian Portuguese for visible application content, user messages, validation messages, README explanations, and beginner-facing documentation.

# Source-Code Cleanliness

- Write concise, readable, self-explanatory code with expressive names and small methods.
- Add a source comment only when naming and structure cannot communicate a genuinely non-obvious technical decision, workaround, limitation, or complex rule.
- Do not add decorative comments, section dividers, tutorial comments, comments above obvious code, or excessive Javadoc.
- Do not add commented-out code.
- Do not add `TODO`, `FIXME`, `NOTE`, speculative comments, or future placeholders.
- Put educational explanations in `README.md` and final implementation reports, not throughout source files.

# Architecture Direction

- Keep a modular-monolith architecture.
- Do not introduce microservices prematurely.
- Create packages and abstractions only for real, currently authorized behavior.
- Do not add unnecessary interfaces, factories, DTOs, services, design patterns, or speculative layers.
- Keep controllers small and focused on HTTP responsibilities.

# Dependency Governance

The current dependency allowlist is:

- `spring-boot-starter-webmvc`
- `spring-boot-starter-thymeleaf`
- `spring-boot-starter-validation`
- `spring-boot-devtools`
- `spring-boot-starter-webmvc-test` in test scope

Obtain explicit approval before adding, removing, or replacing a dependency. Do not add Lombok, Java preview features, frontend build tools, databases, security, or infrastructure without an authorized milestone requirement.

# Feature Authorization

The first milestone is authorized only to prove the build, tests, executable JAR, server startup, MVC routing, Thymeleaf rendering, JSON serialization, IDE recognition, and platform compatibility.

Do not implement refrigerant registration, cylinder registration, activities, consumption calculation, history, warnings, catalog data, backup, export, traceability, database persistence, authentication, authorization, dashboards, unfinished navigation, or placeholders during this milestone.

Future agents must not broaden a milestone without a new explicit request.

# Testing Expectations

- Keep a minimal full-context test for application startup.
- Test rendered pages with the real application and Thymeleaf configuration when rendering is part of the requirement.
- Use focused MVC tests for isolated controllers when appropriate.
- Run the complete Maven verification lifecycle after implementation changes.
- Do not leave skipped or failing tests in completed work.

# Cross-Platform Rules

- Use `mvnw.cmd` on Windows and `./mvnw` on Debian.
- Keep `mvnw` with LF line endings and `mvnw.cmd` with CRLF line endings.
- Document `chmod +x mvnw` as a Debian fallback because line-ending rules do not guarantee executable permission.
- Avoid machine-specific paths in project source and configuration.

# Git and Repository Hygiene

- Never commit build output, IDE caches, machine configuration, credentials, environment secrets, private keys, or local logs.
- Preserve user files and unrelated changes.
- Do not run destructive Git commands.
- Do not run `git init`, stage files, modify the index, create commits, rewrite history, force operations, or delete branches without explicit authorization.
- Read-only commands such as `git status`, `git diff`, `git branch --show-current`, and `git check-ignore` are allowed when relevant.

# Secrets and IDE Metadata

- Never store credentials or secrets in source control.
- Do not edit `.idea`, `.iml` files, project SDK settings, language-level settings, run configurations, or other IntelliJ metadata without explicit authorization.

# Operational Safety

- Never use destructive commands or bypass permissions.
- Request approval when an action requires network, write, or external-system access.
- Do not terminate unrelated processes.
- Stop only processes started by the current task.

# Documentation

- Keep `README.md` accurate, beginner-friendly, and written in Brazilian Portuguese.
- Document important commands, dependencies, decisions, limitations, and cross-platform differences.

# Definition of Done

Work is complete only when applicable compilation, tests, packaging, startup, endpoint verification, file review, and Git status checks succeed. Confirm that no unapproved feature, dependency, comment, secret, IDE metadata, staged file, or commit was introduced.

# Final Implementation Reporting

Report files changed, commands run, dependency decisions, test results, JAR generation, startup behavior, endpoint responses, logs, Git status, ignored files, and known limitations. State explicitly whether anything was staged or committed.
