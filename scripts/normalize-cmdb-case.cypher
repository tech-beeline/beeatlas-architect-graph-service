// =====================================================================================
// normalize-cmdb-case.cypher
//
// Проблема: часть кода исторически сравнивала SoftwareSystem.cmdb регистронезависимо
// (toLower(cmdb) = toLower($cmdb)), а ноды создавались по точному совпадению cmdb.
// В результате на проде для одного продукта появились дубли SoftwareSystem-нод,
// отличающихся только регистром cmdb (пример: {cmdb:"RICH"} и {cmdb:"rich"}, у обеих
// name:"RICH"). Это ломает поиск (/search/software-system возвращает 2 записи) и
// построение L1/влияния (getInfluencingSystems/getDependentSystems используют
// Result.single() и падают, если под старое регистронезависимое сравнение попадали
// 2 ноды).
//
// Код (SoftwareSystemRepository, GenericRepository, ContainerRepository) уже исправлен
// на точное регистрозависимое сравнение cmdb. Этот скрипт приводит существующие
// прод-данные к единой форме, чтобы новое поведение не "теряло" дубли молча.
//
// ВАЖНО:
//   - Перед запуском Step 3-5 (мутирующие шаги) сделайте бэкап БД (neo4j-admin dump
//     или apoc.export) и прогоните скрипт сначала на копии/staging.
//   - Скрипт рассчитан на APOC (apoc.coll.*, apoc.refactor.mergeNodes) — тот же
//     плагин уже используется в коде сервиса.
//   - Область действия — ноды с graphTag = "Global" (это единственный граф, где
//     работают поиск и влияние; Local-графы транзиентны и пересоздаются заново).
//   - Канонический регистр cmdb в группе дублей выбирается автоматически:
//     нода с наибольшим числом связей (degree), при равенстве — нода с наименьшим
//     internal id (создана раньше).
// =====================================================================================


// -------------------------------------------------------------------------------------
// STEP 0. Аудит: найти все группы SoftwareSystem-нод, у которых cmdb совпадает без
// учёта регистра, но отличается по факту. Только чтение, ничего не меняет.
// -------------------------------------------------------------------------------------
MATCH (n:SoftwareSystem {graphTag: "Global"})
WITH toLower(n.cmdb) AS cmdbLower, collect(n) AS nodes
WHERE size(apoc.coll.toSet([x IN nodes | x.cmdb])) > 1
UNWIND nodes AS n
RETURN cmdbLower AS productKey,
       id(n) AS nodeId,
       n.cmdb AS cmdb,
       n.name AS name,
       size((n)--()) AS degree
ORDER BY productKey, degree DESC;


// -------------------------------------------------------------------------------------
// STEP 1. То же самое, но сгруппировано — по одной строке на продукт, с выбором
// канонической ноды (максимальный degree, при равенстве — минимальный id). Проверьте
// колонку canonicalCmdb перед тем, как продолжать. Если авто-выбор не устраивает для
// конкретного продукта — обработайте его вручную отдельным запросом вместо Step 3-5.
// -------------------------------------------------------------------------------------
MATCH (n:SoftwareSystem {graphTag: "Global"})
WITH toLower(n.cmdb) AS cmdbLower,
     collect({cmdb: n.cmdb, nodeId: id(n), degree: size((n)--())}) AS entries
WHERE size(apoc.coll.toSet([e IN entries | e.cmdb])) > 1
WITH cmdbLower, entries, apoc.coll.max([e IN entries | e.degree]) AS maxDegree
WITH cmdbLower, entries,
     apoc.coll.min([e IN entries WHERE e.degree = maxDegree | e.nodeId]) AS canonicalNodeId
WITH cmdbLower, entries, [e IN entries WHERE e.nodeId = canonicalNodeId][0] AS canonical
RETURN cmdbLower AS productKey,
       canonical.cmdb AS canonicalCmdb,
       canonical.nodeId AS canonicalNodeId,
       [e IN entries WHERE e.nodeId <> canonical.nodeId | e.cmdb] AS duplicateCmdbValues,
       entries AS allVariants
ORDER BY productKey;


// -------------------------------------------------------------------------------------
// STEP 2. Ручное переопределение (опционально).
// Если для какого-то productKey авто-выбор канонического cmdb из Step 1 не устраивает,
// не запускайте Step 3-5 as-is — замените в них выбор `canonical` на константу для
// этого конкретного productKey перед выполнением, либо обработайте этот продукт
// отдельным ручным запросом по аналогии со Step 3-5.
// -------------------------------------------------------------------------------------


// -------------------------------------------------------------------------------------
// STEP 3. Нормализовать sourceWorkspace на связях (Relationship), чтобы будущие
// перезаливки DSL с каноническим cmdb корректно находили и версионировали старые
// связи (getRelationshipsByTagAndCmdb ищет по точному sourceWorkspace = cmdb).
// Мутирующий, но безопасный шаг (переименование свойства, структура графа не меняется).
// -------------------------------------------------------------------------------------
MATCH (n:SoftwareSystem {graphTag: "Global"})
WITH toLower(n.cmdb) AS cmdbLower,
     collect({cmdb: n.cmdb, nodeId: id(n), degree: size((n)--())}) AS entries
