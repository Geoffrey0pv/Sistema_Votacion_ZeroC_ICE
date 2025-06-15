#!/bin/bash

# Script para compilar y ejecutar TestConsultaMesa
# Autor: Sistema de Votación ZeroC ICE
# Versión: 2.0

set -e  # Salir si hay algún error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para mostrar mensajes con colores
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Función para verificar prerequisitos
check_prerequisites() {
    print_status "Verificando prerequisitos..."
    
    # Verificar Java
    if ! command -v java &> /dev/null; then
        print_error "Java no está instalado"
        exit 1
    fi
    
    # Verificar javac
    if ! command -v javac &> /dev/null; then
        print_error "javac no está instalado"
        exit 1
    fi
    
    # Verificar Gradle
    if ! command -v gradle &> /dev/null; then
        print_error "Gradle no está instalado"
        exit 1
    fi
    
    # Verificar que el proyecto esté compilado
    if [ ! -f "servidorNacional/build/libs/servidorNacional.jar" ]; then
        print_warning "JAR del servidor nacional no encontrado. Compilando proyecto..."
        gradle build
    fi
    
    print_success "Todos los prerequisitos están disponibles"
}

# Función para compilar el test
compile_test() {
    print_status "Compilando TestConsultaMesa..."
    
    # Crear directorio para clases compiladas si no existe
    mkdir -p build/test-classes
    
    # Definir classpath con todas las dependencias necesarias
    CLASSPATH="servidorNacional/build/libs/servidorNacional.jar"
    CLASSPATH="$CLASSPATH:servidorNacional/build/generated/slice/main/java"
    CLASSPATH="$CLASSPATH:servidorNacional/build/classes/java/main"
    
    # Agregar dependencias de Gradle
    for jar in ~/.gradle/caches/modules-2/files-2.1/com.zeroc/ice/*/*/*.jar; do
        if [ -f "$jar" ]; then
            CLASSPATH="$CLASSPATH:$jar"
        fi
    done
    
    # Agregar PostgreSQL driver
    for jar in ~/.gradle/caches/modules-2/files-2.1/org.postgresql/postgresql/*/*/*.jar; do
        if [ -f "$jar" ]; then
            CLASSPATH="$CLASSPATH:$jar"
        fi
    done
    
    # Compilar el test
    javac -cp "$CLASSPATH" -d build/test-classes TestConsultaMesa.java
    
    if [ $? -eq 0 ]; then
        print_success "TestConsultaMesa compilado exitosamente"
    else
        print_error "Error al compilar TestConsultaMesa"
        exit 1
    fi
}

# Función para ejecutar el test
run_test() {
    print_status "Ejecutando TestConsultaMesa..."
    
    # Definir classpath para ejecución
    CLASSPATH="build/test-classes"
    CLASSPATH="$CLASSPATH:servidorNacional/build/libs/servidorNacional.jar"
    CLASSPATH="$CLASSPATH:servidorNacional/build/generated/slice/main/java"
    CLASSPATH="$CLASSPATH:servidorNacional/build/classes/java/main"
    
    # Agregar dependencias de Gradle
    for jar in ~/.gradle/caches/modules-2/files-2.1/com.zeroc/ice/*/*/*.jar; do
        if [ -f "$jar" ]; then
            CLASSPATH="$CLASSPATH:$jar"
        fi
    done
    
    # Agregar PostgreSQL driver
    for jar in ~/.gradle/caches/modules-2/files-2.1/org.postgresql/postgresql/*/*/*.jar; do
        if [ -f "$jar" ]; then
            CLASSPATH="$CLASSPATH:$jar"
        fi
    done
    
    # Ejecutar el test
    java -cp "$CLASSPATH" TestConsultaMesa "$@"
}

# Función para limpiar archivos temporales
cleanup() {
    if [ "$1" = "--clean" ]; then
        print_status "Limpiando archivos temporales..."
        rm -rf build/test-classes
        print_success "Limpieza completada"
    fi
}

# Función para mostrar ayuda
show_help() {
    echo "Uso: $0 [opciones] [argumentos_para_test]"
    echo ""
    echo "Opciones:"
    echo "  --help          Mostrar esta ayuda"
    echo "  --clean         Limpiar archivos temporales después de ejecutar"
    echo "  --compile-only  Solo compilar, no ejecutar"
    echo "  --run-only      Solo ejecutar (asume que ya está compilado)"
    echo ""
    echo "Ejemplos:"
    echo "  $0                    # Compilar y ejecutar test básico"
    echo "  $0 --clean           # Compilar, ejecutar y limpiar"
    echo "  $0 --compile-only    # Solo compilar"
    echo "  $0 --run-only 12345  # Solo ejecutar con documento 12345"
    echo ""
    echo "Argumentos para el test:"
    echo "  [documento]     Documento específico para consultar"
    echo "  performance     Ejecutar test de rendimiento"
}

# Función principal
main() {
    local compile_only=false
    local run_only=false
    local clean_after=false
    local test_args=()
    
    # Procesar argumentos
    while [[ $# -gt 0 ]]; do
        case $1 in
            --help)
                show_help
                exit 0
                ;;
            --clean)
                clean_after=true
                shift
                ;;
            --compile-only)
                compile_only=true
                shift
                ;;
            --run-only)
                run_only=true
                shift
                ;;
            *)
                test_args+=("$1")
                shift
                ;;
        esac
    done
    
    print_status "=== Test ConsultaMesa - Sistema de Votación ==="
    
    # Verificar prerequisitos
    check_prerequisites
    
    # Compilar si es necesario
    if [ "$run_only" = false ]; then
        compile_test
    fi
    
    # Ejecutar si es necesario
    if [ "$compile_only" = false ]; then
        run_test "${test_args[@]}"
    fi
    
    # Limpiar si se solicita
    if [ "$clean_after" = true ]; then
        cleanup --clean
    fi
    
    print_success "=== Test completado ==="
}

# Ejecutar función principal con todos los argumentos
main "$@" 