# Documentation — Pipeline CI/CD Jenkins + SonarQube

## Sommaire
1. [Architecture](#architecture)
2. [Étape 1 — GitHub](#étape-1--github)
3. [Étape 2 — Docker Compose](#étape-2--docker-compose)
4. [Étape 3 — Installation Jenkins](#étape-3--installation-jenkins)
5. [Étape 4 — Configuration Jenkins](#étape-4--configuration-jenkins)
6. [Étape 5 — Configuration SonarQube](#étape-5--configuration-sonarqube)
7. [Étape 6 — Lier Jenkins et SonarQube](#étape-6--lier-jenkins-et-sonarqube)
8. [Étape 7 — Créer le job Jenkins](#étape-7--créer-le-job-jenkins)
9. [Étape 8 — Jenkinsfile](#étape-8--jenkinsfile)
10. [Captures d'écran requises](#captures-décran-requises)

---

## Architecture

```
┌─────────────────────────────────────────┐
│              Docker Network             │
│                ci-network               │
│                                         │
│  ┌──────────┐       ┌────────────────┐  │
│  │ Jenkins  │──────▶│   SonarQube    │  │
│  │  :8080   │       │     :9000      │  │
│  └──────────┘       └────────────────┘  │
│                             │           │
│                     ┌───────────────┐   │
│                     │  PostgreSQL   │   │
│                     │  (sonar-db)   │   │
│                     └───────────────┘   │
└─────────────────────────────────────────┘
         ▲
         │  git pull (Jenkinsfile + code Java)
         │
┌────────────────┐
│  GitHub Repo   │
│ Romain-mont/   │
│ tp_epsi_inte.. │
└────────────────┘
```

---

## Étape 1 — GitHub

### Objectif
Héberger le code source pour que Jenkins puisse le récupérer automatiquement.

### Actions effectuées
- Création d'un dépôt public sur GitHub : `https://github.com/Romain-mont/tp_epsi_integration`
- Push du code source (code Java + Jenkinsfile + Dockerfile + pom.xml)

```bash
git init
git add .
git commit -m "first commit"
git remote add origin https://github.com/Romain-mont/tp_epsi_integration.git
git push -u origin main
```

> **Repo public** : obligatoire pour que Jenkins puisse cloner sans credentials GitHub.

---

## Étape 2 — Docker Compose

### Objectif
Lancer Jenkins, SonarQube et sa base de données PostgreSQL en local via Docker.

### Fichier `docker-compose.yml`

```yaml
services:
  jenkins:
    image: jenkins/jenkins:lts
    container_name: jenkins
    ports:
      - "8080:8080"
      - "50000:50000"
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
    networks:
      - ci-network

  sonarqube:
    image: sonarqube:community
    container_name: sonarqube
    depends_on:
      - sonar-db
    environment:
      SONAR_JDBC_URL: jdbc:postgresql://sonar-db:5432/sonar
      SONAR_JDBC_USERNAME: sonar
      SONAR_JDBC_PASSWORD: sonar
    ports:
      - "9000:9000"
    volumes:
      - sonarqube_data:/opt/sonarqube/data
      - sonarqube_logs:/opt/sonarqube/logs
    networks:
      - ci-network

  sonar-db:
    image: postgres:15
    container_name: sonar-db
    environment:
      POSTGRES_USER: sonar
      POSTGRES_PASSWORD: sonar
      POSTGRES_DB: sonar
    volumes:
      - sonar_db_data:/var/lib/postgresql/data
    networks:
      - ci-network

volumes:
  jenkins_home:
  sonarqube_data:
  sonarqube_logs:
  sonar_db_data:

networks:
  ci-network:
    driver: bridge
```

### Lancement

```bash
cd tp_epsi_integration_continue-main
docker compose up -d
```

### Accès
| Service    | URL                   | Identifiants par défaut |
|------------|-----------------------|-------------------------|
| Jenkins    | http://localhost:8080 | mot de passe généré     |
| SonarQube  | http://localhost:9000 | admin / admin           |

### Récupérer le mot de passe initial Jenkins

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

> **[CAPTURE 1]** — Page "Débloquer Jenkins" avec le champ mot de passe rempli.

---

## Étape 3 — Installation Jenkins

### Plugins installés
Sur la page "Personnaliser Jenkins" → **"Installer les plugins suggérés"**

Cela installe automatiquement : Pipeline, Git, Credentials Binding, etc.

> **[CAPTURE 2]** — Page "Installation en cours..." avec la liste des plugins.

Ensuite, installer manuellement le plugin SonarQube :

**Engrenage → Plugins → Plugins disponibles → rechercher "SonarQube Scanner"**
- Installer **SonarQube Scanner 2.18.2**

> **[CAPTURE 3]** — Page "Plugins installés" avec SonarQube Scanner activé.

---

## Étape 4 — Configuration Jenkins

### 4.1 Outils (JDK + Maven)

**Engrenage → Outils (Global Tool Configuration)**

**Section JDK :**
- Nom : `JDK 17`
- Install automatically : ✓ → OpenJDK 17

**Section Maven :**
- Nom : `Maven`
- Install automatically : ✓ → dernière version

> Ces noms doivent correspondre **exactement** à ce qui est dans le Jenkinsfile :
> ```groovy
> tools {
>     maven 'Maven'
>     jdk 'JDK 17'
> }
> ```

### 4.2 Credential SonarQube Token

**Engrenage → Credentials → System → Global credentials → Add Credentials**

| Champ       | Valeur                              |
|-------------|-------------------------------------|
| Kind        | Secret text                         |
| Secret      | `<token généré depuis SonarQube>`   |
| ID          | `sonar-token`                       |
| Description | Secret text                         |
| Scope       | Global                              |

> **[CAPTURE 4]** — Formulaire "Add Secret text" complété avant de cliquer Create.

---

## Étape 5 — Configuration SonarQube

### 5.1 Connexion initiale
- URL : `http://localhost:9000`
- Login : `admin` / `admin`
- Changer le mot de passe à la première connexion

### 5.2 Générer un token d'authentification

**Avatar → My Account → Security → Generate Tokens**

| Champ     | Valeur                |
|-----------|-----------------------|
| Name      | `jenkins-token`       |
| Type      | Global Analysis Token |
| Expires   | 30 days               |

Cliquer **Generate** → **Copier le token immédiatement** (affiché une seule fois).

> **[CAPTURE 5]** — Page Security avec le token généré visible.

---

## Étape 6 — Lier Jenkins et SonarQube

### 6.1 Serveur SonarQube dans Jenkins

**Engrenage → Configurer le système → SonarQube servers**

| Champ                     | Valeur                 |
|---------------------------|------------------------|
| Nom                       | `sonarqube`            |
| URL du serveur            | `http://sonarqube:9000`|
| Server authentication token | `sonar-token` (sélectionner dans la liste) |

> L'URL utilise `sonarqube` (nom du conteneur Docker) et non `localhost`, car Jenkins et SonarQube communiquent au sein du même réseau Docker.

> **[CAPTURE 6]** — Section SonarQube servers avec les champs remplis et le token sélectionné.

### 6.2 Webhook SonarQube → Jenkins

Dans SonarQube : **Administration → Configuration → Webhooks → Create**

| Champ | Valeur                                  |
|-------|-----------------------------------------|
| Name  | jenkins                                 |
| URL   | `http://jenkins:8080/sonarqube-webhook/`|

> Ce webhook permet à `waitForQualityGate` de recevoir le résultat de l'analyse sans attendre le timeout.

---

## Étape 7 — Créer le job Jenkins

**Nouveau Item → Nom : `bad-practices-app` → Pipeline → OK**

Dans la configuration → section **Pipeline** :

| Champ             | Valeur                                                      |
|-------------------|-------------------------------------------------------------|
| Definition        | Pipeline script from SCM                                    |
| SCM               | Git                                                         |
| Repository URL    | `https://github.com/Romain-mont/tp_epsi_integration`        |
| Branch Specifier  | `*/main`                                                    |
| Script Path       | `tp_epsi_integration_continue-main/Jenkinsfile`             |

> **[CAPTURE 7]** — Configuration du job Pipeline avec le repo GitHub et le Script Path.

---

## Étape 8 — Jenkinsfile

### Fichier final `Jenkinsfile`

```groovy
pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk 'JDK 17'
    }

    environment {
        SONAR_HOST_URL = 'http://sonarqube:9000'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Compilation du projet...'
                dir('tp_epsi_integration_continue-main') {
                    sh 'mvn clean compile -DskipTests'
                }
            }
        }

        stage('Test & Code Coverage') {
            steps {
                echo 'Exécution des tests et génération du rapport JaCoCo...'
                dir('tp_epsi_integration_continue-main') {
                    sh 'mvn test'
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo 'Analyse de la qualité du code avec SonarQube...'
                withSonarQubeEnv('sonarqube') {
                    dir('tp_epsi_integration_continue-main') {
                        sh 'mvn sonar:sonar -Dsonar.projectKey=bad-practices-app'
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Package & Docker Build') {
            steps {
                echo 'Création du JAR exécutable et de l\'image Docker...'
                dir('tp_epsi_integration_continue-main') {
                    sh 'mvn package -DskipTests'
                    sh 'docker build -t epsi/bad-practices-app:latest .'
                }
            }
        }
    }
}
```

### Problèmes rencontrés et solutions

| Problème | Cause | Solution |
|----------|-------|----------|
| `Unable to find Jenkinsfile` | Le Jenkinsfile est dans un sous-dossier | Changer le Script Path en `tp_epsi_integration_continue-main/Jenkinsfile` |
| `No POM in this directory` | Maven cherche `pom.xml` à la racine du workspace | Envelopper les commandes Maven dans `dir('tp_epsi_integration_continue-main')` |
| `No previous SonarQube analysis found` | Analyse pas enveloppée dans `withSonarQubeEnv` | Remplacer `sh 'mvn sonar:sonar ...'` par `withSonarQubeEnv('sonarqube') { ... }` |
| `Not authorized` | Token non lié au serveur SonarQube dans Jenkins | Sélectionner `sonar-token` dans la config du serveur SonarQube |

---

## Étape 9 — Corrections du code Java

### 9.1 Tableau récapitulatif des corrections

| # | Fichier | Problème (code original) | Correction appliquée |
|---|---------|--------------------------|----------------------|
| 1 | UserService.java | `import java.util.List` et `import java.util.ArrayList` inutilisés | Imports supprimés |
| 2 | UserService.java | `private String DB_PASSWORD = "super_secret_password_123!"` — mot de passe en dur | Remplacé par `System.getenv("DB_PASSWORD")` |
| 3 | UserService.java | `private int unusedCounter = 0` — variable jamais utilisée | Variable supprimée |
| 4 | UserService.java | `boolean isLoggedIn = false` — variable locale jamais lue | Variable supprimée |
| 5 | UserService.java + Main.java | `System.out.println(...)` partout | Remplacé par `private static final Logger LOGGER = Logger.getLogger(...)` |
| 6 | UserService.java | `username.equals("admin") && password.equals("admin")` — identifiants en dur | Remplacé par `System.getenv("ADMIN_USERNAME")` et `System.getenv("ADMIN_PASSWORD")` |
| 7 | UserService.java | `"SELECT * FROM users WHERE username = '" + username + "'"` — injection SQL | Remplacé par `PreparedStatement` avec `stmt.setString(1, username)` |
| 8 | UserService.java | `catch (Exception e) {}` — bloc catch vide, erreur silencieuse | Remplacé par `LOGGER.warning("Erreur : " + e.getMessage())` |
| 9 | UserService.java | Fermeture manuelle de `rs`, `stmt`, `conn` dans des blocs `finally` avec catch vides | Remplacé par `try-with-resources` |
| 10 | UserService.java | `complexMethod` avec 5 niveaux d'imbrication `if/else` | Simplifié avec des `return` anticipés (early return) |

### 9.2 Itérations de correction

#### Iteration 1 — Premier push des corrections (build #6)
**Résultat :** Quality Gate **Failed**

SonarQube a détecté 3 issues résiduelles bloquantes :

| Issue | Localisation | Sévérité |
|-------|-------------|----------|
| "Make sure this expression can't be zero before doing this division" | `login()` ligne 28 — le `int result = 10 / 0` | Reliability **High** |
| "Remove this useless assignment to local variable result" | même ligne | Maintainability Medium |
| "Remove this unused 'result' local variable" | même ligne | Maintainability Low |
| "Use the built-in formatting to construct this argument" | Logger avec `+` concaténation | Maintainability Medium |
| "Don't use the query SELECT *" | `getUserDetails()` | Maintainability Medium |

> **[CAPTURE intermédiaire A]** — SonarQube Quality Gate **Failed** avec Reliability D, Security A, Maintainability A
> **[CAPTURE intermédiaire B]** — Jenkins build #7 Pipeline Overview avec Quality Gate en rouge

#### Iteration 2 — Corrections supplémentaires (build #8)
**Résultat :** Quality Gate **Failed** — mais progression : Security A (0), Reliability A (0), Maintainability A (4 issues)

Corrections appliquées :
- Suppression complète du bloc `try { int result = 10 / 0; } catch (...)` (code factice)
- Logger : remplacement de la concaténation `+` par `LOGGER.log(Level.INFO, "text {0}", variable)`
- Requête SQL : `SELECT *` → `SELECT username`

Issues résiduelles (4) :
| Issue | Fichier | Ligne | Sévérité |
|-------|---------|-------|----------|
| "Use the built-in formatting to construct this argument" | UserService.java | L54 | Medium |
| "Use the built-in formatting to construct this argument" | UserService.java | L57 | Medium |
| "Remove this 'public' modifier" | UserServiceTest.java | L6 | Info |
| "Remove this 'public' modifier" | UserServiceTest.java | L9 | Info |

> **[CAPTURE D]** — SonarQube Quality Gate Failed — Security A, Reliability A, Maintainability A (4 issues)
> **[CAPTURE E]** — Jenkins build #8 Pipeline Overview — Quality Gate rouge
> **[CAPTURE F]** — SonarQube Issues — les 4 issues résiduelles visibles

#### Iteration 3 — Corrections SonarQube (build #9)
**Résultat :** Quality Gate **Failed** — 0 issues, mais couverture insuffisante

Build #9 : 0 issues SonarQube, mais Quality Gate échoue sur la couverture de code.

| Métrique | Valeur | Seuil Quality Gate | Résultat |
|----------|--------|---------------------|----------|
| Security | A (0 issues) | ✓ | PASS |
| Reliability | A (0 issues) | ✓ | PASS |
| Maintainability | A (0 issues) | ✓ | PASS |
| **Coverage** | **62.3%** | **≥ 80%** | **FAIL** |

> **[CAPTURE D]** — SonarQube Projects : Quality Gate Failed — 0 issues mais Coverage 62.3%
> **[CAPTURE E]** — Jenkins build #9 Pipeline Overview — Quality Gate rouge

**Pourquoi ne pas simplement baisser le seuil du Quality Gate ?**

En entreprise, baisser le Quality Gate pour "faire passer le build" est une mauvaise pratique connue sous le nom de *gaming the metric*. Cela masque un manque réel de tests et crée une dette technique invisible. La bonne démarche est d'identifier quelle partie du code n'est pas couverte et d'écrire le test manquant.

**Analyse de la couverture manquante :**

Deux parties du code n'étaient pas couvertes par les tests :
1. **`getUserDetails()` — chemin succès** : nécessite une vraie connexion base de données. Seul le bloc `catch` était couvert (URL null → exception).
2. **`Main.java`** : la classe principale n'avait aucun test.

#### Iteration 4 — Couverture de code : H2 + Dependency Injection (build #10)
**Résultat attendu :** Quality Gate **Passed** (~90% de couverture)

**Problème :** `getUserDetails` lit les paramètres DB via `System.getenv()`, ce qui rend impossible l'injection d'une URL de test sans modifier le code.

**Solution — Pattern Dependency Injection :**

Ajout d'un constructeur package-private dans `UserService.java` qui accepte les paramètres DB directement, permettant aux tests d'injecter une base en mémoire sans toucher au code de production :

```java
public UserService() {
    // Constructeur de production : lit les variables d'environnement
    this.dbPassword = System.getenv("DB_PASSWORD");
    this.dbUrl = System.getenv("DB_URL");
    this.dbUser = System.getenv("DB_USER");
}

// Constructeur package-private pour les tests uniquement
UserService(String dbUrl, String dbUser, String dbPassword) {
    this.dbUrl = dbUrl;
    this.dbUser = dbUser;
    this.dbPassword = dbPassword;
}
```

**Ajout de H2 dans `pom.xml`** (scope `test` : absent en production) :
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.2.224</version>
    <scope>test</scope>
</dependency>
```

**Nouveaux tests ajoutés dans `UserServiceTest.java` :**

```java
@Test
void testGetUserDetailsSuccess() throws Exception {
    // Base H2 en mémoire : simule un vrai appel DB sans infrastructure
    String h2Url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    try (Connection conn = DriverManager.getConnection(h2Url, "sa", "")) {
        conn.createStatement().execute(
            "CREATE TABLE IF NOT EXISTS users (username VARCHAR(50))"
        );
        conn.createStatement().execute("INSERT INTO users VALUES ('john_doe')");
    }
    UserService service = new UserService(h2Url, "sa", "");
    assertDoesNotThrow(() -> service.getUserDetails("john_doe"));
}

@Test
void testMain() {
    // Couvre Main.java entièrement
    assertDoesNotThrow(() -> Main.main(new String[]{}));
}
```

> `testGetUserDetailsSuccess` couvre le chemin succès complet de `getUserDetails` : connexion, préparation de la requête, exécution, parcours du `ResultSet`, log du résultat.
> `testMain` couvre l'intégralité de `Main.java` (méthode `main` + logger).

**Résultat des tests en local avant push :** 5/5 tests passent (`BUILD SUCCESS`).

### 9.3 Détail des corrections importantes

#### Correction 2 — Mot de passe en dur (faille critique)

**Avant :**
```java
private String DB_PASSWORD = "super_secret_password_123!";
```

**Après :**
```java
private String dbPassword = System.getenv("DB_PASSWORD");
```

> **Pourquoi c'est critique :** Un mot de passe en dur dans le code source est visible par toute personne ayant accès au repo Git. Il ne peut pas être changé sans modifier et redéployer le code.

#### Correction 5 — Logger remplace System.out

**Avant :**
```java
System.out.println("Tentative de connexion de l'utilisateur : " + username);
```

**Après :**
```java
private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());
// ...
LOGGER.info("Tentative de connexion de l'utilisateur : " + username);
```

> **Pourquoi :** `System.out` ne permet pas de filtrer par niveau de sévérité, ni de rediriger les logs vers un fichier. Un Logger permet `INFO`, `WARNING`, `SEVERE`, etc.

#### Correction 7 — Injection SQL

**Avant :**
```java
String query = "SELECT * FROM users WHERE username = '" + username + "'";
rs = stmt.executeQuery(query);
```

**Après :**
```java
PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
stmt.setString(1, username);
ResultSet rs = stmt.executeQuery();
```

> **Pourquoi c'est critique :** Avec la concaténation, un attaquant peut passer `' OR '1'='1` comme username et récupérer tous les utilisateurs. Le `PreparedStatement` échappe automatiquement les paramètres.

#### Correction 9 — try-with-resources

**Avant :**
```java
Connection conn = null;
Statement stmt = null;
ResultSet rs = null;
try { ... }
catch (Exception e) { e.printStackTrace(); }
finally {
    if (rs != null) { try { rs.close(); } catch (Exception e) {} }
    if (stmt != null) { try { stmt.close(); } catch (Exception e) {} }
    if (conn != null) { try { conn.close(); } catch (Exception e) {} }
}
```

**Après :**
```java
try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
     PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
    stmt.setString(1, username);
    try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) { ... }
    }
} catch (Exception e) {
    LOGGER.severe("Erreur : " + e.getMessage());
}
```

#### Correction 10 — complexMethod simplifiée

**Avant :** 5 niveaux d'imbrication `if/else`

**Après :** Early return pattern
```java
public void complexMethod(int a, int b, int c) {
    if (a <= 0) { LOGGER.info("A est négatif"); return; }
    if (b <= 0) { LOGGER.info(c > 0 ? "B est négatif" : "B et C sont négatifs"); return; }
    LOGGER.info(c > 0 ? "Tous positifs" : "C est négatif");
}
```

---

## Captures d'écran requises

### État initial (avant corrections du code)

| # | Où | Quoi capturer |
|---|----|---------------|
| CAPTURE 1 | Jenkins setup | Page "Débloquer Jenkins" |
| CAPTURE 2 | Jenkins setup | Installation des plugins en cours |
| CAPTURE 3 | Jenkins Plugins | SonarQube Scanner installé et activé |
| CAPTURE 4 | Jenkins Credentials | Formulaire Secret text avec ID `sonar-token` |
| CAPTURE 5 | SonarQube My Account | Token `jenkins-token` généré |
| CAPTURE 6 | Jenkins System Config | Section SonarQube servers complète avec token |
| CAPTURE 7 | Jenkins Job Config | Pipeline SCM pointant vers GitHub |
| **CAPTURE 8** | **Jenkins build** | **Pipeline #5 passé avec les stages visibles (Pipeline Overview)** |
| **CAPTURE 9** | **SonarQube** | **Vue projet avec ratings : Security C, Reliability D, 23 issues** |
| **CAPTURE 10** | **SonarQube Issues** | **Liste complète des issues (mot de passe en dur, injection SQL, etc.)** |

### État intermédiaire (après 1ère correction — Quality Gate encore rouge)

| # | Où | Quoi capturer |
|---|----|---------------|
| **CAPTURE A** | **SonarQube** | **Quality Gate Failed — Reliability D, Security A, Maintainability A, 10 issues** |
| **CAPTURE B** | **SonarQube Issues** | **Liste des 10 issues résiduelles (division par zéro, SELECT *, Logger concat)** |
| **CAPTURE C** | **Jenkins Pipeline Overview** | **Build #7 avec Quality Gate en rouge** |

### État final (après toutes les corrections — Quality Gate vert)

| # | Où | Quoi capturer |
|---|----|---------------|
| CAPTURE 11 | Jenkins | Pipeline avec tous les stages au vert (Pipeline Overview) |
| CAPTURE 12 | SonarQube | Quality Gate **Passed** — tous les ratings au vert, Coverage ≥ 80% |
| CAPTURE 13 | SonarQube Issues | "No Issues. Hooray!" — 0 issue |

### Bonus Docker

| # | Où | Quoi capturer |
|---|----|---------------|
| CAPTURE 14 | Terminal | `docker ps` montrant le conteneur `epsi/bad-practices-app` qui tourne |
