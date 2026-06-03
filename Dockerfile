# Stage 1: Build & Compile
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY backend ./backend
RUN mkdir -p backend/out
RUN javac -d backend/out -cp "backend/lib/sqlite-jdbc.jar" backend/src/*.java

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/backend/out ./backend/out
COPY --from=builder /app/backend/lib ./backend/lib
COPY config.properties ./config.properties
COPY frontend ./frontend

# Create data directory for SQLite database
RUN mkdir -p data

EXPOSE 7071
# Note: Linux classpath uses ":" as separator
CMD ["java", "-cp", "backend/out:backend/lib/sqlite-jdbc.jar", "Main"]
