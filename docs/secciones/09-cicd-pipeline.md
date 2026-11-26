[⬅ Volver al README Principal](../../README.md)

---

## 9. CI/CD CON EL BADGE DEL PIPELINE

El ciclo de vida del microservicio implementa integración y entrega continua automatizada mediante **GitHub Actions** (`.github/workflows/ci-cd.yml`), **Azure Container Registry (`acrstyleradarestadisticas.azurecr.io`)** y **Azure App Service**.

### Badges de Pipelines y Calidad (GitHub Actions)

<p align="center">
  <a href="https://github.com/StyleRadar1/NQS-Statistics-Service/actions/workflows/ci-cd.yml"><img src="https://img.shields.io/badge/CI_Pipeline-passing-16A34A?style=for-the-badge&logo=githubactions&logoColor=white" alt="CI Pipeline" /></a>
  <a href="https://github.com/StyleRadar1/NQS-Statistics-Service/actions/workflows/ci-cd.yml"><img src="https://img.shields.io/badge/CD_QA_Azure-active-5600EF?style=for-the-badge&logo=microsoftazure&logoColor=white" alt="CD QA Pipeline" /></a>
  <a href="https://github.com/StyleRadar1/NQS-Statistics-Service/actions/workflows/ci-cd.yml"><img src="https://img.shields.io/badge/CD_PROD_Azure-configured-12063A?style=for-the-badge&logo=microsoftazure&logoColor=white" alt="CD PROD Pipeline" /></a>
  <a href="../secciones/08-pruebas-y-cobertura.md"><img src="https://img.shields.io/badge/JaCoCo_Line_Coverage-%E2%89%A5_85%25-16A34A?style=for-the-badge&logo=codecov&logoColor=white" alt="JaCoCo Coverage" /></a>
</p>

### Matriz de Pipelines en `.github/workflows/ci-cd.yml`

| Job / Pipeline | Rama / Entorno | Trigger de Ejecución | Quality Gate y Acciones Ejecutadas | Estado |
| :--- | :---: | :--- | :--- | :---: |
| **CI (`build-and-test`)** | `develop`, `master`, `main` *(y PRs hacia estas ramas)* | `push` o `pull_request` hacia `develop`, `master` o `main` | Setup JDK 21 Temurin + caché Maven → `mvn -B clean compile` → `mvn -B test jacoco:report` → `mvn -B verify` (JaCoCo ≥ 85%) → `mvn -B package -DskipTests` → `docker build` (`sr-ms-estadisticas:latest` con `RABBITMQ_CA_PEM_BASE64`) | [![CI Build](https://img.shields.io/badge/build--and--test-passing-16A34A?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/StyleRadar1/NQS-Statistics-Service/actions/workflows/ci-cd.yml) |
| **CD QA (`deploy-qa`)** | `develop` *(Azure QA · `sr-ms-estadisticas-qa`)* | `push` directo a `develop` (requiere `build-and-test` en verde) | Valida secreto `RABBITMQ_CA_PEM_BASE64` → `azure/login@v2` → ACR push (`acrstyleradarestadisticas.azurecr.io/sr-ms-estadisticas:qa`) → `az webapp restart --name sr-ms-estadisticas-qa` | [![CD QA](https://img.shields.io/badge/deploy--qa-active-5600EF?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/StyleRadar1/NQS-Statistics-Service/actions/workflows/ci-cd.yml) |
| **CD PROD (`deploy-prod`)** | `master` / `main` *(Azure PROD · `sr-ms-estadisticas-prod`)* | `push` directo a `master` o `main` (requiere `build-and-test` en verde) | Valida secreto `RABBITMQ_CA_PEM_BASE64` → `azure/login@v2` → ACR push (`acrstyleradarestadisticas.azurecr.io/sr-ms-estadisticas:prod`) → `az webapp restart --name sr-ms-estadisticas-prod` | [![CD PROD](https://img.shields.io/badge/deploy--prod-configured-12063A?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/StyleRadar1/NQS-Statistics-Service/actions/workflows/ci-cd.yml) |

### Diagrama del Flujo Automatizado (Pipeline Stages)

```mermaid
flowchart LR
    A["Push / PR<br>(develop / master / main)"] --> B["Setup JDK 21<br>(Temurin & Maven Cache)"]
    B --> C["Compile, Test & JaCoCo<br>(mvn test jacoco:report)"]
    C --> D["Quality Gate Verify<br>(Line Coverage ≥ 85%)"]
    D --> E["Package JAR & Docker Build<br>(Inject RABBITMQ_CA_PEM_BASE64)"]
    E --> F["Push ACR & Restart Web App<br>(QA :qa / PROD :prod)"]

    classDef step fill:#12063A,stroke:#5600EF,stroke-width:2px,color:#FFFFFF;
    classDef pass fill:#DCFCE7,stroke:#16A34A,stroke-width:2px,color:#0C0C12;

    class A,B,E,F step;
    class C,D pass;
```

### Estructura de Jobs en `.github/workflows/ci-cd.yml`

1. **`build-and-test`:**
   - Ejecuta sobre `ubuntu-latest` con JDK 21 (Eclipse Temurin) y caché de dependencias Maven.
   - Ejecuta compilación (`mvn -B clean compile`), suite de pruebas con reporte (`mvn -B test jacoco:report`), verificación de cobertura mínima del 85% (`mvn -B verify`), empaquetado del JAR (`mvn -B package -DskipTests`) y validación del `Dockerfile` inyectando `RABBITMQ_CA_PEM_BASE64` para generar `/app/rabbitmq-truststore.jks`.
2. **`deploy-qa`:**
   - Se dispara únicamente en `push` a `develop` tras completar `build-and-test`.
   - Verifica el secreto `RABBITMQ_CA_PEM_BASE64`, autentica en Azure (`AZURE_CREDENTIALS`), construye y publica la imagen `:qa` en `acrstyleradarestadisticas.azurecr.io` y reinicia `sr-ms-estadisticas-qa` en `rg-styleradar-estadisticas-qa`.
3. **`deploy-prod`:**
   - Se dispara únicamente en `push` a `master` o `main` tras completar `build-and-test`.
   - Construye y publica la imagen `:prod` en `acrstyleradarestadisticas.azurecr.io` y reinicia `sr-ms-estadisticas-prod` en `rg-styleradar-estadisticas-prod`.

<div align="center">
  <img src="../images/pipeline-ci-cd.png" alt="Evidencia 04 - Pipeline CI/CD en GitHub Actions" width="100%" style="border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);" />
  <p><sub><b>Figura 9.1:</b> Ejecución exitosa del pipeline automatizado de CI/CD en GitHub Actions (<code>.github/workflows/ci-cd.yml</code>), validando compilación con JDK 21, pruebas automatizadas, Quality Gate JaCoCo ≥ 85% y empaquetado Docker.</sub></p>
</div>

---

[⬅ Volver al README Principal](../../README.md)
