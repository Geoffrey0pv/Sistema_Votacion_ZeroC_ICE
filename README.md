# Sistema_Votacion_ZeroC_ICE

## **Setup aplicación gradle usando el RCP ZeroC ICE**

*1. Sobre el directorio raiz ejecutar primero este comando para builder todo el proyecto tipo gradle:*

```bash

Para los que no usan el wrapper

gradle build // para la primera vez que se ejecuta
gradle clean build // para limpiar archivos de buildeo anteriores
```

*2. En caso de error en el paso anterior por incompatibilidad de versiones superiores con el gradle, ejecutamos el siguiente comando para envolverlo en la versión anterior compatible*

```bash
./gradlew wrapper --gradle-version 6.6

// y luego ejecutamos

./gradlew build

// o

./gradlew clean build
```

*3. Buildeamos especificamente el subsitema que querramos ejecutar, el cuál debe estar incluido primeramente en el settings.gradle de esta forma por el nombre del directorio del proyecto*

```bash
  rootProject.name = 'Sistema_Votacion'
  include('ServidorRegional')
  include('MesaVotacion')  
```
y ejecutamos los siguiente comandos para general el build con el archivo .jar de los subsitema que queremos ejecutar en especifico:

```bash
./gradlew :mesaVotacion:build
./gradlew :servidorRegional:build

// para los que no usan el wrapper usar

./gradle :mesaVotacion:build
./gradle :servidorRegional:build
```

*4. Para levantar el servidor del broker-proxy que provee ZEROC ICE a través del servicio de icegrid, debemos pararnos en el directorio "config" y ejecutar*

> ⚠️ **Nota:** Asegurarse que los nodos involucrados en el patrón broker esten bien configurados en el application.xml dentro deL config
> para esto deberá verificar que el adaptador definido en el código de cada nodo, sea identico al definido en el .xml
> además verifique que su ruta hacia el archivo de compilación empaquetado .jar sea correcto


```bash
EN WINDOWS
icegridregistry.exe --Ice.Config=grid.config

EN LINUX
icegridregistry --Ice.Config=grid.config
```

*4.1 Para levantar el nodo del broker-proxy que provee ZEROC ICE a través del servicio de icegrid, debemos pararnos en el directorio .config y ejecutar en una nueva terminal diferente al registry*

```bash
icegridnode --Ice.Config=node.config 
```

*4.2 Ahora abrimos una nueva terminal y entramos con:*

```bash
icegridadmin
```

> ⚠️ **Nota:** Cada servidor debe registrarse en el application.xml y debe cargarse
> usando el siguiente comando dentro de la terminal que activo el icegridadmin

```bash
Application add application.xml
```

*5. Ubicados en el directorio raiz ejecutamos el siguiente comando en una terminal diferente, esto es para correr en el local los diferentes clientes del sistema.*

```bash
java -jar mesaVotacion/build/libs/MesaVotacion.jar  
```

*6. Desplegar en computadores dentro de una red privada (VPN) de una organización.*



