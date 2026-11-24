FROM node:22-alpine AS compilacion

WORKDIR /aplicacion
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=compilacion /aplicacion/dist/frontend-web/browser /usr/share/nginx/html

EXPOSE 80
