package com.example.goophubbackend.controller;

import com.complexible.stardog.ext.spring.SnarlTemplate;
import com.complexible.stardog.ext.spring.mapper.SimpleRowMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.complexible.stardog.StardogException;
import com.complexible.stardog.api.search.SearchConnection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    public SnarlTemplate snarlTemplate;
    
    /**
     * Advanced query using SPARQL Sintax
     * @param query
     * @return
     */
    @RequestMapping(value = "/advanced{query}", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public JsonObject sparql(@RequestParam(value="query") String query) {
        try {
            JsonObject jsonObject = new JsonObject();
            System.out.println("[/advanced] Query request with param: " + query);
            String finalQuery = query;    
            finalQuery = URLDecoder.decode(query, StandardCharsets.UTF_8.toString());
            System.out.println("[/advanced] Query request with param: " + finalQuery);
            if(finalQuery.isEmpty()) {
                jsonObject.addProperty("error", "No query provided");
                return jsonObject;
            }
            // Queries the database using the SnarlTemplate and gets back a list of mapped objects
            List<Map<String, String>> results = snarlTemplate.query(finalQuery, new SimpleRowMapper());
            System.out.println("[/advanced]" + results.toString());
            if(!results.isEmpty()) {
                JsonArray jsonArray = new JsonArray();
                for(Map<String, String> result : results) {
                    JsonObject resultJson = new JsonObject();
                    for(Map.Entry<String, String> entry : result.entrySet()) {
                        resultJson.addProperty(entry.getKey(), entry.getValue());
                    }                    
                    jsonArray.add(resultJson);
                    jsonObject.add("results", jsonArray);
                }
            }
            System.out.println("[/advanced]" + jsonObject.toString());
            return jsonObject;
        } catch (UnsupportedEncodingException e1) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("error", "Error decoding string");
            return jsonObject;
        }
        catch (StardogException e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("error", "Error querying database");
            return jsonObject;
        }
    }

    /* 
        Consulta para buscar os goops de acordo com o objetivo inserido 
        CORRIGIDA
    */
    @RequestMapping(value = "/goop{query}", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public JsonObject searchGoop(@RequestParam(value="query") String query) {
        System.out.println("[/goop] Query request with param: " + query);
        String finalQuery = query.replace("%20", " ");
        System.out.println("[/goop] Query request with param curated: " + finalQuery);
        if (finalQuery.isEmpty()) {
            return null;
        }
        // Full text search has the ability to do exactly that. Search the database for a specific value.
        // Here we will specify that we only want results over a score of `1.7`.
        snarlTemplate.setReasoning(false);
        String aQuery =
                    "PREFIX goop: <https://nemo.inf.ufes.br/dev/ontology/Goop#>\n" +
                    "SELECT DISTINCT ?uri ?type ?name WHERE {\n" +
                        "\t?uri a?type ." +
                        "\t?uri rdfs:label ?name .\n" +
                        "\t(?name ?score) <" + SearchConnection.MATCH_PREDICATE + "> ( \"" + finalQuery + "\" 1.7).\n" +
                        "\tFILTER (?type IN (goop:Atomic_Goal, goop:Complex_Goal))" +
                    "}";
        List<Map<String, String>> finalResult = snarlTemplate.query(aQuery, new SimpleRowMapper());

        if (finalResult == null) {
            return null;
        }
        
        JsonObject response = new JsonObject();
        JsonArray goops = new JsonArray();
        for(Map<String, String> result : finalResult) {
            JsonObject goop = new JsonObject();
            goop.addProperty("uri", result.get("uri").toString());
            goop.addProperty("type", result.get("type").toString().split("#")[1].replace("_", " "));
            goop.addProperty("name", result.get("name").toString().split("\\^")[0]);
            goops.add(goop);
        }
        response.add("goops", goops);
        return response;
    }
    
    /* Consulta para retornar os elementos do GOOP */
    @RequestMapping(value = "/concepts{query}", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public JsonObject searchConcept(@RequestParam(value="query") String query) {
        try {
            System.out.println("[/concepts] Query request with param: " + query);
            String goal = query;    
            goal = URLDecoder.decode(query, StandardCharsets.UTF_8.toString());
            System.out.println("[/concepts] Goal: " + goal);

            String defaultClassQuery =
                "PREFIX goop: <https://nemo.inf.ufes.br/dev/ontology/Goop#>\n" +
                "SELECT ?class\n" +
                    "WHERE {\n" +
                        "\t?goop a goop:Goop ." +
                        "\t?goop goop:used_to_achieve <" + goal + ">." +
                        "\t?goop goop:composed_by ?class ." +
                        "\t?class a goop:owl:Class ." +
                    "}";

            String defaultPropertyQuery =
                    "PREFIX goop: <https://nemo.inf.ufes.br/dev/ontology/Goop#>\n" +
                            "SELECT ?obj ?domain ?range\n" +
                            "WHERE {\n" +
                            "\t?goop a goop:Goop ." +
                            "\t?goop goop:used_to_achieve <" + goal + ">." +
                            "\t?goop goop:composed_by ?obj ." +
                            "\t?obj a goop:owl:Object_Property ." +
                            "\t?obj  owl:domain ?domain ." +
                            "\t?obj  owl:range ?range ." +
                            "}";

            String defaultURLQuery =
                    "PREFIX goop: <https://nemo.inf.ufes.br/dev/ontology/Goop#>\n" +
                            "SELECT ?uuid\n" +
                            "WHERE {\n" +
                            "\t?goop a goop:Goop ." +
                            "\t?goop goop:used_to_achieve <" + goal + ">." +
                            "\t?goop goop:hasURL ?uuid ." +
                            "}";
            
            JsonObject response = new JsonObject();
            JsonArray classes = new JsonArray();
            JsonArray properties = new JsonArray();
            
            if(goal.isEmpty()) {
                response.addProperty("error", "Query Empty");
                return response;
            }

            // Queries the database using the SnarlTemplate and gets back a list of mapped objects
            List<Map<String, String>> resultsClass = snarlTemplate.query(defaultClassQuery, new SimpleRowMapper());
            System.out.println("[/concepts]: " + resultsClass.toString());
            if(!resultsClass.isEmpty()) {
                for(Map<String, String> result : resultsClass) {
                    JsonObject eachClass = new JsonObject();
                    eachClass.addProperty("name", result.get("class"));
                    classes.add(eachClass);
                }
            }

            List<Map<String, String>> resultsProperty = snarlTemplate.query(defaultPropertyQuery, new SimpleRowMapper());
            System.out.println("[/concepts]: " + resultsProperty.toString());
            if(!resultsProperty.isEmpty()) {
                for(Map<String, String> result : resultsProperty) {
                    JsonObject eachProperty = new JsonObject();
                    eachProperty.addProperty("name", result.get("obj"));
                    eachProperty.addProperty("domain", result.get("domain"));
                    eachProperty.addProperty("range", result.get("range"));
                    properties.add(eachProperty);
                }
            }

            List<Map<String, String>> resultURL = snarlTemplate.query(defaultURLQuery, new SimpleRowMapper());
            System.out.println("[/concepts]: " + resultURL.toString());
            if(!resultURL.isEmpty()) {
                for(Map<String, String> result : resultURL) {
                    response.addProperty("url", result.get("uuid"));
                }
            }

            response.add("classes", classes);
            response.add("properties", properties);
            return response;

        }
        catch (UnsupportedEncodingException e1) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("error", "Error decoding string");
            return jsonObject;
        }
    }
}


