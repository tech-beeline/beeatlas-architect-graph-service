package ru.beeline.architecting_graph.graph.functionsForJson;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ru.beeline.architecting_graph.graphAPI.RestConfig;

public class FunctionsForWorkingWithJson {

    public static String getJson(Long docId, RestConfig autorization) throws Exception {

        RestTemplate restTemplate = new RestTemplate();
        String url = autorization.getDocUrl() + "/api/v1/documents/" + Long.toString(docId);

        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            return new String(response.getBody(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        }
    }

    public static String changeJson(String json, RestConfig autorization) throws Exception {
        String url = autorization.getGraphvizUrl();
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(json, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new Exception("Failed to call Graphviz API: " + e.getMessage(), e);
        }
    }
}
