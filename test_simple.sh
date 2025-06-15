#!/bin/bash

echo "🗳️ Test Simple de Votación"
echo "=========================="

# Configuración
SLICE_FILE="../../System.ice"
ICE_JAR="/usr/share/maven-repo/com/zeroc/ice/3.7/ice-3.7.jar"
GSON_JAR="/usr/share/java/gson-2.8.8.jar"

echo "📁 Verificando archivos..."
echo "   Slice: $SLICE_FILE"
echo "   ICE JAR: $ICE_JAR"
echo "   Gson JAR: $GSON_JAR"

if [[ ! -f "$SLICE_FILE" ]]; then
    echo "❌ Slice file no encontrado"
    exit 1
fi

if [[ ! -f "$ICE_JAR" ]]; then
    echo "❌ ICE JAR no encontrado"
    exit 1
fi

if [[ ! -f "$GSON_JAR" ]]; then
    echo "❌ Gson JAR no encontrado"
    exit 1
fi

echo "✅ Todos los archivos encontrados"

# Limpiar
echo "🧹 Limpiando archivos anteriores..."
rm -rf Demo/ *.class 2>/dev/null

# Compilar Slice
echo "🔨 Compilando Slice..."
if slice2java "$SLICE_FILE"; then
    echo "✅ Slice compilado"
else
    echo "❌ Error compilando Slice"
    exit 1
fi

# Compilar Java
echo "🔨 Compilando Java..."
CLASSPATH=".:$ICE_JAR:$GSON_JAR"

if javac -cp "$CLASSPATH" Demo/*.java TestVotoEndpoint.java TestLoteVotosEndpoint.java; then
    echo "✅ Java compilado"
else
    echo "❌ Error compilando Java"
    exit 1
fi

echo ""
echo "🚀 EJECUTANDO TESTS..."
echo ""

# Test 1: Solo mostrar que se puede conectar
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔍 TEST 1: Conexión a Servidor Regional (puerto 9091)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

java -cp "$CLASSPATH" TestVotoEndpoint
TEST1_EXIT=$?

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔍 TEST 2: Conexión a Servidor Nacional (puerto 9090)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

java -cp "$CLASSPATH" TestLoteVotosEndpoint
TEST2_EXIT=$?

echo ""
echo "📊 RESUMEN:"
if [[ $TEST1_EXIT -eq 0 ]]; then
    echo "   Test 1 (Voto Individual): ✅ PASÓ"
else
    echo "   Test 1 (Voto Individual): ❌ FALLÓ"
fi

if [[ $TEST2_EXIT -eq 0 ]]; then
    echo "   Test 2 (Lote de Votos): ✅ PASÓ"
else
    echo "   Test 2 (Lote de Votos): ❌ FALLÓ"
fi

# Limpiar
echo "🧹 Limpiando..."
rm -rf Demo/ *.class 2>/dev/null

echo "�� Test completado" 