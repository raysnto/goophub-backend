package com.example.goophubbackend.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.complexible.stardog.ext.spring.SnarlTemplate;
import com.complexible.stardog.ext.spring.mapper.SimpleRowMapper;
import com.example.goophubbackend.utils.FileConverter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/download")
public class DownloadController {

    @Autowired
    public SnarlTemplate snarlTemplate;

    private static final Logger logger = LoggerFactory.getLogger(DownloadController.class);

    @RequestMapping(value = "/file{uri}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody byte[] download(@RequestParam(value="uri") String uri) {

        FileConverter converter = new FileConverter();
        try {
            System.out.println("[/concepts] Query request with param: " + uri);
            String goal = uri;            
            goal = URLDecoder.decode(uri, StandardCharsets.UTF_8.toString());
            goal = goal.replace("\"", "");
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

            // Queries the database using the SnarlTemplate and gets back a list of mapped objects
            List<Map<String, String>> resultsClass = snarlTemplate.query(defaultClassQuery, new SimpleRowMapper());
            System.out.println("[/download]: " + resultsClass.toString());
            
            List<Map<String, String>> resultsProperty = snarlTemplate.query(defaultPropertyQuery, new SimpleRowMapper());
            System.out.println("[/download]: " + resultsProperty.toString());
            
            String fileContent = converter.convertGoopToOWL(resultsClass, resultsProperty);
            InputStream targetStream = new ByteArrayInputStream(fileContent.getBytes());
            return IOUtils.toByteArray(targetStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}


