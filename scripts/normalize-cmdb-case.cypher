
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
       COUNT { (n)--() } AS degree
ORDER BY productKey, degree DESC;


// -------------------------------------------------------------------------------------
// STEP 1. То же самое, но сгруппировано — по одной строке на продукт, с выбором
// канонической ноды (максимальный degree, при равенстве — минимальный id). Проверьте
// колонку canonicalCmdb перед тем, как продолжать. Если авто-выбор не устраивает для
// конкретного продукта — обработайте его вручную отдельным запросом вместо Step 3-5.
// -------------------------------------------------------------------------------------
MATCH (n:SoftwareSystem {graphTag: "Global"})
WITH toLower(n.cmdb) AS cmdbLower,
     collect({cmdb: n.cmdb, nodeId: id(n), degree: COUNT { (n)--() }}) AS entries
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
     collect({cmdb: n.cmdb, nodeId: id(n), degree: COUNT { (n)--() }}) AS entries
WHERE size(apoc.coll.toSet([e IN entries | e.cmdb])) > 1
WITH cmdbLower, entries, apoc.coll.max([e IN entries | e.degree]) AS maxDegree
WITH cmdbLower, entries,
     apoc.coll.min([e IN entries WHERE e.degree = maxDegree | e.nodeId]) AS canonicalNodeId
WITH cmdbLower, entries, [e IN entries WHERE e.nodeId = canonicalNodeId][0].cmdb AS canonicalCmdb
UNWIND [e IN entries WHERE e.cmdb <> canonicalCmdb | e.cmdb] AS losingCmdb
MATCH ()-[r:Relationship {graphTag: "Global", sourceWorkspace: losingCmdb}]->()
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
     collect({cmdb: n.cmdb, nodeId: id(n), degree: COUNT { (n)--() }}) AS entries
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
// STEP 4.5. Снимок "до" — зафиксировать состояние перед необратимым Step 5, чтобы было
// с чем сравнивать в Step 6 (post-merge validation). Только чтение. Сохраните вывод.
// -------------------------------------------------------------------------------------
MATCH (n:SoftwareSystem {graphTag: "Global"})
WITH toLower(n.cmdb) AS cmdbLower, collect(n) AS nodes
WHERE size(apoc.coll.toSet([x IN nodes | x.cmdb])) > 1
UNWIND nodes AS n
OPTIONAL MATCH (n)-[:Child*0..]->(child)
WHERE child:Container OR child:Component
WITH cmdbLower, n, collect(DISTINCT child) AS children
RETURN cmdbLower AS productKey,
       n.cmdb AS cmdb,
       id(n) AS nodeId,
       COUNT { (n)--() } AS degree,
       size(children) AS containerAndComponentCount
ORDER BY productKey, cmdb;


// -------------------------------------------------------------------------------------
// STEP 5. Слить дубли SoftwareSystem-нод в каноническую.
//
// Перед вызовом apoc.refactor.mergeNodes сначала вручную удаляем у дубля любые
// :Relationship-рёбра, ведущие к соседу, к которому такое же ребро (тип+направление)
// уже есть у канонической ноды. Это принципиально: если этого не сделать,
// apoc.refactor.mergeNodes с mergeRels:true схлопывает такие параллельные рёбра сам,
// но при этом СВОЙСТВА выжившего ребра берутся от ребра дубля, а не канонической ноды —
// то есть настоящие sourceWorkspace/description канонической связи молча перезаписываются
// значениями от дубля (проверено на практике: связь дубля к общему соседу победила).
// Удаляя конфликтующие рёбра дубля заранее, мы гарантируем, что ребро канонической ноды
// остаётся нетронутым, а mergeRels просто переносит оставшиеся (неконфликтующие) рёбра
// дубля на каноническую ноду как обычно.
//
// apoc.refactor.mergeNodes переносит на каноническую ноду ВСЕ оставшиеся связи дубля
// (включая :Child к его Container'ам), свойства дубля отбрасываются
// (properties: {`.*`: "discard"} => побеждают свойства первой ноды в списке, т.е.
// канонической). После merge поле cmdb дополнительно принудительно выставляется
// в canonicalCmdb — на случай, если properties-policy сработала не так, как ожидалось.
//
// НЕОБРАТИМЫЙ шаг. Запускайте только после проверки Step 1 и после Step 3-4, и
// только после бэкапа БД.
// -------------------------------------------------------------------------------------
MATCH (n:SoftwareSystem {graphTag: "Global"})
WITH toLower(n.cmdb) AS cmdbLower, collect(n) AS nodes
WHERE size(apoc.coll.toSet([x IN nodes | x.cmdb])) > 1
WITH cmdbLower, [x IN nodes | {node: x, degree: COUNT { (x)--() }}] AS entries
WITH cmdbLower, entries, apoc.coll.max([e IN entries | e.degree]) AS maxDegree
WITH cmdbLower, entries,
     apoc.coll.min([e IN entries WHERE e.degree = maxDegree | id(e.node)]) AS canonicalNodeId
