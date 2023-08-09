FROM node:18-bullseye-slim as oscars-frontend
WORKDIR /app
ENV PATH /app/node_modules/.bin:$PATH
COPY frontend/package.json  ./
COPY frontend/yarn.lock ./
RUN yarn install --production --pure-lockfile
CMD ["yarn", "run", "start"]