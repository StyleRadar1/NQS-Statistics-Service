[⬅ Volver al README Principal](../../README.md)

---

## 10. RAMAS Y CÓMO CONTRIBUIR

El equipo de **Squad NQS (`No Queremos Scrum`)** adopta el modelo de trabajo **GitFlow adaptado para microservicios**. Las ramas `master` y `develop` están protegidas y reciben cambios exclusivamente mediante Pull Requests validados por el pipeline automatizado de GitHub Actions.

### Estructura Oficial de Ramas

```text
master                  <-- Rama de producción (Desplegada en Azure PROD · el workflow también soporta main)
└── develop             <-- Rama de integración continua (Desplegada en Azure QA)
    ├── feature/NQS-<ID>-<descripcion>   <-- Nuevas funcionalidades o historias de usuario
    ├── refactor/<alcance>               <-- Refactorizaciones arquitectónicas o de dominio
    ├── fix/<alcance>                    <-- Corrección de defectos o auditorías
    ├── chore/<alcance>                  <-- Tareas de CI/CD, configuración o mantenimiento
    ├── docs/<alcance>                   <-- Actualización de documentación y diagramas
    └── hotfix/<descripcion-urgente>     <-- Parche crítico directo a producción
```

### Convención de Commits (*Conventional Commits*)

Todo mensaje de commit debe seguir la estructura estándar:

```text
<tipo>(<alcance>): <descripción imperativa en español o inglés> [#NQS-XX]

Ejemplos válidos en ms-estadisticas:
feat(estadisticas): agregar consulta de ingresos mensuales por suscripciones [#NQS-49]
fix(rabbit): garantizar idempotencia transaccional en processed_event [#NQS-41]
test(security): validar aislamiento de alcance por rol en AuthenticatedScopeResolver [#NQS-46]
docs(readme): reestructurar documentación oficial y contratos de eventos [#NQS-6]
```

### Checklist Obligatorio para Pull Requests

- [x] La rama parte de la versión más reciente de `develop`.
- [x] El código compila limpiamente con `./mvnw clean compile`.
- [x] Todas las pruebas automatizadas pasan en verde (`./mvnw clean verify`).
- [x] La cobertura de líneas en JaCoCo cumple el umbral mínimo obligatorio del **85%**.
- [x] No se introducen credenciales, secretos (`JWT_SECRET`), contraseñas ni certificados privados en el código (uso estricto de `.env.example` y *Secrets* de GitHub/Azure).
- [x] Los contenedores OCI se construyen exitosamente con `docker compose build`.
- [ ] El PR cuenta con al menos un revisor aprobado del squad antes de mezclarse a `develop` o `master`.

---

[⬅ Volver al README Principal](../../README.md)
