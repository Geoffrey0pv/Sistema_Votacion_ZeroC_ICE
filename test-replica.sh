#!/bin/bash

echo "🧪 ===== SCRIPT DE PRUEBA REPLICA INFO ====="
echo "   🔧 Preparando entorno de pruebas..."

# Configurar JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

# Verificar que el proyecto esté compilado
echo "   📦 Compilando proyecto..."
./gradlew :test:compileJava --no-daemon

if [ $? -ne 0 ]; then
    echo "❌ Error en la compilación. Abortando pruebas."
    exit 1
fi

echo "   ✅ Compilación exitosa"
echo ""

# Configurar classpath
CLASSPATH="test/build/classes/java/main:servidorNacional/build/generated-src"

# Agregar todas las librerías JAR del componente test
for jar in test/build/libs/*.jar; do
    if [ -f "$jar" ]; then
        CLASSPATH="$CLASSPATH:$jar"
    fi
done

# Agregar librerías de Gradle
GRADLE_CACHE="$HOME/.gradle/caches/modules-2/files-2.1"
if [ -d "$GRADLE_CACHE" ]; then
    # Buscar ICE JAR
    ICE_JAR=$(find "$GRADLE_CACHE" -name "ice-*.jar" | head -1)
    if [ -n "$ICE_JAR" ]; then
        CLASSPATH="$CLASSPATH:$ICE_JAR"
    fi
    
    # Buscar PostgreSQL JAR
    POSTGRES_JAR=$(find "$GRADLE_CACHE" -name "postgresql-*.jar" | head -1)
    if [ -n "$POSTGRES_JAR" ]; then
        CLASSPATH="$CLASSPATH:$POSTGRES_JAR"
    fi
fi

echo "🚀 ===== EJECUTANDO PRUEBAS DE REPLICA INFO ====="
echo "   📍 Classpath configurado"
echo "   🔌 Conectando a localhost:9090"
echo "   ⚠️  NOTA: Asegúrate de que el servidor esté ejecutándose"
echo ""

# Ejecutar el test
java -cp "$CLASSPATH" test.ReplicaInfoTest

echo ""
echo "🏁 ===== PRUEBAS COMPLETADAS =====" 