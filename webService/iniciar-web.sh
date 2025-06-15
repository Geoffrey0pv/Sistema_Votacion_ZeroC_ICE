#!/bin/bash

echo "🌐 Iniciando Servicio Web de Consulta de Mesa de Votación"
echo "=========================================================="

# Verificar si Java está disponible
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java no está instalado o no está en el PATH"
    exit 1
fi

# Cambiar al directorio del webService
cd "$(dirname "$0")"

echo "📦 Compilando proyecto..."

# Crear directorio de clases si no existe
mkdir -p build/classes

# Compilar con javac
javac src/main/java/*.java -d build/classes

if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa"
    echo ""
    echo "🚀 Iniciando servidor web..."
    echo "📱 Una vez iniciado, podrás acceder desde tu navegador a:"
    echo "   👉 http://localhost:8080"
    echo ""
    echo "💡 Presiona CTRL+C para detener el servidor"
    echo "🔴 Funcionando en modo de demostración"
    echo "=========================================================="
    
    # Ejecutar el servicio web
    java -cp build/classes WebServiceMain
else
    echo "❌ Error en la compilación"
    exit 1
fi 