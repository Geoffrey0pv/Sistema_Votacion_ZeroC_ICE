#!/bin/bash

# Script para exportar ciudadanos procesando cada departamento por separado de forma simultánea
# Genera un archivo individual por departamento con información completa

echo "[INFO] === Exportación Paralela por Departamentos ==="
echo "[INFO] Cada departamento se procesa simultáneamente en su propio hilo"
echo "[INFO] Se genera un archivo individual por departamento"
echo

# Verificar que se proporcionaron argumentos
if [ $# -eq 0 ]; then
    echo "[ERROR] Debe proporcionar al menos un departamento"
    echo "[INFO] Uso: $0 \"DEPARTAMENTO1\" \"DEPARTAMENTO2\" ..."
    echo "[INFO] Ejemplo: $0 \"VALLE DEL CAUCA\" \"QUINDÍO\" \"GUAVIARE\""
    exit 1
fi

echo "[INFO] Departamentos a procesar: $*"
echo

# Verificar prerequisitos
echo "[INFO] Verificando prerequisitos..."

if ! command -v slice2java &> /dev/null; then
    echo "[ERROR] slice2java no encontrado. Instale ZeroC Ice."
    exit 1
fi

if ! command -v javac &> /dev/null; then
    echo "[ERROR] javac no encontrado. Instale Java JDK."
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo "[ERROR] java no encontrado. Instale Java JRE."
    exit 1
fi

echo "[SUCCESS] Todos los prerequisitos están disponibles"

# Regenerar clases ICE (necesario por los cambios en CiudadanoInfo)
echo "[INFO] Regenerando clases ICE con campos adicionales..."
slice2java --output-dir . System.ice
if [ $? -ne 0 ]; then
    echo "[ERROR] Error generando clases ICE"
    exit 1
fi

# Buscar librerías ICE
echo "[INFO] Buscando librerías ICE..."
ICE_JARS=""

# Buscar en Maven repository
if [ -d "$HOME/.m2/repository" ]; then
    ICE_JAR=$(find "$HOME/.m2/repository" -name "ice-*.jar" | head -1)
    if [ -n "$ICE_JAR" ]; then
        ICE_JARS="$ICE_JAR"
        echo "[INFO] Usando librerías Maven: $ICE_JARS"
    fi
fi

# Si no se encontró en Maven, buscar en el proyecto
if [ -z "$ICE_JARS" ] && [ -f "servidorNacional/build/libs/servidorNacional.jar" ]; then
    ICE_JARS="servidorNacional/build/libs/servidorNacional.jar"
    echo "[INFO] Usando JAR del proyecto: $ICE_JARS"
fi

# Buscar en ubicaciones estándar
if [ -z "$ICE_JARS" ]; then
    for path in "/usr/share/java/ice.jar" "/usr/local/share/java/ice.jar"; do
        if [ -f "$path" ]; then
            ICE_JARS="$path"
            echo "[INFO] Usando ICE estándar: $ICE_JARS"
            break
        fi
    done
fi

# Compilar
echo "[INFO] Compilando exportador paralelo..."

if [ -n "$ICE_JARS" ]; then
    echo "[INFO] Compilando clases ICE con librerías..."
    javac -cp "$ICE_JARS" Demo/*.java
    if [ $? -ne 0 ]; then
        echo "[ERROR] Error compilando clases ICE"
        exit 1
    fi
    
    echo "[INFO] Compilando test exportador paralelo..."
    javac -cp ".:$ICE_JARS" TestExportarCiudadanosParalelo.java
    if [ $? -ne 0 ]; then
        echo "[ERROR] Error compilando TestExportarCiudadanosParalelo"
        exit 1
    fi
else
    echo "[INFO] Compilando sin librerías externas..."
    javac Demo/*.java
    javac -cp "." TestExportarCiudadanosParalelo.java
    if [ $? -ne 0 ]; then
        echo "[ERROR] Error en compilación"
        exit 1
    fi
fi

echo "[SUCCESS] Compilación exitosa"

# Mostrar información antes de ejecutar
echo
echo "[INFO] ===== INFORMACIÓN DE EXPORTACIÓN PARALELA ====="
echo "[INFO] • Cada departamento se procesará en un hilo separado (máximo 3 simultáneos)"
echo "[INFO] • Se generará un archivo individual por departamento"
echo "[INFO] • Formato: ID|DOCUMENTO|NOMBRE|APELLIDO|MESA|PUESTO|MUNICIPIO|DEPARTAMENTO"
echo "[INFO] • Los archivos incluirán estadísticas individuales"
echo "[INFO] • El procesamiento paralelo es más eficiente para múltiples departamentos"
echo "[INFO] =============================================================="
echo

# Ejecutar
echo "[INFO] Iniciando exportación paralela..."
echo

if [ -n "$ICE_JARS" ]; then
    java -cp ".:$ICE_JARS" TestExportarCiudadanosParalelo "$@"
else
    java -cp "." TestExportarCiudadanosParalelo "$@"
fi

exit_code=$?

echo
if [ $exit_code -eq 0 ]; then
    echo "[SUCCESS] Exportación paralela completada exitosamente"
    
    # Mostrar archivos generados
    echo "[INFO] Archivos generados en este directorio:"
    ls -lh ciudadanos_*.txt 2>/dev/null | while read line; do
        echo "  $line"
    done
    
    # Contar líneas en cada archivo (excluyendo comentarios)
    echo
    echo "[INFO] Resumen de registros por archivo:"
    for archivo in ciudadanos_*.txt; do
        if [ -f "$archivo" ]; then
            registros=$(grep -v '^#' "$archivo" | grep -v '^$' | wc -l)
            echo "  📄 $archivo: $registros registros"
        fi
    done
    
else
    echo "[ERROR] Exportación paralela falló con código: $exit_code"
fi

echo
echo "[INFO] Script completado" 