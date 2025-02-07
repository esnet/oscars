FROM node:19-alpine as builder

WORKDIR /app
ENV REACT_APP_VERSION 10
ENV PATH /app/node_modules/.bin:$PATH
COPY package.json package-lock.json tsconfig.json .babelrc .eslintrc  ./
# note this uses webpack-DEV.config.js
COPY webpack-dev.config.js ./
COPY public ./public

# here we rely that src will be mounted as a volume

RUN npm install
ENV NODE_OPTIONS=--openssl-legacy-provider
EXPOSE 3000
# Debugger port, requires --inspect=0.0.0.0 argument when running this node app
EXPOSE 9191

# we count on our volumes at
CMD ["npm", "run", "start", "--", "--inspect=0.0.0.0"]