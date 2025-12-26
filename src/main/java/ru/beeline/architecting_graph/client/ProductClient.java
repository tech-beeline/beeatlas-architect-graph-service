/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.architecting_graph.dto.ProductInfoShortDTO;
import ru.beeline.architecting_graph.dto.ProductInfraSearchDTO;

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

    public List<ProductInfraSearchDTO> getProductInfraByVimIp(String search) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = String.format("%s/api/v1/product/infra/search?parameter=vimIp&value=%s", productServerUrl, search);

            log.info("Request to Product ServerUrl: GET " + url);

            ResponseEntity<List<ProductInfraSearchDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<List<ProductInfraSearchDTO>>() {}
            );
            log.info("Response from Product ServerUrl: " + response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Product infra search endpoint not found: " + e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Exception calling product infra search service: " + e.getMessage(), e);
            return List.of();
        }
    }
}
