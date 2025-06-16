# 🧪 Test de ReplicaInfo

Este archivo contiene el test para probar el llamado a la réplica y verificar la funcionalidad del servicio `ReplicaInfo`.

## 📋 Descripción

El `ReplicaInfoTest` es un cliente de prueba que se conecta al servicio `ReplicaInfo` del servidor nacional y ejecuta una serie de pruebas para verificar:

- ✅ Obtención del puerto de ejecución
- ✅ Obtención del endpoint completo
- ✅ Obtención del ID de la réplica
- ✅ Verificación del estado activo
- ✅ Obtención del tiempo de actividad
- ✅ Obtención de información completa de la réplica
- ✅ Pruebas de conectividad múltiple

## 🚀 Cómo ejecutar las pruebas

### Opción 1: Usar el script automatizado (Recomendado)

Desde el directorio raíz del proyecto:

```bash
./test-replica.sh
```

### Opción 2: Ejecución manual

1. **Compilar el componente test:**
   ```bash
   JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ./gradlew :test:compileJava --no-daemon
   ```

2. **Configurar el classpath:**
   ```bash
   CLASSPATH="test/build/classes/java/main:servidorNacional/build/generated-src"
   # Agregar librerías ICE y PostgreSQL según sea necesario
   ```

3. **Ejecutar el test:**
   ```bash
   java -cp "$CLASSPATH" test.ReplicaInfoTest
   ```

## 📋 Prerrequisitos

1. **Servidor ejecutándose:** El servidor nacional debe estar ejecutándose en `localhost:9090`
2. **Java 11:** Configurado correctamente
3. **Compilación exitosa:** El componente test debe compilar sin errores
4. **Clases ICE generadas:** Las clases ICE deben estar generadas en `servidorNacional/build/generated-src`

## 🔧 Configuración

El test se conecta por defecto a:
- **Host:** localhost
- **Puerto:** 9090
- **Servicio:** ReplicaInfo

Para cambiar la configuración, modifica la línea en `ReplicaInfoTest.java`:
```java
ObjectPrx base = communicator.stringToProxy("ReplicaInfo:tcp -h localhost -p 9090");
```

## 📊 Salida esperada

```
🧪 ===== TEST DE REPLICA INFO =====
   🔧 Iniciando pruebas del servicio ReplicaInfo...
✅ Conexión establecida con ReplicaInfo

🔌 TEST: Obtener Puerto de Ejecución
   ✅ Puerto obtenido: 9090
   ✅ Puerto válido

📡 TEST: Obtener Endpoint
   ✅ Endpoint obtenido: tcp -h localhost -p 9090
   ✅ Endpoint válido
   ✅ Formato de endpoint correcto

🆔 TEST: Obtener ID de Réplica
   ✅ Réplica ID obtenido: 1
   ✅ Réplica ID válido

✅ TEST: Verificar Estado Activo
   ✅ Estado obtenido: ACTIVA
   ✅ Réplica funcionando correctamente

⏱️ TEST: Obtener Tiempo de Actividad
   ✅ Tiempo de actividad: 45230ms
   📊 Tiempo legible: 45s
   ✅ Tiempo de actividad válido

📊 TEST: Obtener Información Completa
   ✅ Información completa obtenida:
   📍 Réplica ID: 1
   🌐 Node ID: nodeNacional1
   🔌 Puerto: 9090
   🏠 Host: localhost
   📡 Endpoint: tcp -h localhost -p 9090
   ✅ Activa: SÍ
   ⏰ Tiempo inicio: 1703123456789
   📈 Métricas:
      💾 Uso memoria: 45.67%
      🖥️  Uso CPU: 0.00%
      🌐 Uso red: 0.00%
      📊 Timestamp: 1703123501019
   ✅ Toda la información es válida

🔄 TEST: Conectividad Múltiple
   ✅ Intento 1: Éxito - ID: 1
   ✅ Intento 2: Éxito - ID: 1
   ✅ Intento 3: Éxito - ID: 1
   ✅ Intento 4: Éxito - ID: 1
   ✅ Intento 5: Éxito - ID: 1
   📊 Resultado: 5/5 (100.0% éxito)
   ✅ Conectividad excelente

🎉 ===== TODAS LAS PRUEBAS COMPLETADAS =====
```

## ❌ Solución de problemas

### Error de conexión
```
❌ No se pudo conectar al servicio ReplicaInfo
   💡 Asegúrate de que el servidor esté ejecutándose en puerto 9090
```
**Solución:** Verificar que el servidor nacional esté ejecutándose.

### Error de compilación
```
❌ Error durante las pruebas: ClassNotFoundException
```
**Solución:** Verificar que el classpath incluya todas las librerías necesarias y que las clases ICE estén generadas.

### Error de ICE
```
❌ Error: Ice.ConnectionRefusedException
```
**Solución:** Verificar que el puerto 9090 esté disponible y el servidor esté escuchando.

## 📝 Notas

- El test utiliza las clases generadas automáticamente por ICE
- Las métricas mostradas son básicas y pueden expandirse según necesidades
- El test es no destructivo y solo consulta información
- Se puede ejecutar múltiples veces sin afectar el servidor
- El test está ubicado en el componente `test` del proyecto, no en `servidorNacional` 