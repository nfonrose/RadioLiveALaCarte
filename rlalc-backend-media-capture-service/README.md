
# rlalc-backend-media-capture-service




## Role

Captures audio segments from radio broadcasts using tools like `ffmpeg`. 
It records audio streams, handles network buffering, and stores raw segments in the datastore.




## Execution

Runs in batch mode, capturing media as defined in its 'rlalc-backend-media-capture-service.conf' configuration file.

### Dev environment via docker

```bash
gradle prtRunViaDocker
```

```bash
cd ${RLAC_PROJECT_BASEDIR}
cd ./rlalc-backend-media-capture-service
docker compose \
    -f ./deploy/docker/docker-composition_rlalc-backend-media-capture-service.yml \
    --env-file ./deploy/docker/parameters.env \
    up --build
```

### Observability

Cf: https://chatgpt.com/g/g-p-68ffa9a764608191b25d5d631ec17c1a-groovymorningfm/shared/c/69135d15-7a00-8328-8be2-015e7e2f8b22?owner_user_id=user-VmnxBUs5FJBucjgbj1hLzyHA

OpenTelemetry traces can be pushed by using the OTEL agent
```Bash
java \
-javaagent:/path/to/opentelemetry-javaagent.jar \
-Dotel.service.name=my-service \
-Dotel.exporter.otlp.endpoint=http://<signoz-otel-collector>:4317 \
-jar myapp.jar
```

The agent is included in the Docker image. It can be downloaded via 
```Bash
curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
```