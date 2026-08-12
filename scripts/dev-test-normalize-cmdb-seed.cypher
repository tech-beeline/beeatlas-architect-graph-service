// =====================================================================================
// dev-test-normalize-cmdb-seed.cypher
//
// ТОЛЬКО ДЛЯ DEV. Не запускать на проде.
//
// Готовит на dev-графе искусственную ситуацию "дубли SoftwareSystem по регистру cmdb",
// чтобы прогнать normalize-cmdb-case.cypher и глазами убедиться, что:
//   1) дублирующая нода(ы) удаляется, все её связи перекидываются на оригинальную;
//   2) сценарий с количеством дублей > 1 закрывается полностью (все схлопываются
//      в одну каноническую ноду);
//   3) если оригинал и дубль оба имели :Relationship к одному и тому же соседу,
//      после merge остаётся ОДНА связь, а не две (mergeRels: true).
//
// Все созданные тестовые сущности помечаются свойством seedTest: true, чтобы их
// можно было гарантированно найти и удалить (Section E), не задев реальные данные.
//
// Перед началом поменяйте targetCmdb ниже на реальный существующий на dev cmdb
// (в задаче фигурировали "FDMSHOWCASEAPP" и "RICH" — нода с graphTag:"Global" и таким
// cmdb уже должна существовать, иначе Section C не найдёт соседа для теста п.3).
// =====================================================================================


// -------------------------------------------------------------------------------------
// SECTION A. Создать N "левых" дублей существующей SoftwareSystem с другим регистром
// cmdb. По умолчанию — 2 дубля (проверяет случай "дублей больше одного").
// -------------------------------------------------------------------------------------
WITH "RICH" AS targetCmdb, ["rich", "Rich"] AS duplicateCmdbVariants
MATCH (original:SoftwareSystem {graphTag: "Global", cmdb: targetCmdb})
UNWIND duplicateCmdbVariants AS dupCmdb
CREATE (dup:SoftwareSystem {
  graphTag: "Global",
  cmdb: dupCmdb,
  name: original.name,
  seedTest: true
})
RETURN dup.cmdb AS createdDuplicateCmdb, id(dup) AS duplicateNodeId;


// -------------------------------------------------------------------------------------
// SECTION B. Повесить на каждый дубль по фейковому Container с :Child-связью —
// проверяет, что дочерние ноды дубля после merge окажутся под канонической нодой.
// -------------------------------------------------------------------------------------
WITH "RICH" AS targetCmdb
MATCH (dup:SoftwareSystem {graphTag: "Global", seedTest: true})
WHERE toLower(dup.cmdb) = toLower(targetCmdb) AND dup.cmdb <> targetCmdb
CREATE (dup)-[:Child {graphTag: "Global", seedTest: true}]->(c:Container {
  graphTag: "Global",
  name: "SeedTestContainer~" + dup.cmdb,
  seedTest: true
})
RETURN dup.cmdb AS duplicateCmdb, c.name AS createdContainerName, id(c) AS containerId;


// -------------------------------------------------------------------------------------
// SECTION C. Самый важный кейс: у оригинала и у ОДНОГО из дублей появляется
// :Relationship к одному и тому же соседу. Соседа берём реального — первого, к кому
// уже есть исходящая :Relationship от оригинала (если у оригинала таких связей нет,
// секция ничего не создаст и Section D/E это покажут — тогда возьмите cmdb продукта,
// у которого точно есть исходящие связи).
// -------------------------------------------------------------------------------------
WITH "RICH" AS targetCmdb
MATCH (original:SoftwareSystem {graphTag: "Global", cmdb: targetCmdb})-[:Relationship]->(sharedNeighbor)
WITH targetCmdb, sharedNeighbor LIMIT 1
MATCH (dup:SoftwareSystem {graphTag: "Global", seedTest: true})
WHERE toLower(dup.cmdb) = toLower(targetCmdb) AND dup.cmdb <> targetCmdb
WITH dup, sharedNeighbor LIMIT 1
CREATE (dup)-[r:Relationship {
  graphTag: "Global",
  sourceWorkspace: dup.cmdb,
  description: "seed test — should collapse into the original's existing relationship",
  seedTest: true
}]->(sharedNeighbor)
RETURN dup.cmdb AS duplicateCmdb, id(sharedNeighbor) AS sharedNeighborId,
       labels(sharedNeighbor) AS sharedNeighborLabels;


// -------------------------------------------------------------------------------------
// SECTION D. "До" — посмотреть, что получилось: сколько нод в группе, degree, дети,
// и сколько сейчас параллельных связей к sharedNeighbor (ожидаем 2: одна от оригинала,
// одна от дубля — после merge в Step 6c основного скрипта должно стать 0 "лишних").
// -------------------------------------------------------------------------------------
WITH "RICH" AS targetCmdb
MATCH (n:SoftwareSystem {graphTag: "Global"})
WHERE toLower(n.cmdb) = toLower(targetCmdb)
OPTIONAL MATCH (n)-[:Child]->(child)
WITH n, count(DISTINCT child) AS directChildren
RETURN n.cmdb AS cmdb, id(n) AS nodeId, coalesce(n.seedTest, false) AS isSeedDuplicate,
       COUNT { (n)--() } AS degree, directChildren
ORDER BY isSeedDuplicate, cmdb;


// -------------------------------------------------------------------------------------
// SECTION E. Откат / уборка. Запускайте:
//   - если передумали и хотите начать заново, ДО прогона normalize-cmdb-case.cypher —
//     удалит все seedTest-ноды/связи целиком, оригинал не тронет;
//   - ПОСЛЕ прогона normalize-cmdb-case.cypher — к этому моменту дубли-ноды уже
//     удалены самим merge'ем (apoc.refactor.mergeNodes), останутся только
//     seedTest-контейнер(ы) и seedTest-связь, повисшие на оригинальной ноде —
//     этот запрос уберёт и их, вернув dev-граф в исходное состояние.
// -------------------------------------------------------------------------------------
MATCH ()-[r:Relationship {seedTest: true}]->() DELETE r;
MATCH ()-[c:Child {seedTest: true}]->() DELETE c;
MATCH (c:Container {seedTest: true}) DETACH DELETE c;
MATCH (dup:SoftwareSystem {seedTest: true}) DETACH DELETE dup;
