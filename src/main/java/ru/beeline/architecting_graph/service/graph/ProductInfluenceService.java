package ru.beeline.architecting_graph.service.graph;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.repository.neo4j.SoftwareSystemRepository;
import ru.beeline.fdmlib.dto.graph.ProductInfluenceDTO;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductInfluenceService {

    @Autowired
    private Driver driver;

    @Autowired
    SoftwareSystemRepository softwareSystemRepository;

    public ProductInfluenceDTO getRelatedSystems(String cmdb) {
        try (Session session = driver.session()) {
            if (!softwareSystemRepository.productExists(session, cmdb)) {
                throw new NoSuchElementException("Продукт с cmdb = " + cmdb + " не найден");
            }
            List<String> dependentSystems = softwareSystemRepository.getDependentSystems(session, cmdb);
            List<String> influencingSystems = softwareSystemRepository.getInfluencingSystems(session, cmdb);

            return new ProductInfluenceDTO(
                    dependentSystems != null ?
                            dependentSystems.stream().distinct().collect(Collectors.toList()) :
                            Collections.emptyList(),
                    influencingSystems != null ?
                            influencingSystems.stream().distinct().collect(Collectors.toList()) :
                            Collections.emptyList()
            );
        }
    }
}
