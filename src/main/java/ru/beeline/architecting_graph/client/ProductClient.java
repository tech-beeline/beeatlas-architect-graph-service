package ru.beeline.architecting_graph.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.architecting_graph.dto.ProductInfoShortDTO;

import java.util.List;


@Slf4j
@Service
public class ProductClient {

    RestTemplate restTemplate;
    private final String productServerUrl;

    public ProductClient(@Value("${integration.product-server-url}") String productServerUrl,
                         RestTemplate restTemplate) {
        this.productServerUrl = productServerUrl;
        this.restTemplate = restTemplate;
    }

    public List<ProductInfoShortDTO> getAllProductsInfo() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            log.info("Request to Product ServerUrl: GET /api/v1/product/info");
            ResponseEntity<List<ProductInfoShortDTO>> response = restTemplate.exchange(
                    productServerUrl + "/api/v1/product/info",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<List<ProductInfoShortDTO>>() {}
            );
            log.info("Response from Product ServerUrl: " + response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Product info endpoint not found: " + e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Exception calling product service: " + e.getMessage(), e);
            return List.of();
        }
    }
}
