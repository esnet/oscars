FROM node:19-alpine as dev-builder
WORKDIR /app
ENV PATH /app/node_modules/.bin:$PATH
COPY package.json package-lock.json ./
COPY webpack-dev.config.js tsconfig.json ./
COPY .babelrc .eslintrc ./
RUN npm install
ENV NODE_OPTIONS=--openssl-legacy-provider
CMD ["npm", "run", "start"]