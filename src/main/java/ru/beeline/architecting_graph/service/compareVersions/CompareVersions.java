package ru.beeline.architecting_graph.service.compareVersions;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.graph.commonFunctions.CommonFunctions;
import ru.beeline.architecting_graph.graph.graphObject.GraphObject;

import java.util.Set;

@Slf4j
@Service
public class CompareVersions {

    private final Driver driver;

    public CompareVersions(Driver driver) {
        this.driver = driver;
    }

    public ResponseEntity<String> compareVersion(String cmdb, Integer firstVersion, Integer secondVersion) {
        try (Session session = driver.session()) {
            GraphObject systemGraphObject = GraphObject.createGraphObject("SoftwareSystem", "cmdb", cmdb);
            boolean exists = CommonFunctions.checkIfObjectExists(session, "Global", systemGraphObject);

            if (!exists) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Система не найдена");
            }

            String curVersionString = CommonFunctions.getObjectParameter(session, "Global", systemGraphObject, "version")
                    .toString();

            if (curVersionString.equals("NULL")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("У данной системы отсутствует версионность");
            }

            Integer curVersion = Integer.parseInt(curVersionString);

            if (secondVersion == null) {
                secondVersion = curVersion;
            }

            if (firstVersion.equals(secondVersion)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Версии не могут быть равны");
            } else if (Math.min(firstVersion, secondVersion) < 1 || Math.max(firstVersion, secondVersion) > curVersion) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Неверное значение версий");
            }

            // упорядочим версии
            if (firstVersion > secondVersion) {
                int tmp = firstVersion;
                firstVersion = secondVersion;
                secondVersion = tmp;
            }

            Set<Pair> addElements = FindChanges.LaterChanges(firstVersion, secondVersion, curVersion, cmdb, session);
            Set<Pair> removeElements = FindChanges.EarlierChanges(firstVersion, secondVersion, curVersion, cmdb, session);

            String out = "{\n\t\"addElements\": [\n" + constactOutput(addElements) + "\t],\n\t\"removeElements\": [\n"
                    + constactOutput(removeElements) + "\t]\n}";

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(out);

        } catch (ServiceUnavailableException e) {

            log.info("Нет подключения к БД", e);
            return ResponseEntity.badRequest().body("Нет подключения к БД");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при сравнении версий");
        }
    }

    private String constactOutput(Set<Pair> elements) {
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (Pair element : elements) {
            boolean isLast = count == elements.size() - 1;
            if (element.getSecond().equals("Relationship")) {
                out.append(element.getFirst());
            } else {
                out.append("\t\t{\n")
                        .append("\t\t\t\"name\": \"").append(element.getFirst()).append("\",\n")
                        .append("\t\t\t\"type\": \"").append(element.getSecond()).append("\"\n")
                        .append("\t\t}");
            }
            if (!isLast) {
                out.append(",\n");
            } else {
                out.append("\n");
            }
            count++;
        }
        return out.toString();
    }
}

