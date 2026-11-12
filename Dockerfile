# ==============================================================================
# Estágio 1: Build do Frontend (React 18 + Vite)
# ==============================================================================
FROM node:20-alpine AS frontend-build
WORKDIR /app

# Copia dependências do frontend e instala
COPY package*.json ./
RUN npm ci || npm install

# Copia configurações e telas (views)
COPY vite.config.js ./
COPY views ./views

# Gera os arquivos estáticos de produção na pasta /app/dist
RUN npm run build

# ==============================================================================
# Estágio 2: Build do Backend (Spring Boot 3 + Maven + OpenJDK 21)
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-21 AS backend-build
WORKDIR /app

# Copia o pom.xml para cache eficiente das dependências Maven
COPY pom.xml .
RUN mvn -B dependency:go-offline || true

# Copia os códigos-fonte, banco e recursos da aplicação
COPY config ./config
COPY database ./database
COPY controllers ./controllers
COPY models ./models
COPY repositories ./repositories
COPY services ./services
COPY tests ./tests

# Copia o build do frontend para a pasta static declarada nos recursos do pom.xml
COPY --from=frontend-build /app/dist ./static

# Compila e empacota a aplicação Java com o frontend embutido
RUN mvn -B clean package -DskipTests

# Padroniza o nome do JAR final
RUN cp target/saluris-ia-*.jar target/app.jar

# ==============================================================================
# Estágio 3: Runtime de Produção (JRE 21 leve)
# ==============================================================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copia apenas o artefato final executável
COPY --from=backend-build /app/target/app.jar app.jar

# Otimizações de JVM para o plano gratuito do Render (512 MB de RAM)
# Limita o heap em 65% da memória para prevenir OOMKilled
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -Xss512k -Dfile.encoding=UTF-8"

# Expõe as portas utilizadas (10000 para o Render, 8080 padrão)
EXPOSE 10000 8080

# Inicialização:
# 1. Monta a URL JDBC caso DB_HOST esteja definido e DB_URL esteja vazia.
# 2. Garante chave JWT em Base64 válida (mínimo 32 bytes) caso não configurada.
# 3. Configura a porta do servidor para o valor exigido pelo Render ($PORT).
CMD ["sh", "-c", "if [ -z \"$DB_URL\" ] && [ -n \"$DB_HOST\" ]; then export DB_URL=\"jdbc:postgresql://${DB_HOST}:${DB_PORT:-5432}/${DB_NAME:-saluris}\"; fi; if [ -z \"$SALURIS_JWT_SECRET\" ]; then export SALURIS_JWT_SECRET=\"U2FsdXJpc0lhU2VjdXJlSldUU2VjcmV0S2V5Rm9yUmVuZGVyRGVwbG95MzJCeXRlczE=\"; fi; export SERVER_PORT=\"${PORT:-10000}\"; exec java -jar app.jar --server.port=${SERVER_PORT}"]
