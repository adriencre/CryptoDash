#!/bin/bash

# Script de lancement du projet CryptoDash

# Couleurs pour le terminal
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Lancement de CryptoDash ===${NC}"

# 1. Démarrage de la base de données
echo -e "
${GREEN}[1/3] Démarrage de la base de données (Docker)...${NC}"
docker compose up -d
if [ $? -ne 0 ]; then
    echo -e "${RED}Erreur lors du démarrage de Docker. Assurez-vous que Docker est lancé.${NC}"
    exit 1
fi

# 2. Lancement du Backend (en arrière-plan)
echo -e "
${GREEN}[2/3] Lancement du Backend (Spring Boot)...${NC}"
cd backend
mvn spring-boot:run > ../backend.log 2>&1 &
BACKEND_PID=$!
cd ..
echo -e "Backend lancé (PID: $BACKEND_PID). Logs disponibles dans backend.log"

# 3. Lancement du Frontend (en arrière-plan)
echo -e "
${GREEN}[3/3] Lancement du Frontend (Angular)...${NC}"
cd frontend
# On vérifie si node_modules existe, sinon npm install
if [ ! -d "node_modules" ]; then
    echo "Installation des dépendances frontend..."
    npm install
fi
npm start > ../frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..
echo -e "Frontend lancé (PID: $FRONTEND_PID). Logs disponibles dans frontend.log"

echo -e "
${BLUE}=== Application en cours de démarrage ===${NC}"
echo -e "Backend : http://localhost:8080"
echo -e "Frontend: http://localhost:4200"
echo -e "
${RED}Appuyez sur [CTRL+C] pour arrêter tous les services.${NC}"

# Fonction pour tout arrêter proprement
cleanup() {
    echo -e "

${RED}Arrêt des services...${NC}"
    kill $BACKEND_PID
    kill $FRONTEND_PID
    docker compose stop
    echo -e "${GREEN}Services arrêtés.${NC}"
    exit
}

# Attrape le signal d'arrêt (CTRL+C)
trap cleanup SIGINT

# Garde le script en vie pour voir les logs ou attendre l'arrêt
tail -f backend.log
