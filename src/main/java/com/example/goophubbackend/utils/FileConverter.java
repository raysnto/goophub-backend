package com.example.goophubbackend.utils;

import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class FileConverter {

    private String getGoopMetamodel() {
        File file;
        try {
            file = ResourceUtils.getFile("C:\\Users\\gabri\\OneDrive\\Documentos\\Mestrado\\GoopHub\\goophub-backend\\src\\main\\resources\\goop-meta-model.owl");
            InputStream in = new FileInputStream(file);
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Path getTempFile(Boolean isConverted) {
        Path file;
        if(isConverted) {
            String rootPath = System.getProperty("user.dir");
            file = Paths.get(rootPath + "/src/main/resources/" + "temp-converted.owl");
            //file = ResourceUtils.getFile("classpath:temp-converted.owl");
        } else {
            String rootPath = System.getProperty("user.dir");
            file = Paths.get(rootPath + "/src/main/resources/" + "temp.owl");
            //file = ResourceUtils.getFile("classpath:temp.owl");
        }
        return file;
    }

    public void convertOWLtoGoopAtomic(String file, String actorName, String goalId, String url, String uuid) {
        
        try {
            // Creating Ontology Manager
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

            // Loading GOOP Metamodel
            OWLOntology goop = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(getGoopMetamodel()));

            // Loading source ontology and creating a resource factory
            OWLOntology ontologiaAlvo = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(file));
            String sourceIRI = ontologiaAlvo.getOntologyID().getOntologyIRI().get().toString();
            OWLDataFactory factory = OWLManager.getOWLDataFactory();

            // Reasoner
            OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
            OWLReasoner reasoner = reasonerFactory.createReasoner(ontologiaAlvo);
            reasoner.precomputeInferences();

            // Utils
            OWLIndividual clss;
            OWLIndividual subClss;
            OWLIndividual objectProperty;
            OWLIndividual actor;
            OWLIndividual goal;
            List<OWLIndividual> classIndividualsList = new LinkedList<OWLIndividual>();

            String actorRole = actorName;
            String goalName = goalId;

            // Classes needed for GOOPs
            OWLClass goopType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#Goop"));
            OWLClass classType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#owl:Class"));
            OWLClass objType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#owl:Object_Property"));
            OWLClass actorType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#Actor"));
            OWLClass goalType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#Atomic_Goal"));

            // Object Properties needed for GOOP
            OWLObjectProperty subClassOf = factory.getOWLObjectProperty("http://www.w3.org/2002/07/owl#subClassOf");
            OWLObjectProperty composedBy = factory.getOWLObjectProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#composed_by");
            OWLObjectProperty achievesGoalOf = factory.getOWLObjectProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#achieves_goal_of");
            OWLObjectProperty usedToAchieve = factory.getOWLObjectProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#used_to_achieve");
            OWLObjectProperty has = factory.getOWLObjectProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#has");
            OWLObjectProperty domain = factory.getOWLObjectProperty("http://www.w3.org/2002/07/owl#domain");
            OWLObjectProperty range = factory.getOWLObjectProperty("http://www.w3.org/2002/07/owl#range");

            // Data Propety needed to image property
            OWLDataProperty hasURL = factory.getOWLDataProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#hasURL");
            OWLDataProperty hasUUID = factory.getOWLDataProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#hasUUID");

            // Variables to label entities
            OWLLiteral lbl;
            OWLAnnotation label;
            OWLAxiom labelAxiom;


            OWLClassAssertionAxiom classAxiom;

            // Creating GOOP of source ontology
            int auxIndex = sourceIRI.lastIndexOf("/");
            String goopName = sourceIRI.substring(auxIndex + 1);
            OWLIndividual placeGoop = factory.getOWLNamedIndividual(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#" + goopName));
            classAxiom = factory.getOWLClassAssertionAxiom(goopType, placeGoop);
            manager.applyChange(new AddAxiom(goop, classAxiom));

            actor = factory.getOWLNamedIndividual(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#" + actorRole));

            // Add Label to Actor
            lbl = factory.getOWLLiteral(actorName);
            label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
            labelAxiom = factory.getOWLAnnotationAssertionAxiom(actor.asOWLNamedIndividual().getIRI(), label);
            manager.applyChange(new AddAxiom(goop, labelAxiom));

            classAxiom = factory.getOWLClassAssertionAxiom(actorType, actor);
            manager.applyChange(new AddAxiom(goop, classAxiom));

            goal = factory.getOWLNamedIndividual(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#" + goalName));
            classAxiom = factory.getOWLClassAssertionAxiom(goalType, goal);
            manager.applyChange(new AddAxiom(goop, classAxiom));

            // Add Label to Goal
            lbl = factory.getOWLLiteral(goalName.replace("_", " "));
            label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
            labelAxiom = factory.getOWLAnnotationAssertionAxiom(goal.asOWLNamedIndividual().getIRI(), label);
            manager.applyChange(new AddAxiom(goop, labelAxiom));

            System.out.println("----- Retrieving All Classes of " + ontologiaAlvo.getOntologyID().getOntologyIRI().get() + " -----");

            Set<OWLClass> subClses;
            Set<OWLClass> placeClasses = ontologiaAlvo.getClassesInSignature();
            for (OWLClass iter : placeClasses) {
                System.out.println(iter.toString());

                String iterName = iter.toString().replace("<", "").replace(">", "");

                clss = factory.getOWLNamedIndividual(iterName);
                classAxiom = factory.getOWLClassAssertionAxiom(classType, clss);
                AddAxiom addClassAxiom = new AddAxiom(goop, classAxiom);

                // Add label to Classes
                lbl = factory.getOWLLiteral(((OWLNamedIndividual) clss).getIRI().getFragment());
                label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
                labelAxiom = factory.getOWLAnnotationAssertionAxiom(clss.asOWLNamedIndividual().getIRI(), label);
                manager.applyChange(new AddAxiom(goop, labelAxiom));

                manager.applyChange(addClassAxiom);

                classIndividualsList.add(clss);

                // Reasoning over direct subClasses
                subClses = reasoner.getSubClasses(iter, true).getFlattened();
                for (OWLClass iter2 : subClses) {
                    System.out.println("\t" + iter2.toString());
                    String iter2Name = iter2.toString().replace("<", "").replace(">", "");
                    subClss = factory.getOWLNamedIndividual(iter2Name);
                    if (!iter2.toString().equals("owl:Nothing")) {
                        OWLObjectPropertyAssertionAxiom subClassOfAxiom = factory.getOWLObjectPropertyAssertionAxiom(subClassOf, subClss, clss);
                        manager.applyChange(new AddAxiom(goop, subClassOfAxiom));
                        // Add label to Classes
                        lbl = factory.getOWLLiteral(((OWLNamedIndividual) subClss).getIRI().getFragment());
                        label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
                        labelAxiom = factory.getOWLAnnotationAssertionAxiom(subClss.asOWLNamedIndividual().getIRI(), label);
                        manager.applyChange(new AddAxiom(goop, labelAxiom));
                    }
                }
            }

            // Add composedBy relations between Goop -> Classes
            for (OWLIndividual iter : classIndividualsList) {
                OWLObjectPropertyAssertionAxiom composedByAxiom = factory.getOWLObjectPropertyAssertionAxiom(composedBy, placeGoop, iter);
                manager.applyChange(new AddAxiom(goop, composedByAxiom));
            }

            System.out.println("----- Retrieving All Object Properties of " + ontologiaAlvo.getOntologyID().getOntologyIRI().get() + " -----");

            Set<OWLObjectProperty> placeObj = ontologiaAlvo.getObjectPropertiesInSignature();
            for (OWLObjectProperty iter : placeObj) {
                System.out.println(iter.toString());
                String iterName = iter.toString().replace("<", "").replace(">", "");

                objectProperty = factory.getOWLNamedIndividual(iterName);
                classAxiom = factory.getOWLClassAssertionAxiom(objType, objectProperty);
                AddAxiom addClassAxiom = new AddAxiom(goop, classAxiom);
                manager.applyChange(addClassAxiom);

                // Add label to ObjectProperties
                lbl = factory.getOWLLiteral(((OWLNamedIndividual) objectProperty).getIRI().getFragment());
                label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
                labelAxiom = factory.getOWLAnnotationAssertionAxiom(objectProperty.asOWLNamedIndividual().getIRI(), label);
                manager.applyChange(new AddAxiom(goop, labelAxiom));

                OWLObjectPropertyAssertionAxiom composedByAxiom = factory.getOWLObjectPropertyAssertionAxiom(composedBy, placeGoop, objectProperty);
                manager.applyChange(new AddAxiom(goop, composedByAxiom));

                // Defining the domain of each property
                Set<OWLClass> domainSet = reasoner.getObjectPropertyDomains(iter, true).getFlattened();
                for (OWLClass iter2 : domainSet) {
                    for (OWLIndividual iter3 : classIndividualsList) {
                        String iter3Name = iter3.toString().replace("<", "").replace(">", "");
                        String iter2Name = iter2.toString().replace("<", "").replace(">", "");
                        if (iter3Name.equals(iter2Name)) {
                            OWLObjectPropertyAssertionAxiom domainAxiom = factory.getOWLObjectPropertyAssertionAxiom(domain, objectProperty, iter3);
                            manager.applyChange(new AddAxiom(goop, domainAxiom));
                        }
                    }
                }

                // Defining the range of each property
                Set<OWLClass> rangeSet = reasoner.getObjectPropertyRanges(iter, true).getFlattened();
                for (OWLClass iter2 : rangeSet) {
                    for (OWLIndividual iter3 : classIndividualsList) {
                        String iter3Name = iter3.toString().replace("<", "").replace(">", "");
                        String iter2Name = iter2.toString().replace("<", "").replace(">", "");
                        if (iter3Name.equals(iter2Name)) {
                            OWLObjectPropertyAssertionAxiom rangeAxiom = factory.getOWLObjectPropertyAssertionAxiom(range, objectProperty, iter3);
                            manager.applyChange(new AddAxiom(goop, rangeAxiom));
                        }
                    }
                }
            }

            // Add composedBy relations between Goop -> Classes
            for (OWLIndividual iter : classIndividualsList) {
                OWLObjectPropertyAssertionAxiom composedByAxiom = factory.getOWLObjectPropertyAssertionAxiom(composedBy, placeGoop, iter);
                manager.applyChange(new AddAxiom(goop, composedByAxiom));
            }

            OWLObjectPropertyAssertionAxiom achievesAxiom = factory.getOWLObjectPropertyAssertionAxiom(achievesGoalOf, placeGoop, actor);
            manager.applyChange(new AddAxiom(goop, achievesAxiom));

            OWLObjectPropertyAssertionAxiom goalAxiom = factory.getOWLObjectPropertyAssertionAxiom(usedToAchieve, placeGoop, goal);
            manager.applyChange(new AddAxiom(goop, goalAxiom));

            OWLObjectPropertyAssertionAxiom hasAxiom = factory.getOWLObjectPropertyAssertionAxiom(has, actor, goal);
            manager.applyChange(new AddAxiom(goop, hasAxiom));

            OWLDataPropertyAssertionAxiom dataPropertyAssertion = factory.getOWLDataPropertyAssertionAxiom(hasURL, placeGoop, url);
            manager.applyChange(new AddAxiom(goop, dataPropertyAssertion));

            OWLDataPropertyAssertionAxiom uuidPropertyAssertion = factory.getOWLDataPropertyAssertionAxiom(hasUUID, placeGoop, uuid);
            manager.applyChange(new AddAxiom(goop, uuidPropertyAssertion));


            File fileformated = getTempFile(false).toFile();

            manager.saveOntology(goop, IRI.create(fileformated.toURI()));
            return;
        } catch (Exception e) {
            return;
        }
    }

    public String convertOWLtoGoopComplex(String file, String actorName, String goalId, String decompositionType, JsonArray atomicGoals, String url, String uuid) {

        try {
            // Creating Ontology Manager
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

            // Loading GOOP Metamodel
            System.out.println("Loading GOOP Metamodel");
            OWLOntology goop = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(getGoopMetamodel()));

            // Loading source ontology and creating a resource factory
            System.out.println("Loading source ontology and creating a resource factory");
            OWLOntology ontologiaAlvo = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(file));
            String sourceIRI = ontologiaAlvo.getOntologyID().getOntologyIRI().get().toString();
            OWLDataFactory factory = OWLManager.getOWLDataFactory();

            // Reasoner
            OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
            OWLReasoner reasoner = reasonerFactory.createReasoner(ontologiaAlvo);
            reasoner.precomputeInferences();

            // Utils
            OWLIndividual clss;
            OWLIndividual subClss;
            OWLIndividual objectProperty;
            OWLIndividual actor;
            OWLIndividual goal;
            List<OWLIndividual> classIndividualsList = new LinkedList<OWLIndividual>();

            String actorRole = actorName;
            String goalName = goalId;

            // Classes needed for GOOPs
            OWLClass goopType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#Goop"));
            OWLClass classType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#owl:Class"));
            OWLClass objType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#owl:ObjectProperty"));
            OWLClass actorType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#Actor"));
            OWLClass complexGoalType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#Complex_Goal"));
            OWLClass atomicGoalType = factory.getOWLClass(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#Atomic_Goal"));

            // Object Properties needed for GOOP
            OWLObjectProperty subClassOf = factory.getOWLObjectProperty("http://www.w3.org/2002/07/owl#subClassOf");
            OWLObjectProperty composedBy = factory.getOWLObjectProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#composed_by");
            OWLObjectProperty achievesGoalOf = factory.getOWLObjectProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#achieves_goal_of");
            OWLObjectProperty usedToAchieve = factory.getOWLObjectProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#used_to_achieve");
            OWLObjectProperty has = factory.getOWLObjectProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#has");
            OWLObjectProperty domain = factory.getOWLObjectProperty("http://www.w3.org/2002/07/owl#domain");
            OWLObjectProperty range = factory.getOWLObjectProperty("http://www.w3.org/2002/07/owl#range");            
            OWLObjectProperty decompositionProperty;

            // Data Propety needed to image property
            OWLDataProperty hasURL = factory.getOWLDataProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#hasURL");
            OWLDataProperty hasUUID = factory.getOWLDataProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#hasUUID");
            


            // Decomposition Property
            if (decompositionType.equals("AND_decomposition")) {
                decompositionProperty = factory.getOWLObjectProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#AND_decomposition");
            }
            else {
                decompositionProperty = factory.getOWLObjectProperty("https://nemo.inf.ufes.br/dev/ontology/Goop#OR_decomposition");
            }

            // Variables to label entities
            OWLLiteral lbl;
            OWLAnnotation label;
            OWLAxiom labelAxiom;


            OWLClassAssertionAxiom classAxiom;

            // Creating GOOP of source ontology
            System.out.println("Creating GOOP of source ontology");
            int auxIndex = sourceIRI.lastIndexOf("/");
            String goopName = sourceIRI.substring(auxIndex + 1);
            System.out.println(goopName);
            OWLIndividual placeGoop = factory.getOWLNamedIndividual(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#" + goopName));
            classAxiom = factory.getOWLClassAssertionAxiom(goopType, placeGoop);
            manager.applyChange(new AddAxiom(goop, classAxiom));

            actor = factory.getOWLNamedIndividual(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#" + actorRole));

            // Add Label to Actor
            System.out.println("Add Label to Actor");
            lbl = factory.getOWLLiteral(actorName);
            label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
            labelAxiom = factory.getOWLAnnotationAssertionAxiom(actor.asOWLNamedIndividual().getIRI(), label);
            manager.applyChange(new AddAxiom(goop, labelAxiom));

            classAxiom = factory.getOWLClassAssertionAxiom(actorType, actor);
            manager.applyChange(new AddAxiom(goop, classAxiom));

            goal = factory.getOWLNamedIndividual(IRI.create(goop.getOntologyID().getOntologyIRI().get() + "#" + goalName));
            classAxiom = factory.getOWLClassAssertionAxiom(complexGoalType, goal);
            manager.applyChange(new AddAxiom(goop, classAxiom));

            // Add Label to Goal
            System.out.println("Add Label to Goal");
            lbl = factory.getOWLLiteral(goalName.replace("_", " "));
            label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
            labelAxiom = factory.getOWLAnnotationAssertionAxiom(goal.asOWLNamedIndividual().getIRI(), label);
            manager.applyChange(new AddAxiom(goop, labelAxiom));

            // Handle with Atomics
            System.out.println("Handle with Atomics");
            for(JsonElement atomicGoal : atomicGoals) {
                OWLNamedIndividual atomicIndividual = factory.getOWLNamedIndividual(atomicGoal.getAsJsonObject().get("uri").getAsString());
                classAxiom = factory.getOWLClassAssertionAxiom(atomicGoalType, atomicIndividual);
                manager.applyChange(new AddAxiom(goop, classAxiom));

                // Add Label to Atomic Goals
                String atomicName = atomicGoal.getAsJsonObject().get("name").getAsString();
                lbl = factory.getOWLLiteral(atomicName.replace("_", " "));
                label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
                labelAxiom = factory.getOWLAnnotationAssertionAxiom(atomicIndividual.asOWLNamedIndividual().getIRI(), label);
                manager.applyChange(new AddAxiom(goop, labelAxiom));

                OWLObjectPropertyAssertionAxiom decompositionAxiom = factory.getOWLObjectPropertyAssertionAxiom(decompositionProperty, goal, atomicIndividual);
                manager.applyChange(new AddAxiom(goop, decompositionAxiom));
            }

            System.out.println("----- Retrieving All Classes of " + ontologiaAlvo.getOntologyID().getOntologyIRI().get() + " -----");

            Set<OWLClass> subClses;
            Set<OWLClass> placeClasses = ontologiaAlvo.getClassesInSignature();
            for (OWLClass iter : placeClasses) {
                System.out.println(iter.toString());

                String iterName = iter.toString().replace("<", "").replace(">", "");

                clss = factory.getOWLNamedIndividual(iterName);
                classAxiom = factory.getOWLClassAssertionAxiom(classType, clss);
                AddAxiom addClassAxiom = new AddAxiom(goop, classAxiom);

                // Add label to Classes
                lbl = factory.getOWLLiteral(((OWLNamedIndividual) clss).getIRI().getFragment());
                label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
                labelAxiom = factory.getOWLAnnotationAssertionAxiom(clss.asOWLNamedIndividual().getIRI(), label);
                manager.applyChange(new AddAxiom(goop, labelAxiom));

                manager.applyChange(addClassAxiom);

                classIndividualsList.add(clss);

                // Reasoning over direct subClasses
                subClses = reasoner.getSubClasses(iter, true).getFlattened();
                for (OWLClass iter2 : subClses) {
                    System.out.println("\t" + iter2.toString());
                    String iter2Name = iter2.toString().replace("<", "").replace(">", "");
                    subClss = factory.getOWLNamedIndividual(iter2Name);
                    if (!iter2.toString().equals("owl:Nothing")) {
                        OWLObjectPropertyAssertionAxiom subClassOfAxiom = factory.getOWLObjectPropertyAssertionAxiom(subClassOf, subClss, clss);
                        manager.applyChange(new AddAxiom(goop, subClassOfAxiom));
                        // Add label to Classes
                        lbl = factory.getOWLLiteral(((OWLNamedIndividual) subClss).getIRI().getFragment());
                        label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
                        labelAxiom = factory.getOWLAnnotationAssertionAxiom(subClss.asOWLNamedIndividual().getIRI(), label);
                        manager.applyChange(new AddAxiom(goop, labelAxiom));
                    }
                }
            }

            // Add composedBy relations between Goop -> Classes
            for (OWLIndividual iter : classIndividualsList) {
                OWLObjectPropertyAssertionAxiom composedByAxiom = factory.getOWLObjectPropertyAssertionAxiom(composedBy, placeGoop, iter);
                manager.applyChange(new AddAxiom(goop, composedByAxiom));
            }

            System.out.println("----- Retrieving All Object Properties of " + ontologiaAlvo.getOntologyID().getOntologyIRI().get() + " -----");

            Set<OWLObjectProperty> placeObj = ontologiaAlvo.getObjectPropertiesInSignature();
            for (OWLObjectProperty iter : placeObj) {
                System.out.println(iter.toString());
                String iterName = iter.toString().replace("<", "").replace(">", "");

                objectProperty = factory.getOWLNamedIndividual(iterName);
                classAxiom = factory.getOWLClassAssertionAxiom(objType, objectProperty);
                AddAxiom addClassAxiom = new AddAxiom(goop, classAxiom);
                manager.applyChange(addClassAxiom);

                // Add label to ObjectProperties
                lbl = factory.getOWLLiteral(((OWLNamedIndividual) objectProperty).getIRI().getFragment());
                label = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()), lbl);
                labelAxiom = factory.getOWLAnnotationAssertionAxiom(objectProperty.asOWLNamedIndividual().getIRI(), label);
                manager.applyChange(new AddAxiom(goop, labelAxiom));

                OWLObjectPropertyAssertionAxiom composedByAxiom = factory.getOWLObjectPropertyAssertionAxiom(composedBy, placeGoop, objectProperty);
                manager.applyChange(new AddAxiom(goop, composedByAxiom));

                // Defining the domain of each property
                Set<OWLClass> domainSet = reasoner.getObjectPropertyDomains(iter, true).getFlattened();
                for (OWLClass iter2 : domainSet) {
                    for (OWLIndividual iter3 : classIndividualsList) {
                        String iter3Name = iter3.toString().replace("<", "").replace(">", "");
                        String iter2Name = iter2.toString().replace("<", "").replace(">", "");
                        if (iter3Name.equals(iter2Name)) {
                            OWLObjectPropertyAssertionAxiom domainAxiom = factory.getOWLObjectPropertyAssertionAxiom(domain, objectProperty, iter3);
                            manager.applyChange(new AddAxiom(goop, domainAxiom));
                        }
                    }
                }

                // Defining the range of each property
                Set<OWLClass> rangeSet = reasoner.getObjectPropertyRanges(iter, true).getFlattened();
                for (OWLClass iter2 : rangeSet) {
                    for (OWLIndividual iter3 : classIndividualsList) {
                        String iter3Name = iter3.toString().replace("<", "").replace(">", "");
                        String iter2Name = iter2.toString().replace("<", "").replace(">", "");
                        if (iter3Name.equals(iter2Name)) {
                            OWLObjectPropertyAssertionAxiom rangeAxiom = factory.getOWLObjectPropertyAssertionAxiom(range, objectProperty, iter3);
                            manager.applyChange(new AddAxiom(goop, rangeAxiom));
                        }
                    }
                }
            }

            // Add composedBy relations between Goop -> Classes
            for (OWLIndividual iter : classIndividualsList) {
                OWLObjectPropertyAssertionAxiom composedByAxiom = factory.getOWLObjectPropertyAssertionAxiom(composedBy, placeGoop, iter);
                manager.applyChange(new AddAxiom(goop, composedByAxiom));
            }

            OWLObjectPropertyAssertionAxiom achievesAxiom = factory.getOWLObjectPropertyAssertionAxiom(achievesGoalOf, placeGoop, actor);
            manager.applyChange(new AddAxiom(goop, achievesAxiom));

            OWLObjectPropertyAssertionAxiom goalAxiom = factory.getOWLObjectPropertyAssertionAxiom(usedToAchieve, placeGoop, goal);
            manager.applyChange(new AddAxiom(goop, goalAxiom));

            OWLObjectPropertyAssertionAxiom hasAxiom = factory.getOWLObjectPropertyAssertionAxiom(has, actor, goal);
            manager.applyChange(new AddAxiom(goop, hasAxiom));

            OWLDataPropertyAssertionAxiom dataPropertyAssertion = factory.getOWLDataPropertyAssertionAxiom(hasURL, placeGoop, url);
            manager.applyChange(new AddAxiom(goop, dataPropertyAssertion));
            
            OWLDataPropertyAssertionAxiom uuidPropertyAssertion = factory.getOWLDataPropertyAssertionAxiom(hasUUID, placeGoop, uuid);
            manager.applyChange(new AddAxiom(goop, uuidPropertyAssertion));

            File fileformated = getTempFile(false).toFile();

            StringDocumentTarget target = new StringDocumentTarget();
            

            manager.saveOntology(goop, target);
            
            //manager.saveOntology(goop, IRI.create(fileformated.toURI()));
            return target.toString();
        } catch (Exception e) {
            String error = "Erro ao gerar o arquivo: " + e.getMessage();
            return error;
        }
    }

    public String convertGoopToOWL(List<Map<String, String>> classes, List<Map<String, String>> properties) {

        String fileContent = "Success";
        try {
            // Creating Ontology Manager
            String sourceIRI = classes.get(0).toString();
            sourceIRI = sourceIRI.split("=")[1];
            sourceIRI = sourceIRI.replace("\"", "");
            sourceIRI = sourceIRI.replace("}", "");
            sourceIRI = sourceIRI.split("#")[0];

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontologiaAlvo = manager.createOntology(IRI.create(sourceIRI));
            OWLDataFactory factory = OWLManager.getOWLDataFactory();

            // Converting Classes
            for (Map<String, String> map : classes) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    value = value.replace("\"", "");

                    OWLClass c = factory.getOWLClass(IRI.create(value));
                    OWLAxiom declareC = factory.getOWLDeclarationAxiom(c);
                    // adding declareC to the ontology is necessary to have any output
                    manager.addAxiom(ontologiaAlvo, declareC);
                }
            }

            // Converting Object Properties"
            for (Map<String, String> map : properties) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    value = value.replace("\"", "");

                    OWLObjectProperty c = factory.getOWLObjectProperty(IRI.create(value));
                    OWLAxiom declareC = factory.getOWLDeclarationAxiom(c);
                    // adding declareC to the ontology is necessary to have any output
                    manager.addAxiom(ontologiaAlvo, declareC);

                }
            }

            File fileformated = getTempFile(true).toFile();
            manager.saveOntology(ontologiaAlvo, IRI.create(fileformated.toURI()));
            
            InputStream in = new FileInputStream(fileformated);
            fileContent = IOUtils.toString(in, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return fileContent;
        }
    }
}