WITH [e IN entries WHERE id(e.node) = canonicalNodeId][0].node AS canonical,
     [e IN entries WHERE id(e.node) <> canonicalNodeId | e.node] AS duplicates
UNWIND duplicates AS dup
WITH canonical, canonical.cmdb AS canonicalCmdb, dup
CALL (canonical, dup) {
  MATCH (dup)-[dupOut:Relationship]->(neighbor)
  WHERE EXISTS { (canonical)-[:Relationship]->(neighbor) }
  DELETE dupOut
}
CALL (canonical, dup) {
  MATCH (neighbor)-[dupIn:Relationship]->(dup)
  WHERE EXISTS { (neighbor)-[:Relationship]->(canonical) }
  DELETE dupIn
}
WITH canonical, canonicalCmdb, dup
CALL apoc.refactor.mergeNodes([canonical, dup], {
  properties: {`.*`: "discard"},
  mergeRels: true
}) YIELD node
SET node.cmdb = canonicalCmdb
RETURN canonicalCmdb, id(node) AS mergedNodeId;


// -------------------------------------------------------------------------------------
// STEP 6. Post-merge валидация. Каждый подзапрос в норме должен вернуть 0 строк.
// Прогонять после Step 5 (можно и раньше, чтобы проверить baseline на "чистом" графе).
// -------------------------------------------------------------------------------------

// 6a. Не осталось групп cmdb, отличающихся только регистром — дубли схлопнулись
// (закрывает случай, когда дублей было > 1: если хоть один не смёржился, тут появится
// группа с size(variants) > 1).
MATCH (n:SoftwareSystem {graphTag: "Global"})
WITH toLower(n.cmdb) AS cmdbLower, collect(DISTINCT n.cmdb) AS variants
WHERE size(variants) > 1
RETURN cmdbLower, variants;

// 6b. Ни один Container/Component не остался "сиротой" — у каждого есть путь наверх
// к какой-то SoftwareSystem-ноде. Если merge потерял :Child-связь дубля, тут появятся
// строки.
MATCH (c {graphTag: "Global"})
WHERE (c:Container OR c:Component)
  AND NOT (c)<-[:Child*1..]-(:SoftwareSystem)
RETURN labels(c) AS label, id(c) AS nodeId, c.name AS name;

// 6c. Параллельные Relationship-связи одного типа/направления между SoftwareSystem
// и одним и тем же соседом. Это ровно тест "оригинал и дубль указывали на одну и ту
// же ноду" — после merge (mergeRels: true) должна остаться ОДНА связь, не две. Если
// тут есть строки с relCount > 1 — mergeRels не отработал, связь задвоилась.
MATCH (ss:SoftwareSystem {graphTag: "Global"})-[r:Relationship]->(neighbor)
WITH ss, neighbor, count(r) AS relCount
WHERE relCount > 1
RETURN id(ss) AS softwareSystemId, ss.cmdb AS cmdb, id(neighbor) AS neighborId, relCount
UNION
MATCH (neighbor)-[r:Relationship]->(ss:SoftwareSystem {graphTag: "Global"})
WITH ss, neighbor, count(r) AS relCount
WHERE relCount > 1
RETURN id(ss) AS softwareSystemId, ss.cmdb AS cmdb, id(neighbor) AS neighborId, relCount;

// 6d. Container/Component с одинаковым именем под одним и тем же родителем
// (после Step 4 у обеих "половин" бывшего дубля имена приведены к одному cmdb-суффиксу
// — если это привело к настоящей коллизии имён под одной SoftwareSystem, стоит
// разобраться руками, автоматический merge их не трогает).
MATCH (parent {graphTag: "Global"})-[:Child]->(c)
WHERE parent:SoftwareSystem OR parent:Container
WITH parent, c.name AS name, collect(id(c)) AS ids
WHERE size(ids) > 1
RETURN id(parent) AS parentId, name, ids;

// 6e. Не осталось Relationship со старым (проигравшим) значением sourceWorkspace —
// подтверждение, что Step 3 применился ко всем связям, включая перенесённые в Step 5.
MATCH (n:SoftwareSystem {graphTag: "Global"})
WITH collect(DISTINCT n.cmdb) AS liveCmdbValues
MATCH ()-[r:Relationship {graphTag: "Global"}]->()
WHERE NOT r.sourceWorkspace IN liveCmdbValues
RETURN DISTINCT r.sourceWorkspace AS danglingSourceWorkspace, count(r) AS relCount;

// 6f. Не осталось имён Container/DeploymentNode/Component с суффиксом "~<cmdb>",
// который не соответствует ни одному текущему (живому) cmdb — подтверждение Step 4.
MATCH (n:SoftwareSystem {graphTag: "Global"})
WITH collect(DISTINCT n.cmdb) AS liveCmdbValues
MATCH (c {graphTag: "Global"})
WHERE (c:Container OR c:DeploymentNode OR c:Component)
  AND NOT any(cmdb IN liveCmdbValues WHERE c.name ENDS WITH ("~" + cmdb))
  AND c.name CONTAINS "~"
RETURN labels(c) AS label, id(c) AS nodeId, c.name AS name;
