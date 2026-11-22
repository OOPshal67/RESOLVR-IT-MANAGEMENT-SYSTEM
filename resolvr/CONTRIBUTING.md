# Contributing to RESOLVR

1. Create a branch from `main` for your change.
2. Use Java 21 and Maven. Use the Docker Compose setup in the [README](README.md) to run the application locally.
3. Run `mvn verify`. If your change affects database migrations or account workflows, also run `mvn -Ppostgres-tests verify` with Docker running.
4. Update the relevant documentation when behaviour changes.
5. Open a pull request describing what changed and how you checked it.

Keep .env files, generated demo credentials, private data and build output out of commits.
