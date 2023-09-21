FROM node:19-alpine as builder

WORKDIR /app
ENV PATH /app/node_modules/.bin:$PATH
COPY package.json package-lock.json tsconfig.json .babelrc .eslintrc  ./
COPY webpack.config.js ./

RUN npm install
ENV NODE_OPTIONS=--openssl-legacy-provider

COPY src ./src
COPY public ./public
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/build /usr/share/nginx/html

# this does environment var substitutions
COPY ./prod/nginx.conf.template /etc/nginx/conf.d/default.conf.template
COPY ./prod/docker-entrypoint.sh /
RUN chmod 755 docker-entrypoint.sh
ENTRYPOINT ["/docker-entrypoint.sh"]

EXPOSE 80 443
CMD ["nginx", "-g", "daemon off;"]

