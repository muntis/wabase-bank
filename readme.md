# Wabase bank sample application 

This project demonstrates hot to use wabase framework and goes in tandem with the [wabase wiki](https://github.com/muntis/wabase-wiki).

## Setting up the environment

Before we start off with wabase, let's set up the test environment, so that you can try the features yourself as you go.
We are going to use Swagger to send different HTTP requests to a local wabase server and see what the response is.

### List of required software to run the project:

Project is dockerized, so you need only docker and docker compose installed.

to install docker and docker compose on ubuntu:

```shell
sudo apt-get remove docker docker.io containerd runc

sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$UBUNTU_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update


sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo apt-get install -y docker-compose-plugin

sudo groupadd docker

sudo usermod -aG docker $USER
```


## Running the project

Copy env file:

```shell
cp .env.sample .env
```

Replace the values in the .env file with your own values.

```env
GROUP_ID=1000
USER_ID=1000
```

Build docker images

```shell
docker compose build --no-cache
```

To start docker:

```shell
  docker compose up
```

To stop docker:

```shell
  docker compose down
```


## Usage

Open swagger api at: 

http://localhost:8090/api-docs/ui/index.html

For login use:

```json
{
  "username": "admin@localhost",
  "password": "admin"
}
```

To run automated tests:

```shell
docker compose exec instance-api sbt test it:test
```

To run single test:

```shell
docker compose exec instance-api sbt "it:testOnly uniso.app.DataSpecs"
```

# TODO 

* Must have case where have multiple lists filtered by field and still savable:
```
- nokrisnu_novērojumi * [ v_atm.code = 'nokrisnu_novērojumi'] novērojumi:
   field db: readonly
- atmosferas_novērojumi * [ v_atm.code = 'atmosferas_novērojumi'] novērojumi :
   field db: readonly
- all_novērojumi * novērojumi:
   field api: excluded
save:
 - build cursors // varbūt vajag varbūt ne
 - all_novērojumi = :nokrisnu_novērojumi ++ :atmosferas_novērojumi
 - save this
```

* Licencing. SWAGGER UI is licensed under Apache 2.0. Need to add this to the project.
* 