package com.example.goophubbackend.controller;

import com.complexible.stardog.ext.spring.SnarlTemplate;
import com.example.goophubbackend.utils.FileConverter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stardog.stark.io.RDFFormats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import java.util.UUID;

import javax.servlet.ServletContext;

import java.util.Map;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@RestController
@RequestMapping("/upload")
public class UploadController {

    @Autowired
    SnarlTemplate snarlTemplate;


    @Autowired
    ServletContext context;

    @Value("${cloudinary.url}")
    private String cloudinary_url;
    
    @RequestMapping(value = "/complex", method = RequestMethod.POST)
    @ResponseBody
    public JsonObject uploadfile(@RequestParam(value="name") String name, @RequestParam(value="email") String email,
                         @RequestParam(value="organization") String organization, @RequestParam(value="role") String role,
                         @RequestParam(value="goal") String goal, @RequestParam(value="atomics") String atomicGoals,
                         @RequestParam(value="decomposition") String decomposition, @RequestParam("file")MultipartFile[] files,
                         @RequestParam("image")MultipartFile[] images) {
        
        Cloudinary cloudinary = new Cloudinary(cloudinary_url);

        String uuid = UUID.randomUUID().toString();
        FileConverter converter = new FileConverter();
        String url = "";
        
        try {

            System.out.println("Upload Request:");
            System.out.println("\tName: " + name + "\tEmail: " + email);
            System.out.println("\tOrganization: " + organization + "\tRole: " + role);
            System.out.println("\tGoal: " + goal);
            System.out.println("\tGoal Decomposition: " + decomposition);
            System.out.println("\tAtomic Goals: " + atomicGoals);
            JsonParser jsonParser = new JsonParser();
            JsonArray atomicGoalArray = jsonParser.parse(atomicGoals).getAsJsonArray();
            System.out.println("\tAtomic Goals Converted: " + atomicGoalArray);

            StringBuilder imagesNames = new StringBuilder();
            
            if(goal.isEmpty() || (files.length == 0 || files.length > 1)) {
                JsonObject json = new JsonObject();
                json.addProperty("error", "Upload error: " + "Invalid Form");
                return json;
            }

            // OWL file
            for (MultipartFile file : files) {
                try {
                    String rootPath = System.getProperty("user.dir");
                    file.transferTo(Paths.get(rootPath + "/src/main/resources/" + file.getOriginalFilename()));
                    //Files.write(, file.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Image File
            for (MultipartFile image : images) {
                String extension = image.getOriginalFilename().split("\\.")[image.getOriginalFilename().split("\\.").length - 1];
                String imageName = uuid + "." + extension;

                String rootPath = System.getProperty("user.dir");
                image.transferTo(Paths.get(rootPath + "/src/main/resources/" + imageName));
                try {
                    Map uploadResult = cloudinary.uploader().upload(ResourceUtils.getFile("classpath:" + imageName), ObjectUtils.emptyMap());
                    url = uploadResult.get("url").toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("\tImage: "+ imagesNames.toString());


            byte[] fileContent = files[0].getBytes();
            String s = new String(fileContent);

            String complexGoopContent = converter.convertOWLtoGoopComplex(s, role, goal.replace(" ", "_"), decomposition, atomicGoalArray, url, uuid);
            Thread.sleep(5000);
            if (complexGoopContent.contains("Erro ao gerar o arquivo: ")){
                throw new Exception(complexGoopContent);
            }
            return snarlTemplate.execute(connection -> {
                try{
                	System.out.println("Inserindo arquivo");
                    InputStream targetStream = new ByteArrayInputStream(complexGoopContent.getBytes());
                    connection.add().io().format(RDFFormats.RDFXML).stream(targetStream);
                    //connection.add().io().file(ResourceUtils.getFile("classpath:temp.owl").toPath());
                    JsonObject json = new JsonObject();
                    json.addProperty("info", "upload completed.");
                    return json;
                }
                catch (Exception e) {
                    JsonObject json = new JsonObject();
                    json.addProperty("error", "Upload error: " + e.getMessage());
                    return json;
                }
            });
        }
        catch (Exception e) {
            JsonObject json = new JsonObject();
            json.addProperty("error", "Upload error: " + e.getMessage());
            return json;
        }
    }

    @RequestMapping(value = "/atomic", method = RequestMethod.POST)
    @ResponseBody
    public JsonObject uploadAtomicFile(@RequestParam(value="name") String name, @RequestParam(value="email") String email,
                             @RequestParam(value="organization") String organization, @RequestParam(value="role") String role,
                             @RequestParam(value="goal") String goal,  @RequestParam("file")MultipartFile[] files,
                             @RequestParam("image")MultipartFile[] images) {

        Cloudinary cloudinary = new Cloudinary(cloudinary_url);
        String uuid = UUID.randomUUID().toString();
        String url = "";
        FileConverter converter = new FileConverter();

        try {
            System.out.println("Upload Request:");
            System.out.println("\tName: " + name + "\tEmail: " + email);
            System.out.println("\tOrganization: " + organization + "\tRole: " + role);

            if(goal.isEmpty() || (files.length == 0 || files.length > 1)) {
                Exception e = new Exception("Invalid Form");
                throw e;
            }

            // OWL File
            for (MultipartFile file : files) {
                try {
                    String rootPath = System.getProperty("user.dir");
                    file.transferTo(Paths.get(rootPath + "\\src\\main\\resources\\" + file.getOriginalFilename()));
                    //Files.write(, file.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // Image File
            for (MultipartFile image : images) {
                String extension = image.getOriginalFilename().split("\\.")[image.getOriginalFilename().split("\\.").length - 1];
                String imageName = uuid + "." + extension;

                String rootPath = System.getProperty("user.dir");
                image.transferTo(Paths.get(rootPath + "/src/main/resources/" + imageName));
                try {
                    Map uploadResult = cloudinary.uploader().upload(ResourceUtils.getFile("classpath:" + imageName), ObjectUtils.emptyMap());
                    url = uploadResult.get("url").toString();
                } catch (IOException e) {
                    JsonObject json = new JsonObject();
                    json.addProperty("error", "Upload error: " + e.getMessage());
                    return json;
                }
            }

            byte[] fileContent = files[0].getBytes();
            String s = new String(fileContent);

            
            converter.convertOWLtoGoopAtomic(s, role, goal.replace(" ", "_"), url, uuid);
            Thread.sleep(5000);
            // Add file to DataBase
            return snarlTemplate.execute(connection -> {
                try{
                    connection.add().io().file(ResourceUtils.getFile("C:\\Users\\gabri\\OneDrive\\Documentos\\Mestrado\\GoopHub\\goophub-backend\\src\\main\\resources\\temp.owl").toPath());
                    JsonObject json = new JsonObject();
                    json.addProperty("info", "upload completed.");
                    return json;
                }
                catch (Exception e) {
                    JsonObject json = new JsonObject();
                    json.addProperty("error", "Upload error: " + e.getMessage());
                    return json;
                }
            });
        }
        catch (Exception e) {
            JsonObject json = new JsonObject();
            json.addProperty("error", "Upload error: " + e.getMessage());
            return json;
        }
    }
}