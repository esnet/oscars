# TESTING

Testing suite(s) for `oscars/`

## Back-end (JUnit, Cucumber)

Run the following docker compose service to run tests on the oscars-backend.

```shell
docker compose build --progress=plain --no-cache oscars-backend-test
```

Alternatively, we can also run `docker build` directly by using the following command:

```shell
docker build -t oscars-backend --progress=plain --no-cache --target=test --build-arg JAVA_OPTS="-XX:UseSVE=0" --build-arg MAVEN_OPTS="-XX:UseSVE=0" -f ../oscars/deploy/devel/backend.dockerfile .
```