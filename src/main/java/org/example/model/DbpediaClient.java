package org.example.model;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;


import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


import org.example.model.asianCountry.CountryFact;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class DbpediaClient {
    public static final String DEFAULT_ENDPOINT = "https://dbpedia.org/sparql";
    
    private final String endpoint;
    private final HttpClient http;
    private final Duration requestTimeout;
    private final int maxRetries;
    private final Random rnd = new Random();
    
    public DbpediaClient() {
        this(DEFAULT_ENDPOINT, Duration.ofSeconds(12),2);
    }


    public DbpediaClient(String endpoint, Duration timeout, int maxRetries) {
        this.endpoint = Objects.requireNonNull(endpoint);
        this.requestTimeout = timeout == null ? Duration.ofSeconds(12) : timeout;
        this.maxRetries = Math.max(0, maxRetries);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    ///API

    public List<CountryFact> getRandomAsianCountryFacts(int limit) throws IOException, InterruptedException {
        String sparql = """
                PREFIX dbo:  <http://dbpedia.org/ontology/>
                        PREFIX dbp:  <http://dbpedia.org/property/>
                        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                        PREFIX dct:  <http://purl.org/dc/terms/>
                        PREFIX dbc:  <http://dbpedia.org/resource/Category:>
                
                        SELECT DISTINCT ?country ?countryLabel ?capital ?capitalLabel ?population ?thumbnail WHERE {
                          # Asian countries (DBpedia category is more complete than dbo:continent)
                          ?country a dbo:Country ;
                                   dct:subject dbc:Countries_in_Asia .
                
                          # English country label
                          ?country rdfs:label ?countryLabel .
                          FILTER ( langMatches(lang(?countryLabel), "EN") )
                
                          # Exclude dissolved countries (Virtuoso-safe way)
                          OPTIONAL { ?country dbo:dissolutionYear ?_diss }
                          FILTER ( !BOUND(?_diss) )
                
                          # Capital (accept dbo:capital or dbp:capital that points to a resource)
                          OPTIONAL {
                            { ?country dbo:capital ?capital }
                            UNION
                            { ?country dbp:capital ?capital . FILTER( isIRI(?capital) ) }
                
                            ?capital rdfs:label ?capitalLabel .
                            FILTER ( langMatches(lang(?capitalLabel), "EN") )
                          }
                
                          # Optional extras
                          OPTIONAL { ?country dbo:populationTotal ?population }
                          OPTIONAL { ?country dbo:thumbnail ?thumbnail }
                        }
                        ORDER BY RAND()
                """.formatted(Math.max(1, limit));

        JSONObject json = executeSelect(sparql);
        return parseCountryFacts(json);
    }

    public List<String> getAsianCapitalName(int limit) throws IOException, InterruptedException {
        String sparql = """
                PREFIX dbo:  <http://dbpedia.org/ontology/>
                        PREFIX dbp:  <http://dbpedia.org/property/>
                        PREFIX dbr:  <http://dbpedia.org/resource/>
                        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                        PREFIX dct:  <http://purl.org/dc/terms/>
                        PREFIX dbc:  <http://dbpedia.org/resource/Category:>
                
                        SELECT DISTINCT ?capitalLabel WHERE {
                          # Use category for Asia instead of dbo:continent (more complete on DBpedia)
                          ?country a dbo:Country ;
                                   dct:subject dbc:Countries_in_Asia .
                
                          # Accept dbo:capital or dbp:capital (skip literals)
                          { ?country dbo:capital ?capital }
                          UNION
                          { ?country dbp:capital ?capital . FILTER(isIRI(?capital)) }
                
                          ?capital rdfs:label ?capitalLabel .
                          FILTER(langMatches(lang(?capitalLabel), "EN"))
                        }
                        ORDER BY RAND()
                """.formatted(Math.max(1, limit));

        JSONObject json = executeSelect(sparql);
        JSONArray rows = json.getJSONObject("results").getJSONArray("bindings");
        List<String> out = new ArrayList<>();
        for(int i=0; i<rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String label = getBindingValue(row, "capitalLabel");
            if(label != null && !label.isBlank()){
                out.add(label);
            }
        }
        return  out;
    }

    private JSONObject executeSelect(String sparql) throws IOException, InterruptedException {
        String body = "query=" + URLEncoder.encode(sparql, StandardCharsets.UTF_8)
                + "&format=" + URLEncoder.encode("application/sparql-results+json", StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Accept","application/sparql-results+json")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .timeout(requestTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        int attempt = 0;
        while(true) {
            try{
                HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
                int status = resp.statusCode();
                if(status >= 200 && status < 300) {
                    System.out.println("Fetch data success!");
                    return new JSONObject(resp.body());
                }else if(status == 429 || status > 500) {
                    System.out.println("Try again");
                    if (attempt++ < maxRetries) {
                        backoff(attempt);
                        continue;
                    }
                    throw new IOException("DBpedia HTTP " + status + ": " + resp.body());
                }else{
                    throw new IOException("DBpedia HTTP " + status + ": " + resp.body());
                }
            }catch(IOException | InterruptedException e) {
                if(attempt++ < maxRetries) {
                    backoff(attempt);
                    continue;
                }
                throw e;
            }
        }
    }

    private void backoff(int attempt) throws InterruptedException {
        long sleep = (long)Math.min(4000, 300*Math.pow(2, attempt - 1)) + rnd.nextInt(200);
        Thread.sleep(sleep);
    }

    private static String getBindingValue(JSONObject row, String var) {
        if (!row.has(var)) return null;
        JSONObject cell = row.getJSONObject(var);
        return cell.optString("value", null);
    }


    private static Long getBindingLong(JSONObject row, String var) {
        String v = getBindingValue(row, var);
        if (v == null) return null;
        try {
            // population values can be big; parse safely
            return Long.parseLong(v.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }


    private static List<CountryFact> parseCountryFacts(JSONObject json) throws JSONException {
        JSONArray rows = json.getJSONObject("results").getJSONArray("bindings");
        List<CountryFact> out = new ArrayList<>();

        for(int i=0; i< rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String countryUri = getBindingValue(row, "country");
            String countryLabel = getBindingValue(row, "countryLabel");
            String capitalUri = getBindingValue(row, "capital");
            String capitalLabel = getBindingValue(row, "capitalLabel");
            Long population = getBindingLong(row, "population");
            String thumbnail = getBindingValue(row, "thumbnail");

            System.out.println("/ncountry: "+countryLabel+"; city: "+capitalLabel);

            if (countryUri == null || countryLabel == null) continue;
            out.add(new CountryFact(countryUri, countryLabel, capitalUri, capitalLabel, population, thumbnail));
        }
        return  out;
    }

}