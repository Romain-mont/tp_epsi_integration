# Utilisation d'une image Java 17 légère
FROM eclipse-temurin:17-jre-alpine

# Création d'un répertoire de travail
WORKDIR /app

# Copie du JAR généré par Maven (étape "Package" du pipeline)
COPY target/bad-practices-app-1.0-SNAPSHOT.jar app.jar

# Commande de démarrage de l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
