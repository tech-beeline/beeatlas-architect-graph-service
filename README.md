# Architect Graph Service

Сервис хранения и обслуживания архитектурного графа корпоративных систем в модели **C4 / Structurizr**. Workspace JSON загружается в **Neo4j**, разделяется на локальный (`graphTag: Local`) и глобальный (`graphTag: Global`) графы; поверх графа доступны поиск, анализ влияния, генерация диаграмм, валидация workspace и сравнение версий.

Версия артефакта: **1.3.0** (`architect-graph-service-1.3.0.jar`).

## Возможности

- загрузка Structurizr Workspace из document-service или из сырого JSON в Neo4j;
- локальный и глобальный графы с версионированием систем в Global;
- поиск узлов (SoftwareSystem, Container, DeploymentNode);
- анализ влияния (influence) на уровне контейнера, продукта и деплоймента;
- генерация C4-диаграмм (context / container / deployment) в JSON и DOT;
- валидация workspace-документов;
- построение sequence-диаграмм по техвозможностям (ТС);
- сравнение версий систем (diff);
- асинхронная сборка графа через RabbitMQ со статусом в Redis.

## Стек

| Компонент | Версия |
|-----------|--------|
| Java | 17 |
| Spring Boot | 2.7.3 |
| Neo4j Java Driver | 5.12.0 |
| RabbitMQ | spring-boot-starter-amqp |
| Redis | spring-boot-starter-data-redis (Lettuce) |
| OpenAPI / Swagger | springdoc-openapi-ui 1.6.14 |
| GraphStream | gs-core 2.0 (генерация DOT) |
| Actuator + Prometheus | health, info, metrics, prometheus |
| OpenTelemetry | opentelemetry-instrumentation-bom 2.19.0 (по умолчанию отключён) |

Хранилище графа — **Neo4j** через прямой драйвер (не Spring Data Neo4j). Layout диаграмм — через Structurizr Graphviz API.

## Требования

- Java 17 и Maven 3.9+ (локальный запуск без Docker)
- Neo4j 5 (с APOC)
- RabbitMQ 3
- Redis
- Доступ к внешним сервисам: document-service, products-service, Auth SSO (при включённых фичах)

## Быстрый старт

### Docker Compose

Из каталога сервиса:

```bash
docker compose up --build
```

Поднимаются:

| Сервис | Порты |
|--------|-------|
| architect-graph-service | **8080** |
| Neo4j | **7474** (Browser), **7687** (Bolt) |
| RabbitMQ | **5672** (AMQP), **15672** (Management UI) |

Учётки по умолчанию в compose: Neo4j `neo4j` / `password`, RabbitMQ `guest` / `guest`.

> Redis, document-service, products-service и Auth SSO в `docker-compose.yml` не входят — ожидаются во внешней сети (`beeatlas-network`) по hostname из env.

### Локально (Maven)

```bash
docker compose up -d neo4j rabbitmq

mvn clean package -DskipTests
java -jar target/architect-graph-service-1.3.0.jar
```

Swagger UI: `http://localhost:8080/swagger-ui.html`  
OpenAPI: `http://localhost:8080/v3/api-docs`  
Actuator: `http://localhost:8080/actuator/health`, `/actuator/prometheus`

## Конфигурация

Основные свойства — в `src/main/resources/application.properties`. Подключения к Neo4j, RabbitMQ, Redis и внешним сервисам задаются через переменные окружения (Spring Boot relaxed binding).

### Feature flags

| Свойство | Env | По умолчанию | Назначение |
|----------|-----|--------------|------------|
| `app.feature.use-doc-service` | `APP_FEATURE_USE_DOC_SERVICE` | `true` | DocumentClient, RabbitMQ/Redis consumers, async-эндпоинты по docId |
| `app.ambassador-auth` | `APP_AMBASSADOR_AUTH` | `true` | Авторизация RabbitMQ через SSO-токен |

При `use-doc-service=false` эндпоинты загрузки графа по `docId` и статус async-задач недоступны (405); загрузка из JSON остаётся.

### Переменные окружения

**Neo4j**

