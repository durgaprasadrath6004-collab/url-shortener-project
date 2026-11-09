# Stage 1: Build React App
FROM node:20-alpine AS build

WORKDIR /app

# Copy package descriptors
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy application source
COPY . .

# Build arguments for frontend environment
ARG VITE_BACKEND_URL=http://localhost:8080
ARG VITE_REACT_FRONT_END_URL=http://localhost:3000
ARG VITE_REACT_SUBDOMAIN=http://url.localhost:3000

ENV VITE_BACKEND_URL=$VITE_BACKEND_URL
ENV VITE_REACT_FRONT_END_URL=$VITE_REACT_FRONT_END_URL
ENV VITE_REACT_SUBDOMAIN=$VITE_REACT_SUBDOMAIN

# Build the production bundle
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:alpine

# Remove default nginx site
RUN rm -rf /usr/share/nginx/html/*

# Copy built assets
COPY --from=build /app/dist /usr/share/nginx/html

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

