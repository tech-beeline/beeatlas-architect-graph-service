package ru.beeline.architecting_graph.compareVersions;

import java.util.Set;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.beeline.architecting_graph.graph.commonFunctions.CommonFunctions;
import ru.beeline.architecting_graph.graph.graphConstruction.GraphConstruction;
import ru.beeline.architecting_graph.graph.graphObject.GraphObject;
import ru.beeline.architecting_graph.otherObjects.RestConfig;

public class CompareVersions {

    public static String constactOutput(Set<Pair> Elements) {
        String out = "";

        Integer numberOfElement = 0;
        for (Pair curElement : Elements) {
            if (numberOfElement != Elements.size() - 1) {
                if (curElement.getSecond().equals("Relationship")) {
                    out = out + curElement.getFirst() + ",\n";
                } else {
                    out = out + "\t\t{\n" + "\t\t\t\"name\": \"" + curElement.getFirst() + "\",\n\t\t\t\"type\": \""
                            + curElement.getSecond() + "\"\n\t\t},\n";
                }
            } else {
                if (curElement.getSecond().equals("Relationship")) {
                    out = out + curElement.getFirst() + "\n";
                } else {
                    out = out + "\t\t{\n" + "\t\t\t\"name\": \"" + curElement.getFirst() + "\",\n\t\t\t\"type\": \""
                            + curElement.getSecond() + "\"\n\t\t}\n";
                }
            }
            numberOfElement = numberOfElement + 1;
        }

        return out;
    }

    public static ResponseEntity<String> compareVersion(RestConfig autorization, String cmdb, Integer firstVersion,
            Integer secondVersion) {

        Driver driver = GraphDatabase.driver(autorization.getUri(),
                AuthTokens.basic(autorization.getUser(), autorization.getPassword()));

        Session session;

        try {
            session = GraphConstruction.connectToDatabase(driver, autorization);
        } catch (ServiceUnavailableException e) {
            return ResponseEntity.badRequest().body("Нет подключения к БД");
        }

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

        if (firstVersion == secondVersion) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Версии не могут быть равны");
        } else if (Math.min(firstVersion, secondVersion) < 1 || Math.max(firstVersion, secondVersion) > curVersion) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Неверное значение версий");
        }

        if (firstVersion > secondVersion) {
            firstVersion = firstVersion + secondVersion;
            secondVersion = firstVersion - secondVersion;
            firstVersion = firstVersion - secondVersion;
        }

        Set<Pair> addElements = FindChanges.LaterChanges(firstVersion, secondVersion, curVersion, cmdb, session);
        Set<Pair> removeElements = FindChanges.EarlierChanges(firstVersion, secondVersion, curVersion, cmdb, session);

        String out = "{\n\t\"addElements\": [\n" + constactOutput(addElements) + "\t],\n\t\"removeElements\": [\n"
                + constactOutput(removeElements) + "\t]\n}";

        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(out);
    }

}