| Env | Пример |
|-----|--------|
| `SPRING_NEO4J_URI` | `bolt://neo4j:7687` |
| `SPRING_NEO4J_AUTHENTICATION_USERNAME` | `neo4j` |
| `SPRING_NEO4J_AUTHENTICATION_PASSWORD` | `password` |

**Внешние сервисы**

| Env | Назначение |
|-----|------------|
| `SPRING_SERVICES_DOCUMENTS_URL` | URL document-service |
| `SPRING_SERVICES_GRAPHVIZ_URL` | URL Structurizr Graphviz API |
| `INTEGRATION_PRODUCT_SERVER_URL` | URL products-service |
| `INTEGRATION_AUTHSSO_SERVER_URL` | URL Auth SSO |

**RabbitMQ**

| Env | Пример / дефолт в compose |
|-----|---------------------------|
| `SPRING_RABBITMQ_HOST` | `rabbitmq` |
| `SPRING_RABBITMQ_PORT` | `5672` |
| `SPRING_RABBITMQ_USERNAME` / `PASSWORD` | `guest` / `guest` |
| `SPRING_RABBITMQ_VIRTUAL_HOST` | `/` |
| `SPRING_RABBITMQ_TEMPLATE_EXCHANGE` | `capability.exchange` |
| `SPRING_RABBITMQ_TEMPLATE_ROUTING_KEY` | `capability.routing` |

**Redis**

| Env | Пример |
|-----|--------|
| `SPRING_REDIS_HOST` | `redis` |
| `SPRING_REDIS_PORT` | `6379` |
| `SPRING_REDIS_PASSWORD` | `redis-password` |

**Прочее:** `JAVA_OPTS`, `OTEL_EXPORTER_OTLP_ENDPOINT` (по умолчанию `otel.sdk.disabled=true`).

### Очереди RabbitMQ

| Очередь | Роль |
|---------|------|
| `create_local_graph` | Приём задачи → Redis `QUEUE` → `build_local_graph` |
| `build_local_graph` | Сборка Local-графа → Redis DONE/ERROR |
| `create_global_graph` | Приём задачи для Global |
| `build_global_graph` | Сборка Global-графа |

Сообщение: `{ "taskKey": "...", "docId": N }`. Статус в Redis: ключ `graph:{taskKey}`, TTL 24 часа (`QUEUE` / `PROCESS` / `DONE` / `ERROR`).

## Модель данных

Workspace зеркалит **Structurizr JSON** (C4):

```
Workspace
├── Model
│   ├── Person[]
│   ├── SoftwareSystem[] → Container[] → Component[]
│   └── DeploymentNode[] → InfrastructureNode / ContainerInstance / …
├── Views (SystemContext, Container, Deployment, …)
└── Documentation, Configuration, properties
```

В Neo4j:

- лейблы: `SoftwareSystem`, `Container`, `Component`, `DeploymentNode`, `InfrastructureNode`, `ContainerInstance`, `Environment` и др.;
- связи: `Child`, `Relationship` (+ `graphTag`, `sourceWorkspace`, `description`);
- **`graphTag`**: `Local` \| `Global`;
- идентификаторы: `cmdb`, `structurizr_dsl_identifier`, `name` / `originalName`;
- в Global — версионность (`version`, `startVersion`, `endVersion`);
- обязательное свойство: `model.properties.workspace_cmdb` = `SoftwareSystem.properties.cmdb`.

## API (префикс `/api/v1`)

Полное описание — в Swagger. Краткий список:

### Граф и поиск (`GraphController`)

