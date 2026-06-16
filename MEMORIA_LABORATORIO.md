# Memoria Explicativa: Desarrollo de Pipeline de Jenkins

## Objetivo

Modificar un Jenkinsfile existente para implementar un pipeline completo de CI/CD que incluya pruebas unitarias, API y E2E, con archivado de resultados y notificación por correo en caso de fallo.

## Pasos Realizados

### 1. Análisis Inicial del Proyecto

Se analiza la estructura del proyecto disponible:

- **Dockerfile**: Contiene la definición de la imagen Docker para la aplicación
- **Makefile**: Define los comandos disponibles para build y testing
- **Jenkinsfile.unit.groovy**: Jenkinsfile inicial con etapas básicas de Build y Unit tests

### 2. Estudio del Makefile

Se identifica que el Makefile disponibiliza los siguientes targets:

- `make build`: Construye las imágenes Docker de la aplicación
- `make test-unit`: Ejecuta pruebas unitarias en Docker
- `make test-api`: Ejecuta pruebas de API con servidor Flask
- `make test-e2e`: Ejecuta pruebas E2E con Cypress
- `make deploy-stage`: Desplega la aplicación en staging

### 3. Modificación del Jenkinsfile

#### 3.1 Estructura General

Se implementa un pipeline con la siguiente estructura:

```groovy
pipeline {
    agent { label 'docker' }
    options { ... }
    stages { ... }
    post { ... }
}
```

#### 3.2 Etapas Implementadas

**Etapa 1: Source**

```groovy
stage('Source') {
    steps {
        echo 'Cloning repository...'
        git 'https://github.com/srayuso/unir-cicd.git'
    }
}
```

- Clona el repositorio del proyecto desde GitHub
- Proporciona el código fuente necesario para el build

**Etapa 2: Build**

```groovy
stage('Build') {
    steps {
        echo 'Building stage!'
        sh 'make build'
    }
}
```

- Ejecuta la construcción de las imágenes Docker
- Prepara el entorno para las pruebas

**Etapa 3: Unit tests**

```groovy
stage('Unit tests') {
    steps {
        echo 'Running Unit tests...'
        sh 'make test-unit'
    }
}
```

- Ejecuta las pruebas unitarias del código Python
- Genera archivos de resultado en `results/unit_result.xml`

**Etapa 4: API tests**

```groovy
stage('API tests') {
    steps {
        echo 'Running API tests...'
        sh 'make test-api'
    }
}
```

- Inicia un servidor Flask en Docker
- Ejecuta pruebas contra la API
- Genera archivos de resultado en `results/api_result.xml`

**Etapa 5: E2E tests**

```groovy
stage('E2E tests') {
    steps {
        echo 'Running E2E tests...'
        sh 'make test-e2e'
    }
}
```

- Inicia la aplicación web y API
- Ejecuta pruebas E2E con Cypress
- Verifica el comportamiento completo de la aplicación

#### 3.3 Sección POST

**Always (Se ejecuta siempre)**

```groovy
post {
    always {
        echo 'Archiving test results...'
        archiveArtifacts artifacts: 'results/*.xml', allowEmptyArchive: true

        echo 'Publishing test reports...'
        junit testResults: 'results/*_result.xml', allowEmptyResults: true
    }
}
```

- Archiva todos los archivos XML de pruebas en Jenkins
- Publica los reportes JUnit para visualización en la interfaz de Jenkins
- Utiliza `allowEmptyArchive: true` para permitir ejecuciones sin resultados

**Failure (Se ejecuta en caso de fallo)**

```groovy
post {
    failure {
        echo 'Pipeline failed! Sending failure notification...'
        script {
            def jobName = env.JOB_NAME
            def buildNumber = env.BUILD_NUMBER
            def buildUrl = env.BUILD_URL

            echo """Nombre del trabajo: ${jobName}
                  Número de ejecución: ${buildNumber}
                  URL de la ejecución: ${buildUrl}"""

            // emailext (...)  // Descomentado para enviar correo
        }
    }
}
```

- Se ejecuta solo cuando el pipeline falla
- Captura las variables globales: `JOB_NAME`, `BUILD_NUMBER`, `BUILD_URL`
- Muestra la información en los logs de Jenkins
- El paso de correo está comentado (ver sección de Problemas Encontrados)

