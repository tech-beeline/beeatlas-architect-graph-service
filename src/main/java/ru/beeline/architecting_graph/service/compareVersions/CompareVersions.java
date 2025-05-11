package ru.beeline.architecting_graph.service.compareVersions;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.exception.NotFoundException;
import ru.beeline.architecting_graph.exception.ValidationException;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.service.graph.CommonFunctions;

import java.util.Set;

@Slf4j
@Service
public class CompareVersions {

    @Autowired
    private Driver driver;

    @Autowired
    FindChanges findChanges;

    public String compareVersion(String cmdb, Integer firstVersion, Integer secondVersion) {
        try (Session session = driver.session()) {
            GraphObject systemGraphObject = new GraphObject("SoftwareSystem", "cmdb", cmdb);
            boolean exists = CommonFunctions.checkIfObjectExists(session, "Global", systemGraphObject);
            if (!exists) {
                throw new NotFoundException("Система не найдена");
            }
            Value versionVal = CommonFunctions.getObjectParameter(session, "Global", systemGraphObject, "version");
            if (versionVal == null || versionVal.isNull()) {
                throw new ValidationException("У данной системы отсутствует версионность");
            }
            String versionValue = versionVal.asString();
            Integer curVersion = Integer.parseInt(versionValue.trim());
            if (secondVersion == null) {
                secondVersion = curVersion;
            }
            if (firstVersion.equals(secondVersion)) {
                throw new ValidationException("Версии не могут быть равны");
            } else if (Math.min(firstVersion, secondVersion) < 1 || Math.max(firstVersion, secondVersion) > curVersion) {
                throw new ValidationException("Неверное значение версий");
            }
            // упорядочим версии
            if (firstVersion > secondVersion) {
                int tmp = firstVersion;
                firstVersion = secondVersion;
                secondVersion = tmp;
            }
            Set<Pair> addElements = findChanges.LaterChanges(firstVersion, secondVersion, curVersion, cmdb, session);
            Set<Pair> removeElements = findChanges.EarlierChanges(firstVersion, secondVersion, curVersion, cmdb, session);
            String out = "{\n\t\"addElements\": [\n" + constactOutput(addElements) + "\t],\n\t\"removeElements\": [\n"
                    + constactOutput(removeElements) + "\t]\n}";
            return out;
        } catch (NotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (ServiceUnavailableException e) {
            log.info("Нет подключения к БД", e);
            throw new ServiceUnavailableException("Нет подключения к БД");
        } catch (Exception e) {
            log.info("Ошибка сервера", e);
            throw new RuntimeException("Ошибка сервера", e);
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

