#!/bin/bash

# 🗳️ Script de Tests de Votación - Sistema Electoral ZeroC ICE
# Tests que consumen los endpoints/interfaces de votación reales

echo "🗳️ =========================================="
echo "   TESTS DE VOTACIÓN - ENDPOINTS REALES"
echo "=========================================="

# Configuración
SLICE_FILE="../../System.ice"
ICE_JAR="/usr/share/java/ice.jar"
GSON_JAR="/usr/share/java/gson.jar"
JAVA_FILES="TestVotoEndpoint.java TestLoteVotosEndpoint.java"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para logging
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Función para encontrar JAR de ICE
find_ice_jar() {
    local ice_paths=(
        "/usr/share/java/ice.jar"
        "/usr/share/java/Ice.jar"
        "/opt/Ice/lib/ice.jar"
        "/usr/local/lib/ice.jar"
        "$(find /usr -name "ice*.jar" 2>/dev/null | head -1)"
        "$(find /opt -name "ice*.jar" 2>/dev/null | head -1)"
    )
    
    for path in "${ice_paths[@]}"; do
        if [[ -f "$path" ]]; then
            echo "$path"
            return 0
        fi
    done
    
    return 1
}

# Función para encontrar JAR de Gson
find_gson_jar() {
    local gson_paths=(
        "/usr/share/java/gson.jar"
        "/usr/share/java/gson-*.jar"
        "$(find /usr -name "gson*.jar" 2>/dev/null | head -1)"
        "$(find /opt -name "gson*.jar" 2>/dev/null | head -1)"
    )
    
    for path in "${gson_paths[@]}"; do
        if [[ -f "$path" ]]; then
            echo "$path"
            return 0
        fi
    done
    
    return 1
}

# Verificar dependencias
log_info "Verificando dependencias..."

if [[ ! -f "$SLICE_FILE" ]]; then
    log_error "Archivo Slice no encontrado: $SLICE_FILE"
    exit 1
fi

ICE_JAR=$(find_ice_jar)
if [[ -z "$ICE_JAR" ]]; then
    log_error "JAR de ICE no encontrado. Instalar con: sudo apt-get install zeroc-ice-all-dev"
    exit 1
fi

GSON_JAR=$(find_gson_jar)
if [[ -z "$GSON_JAR" ]]; then
    log_error "JAR de Gson no encontrado. Instalar con: sudo apt-get install libgoogle-gson-java"
    exit 1
fi

log_success "ICE JAR encontrado: $ICE_JAR"
log_success "Gson JAR encontrado: $GSON_JAR"

# Limpiar archivos anteriores
log_info "Limpiando archivos anteriores..."
rm -rf Demo/ *.class 2>/dev/null

# Compilar Slice
log_info "Compilando archivo Slice..."
if ! slice2java "$SLICE_FILE"; then
    log_error "Error compilando Slice"
    exit 1
fi
log_success "Slice compilado exitosamente"

# Compilar clases Java
log_info "Compilando tests Java..."
CLASSPATH=".:$ICE_JAR:$GSON_JAR"

if ! javac -cp "$CLASSPATH" Demo/*.java $JAVA_FILES; then
    log_error "Error compilando clases Java"
    exit 1
fi
log_success "Clases Java compiladas"

# Ejecutar tests
echo ""
log_info "🚀 EJECUTANDO TESTS DE VOTACIÓN..."
echo ""

# Test 1: Voto Individual
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log_info "TEST 1: Endpoint de Voto Individual (IRegistrarVoto)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if java -cp "$CLASSPATH" TestVotoEndpoint; then
    log_success "Test de voto individual PASÓ"
    TEST1_RESULT="✅ PASÓ"
else
    log_error "Test de voto individual FALLÓ"
    TEST1_RESULT="❌ FALLÓ"
fi

echo ""

# Test 2: Lote de Votos
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log_info "TEST 2: Endpoint de Lote de Votos (IProcesadorLoteVotos)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if java -cp "$CLASSPATH" TestLoteVotosEndpoint; then
    log_success "Test de lote de votos PASÓ"
    TEST2_RESULT="✅ PASÓ"
else
    log_error "Test de lote de votos FALLÓ"
    TEST2_RESULT="❌ FALLÓ"
fi

# Resumen final
echo ""
echo "🗳️ =========================================="
echo "           RESUMEN DE TESTS"
echo "=========================================="
echo "Test 1 - Voto Individual:     $TEST1_RESULT"
echo "Test 2 - Lote de Votos:       $TEST2_RESULT"
echo "=========================================="

# Limpiar archivos temporales
log_info "Limpiando archivos temporales..."
rm -rf Demo/ *.class 2>/dev/null

if [[ "$TEST1_RESULT" == *"✅"* && "$TEST2_RESULT" == *"✅"* ]]; then
    log_success "🎉 TODOS LOS TESTS PASARON!"
    exit 0
else
    log_error "❌ ALGUNOS TESTS FALLARON"
    exit 1
fi 