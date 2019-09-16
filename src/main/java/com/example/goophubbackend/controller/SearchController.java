package com.example.goophubbackend.controller;


import com.complexible.stardog.ext.spring.ConnectionCallback;
import com.complexible.stardog.ext.spring.SnarlTemplate;
import com.complexible.stardog.ext.spring.mapper.SimpleRowMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.stardog.stark.query.BindingSet;
import com.stardog.stark.query.SelectQueryResult;
import com.complexible.stardog.StardogException;
import com.complexible.stardog.api.Connection;
import com.complexible.stardog.api.SelectQuery;
import com.complexible.stardog.api.search.SearchConnection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/search")
public class SearchController {

    @Autowired
    public SnarlTemplate snarlTemplate;
    
    @RequestMapping(value = "/sparql{query}", method = RequestMethod.GET)
    @ResponseBody
    public String sparql(@RequestParam(value="query") String query) {
        String resultJSON = "[";
        String eachResult = "";
        if(query.isEmpty()) {
            return "{\"error\" : \"Query Empty\"}";
        }
        // Queries the database using the SnarlTemplate and gets back a list of mapped objects
        List<Map<String, String>> results = snarlTemplate.query(query, new SimpleRowMapper());
        String[] element;
        if(!results.isEmpty()) {
            for (int i = 0; i < results.size(); i++) {
                //element = results.get(i).toString().split(",");
                //System.out.println(element[0] + " - " + element[1] + " - " + element[2]);
                eachResult = results.get(i).toString().replace("\"", "");
                eachResult = eachResult.replace("=", "\":\"");
                eachResult = eachResult.replace("{", "{\"");
                eachResult = eachResult.replace("}", "\"}");
                eachResult = eachResult.replace(", ", "\", \"");
                eachResult = eachResult.replace("^^", "");
                if(results.size() == 1) {
                    resultJSON += eachResult;
                }
                if(i != results.size()-1) {
                    if (i == 0)
                        resultJSON += eachResult;
                    else
                        resultJSON += ", " + eachResult;
                }
            }
        }
        resultJSON = resultJSON + "]";
        return resultJSON;
    }

    @RequestMapping(value = "/query{query}", method = RequestMethod.GET)
    @ResponseBody
    public String goopSearch(@RequestParam(value="query") String query) {
        System.out.println("Query request with param: " + query);
        // Full text search has the ability to do exactly that. Search the database for a specific value.
        // Here we will specify that we only want results over a score of `0.5`, and no more than `2` results
        // for things that match the search term `man`. Below we will perform the search in two different ways.
        String finalResult = "";
        snarlTemplate.setReasoning(false);
        finalResult = snarlTemplate.execute(new ConnectionCallback<String>() {
            @Override
            public String doWithConnection(Connection connection) {
            	try {
            		String aQuery =
                            "PREFIX goop: <https://nemo.inf.ufes.br/dev/ontology/Goop#>\n" +
                            "SELECT DISTINCT ?s ?type WHERE {\n" +
                                "\t?s a?type ." +
                                "\t?s rdfs:label ?o .\n" +
                                "\t(?o ?score) <" + SearchConnection.MATCH_PREDICATE + "> ( \"" + query + "\" 0.5).\n" +
                            "}";
            		/*
            		"PREFIX goop: <https://nemo.inf.ufes.br/dev/ontology/Goop#>\n" +
                    "SELECT DISTINCT ?s ?type WHERE {\n" +
                        "\t?s a?type ." +
                        "\t?s rdfs:label ?o .\n" +
                        "\t(?o ?score) <" + SearchConnection.MATCH_PREDICATE + "> ( \"" + query + "\" 0.5).\n" +
                        "\tFILTER (?type IN (goop:Atomic_Goal, goop:Complex_Goal))" +
                    "}";
                    */

                    SelectQuery queryMatch = connection
                            .select(aQuery);
                    
                    JsonObject response = new JsonObject();
                    JsonArray goops = new JsonArray();
                   
                    
                    

                    String resultJSON = "{\"goops\": [";
                    String element;
                    String elementType;
                    String[] elementList;

                    try (SelectQueryResult aResult = queryMatch.execute()) {
                        System.out.println("Query results: ");
                        while (aResult.hasNext()) {
                        	BindingSet result = aResult.next();
                        	JsonObject eachGoop = new JsonObject();
                        	
                    		elementList = result.get("s").toString().split("#");
                    		element = elementList.length > 1 ? element = elementList[1] : elementList[0];
                    		int i = element.lastIndexOf("/");
                    		element = element.substring(i+1);  
                            elementType = result.get("type").toString().split("#")[1];
                                                        
                            eachGoop.addProperty("name", element.replace("_", " "));
                            eachGoop.addProperty("type", elementType.replace("_", " "));
                            eachGoop.addProperty("iri", result.get("s").toString());
                                                        
                            goops.add(eachGoop);
                                                       
                            element = "";
                            elementType = "";
                            
                        }
                        response.add("goops", goops);
                        return response.toString();
                    }
                } catch (StardogException e) {
                    System.out.println("Error with full text search: " + e);
                    return "Error with full text search: " + e;
                }
            }
        });
        return finalResult;
    }
        
    @RequestMapping(value = "/querygoal{query}", method = RequestMethod.GET)
    @ResponseBody
    public String queryGoal(@RequestParam(value="query") String query) {
        String goal = query;
        System.out.println("Goal: " + goal);
        String eachResult = "";
        
        JsonObject response = new JsonObject();
        JsonArray classes = new JsonArray();
        JsonArray properties = new JsonArray();
        
        if(query.isEmpty()) {
            return "{\"error\" : \"Query Empty\"}";
        }

        String dafaultClassQuery =
                "PREFIX goop: <https://nemo.inf.ufes.br/dev/ontology/Goop#>\n" +
                "SELECT ?class\n" +
                    "WHERE {\n" +
                        "\t?goop a goop:Goop ." +
                        "\t?goop goop:used_to_achieve <" + goal + ">." +
                        "\t?goop goop:composed_by ?class ." +
                        "\t?class a goop:owl:Class ." +
                    "}";

        String dafaultPropertyQuery =
                "PREFIX goop: <https://nemo.inf.ufes.br/dev/ontology/Goop#>\n" +
                        "SELECT ?obj\n" +
                        "WHERE {\n" +
                        "\t?goop a goop:Goop ." +
                        "\t?goop goop:used_to_achieve <" + goal + ">." +
                        "\t?goop goop:composed_by ?obj ." +
                        "\t?obj a goop:owl:Object_Property ." +
                        "}";

        // Queries the database using the SnarlTemplate and gets back a list of mapped objects
        List<Map<String, String>> resultsClass = snarlTemplate.query(dafaultClassQuery, new SimpleRowMapper());
        String[] element;
        if(!resultsClass.isEmpty()) {
            for (int i = 0; i < resultsClass.size(); i++) {
                element = resultsClass.get(i).toString().split("=");
                eachResult = element[1].substring(0, element[1].length()-1);
                
                classes.add(eachResult);
                
            }
        }

        List<Map<String, String>> resultsProperty = snarlTemplate.query(dafaultPropertyQuery, new SimpleRowMapper());
        if(!resultsProperty.isEmpty()) {
            for (int i = 0; i < resultsProperty.size(); i++) {
                element = resultsProperty.get(i).toString().split("=");
                eachResult = element[1].substring(0, element[1].length()-1);
                properties.add(eachResult);
            }
        }
        response.add("classes", classes);
        response.add("properties", properties);
        return response.toString();
    }

}
