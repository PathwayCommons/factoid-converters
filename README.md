# Factoid Converters

A java web server to convert [factoid](https://github.com/PathwayCommons/factoid/) documents to BioPAX and SBGN models. 

![Build](https://github.com/PathwayCommons/factoid-converters/actions/workflows/Build.yml/badge.svg?branch=custom_intn)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/eba0e725f1bd4d45b4c15b45b8c13488)](https://www.codacy.com/app/IgorRodchenkov/factoid-converters?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=PathwayCommons/factoid-converters&amp;utm_campaign=Badge_Grade)

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

## Input

[Here](https://github.com/PathwayCommons/factoid-converters/blob/custom_intn/src/test/resources/test2.json) is an example data file that factoid converters accepts. Currently accepted interaction types are ``Expression Regulation``, ``Molecular Interaction``, ``Protein Controls State``, ``Custom Interaction`` and ``Other Interaction``.

Note that for ``Custom Interaction`` type the ``scriptPath`` propery must be set. ``scriptPath`` property specifies the path of a Groovy file. The Groovy file is expected to include a Groovy class where ``addIntn()`` methods presents and controls how the interaction will be added to the biopax model. The Groovy script can mainly utilize the functions exposed [here](https://github.com/PathwayCommons/factoid-converters/blob/custom_intn/src/main/java/factoid/model/CustomizableModel.java) and [here](https://github.com/PathwayCommons/factoid-converters/blob/custom_intn/src/main/java/factoid/util/GsonUtil.java). The remaining parameters that a ``Custom Interaction`` is supposed to have (excluding ``type`` property) are determined by the Groovy file that user specifies.

## Using Factoid Converters API

Adding Factoid Converters as a dependency:

See https://jitpack.io/#pathwaycommons/factoid-converters

An example usage of API to convert a model stored in a json file into Biopax:

```java
Gson gson = new Gson();
JsonReader reader = new JsonReader(new FileReader(getClass()
  .getResource("/test2.json").getFile()));
JsonObject template = gson.fromJson(reader, JsonObject.class);
FactoidToBiopax converter = new FactoidToBiopax();
converter.addToModel(template);
String res = converter.convertToBiopax();
```

A demo project that utilizes Factoid Converters api is available [here](https://github.com/metincansiper/custom-converter-demo).


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