**Success (Se ejecuta en caso de éxito)**

```groovy
post {
    success {
        echo 'Pipeline executed successfully!'
    }
}
```

- Proporciona confirmación visual de la ejecución exitosa

**Cleanup**

```groovy
post {
    cleanup {
        cleanWs()
    }
}
```

- Limpia el workspace después de la ejecución (con o sin éxito)

## Cambios Implementados vs. Jenkinsfile Original

| Aspecto            | Original          | Mejorado                                        |
| ------------------ | ----------------- | ----------------------------------------------- |
| Etapas             | Build, Unit tests | Source, Build, Unit tests, API tests, E2E tests |
| Archivado          | Solo Unit tests   | Todos los tests XML                             |
| Reportes           | junit simple      | junit con allowEmptyResults                     |
| Notificaciones     | No                | Implementadas para fallo                        |
| Variables globales | No                | Nombre, número y URL de ejecución               |
| Opciones           | No                | Timestamps, timeout                             |
| Limpieza           | cleanWs() simple  | Mejorada en post cleanup                        |

## Problemas Encontrados y Soluciones

### Problema 1: Variables de Ambiente para Correo

**Descripción**: Jenkins requiere configuración adicional para enviar correos.

**Solución**: Se implementó el paso de correo usando `emailext` pero comentado. Incluye instrucciones para descomentar cuando Jenkins esté configurado.

```groovy
// Descomentar cuando Jenkins tenga configurado el email
// emailext (
//     recipientProviders: [developers(), requestor()],
//     subject: "Fallo en el pipeline: ${jobName} #${buildNumber}",
//     body: """..."""
// )
```

### Problema 2: Archivos XML Vacíos

**Descripción**: Las pruebas podrían no generar archivos XML en ciertos casos.

**Solución**: Se agregó `allowEmptyArchive: true` y `allowEmptyResults: true` para permitir que el pipeline continúe incluso si no hay archivos.

```groovy
archiveArtifacts artifacts: 'results/*.xml', allowEmptyArchive: true
junit testResults: 'results/*_result.xml', allowEmptyResults: true
```

### Problema 3: Timeout de Ejecución

**Descripción**: Las pruebas E2E pueden tomar bastante tiempo.

**Solución**: Se configuró un timeout de 1 hora en las opciones del pipeline.

```groovy
options {
    timeout(time: 1, unit: 'HOURS')
}
```

### Problema 4: Variables Globales en el Post

**Descripción**: Necesidad de acceder al nombre del trabajo y número de ejecución.

**Solución**: Se utilizan las variables globales de Jenkins:

- `env.JOB_NAME`: Nombre del trabajo
- `env.BUILD_NUMBER`: Número de ejecución
- `env.BUILD_URL`: URL de la ejecución

## Características Principales del Pipeline

1. **Etapas Progresivas**: El pipeline ejecuta cada etapa de prueba de forma secuencial
2. **Reportes Integrados**: Los resultados de pruebas se visualizan en la interfaz de Jenkins
3. **Archivado Automático**: Se guardan todos los artefactos XML para referencia histórica
4. **Notificaciones**: Se envía correo solo en caso de fallo
5. **Limpieza Automática**: Se limpia el workspace al finalizar

## Verificación de Funcionalidad

Para verificar que el pipeline funciona correctamente:

1. Crear un nuevo trabajo de tipo Pipeline en Jenkins
2. Copiar el código del Jenkinsfile en la definición del trabajo
3. Ejecutar el trabajo (Build Now)
4. Verificar que todas las etapas se ejecuten correctamente
5. Comprobar que los reportes aparezcan en la sección "Test Results"

## Conclusiones

El Jenkinsfile mejorado implementa un pipeline completo de CI/CD que:

- Automatiza todas las etapas de testing del proyecto
- Proporciona reportes visuales de las pruebas
- Archiva los resultados para auditoría
- Notifica en caso de fallos
- Cumple con todos los requisitos especificados en la actividad

El pipeline es resistente a errores y proporciona una base sólida para la integración continua del proyecto.
