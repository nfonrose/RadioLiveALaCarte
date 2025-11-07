
# Deployment




## Deployment for **PROD** environments

### Docker compose

This directory contains deployment artefacts (docker compositions, configuration files, ...) for PROD environments.

The recommended way to deploy RLALC with Docker compose is to:
 - copy the `/deploy/docker` folder into a "vendor" folder in your target environment
 - create, if need, an `overrides` folder with a `docker-compose-rlalc-full-overrides.yml`
 - create your own `parameters.env` file based on the `docker/parameters.env-example` provided

Start the composition with
```bash
docker compose \
    --env-file ${YOUR_BASE_PATH}/prt-rlalc-groovymorningfm/parameters.env \
    -f         ${YOUR_BASE_PATH}/prt-rlalc-groovymorningfm/vendor/docker-compose-rlalc-full.yml \
    -f         ${YOUR_BASE_PATH}/prt-rlalc-groovymorningfm/overrides/docker-compose-rlalc-full-overrides.yml \
    up -d
```


## Deployment for **Dev** activities

### Docker compose

Each component has their own `deploy` folder with some artefact to easily Docker deploy the component in isolation.