WHERE size(apoc.coll.toSet([e IN entries | e.cmdb])) > 1
WITH cmdbLower, entries, apoc.coll.max([e IN entries | e.degree]) AS maxDegree
WITH cmdbLower, entries,
     apoc.coll.min([e IN entries WHERE e.degree = maxDegree | e.nodeId]) AS canonicalNodeId
WITH cmdbLower, entries, [e IN entries WHERE e.nodeId = canonicalNodeId][0].cmdb AS canonicalCmdb
UNWIND [e IN entries WHERE e.cmdb <> canonicalCmdb | e.cmdb] AS losingCmdb
MATCH (r:Relationship {graphTag: "Global", sourceWorkspace: losingCmdb})
SET r.sourceWorkspace = canonicalCmdb
RETURN losingCmdb, canonicalCmdb, count(r) AS relationshipsUpdated;


// -------------------------------------------------------------------------------------
// STEP 4. Нормализовать суффикс "~<cmdb>" в именах Container / DeploymentNode /
// Component, созданных под "проигравшим" регистром (см.
// GraphUpdateFunctions.setNamesToContainer: name = name + "~" + cmdb).
// Тоже переименование, без изменения структуры графа.
// -------------------------------------------------------------------------------------
MATCH (n:SoftwareSystem {graphTag: "Global"})
WITH toLower(n.cmdb) AS cmdbLower,
     collect({cmdb: n.cmdb, nodeId: id(n), degree: size((n)--())}) AS entries
WHERE size(apoc.coll.toSet([e IN entries | e.cmdb])) > 1
WITH cmdbLower, entries, apoc.coll.max([e IN entries | e.degree]) AS maxDegree
WITH cmdbLower, entries,
     apoc.coll.min([e IN entries WHERE e.degree = maxDegree | e.nodeId]) AS canonicalNodeId
WITH cmdbLower, entries, [e IN entries WHERE e.nodeId = canonicalNodeId][0].cmdb AS canonicalCmdb
UNWIND [e IN entries WHERE e.cmdb <> canonicalCmdb | e.cmdb] AS losingCmdb
MATCH (c {graphTag: "Global"})
WHERE (c:Container OR c:DeploymentNode OR c:Component)
  AND c.name ENDS WITH ("~" + losingCmdb)
SET c.name = left(c.name, size(c.name) - size(losingCmdb)) + canonicalCmdb
RETURN losingCmdb, canonicalCmdb, count(c) AS namesUpdated;


// -------------------------------------------------------------------------------------
// STEP 5. Слить дубли SoftwareSystem-нод в каноническую. apoc.refactor.mergeNodes
// переносит на каноническую ноду ВСЕ связи дубля (включая :Child к его Container'ам),
// свойства дубля отбрасываются (properties: {`.*`: "discard"} => побеждают свойства
// первой ноды в списке, т.е. канонической). После merge поле cmdb дополнительно
// принудительно выставляется в canonicalCmdb — на случай, если properties-policy
// сработала не так, как ожидалось.
//
// НЕОБРАТИМЫЙ шаг. Запускайте только после проверки Step 1 и после Step 3-4, и
// только после бэкапа БД.
// -------------------------------------------------------------------------------------
MATCH (n:SoftwareSystem {graphTag: "Global"})
WITH toLower(n.cmdb) AS cmdbLower, collect(n) AS nodes
WHERE size(apoc.coll.toSet([x IN nodes | x.cmdb])) > 1
WITH cmdbLower, [x IN nodes | {node: x, degree: size((x)--())}] AS entries
WITH cmdbLower, entries, apoc.coll.max([e IN entries | e.degree]) AS maxDegree
WITH cmdbLower, entries,
     apoc.coll.min([e IN entries WHERE e.degree = maxDegree | id(e.node)]) AS canonicalNodeId
WITH [e IN entries WHERE id(e.node) = canonicalNodeId][0].node AS canonical,
     [e IN entries WHERE id(e.node) <> canonicalNodeId | e.node] AS duplicates
UNWIND duplicates AS dup
WITH canonical, canonical.cmdb AS canonicalCmdb, dup
CALL apoc.refactor.mergeNodes([canonical, dup], {
  properties: {`.*`: "discard"},
  mergeRels: true
}) YIELD node
SET node.cmdb = canonicalCmdb
RETURN canonicalCmdb, id(node) AS mergedNodeId;


// -------------------------------------------------------------------------------------
// STEP 6. Верификация — должно вернуть 0 строк.
// -------------------------------------------------------------------------------------
MATCH (n:SoftwareSystem {graphTag: "Global"})
WITH toLower(n.cmdb) AS cmdbLower, collect(DISTINCT n.cmdb) AS variants
WHERE size(variants) > 1
RETURN cmdbLower, variants;
