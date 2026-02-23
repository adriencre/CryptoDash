# CryptoDash Project Analysis (GEMINI.md)

This document provides a comprehensive overview of the CryptoDash project, its architecture, and development workflows. It is intended to be used as a quick reference for developers and as context for AI-powered development tools.

## 1. Project Overview

CryptoDash is a full-stack, real-time cryptocurrency trading dashboard. It allows users to monitor crypto prices, manage a virtual portfolio, and view trading history. The application is designed with a modern, decoupled architecture, making it modular and scalable.

- **Backend:** A Java-based Spring Boot application that serves as the core business logic layer.
- **Frontend:** An Angular single-page application (SPA) providing a reactive and dynamic user interface.
- **E2E Testing:** A dedicated Cypress setup for end-to-end testing of user flows.

## 2. Architecture

The application follows a classic client-server model with a real-time data-push mechanism.

- **Client-Server Communication:** The Angular frontend communicates with the Spring Boot backend via a REST API for standard operations (e.g., authentication, portfolio management) and a WebSocket connection for real-time price updates.
- **Real-time Data Flow:**
    1. The Spring Boot backend establishes a persistent WebSocket connection to the public Binance API (`wss://stream.binance.com`).
    2. It subscribes to live ticker data for a predefined list of cryptocurrency pairs (e.g., `btcusdt`, `ethusdt`).
    3. As the backend receives price updates from Binance, it processes them and broadcasts them to connected frontend clients via its own STOMP/WebSocket endpoint (`/topic/prices`).
    4. The Angular frontend subscribes to this STOMP topic and updates the UI in real-time as new price ticks arrive.
- **Database:** A MySQL database stores user data, portfolio information, transaction history, and other persistent state. Docker is used to provision this database for local development.

## 3. Key Technologies

| Tier      | Technology                  | Purpose                                        |
| :-------- | :-------------------------- | :--------------------------------------------- |
| **Frontend** | Angular 19                 | Core UI framework for the SPA                  |
|           | TypeScript                  | Primary language for Angular development       |
|           | RxJS                        | Reactive programming for managing async data   |
|           | Tailwind CSS                | Utility-first CSS framework for styling        |
|           | `@stomp/rx-stomp`           | For handling STOMP WebSocket communication     |
| **Backend**  | Java 17                     | Core language for the backend application    |
|           | Spring Boot 3               | Framework for building the REST API & WebSocket services |
|           | Spring Web, WebSocket, Security | Handling HTTP, real-time connections, and auth |
|           | Spring Data JPA / Hibernate | Database persistence                         |
|           | Maven                       | Dependency management and build tool         |
| **Database** | MySQL 8.0                   | Relational database for data storage         |
| **Testing**  | Cypress                     | End-to-end testing                           |
|           | JUnit 5 / Mockito           | Backend unit and integration testing         |
|           | Karma / Jasmine             | Frontend unit testing                        |
| **Tooling**  | Docker                      | Containerization for the MySQL database      |

## 4. How to Build, Run, and Test

### Prerequisites
- Java 17+
- Node.js 18+
- Maven 3.8+
- Docker

### A. Full Stack (Recommended)

1.  **Start the Database:**
    Run the MySQL database in a Docker container.

    ```bash
    docker compose up -d
    ```

2.  **Run the Backend:**
    Navigate to the backend directory and use Maven to run the Spring Boot application. It will automatically connect to the Dockerized MySQL instance (using the `dev` profile).

    ```bash
    cd backend
    mvn spring-boot:run
    ```
    The API will be available at `http://localhost:8080`.

3.  **Run the Frontend:**
    In a new terminal, navigate to the frontend directory, install dependencies, and start the development server.

    ```bash
    cd frontend
    npm install
    npm start
    ```
    The application will be accessible at `http://localhost:4200`. The frontend proxies API requests to the backend.

### B. Testing

-   **Backend Tests:**
    ```bash
    cd backend
    mvn test
    ```

-   **Frontend Unit Tests:**
    ```bash
    cd frontend
    npm test
    ```

-   **End-to-End Tests:**
    Ensure both backend and frontend are running, then execute the Cypress tests.
    ```bash
    cd e2e
    npm install
    npx cypress open
    ```

## 5. Development Conventions

- **Configuration:** Project configuration is managed in `application.yml` for the backend and `angular.json` for the frontend. The backend uses Spring profiles (`dev` for MySQL, `dev-h2` for in-memory).
- **API Style:** The backend exposes a RESTful API for synchronous operations and uses STOMP over WebSockets for asynchronous, real-time updates.
- **Code Style:** The codebase is generally consistent with standard Java and TypeScript conventions. Adherence to existing patterns is expected.
- **Dependency Management:** Backend dependencies are managed in `backend/pom.xml`. Frontend dependencies are in `frontend/package.json`.
