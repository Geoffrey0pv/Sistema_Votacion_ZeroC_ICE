#!/usr/bin/env python3
import sys
import Ice
import Demo

def main():
    try:
        # Inicializar comunicador ICE
        with Ice.initialize(sys.argv) as communicator:
            # Conectar al endpoint Hello World
            base = communicator.stringToProxy("HelloWorld:tcp -h localhost -p 9090")
            hello_world = Demo.IHelloWorldPrx.checkedCast(base)
            
            if not hello_world:
                raise RuntimeError("❌ No se pudo conectar al endpoint HelloWorld")
            
            print("🌍 ===== CLIENTE HELLO WORLD (PYTHON) =====")
            print("📡 Conectado a: tcp -h localhost -p 9090")
            print("==========================================")
            
            # Probar los métodos (equivalente a requests POST en Postman)
            print("\n📞 Llamando a sayHello():")
            response = hello_world.sayHello()
            print(f"📨 {response}")
            
            print("\n📞 Llamando a sayHelloTo('Python Client'):")
            response = hello_world.sayHelloTo("Python Client")
            print(f"📨 {response}")
            
            print("\n📞 Llamando a getServerInfo():")
            response = hello_world.getServerInfo()
            print(f"📨 {response}")
            
            print("\n📞 Llamando a getCurrentTime():")
            timestamp = hello_world.getCurrentTime()
            print(f"📨 Timestamp: {timestamp}")
            
            print("\n✅ ¡Todas las pruebas completadas exitosamente!")
            
    except Exception as e:
        print(f"❌ Error: {e}")
        return 1
    
    return 0

if __name__ == "__main__":
    sys.exit(main()) 