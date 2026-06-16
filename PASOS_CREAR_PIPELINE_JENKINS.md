# Pasos para Crear Tarea Pipeline en Jenkins

## Paso 1: Acceder a Jenkins

1. Abre el navegador
2. Ve a: `http://localhost/` (o `http://localhost:8080` si está en otro puerto)
3. Si te pide login, ingresa tus credenciales

---

## Paso 2: Crear Nueva Tarea

1. En la página principal de Jenkins, haz click en **"Nueva Tarea"** (en inglés: "New Item")
2. Se abrirá un formulario con opciones
3. En el campo **"Nombre de la tarea"**: ingresa `unir-cicd-laboratorio`

```
┌─────────────────────────────────────────┐
│ Nombre de la tarea:                     │
│ ┌───────────────────────────────────┐   │
│ │ unir-cicd-laboratorio             │   │
│ └───────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

4. En la lista de tipos de tarea, selecciona **"Pipeline"**
5. Haz click en **"OK"**

---

## Paso 3: Configurar el Pipeline - OPCIÓN A (Recomendada)

### Desde Repositorio Git

En la página de configuración de la tarea:

**Sección: Pipeline**

- **Definition**: Haz click en el dropdown y selecciona **"Pipeline script from SCM"**

```
Definition: [Pipeline script from SCM ▼]
```

**Sección: SCM**

- **SCM**: Selecciona **"Git"** del dropdown

```
SCM: [Git ▼]
```

- **Repository URL**: Ingresa exactamente:

```
https://github.com/byron5074/unir-cicd-laboratorio.git
```

- **Branch Specifier**: Deja como está (debería ser `*/master` o vacío)

```
Branch Specifier (blank for 'any'):
┌──────────────────────┐
│ */master             │
└──────────────────────┘
```

- **Script Path**: Ingresa exactamente:

```
Jenkinsfile.unit.groovy
```

```
Script Path:
┌──────────────────────┐
│ Jenkinsfile.unit.groovy
└──────────────────────┘
```

### Resultado esperado:

```
Definition: Pipeline script from SCM
SCM: Git
Repository URL: https://github.com/byron5074/unir-cicd-laboratorio.git
Branch: */master
Script Path: Jenkinsfile.unit.groovy
```

---

## Paso 4: Configurar el Pipeline - OPCIÓN B (Alternativa)

### Si prefieres Script directo (sin Git):

**Sección: Pipeline**

- **Definition**: Selecciona **"Pipeline script"**

```
Definition: [Pipeline script ▼]
```

- En el área de texto grande **"Script"**, copia TODO el siguiente código:

```groovy
pipeline {
    agent {
        label 'docker'
    }

    options {
        timestamps()
        timeout(time: 1, unit: 'HOURS')
    }

    stages {
        stage('Source') {
            steps {
                echo 'Cloning repository...'
                git 'https://github.com/srayuso/unir-cicd.git'
            }
        }

        stage('Build') {
            steps {
                echo 'Building stage!'
                sh 'make build'
            }
        }

        stage('Unit tests') {
            steps {
                echo 'Running Unit tests...'
                sh 'make test-unit'
            }
        }

        stage('API tests') {
            steps {
                echo 'Running API tests...'
                sh 'make test-api'
            }
        }

        stage('E2E tests') {
            steps {
                echo 'Running E2E tests...'
                sh 'make test-e2e'
            }
        }
    }

    post {
        always {
            echo 'Archiving test results...'
            archiveArtifacts artifacts: 'results/*.xml', allowEmptyArchive: true

            echo 'Publishing test reports...'
            junit testResults: 'results/*_result.xml', allowEmptyResults: true
        }

        failure {
            echo 'Pipeline failed! Sending failure notification...'
            script {
                def jobName = env.JOB_NAME
                def buildNumber = env.BUILD_NUMBER
                def buildUrl = env.BUILD_URL

                echo """
                =====================================
                FALLO EN LA EJECUCIÓN DEL PIPELINE
                =====================================
                Nombre del trabajo: ${jobName}
                Número de ejecución: ${buildNumber}
                URL de la ejecución: ${buildUrl}
                =====================================
                """

                // Descomentar la siguiente línea para enviar correo
                // emailext (
                //     recipientProviders: [developers(), requestor()],
                //     subject: "Fallo en el pipeline: ${jobName} #${buildNumber}",
                //     body: """
                //     El pipeline ha fallado.
                //
                //     Nombre del trabajo: ${jobName}
                //     Número de ejecución: ${buildNumber}
                //     URL: ${buildUrl}
                //
                //     Por favor, revise los logs de la ejecución.
                //     """
                // )
            }
        }

        success {
            echo 'Pipeline executed successfully!'
        }

        cleanup {
            cleanWs()
        }
    }
}
```

---

## Paso 5: Guardar la Configuración

1. Desplázate hasta el final de la página
2. Haz click en **"Guardar"** (en inglés: "Save")

```
┌────────────┐
│   Guardar  │
└────────────┘
```

---

## Paso 6: Ejecutar la Tarea

1. Se abrirá la página de la tarea
2. Haz click en **"Construir Ahora"** (en inglés: "Build Now")

```
┌────────────────────┐
│  Construir Ahora   │
└────────────────────┘
```

---

## Paso 7: Monitorear la Ejecución

### Ver Stage View:

1. En la página principal de la tarea, debajo verás **"Build History"** (Historial de construcción)
2. Haz click en la construcción que acabas de iniciar (debería estar en la primera fila)
3. Se abrirá la página de detalles de la construcción
4. Busca **"Stage View"** para ver el progreso de las etapas

### Etapas que deberías ver:

```
Source ──> Build ──> Unit tests ──> API tests ──> E2E tests
```

Cada etapa mostrará:

- ⏳ En progreso (naranja)
- ✅ Completada (verde)
- ❌ Fallida (rojo)

---

## Paso 8: Ver Resultados de Pruebas

### Reportes JUnit:

1. En la página de detalles de la construcción
2. Busca la sección **"Test Results"**
3. Verás:
   - Número total de pruebas
   - Pruebas pasadas
   - Pruebas fallidas

### Artefactos:

1. En la misma página, busca **"Artifacts"** o **"Archivos Guardados"**
2. Verás los archivos XML archivados:
   - `unit_result.xml`
   - `api_result.xml`
   - (Otros archivos de prueba)

---

## Paso 9: Capturar Screenshots (Para la Entrega)

Debes capturar al menos 3 pantallas:

### Screenshot 1: Configuración de la Tarea

1. Ve a la tarea `unir-cicd-laboratorio`
2. Haz click en **"Configurar"** (Configure)
3. Captura pantalla mostrando:
   - Pipeline definition
   - SCM configuration
   - Repository URL
   - Script Path

### Screenshot 2: Stage View con Ejecución Exitosa

1. En la página de detalles de la construcción
2. Captura el **Stage View** mostrando todas las etapas completadas
3. Debe verse claramente cada etapa con ✅

```
Ejemplo esperado:
Source (5s) ──> Build (2m 30s) ──> Unit tests (1m) ──> API tests (3m) ──> E2E tests (4m)
```

### Screenshot 3: Test Results

1. En la misma página
2. Desplázate hasta la sección **"Test Results"**
3. Captura mostrando los resultados de las pruebas

---

## Troubleshooting

### Problema: "No suitable agents"

**Solución**:

- Cambia en el Pipeline:
  ```groovy
  agent {
      label 'docker'
  }
  ```
- Por:
  ```groovy
  agent any
  ```

### Problema: "Permission denied"

**Solución**:

- Jenkins no tiene permisos para ejecutar Docker
- Ejecuta en terminal:
  ```bash
  sudo usermod -aG docker $(whoami)
  sudo systemctl restart docker
  sudo systemctl restart jenkins
  ```

### Problema: "Git clone fails"

**Solución**:

- Verifica que tengas acceso a internet
- Verifica que la URL del repositorio sea correcta:
  ```
  https://github.com/srayuso/unir-cicd.git
  ```

---

## Verificación Final

Cuando todo esté completado, deberías tener:

✅ Tarea creada en Jenkins  
✅ Configuración Pipeline desde Git  
✅ Ejecución exitosa de todas las etapas  
✅ Test Results visibles  
✅ Artifacts archivados  
✅ Screenshots capturadas  
✅ MEMORIA_LABORATORIO.md en PDF/Word

**Todo listo para entregar el laboratorio** 🎉
