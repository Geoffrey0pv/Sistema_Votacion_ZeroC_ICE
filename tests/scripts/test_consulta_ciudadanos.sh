#!/bin/bash

# Script para probar el servicio de consulta de ciudadanos por departamentos
# Autor: Sistema de Votación ZeroC ICE
# Versión: 1.0

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para imprimir mensajes con colores
print_info() {
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

# Función para mostrar ayuda
show_help() {
    echo "Uso: $0 [OPCIONES] [DEPARTAMENTOS...]"
    echo ""
    echo "Opciones:"
    echo "  --help, -h          Mostrar esta ayuda"
    echo "  --compile, -c       Solo compilar, no ejecutar"
    echo "  --clean             Limpiar archivos compilados"
    echo ""
    echo "Ejemplos:"
    echo "  $0                                    # Ejecutar tests predefinidos"
    echo "  $0 \"VALLE DEL CAUCA\" \"QUINDÍO\"      # Consultar departamentos específicos"
    echo "  $0 \"ANTIOQUIA\"                      # Consultar un solo departamento"
    echo "  $0 --compile                         # Solo compilar"
    echo ""
    echo "Departamentos de ejemplo:"
    echo "  - VALLE DEL CAUCA"
    echo "  - QUINDÍO"
    echo "  - GUAVIARE"
    echo "  - ANTIOQUIA"
    echo "  - BOGOTÁ D.C."
    echo "  - CUNDINAMARCA"
}

# Función para verificar prerequisitos
check_prerequisites() {
    print_info "Verificando prerequisitos..."
    
    # Verificar Java
    if ! command -v java &> /dev/null; then
        print_error "Java no está instalado"
        return 1
    fi
    
    # Verificar Gradle
    if ! command -v ./gradlew &> /dev/null; then
        print_error "Gradle wrapper no encontrado"
        return 1
    fi
    
    # Verificar que el proyecto esté compilado
    if [ ! -f "servidorNacional/build/libs/servidorNacional.jar" ]; then
        print_warning "Proyecto no compilado. Compilando automáticamente..."
        ./gradlew :servidorNacional:build
        if [ $? -ne 0 ]; then
            print_error "Error al compilar el proyecto"
            return 1
        fi
    fi
    
    print_success "Todos los prerequisitos están disponibles"
    return 0
}

# Función para compilar
compile_test() {
    print_info "Compilando TestConsultaCiudadanos..."
    
    # Crear directorio de clases si no existe
    mkdir -p classes
    
    # Verificar que las clases ICE estén disponibles
    if [ ! -d "Demo" ]; then
        print_info "Generando clases ICE..."
        slice2java --output-dir . System.ice
        if [ $? -ne 0 ]; then
            print_error "Error al generar clases ICE"
            return 1
        fi
    fi
    
    # Buscar librerías ICE en el sistema
    ICE_JARS=""
    
    # Buscar en directorios comunes de Maven
    MAVEN_REPO="$HOME/.m2/repository"
    if [ -d "$MAVEN_REPO" ]; then
        ICE_JAR=$(find "$MAVEN_REPO" -name "ice-*.jar" | head -1)
        if [ -n "$ICE_JAR" ]; then
            ICE_JARS="$ICE_JAR"
        fi
    fi
    
    # Si no encontramos ICE en Maven, intentar con el JAR del proyecto compilado
    if [ -z "$ICE_JARS" ] && [ -f "servidorNacional/build/libs/servidorNacional.jar" ]; then
        ICE_JARS="servidorNacional/build/libs/servidorNacional.jar"
    fi
    
    # Si aún no tenemos ICE, usar una ruta por defecto
    if [ -z "$ICE_JARS" ]; then
        # Intentar encontrar ICE en ubicaciones estándar
        for path in /usr/share/java/ice.jar /usr/local/share/java/ice.jar; do
            if [ -f "$path" ]; then
                ICE_JARS="$path"
                break
            fi
        done
    fi
    
    print_info "Usando librerías: ${ICE_JARS:-'ICE integrado en JVM'}"
    
    # Compilar primero las clases ICE
    print_info "Compilando clases ICE..."
    if [ -n "$ICE_JARS" ]; then
        javac -cp "$ICE_JARS" -d classes Demo/*.java
    else
        javac -d classes Demo/*.java
    fi
    
    if [ $? -ne 0 ]; then
        print_error "Error compilando clases ICE"
        return 1
    fi
    
    # Compilar el test
    print_info "Compilando test..."
    if [ -n "$ICE_JARS" ]; then
        javac -cp "$ICE_JARS:classes" -d classes TestConsultaCiudadanos.java
    else
        javac -cp "classes" -d classes TestConsultaCiudadanos.java
    fi
    
    if [ $? -eq 0 ]; then
        print_success "Compilación exitosa"
        return 0
    else
        print_error "Error en compilación del test"
        return 1
    fi
}

# Función para limpiar archivos compilados
clean_files() {
    print_info "Limpiando archivos compilados..."
    rm -rf classes/
    rm -f *.class
    print_success "Archivos limpiados"
}

# Función para ejecutar el test
run_test() {
    local departments=("$@")
    
    print_info "Ejecutando test de consulta de ciudadanos..."
    
    # Buscar librerías ICE (mismo código que en compile_test)
    ICE_JARS=""
    MAVEN_REPO="$HOME/.m2/repository"
    if [ -d "$MAVEN_REPO" ]; then
        ICE_JAR=$(find "$MAVEN_REPO" -name "ice-*.jar" | head -1)
        if [ -n "$ICE_JAR" ]; then
            ICE_JARS="$ICE_JAR"
        fi
    fi
    
    if [ -z "$ICE_JARS" ] && [ -f "servidorNacional/build/libs/servidorNacional.jar" ]; then
        ICE_JARS="servidorNacional/build/libs/servidorNacional.jar"
    fi
    
    if [ -z "$ICE_JARS" ]; then
        for path in /usr/share/java/ice.jar /usr/local/share/java/ice.jar; do
            if [ -f "$path" ]; then
                ICE_JARS="$path"
                break
            fi
        done
    fi
    
    # Ejecutar el test
    if [ ${#departments[@]} -gt 0 ]; then
        print_info "Departamentos especificados: ${departments[*]}"
        if [ -n "$ICE_JARS" ]; then
            java -cp "$ICE_JARS:classes" TestConsultaCiudadanos "${departments[@]}"
        else
            java -cp "classes" TestConsultaCiudadanos "${departments[@]}"
        fi
    else
        print_info "Ejecutando tests predefinidos"
        if [ -n "$ICE_JARS" ]; then
            java -cp "$ICE_JARS:classes" TestConsultaCiudadanos
        else
            java -cp "classes" TestConsultaCiudadanos
        fi
    fi
}

# Función principal
main() {
    local compile_only=false
    local clean_only=false
    local departments=()
    
    # Procesar argumentos
    while [[ $# -gt 0 ]]; do
        case $1 in
            --help|-h)
                show_help
                exit 0
                ;;
            --compile|-c)
                compile_only=true
                shift
                ;;
            --clean)
                clean_only=true
                shift
                ;;
            *)
                departments+=("$1")
                shift
                ;;
        esac
    done
    
    # Ejecutar según opciones
    if [ "$clean_only" = true ]; then
        clean_files
        exit 0
    fi
    
    print_info "=== Test ConsultaCiudadanos - Sistema de Votación ==="
    
    # Verificar prerequisitos
    if ! check_prerequisites; then
        exit 1
    fi
    
    # Compilar
    if ! compile_test; then
        exit 1
    fi
    
    # Si solo compilar, salir
    if [ "$compile_only" = true ]; then
        print_success "Compilación completada"
        exit 0
    fi
    
    # Ejecutar test
    print_info "Iniciando test..."
    echo ""
    
    run_test "${departments[@]}"
    
    local exit_code=$?
    echo ""
    
    if [ $exit_code -eq 0 ]; then
        print_success "Test completado exitosamente"
    else
        print_error "Test falló con código: $exit_code"
    fi
    
    exit $exit_code
}

# Ejecutar función principal con todos los argumentos
main "$@" 