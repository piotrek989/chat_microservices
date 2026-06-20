# Pobiera oficjalny obraz Node.js w wersji 20 oparty o Alpine Linux
# "AS build" nadaje nazwę etapowi budowania
# Dzięki temu później można kopiować pliki z tego etapu
FROM node:20-alpine AS build

# Ustawia katalog roboczy wewnątrz kontenera na /app Wszystkie kolejne komendy będą wykonywane w tym folderze
WORKDIR /app

# Kopiuje package.json z Twojego projektu do kontenera "./" oznacza aktualny katalog roboczy (/app)
COPY package.json ./

# Instaluje zależności npm z package.json
RUN npm install

# Kopiuje cały projekt do kontenera
# "." po lewej = obecny folder na komputerze
# "." po prawej = obecny folder w kontenerze (/app)
COPY . .

# Buduje aplikację produkcyjną np. dla Vite/React stworzy folder dist
RUN npm run build

# DRUGI ETAP — finalny lekki obraz do uruchomienia aplikacji

# Pobiera lekki obraz Nginx oparty o Alpine Linux
FROM nginx:1.27-alpine

# Kopiuje własną konfigurację nginx, Nadpisuje domyślną konfigurację serwera
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Kopiuje gotowe pliki aplikacji z etapu "build" /app/dist = folder wygenerowany przez npm run build /usr/share/nginx/html = miejsce skąd nginx serwuje pliki
COPY --from=build /app/dist /usr/share/nginx/html

# Informacja, że kontener nasłuchuje na porcie 80
EXPOSE 80

# Uruchamia nginx, "daemon off;" sprawia, że nginx działa na pierwszym planie dzięki temu kontener się nie zamyka
CMD ["nginx", "-g", "daemon off;"]