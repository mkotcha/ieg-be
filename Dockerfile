# Immagine di produzione del backend (build completa dentro il container).
#   podman build -t ieg-be .
# Avvio: quadlet ~/.config/containers/systemd/ieg-be.container
#
# I dati di runtime NON stanno nell'immagine: la cartella volume/ del repository (env.properties, data/,
# fatture/, log/) si monta in /app/volume, che è la cartella di lavoro, quindi i percorsi relativi del
# codice (file:env.properties, data/uploads, fatture/..., log/ieg.log) finiscono lì.

# --- build ---
FROM docker.io/library/maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# --- runtime ---
FROM docker.io/library/eclipse-temurin:21-jre
# pg_dump per GET /api/db/dump: deve essere della stessa major del server (Postgres 18), quindi dal repository PGDG
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl gnupg \
    && install -d /usr/share/postgresql-common/pgdg \
    && curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc \
    && . /etc/os-release \
    && echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt ${VERSION_CODENAME}-pgdg main" > /etc/apt/sources.list.d/pgdg.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends postgresql-client-18 \
    && apt-get purge -y curl gnupg && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /build/target/*.jar /app/ieg.jar
ENV TZ=Europe/Rome \
    DB_DUMP_COMANDO=pg_dump
WORKDIR /app/volume
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/ieg.jar"]
