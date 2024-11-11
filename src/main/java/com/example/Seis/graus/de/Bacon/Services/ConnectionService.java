package com.example.Seis.graus.de.Bacon.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;


import java.util.*;

@Service
public class ConnectionService {

    @Value("${tmdb.api.url}")
    private String tmdbApiUrl;

    @Value("${tmdb.api.token}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, Map<String, String>> graph = new HashMap<>();



    public void buildGraph(String actorName1, String actorName2) {
        String actorId1 = getActorId(actorName1);
        String actorId2 = getActorId(actorName2);

        if (actorId1 != null) buildConnectionsForActor(actorId1);
        if (actorId2 != null) buildConnectionsForActor(actorId2);
    }

    // Constrói as conexões para um ator específico, incluindo o filme que conecta os atores
    private void buildConnectionsForActor(String actorId) {
        if (graph.containsKey(actorId)) return; // Se já existe, não reconstrói

        List<String> movies = getMoviesByActor(actorId);
        for (String movie : movies) {
            Map<String, String> coActors = getCoActors(movie);
            for (String coActorId : coActors.keySet()) {
                if (!coActorId.equals(actorId)) {
                    graph.computeIfAbsent(actorId, k -> new HashMap<>()).put(coActorId, movie);
                    graph.computeIfAbsent(coActorId, k -> new HashMap<>()).put(actorId, movie);
                }
            }
        }
    }

    // Busca o ID de um ator pelo nome
    private String getActorId(String actorName) {
        String url = tmdbApiUrl + "/search/person?query=" + actorName + "&api_key=" + apiKey + "&language=pt-BR";
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        if (response.getBody() != null && response.getBody().has("results") && !response.getBody().get("results").isEmpty()) {
            return response.getBody().get("results").get(0).get("id").asText();
        }
        return null;
    }

    // Busca os filmes de um ator pelo ID, em português
    private List<String> getMoviesByActor(String actorId) {
        String url = tmdbApiUrl + "/person/" + actorId + "/movie_credits?api_key=" + apiKey + "&language=pt-BR";
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        List<String> movies = new ArrayList<>();
        if (response.getBody() != null && response.getBody().has("cast")) {
            for (JsonNode movie : response.getBody().get("cast")) {
                movies.add(movie.get("title").asText()); // Título em português
            }
        }
        return movies;
    }

    // Busca os IDs e nomes dos co-atores de um filme específico
    private Map<String, String> getCoActors(String movieTitle) {
        String movieId = getMovieId(movieTitle);
        if (movieId == null) {
            return Collections.emptyMap();
        }

        String url = tmdbApiUrl + "/movie/" + movieId + "/credits?api_key=" + apiKey + "&language=pt-BR";
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        Map<String, String> coActors = new HashMap<>();
        if (response.getBody() != null && response.getBody().has("cast")) {
            for (JsonNode actor : response.getBody().get("cast")) {
                String actorId = actor.get("id").asText();
                String actorName = actor.get("name").asText();
                coActors.put(actorId, actorName);
            }
        }
        return coActors;
    }

    // Busca o ID de um filme pelo título, com título em português
    private String getMovieId(String movieTitle) {
        String url = tmdbApiUrl + "/search/movie?query=" + movieTitle + "&api_key=" + apiKey + "&language=pt-BR";
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        if (response.getBody() != null && response.getBody().has("results") && !response.getBody().get("results").isEmpty()) {
            return response.getBody().get("results").get(0).get("id").asText();
        }
        return null;
    }

    // Realiza a busca em largura para encontrar o caminho entre dois atores, incluindo filmes
    public List<String> findConnection(String actorName1, String actorName2) {
        buildGraph(actorName1, actorName2);

        String actorId1 = getActorId(actorName1);
        String actorId2 = getActorId(actorName2);

        if (actorId1 == null || actorId2 == null) {
            return Collections.singletonList("Um ou ambos os atores não foram encontrados na base de dados.");
        }

        Queue<List<String>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.add(Collections.singletonList(actorId1));
        visited.add(actorId1);

        while (!queue.isEmpty()) {
            List<String> path = queue.poll();
            String currentActorId = path.get(path.size() - 1);

            if (currentActorId.equals(actorId2)) {
                return buildConnectionPath(path);
            }

            for (String neighbor : graph.getOrDefault(currentActorId, new HashMap<>()).keySet()) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    List<String> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);
                    queue.add(newPath);
                    buildConnectionsForActor(neighbor); // Expande o grafo dinamicamente
                }
            }
        }

        return Collections.singletonList("Nenhuma conexão encontrada entre " + actorName1 + " e " + actorName2 + ".");
    }

    // Constrói o caminho de conexão, incluindo os filmes entre os atores
    private List<String> buildConnectionPath(List<String> path) {
        List<String> connectionPath = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            String actorId1 = path.get(i);
            String actorId2 = path.get(i + 1);
            String movie = graph.get(actorId1).get(actorId2);
            connectionPath.add(getActorNameById(actorId1) + " -- " + movie + " --> " + getActorNameById(actorId2));
        }
        return connectionPath;
    }

    // Busca o nome de um ator pelo ID
    private String getActorNameById(String actorId) {
        String url = tmdbApiUrl + "/person/" + actorId + "?api_key=" + apiKey + "&language=pt-BR";
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        if (response.getBody() != null && response.getBody().has("name")) {
            return response.getBody().get("name").asText();
        }
        return "Nome desconhecido";
    }


}
