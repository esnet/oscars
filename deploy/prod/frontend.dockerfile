FROM wharf.es.net/dockerhub-proxy/library/node:19-alpine as builder

WORKDIR /app
ENV PATH /app/node_modules/.bin:$PATH
COPY package.json package-lock.json tsconfig.json .babelrc .eslintrc  ./
COPY webpack.config.js ./

RUN npm install
ENV NODE_OPTIONS=--openssl-legacy-provider

COPY src ./src
COPY public ./public
RUN npm run build

FROM wharf.es.net/dockerhub-proxy/library/nginx:alpine
COPY --from=builder /app/build /usr/share/nginx/html

EXPOSE 80 443
CMD ["nginx", "-g", "daemon off;"]

