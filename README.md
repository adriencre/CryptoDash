# CryptoDash

Application de trading de cryptomonnaies fictive (Angular + Spring Boot + WebSockets).

## Structure

- **backend/** – Spring Boot 3 (relais Binance → STOMP)
- **frontend/** – Angular 19, RxJS, Tailwind CSS
- **e2e/** – Cypress E2E

## Prérequis

- Java 17+
- Node.js 18+ (LTS recommandé)
- Maven 3.8+
- **Docker** (pour MySQL en dev, remplace MAMP)

## Démarrage

### Base de données (Docker)

MySQL remplace MAMP. Une seule commande :

```bash
docker compose up -d
```

La base `cryptodash` est créée automatiquement (utilisateur : `cryptodash` / mot de passe : `cryptodash`). Port exposé : `3306`.

### Backend

```bash
cd backend
mvn spring-boot:run
```

Profil par défaut : `dev` (MySQL via Docker). API + WebSocket : `http://localhost:8080`, endpoint STOMP : `/ws`.

Sans Docker : `mvn spring-boot:run -Dspring-boot.run.profiles=dev-h2` pour utiliser H2 en mémoire.

**Option : tout en Docker** (MySQL + backend) : `docker compose --profile full up -d`

### Frontend

```bash
cd frontend
npm install
npm start
```

Ouvre `http://localhost:4200`. Le service de prix se connecte au backend en `ws://localhost:8080/ws`.

### Tests

- **Backend** : `cd backend && mvn test`
- **Frontend** : `cd frontend && npm test`
- **E2E** : lancer backend + frontend, puis `cd e2e && npm run e2e` (ou `npx cypress open`)

## WebSocket

Le backend se connecte au stream Binance (ticker 24h), mappe les événements en `PriceTickDto` et les diffuse sur le topic STOMP `/topic/prices`. Le frontend s’abonne via `PriceStreamService` (RxJS / @stomp/rx-stomp) et met à jour le tableau de prix en temps réel.