| Метод | Путь | Назначение |
|-------|------|------------|
| GET | `/search/deployment-node?search=` | Поиск DeploymentNode |
| GET | `/search/container?search=` | Поиск контейнеров |
| GET | `/search/software-system?search=` | Поиск SoftwareSystem |
| GET | `/influence?cmdb=&name=` | Системы, связанные с контейнером |
| GET | `/graph/{graph-type}/task/{task-id}` | Статус async-задачи (Redis) |
| GET | `/deployment-nodes/operation?path=&type=` | Deployment nodes по API-методам |
| POST | `/graph/local/{docId}` | Локальный граф из document-service |
| POST | `/graph/{docId}` | Global-граф из document-service |
| POST | `/graph/local/json` | Локальный граф из JSON body |
| POST | `/graph/json` | Global-граф из JSON body |
| POST | `/node/{id}/tag` | Кастомные теги на Global-ноду |
| POST | `/sequence` | Построение sequence-диаграммы |
| GET | `/graph/product/{cmdb}/influence` | Влияние на уровне продукта |
| GET | `/graph/deployment/{cmdb}/influence?name=&env=` | Влияние на уровне deployment |
| GET | `/deployment-node/{id}/containers/tech-capability` | Контейнеры DN + реализованные ТС |
| GET | `/diff/{cmdb}/{firstVersion}/{secondVersion}` | Diff двух версий |
| GET | `/diff/{cmdb}/{firstVersion}` | Diff с актуальной версией |
| GET | `/elements` | Произвольный Cypher (header `CYPHER-QUERY`) |

### Диаграммы (`DiagramController`)

| Метод | Путь | Назначение |
|-------|------|------------|
| GET | `/deployment/{environment}/{softwareSystemMnemonic}` | deploymentView JSON |
| GET | `/context/{softwareSystemMnemonic}?rankDirection=` | contextView JSON |
| GET | `/context/{softwareSystemMnemonic}/{containerMnemonic}` | containerView JSON |
| GET | `/diagram/context?cmdb=&rankDirection=&communicationDirection=` | Context V2 |
| GET | `/diagram/deployment?cmdb=&env=&rank-direction=&deployment-name=` | Deployment V2 |
| GET | `/diagram/dot?id=` | Deployment DOT |
| GET | `/context/dot?cmdb=` | Context DOT (зависимые) |
| GET | `/context/influence/dot?cmdb=` | Context DOT (влияющие) |
| GET | `/diagram/elements?id=` | Элементы deployment-диаграммы |
| GET | `/context/elements?cmdb=` | Элементы context |
| GET | `/influence/dot?id=` | Influence DOT |
| GET | `/influence/elements?id=` | Элементы influence |
| GET | `/context/influence/elements?cmdb=` | Влияющие системы |

`rankDirection`: `LeftRight` (по умолчанию) \| `TopBottom`.

### Валидация (`WorkspaceValidationController`)

| Метод | Путь | Назначение |
|-------|------|------------|
| GET | `/workspace/validate/{doc-id}` | Валидация Structurizr workspace из document-service |

Успех: `{ "valid": true, "workspaceCmdb": "..." }`. Ошибки валидации — `400` с сообщением (нет Model, нет `workspace_cmdb`, нет name у SS/Container/Component/DN и т.д.).

## Интеграции

| Клиент | Конфиг | Вызовы |
|--------|--------|--------|
| DocumentClient | `spring.services.documents.url` | `GET /api/v1/documents/{docId}` |
| StructurizrClient | `spring.services.graphviz.url` | POST JSON → layout (Graphviz) |
| ProductClient | `integration.product-server-url` | product/info, infra/search, operation, tech-capability |
| AuthSSOClient | `integration.authsso-server-url` | получение `access_token` для RabbitMQ |

## Структура проекта

```
src/main/java/ru/beeline/architecting_graph/
├── MainApplication.java
├── client/           # Document, Product, AuthSSO, Structurizr
├── config/           # Neo4j, Rabbit, Redis, Swagger, RestTemplate, OTel
├── controller/       # Graph, Diagram, WorkspaceValidation
├── dto/
├── model/            # C4 / Structurizr сущности и Views
├── repository/neo4j/ # Cypher через neo4j-java-driver
└── service/
    ├── graph/              # построение и обновление графа, influence
    ├── createDiagrams/     # диаграммы JSON/DOT
    ├── compareVersions/    # diff версий
    ├── getElements/        # произвольный Cypher
    └── …                   # валидация, Rabbit, sequence
```

## CI

В `.github/workflows`:

- **maven.yml** — JDK 17, `mvn clean verify` / `package` на push/PR в `main`;
- **compose-smoke.yml** — `docker compose up --wait` на PR;
- **image.yml** — сборка и push образа в ghcr.io.

Dockerfile: multi-stage (`maven:3.9-eclipse-temurin-17` → `eclipse-temurin:17-jre`).
