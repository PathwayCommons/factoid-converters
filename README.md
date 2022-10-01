# factoid-converters

A java web server to convert [factoid](https://github.com/PathwayCommons/factoid/) documents to BioPAX and SBGN models. 

![Build](https://github.com/PathwayCommons/factoid-converters/actions/workflows/Build.yml/badge.svg?branch=master)

## Build

```commandline
./gradlew clean
./gradlew build
```

## Run

```commandline
java -jar build/libs/factoid-converters*.jar --server.port=8080
```

RESTful API (Swagger docs):

Once the app is built and running, 
the auto-generated documentation is available at 
`http://localhost:8080/convert/`

## Docker
You can deploy the server to a docker container by following the steps below  
(`<PORT>` - actual port number where the server will run). 

```commandline
docker build . -t pathwaycommons/factoid-converters
docker run -it --rm --name factoid-converters -p <PORT>:8080 pathwaycommons/factoid-converters 
```

Optionally, a member of 'pathwaycommons' group can now push (upload) the latest Docker image there:

```commandline
docker login
docker push pathwaycommons/factoid-converters

```  

So, other users could skip building from sources and simply run the app:
```commandline
docker pull
docker run -p <PORT>:8080 -t pathwaycommons/factoid-converters
```

(you can `Ctrl-c` and quit the console; the container is still there running; check with `docker ps`)

## Example queries

Using cUrl tool:

```commandline
cd src/test/resources
curl -X POST -H 'Content-Type: application/json' -d @test2.json "http://localhost:8080/convert/v2/json-to-biopax"
```

Using a Node.js client:

```js
let url = 'http://localhost:8080/convert/v2/json-to-biopax';
let content = fs.readFileSync('input/templates.json', 'utf8');
Promise.try( () => fetch( url, {
        method: 'POST',
        body:    content,
        headers: { 'Content-Type': 'application/json' }
    } ) )
    .then( res => res.text() )
    .then( res => {
      // we have biopax result here as a String
      console.log(res);
    } );
```
