/*
 * $Id: NewMatcher.java 1399 2010-03-30 14:07:03Z euzenat $
 *
 * Copyright (C) 2006-2010, INRIA
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.albertmeronyo.matchers;

// Alignment API classes
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.AlignmentException;

// Alignment API implementation classes
import fr.inrialpes.exmo.align.impl.ObjectAlignment;

import fr.inrialpes.exmo.ontowrap.Ontology;
import fr.inrialpes.exmo.ontowrap.OntologyFactory;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;


/**
 * The Skeleton of code for extending the alignment API
 */

public class LeanOMMatcher extends ObjectAlignment implements AlignmentProcess{
	private Object[] entitiesA;
	private Object[] entitiesB;
	private double[][] scores;

    public LeanOMMatcher() {
    }
    
    private void initEntities() {
    	try{
    		entitiesA = new Object[ontology1().getEntities().size()];
    		entitiesB = new Object[ontology2().getEntities().size()];
    		int i = 0;
    		for (Object o: ontology1().getEntities()) {
    			entitiesA[i++] = o;
    		}
    		i = 0;
    		for (Object o: ontology2().getEntities()) {
    			entitiesB[i++] = o;
    		}
    	} catch (OntowrapException e) {
    		e.printStackTrace();
    	}
    }
    
    private void initScores() {
    	scores = new double[entitiesA.length][entitiesB.length];
    	for (int i = 0; i < entitiesA.length; i++) {
    		for (int j = 0; j < entitiesB.length; j++) {
    			scores[i][j] = 1.0;
    		}
    	}
    }
    
    private int getEntityIndex(Object[] e, Object value) {
    	for (int i = 0; i < e.length; i++) {
    		if (e[i].equals(value)) {
    			return i;
    		}
    	}
    	return -1;
    }
    
    private void printScores() {
    	for (int i = 0; i < entitiesA.length; i++) {
 	        for (int j = 0; j < entitiesB.length; j++) {
 	            System.out.print(scores[i][j] + " ");
 	        }
 	        System.out.print("\n");
 	    }
    }
    
    private void printEntities() {
    	System.out.println("Entities index A");
    	for (int i = 0; i < entitiesA.length; i++) {
    		System.out.println(Integer.toString(i) + ", " + entitiesA[i].toString());
    	}
    	System.out.println("Entities index B");
    	for (int i = 0; i < entitiesB.length; i++) {
    		System.out.println(Integer.toString(i) + ", " + entitiesB[i].toString());
    	}
    }
    
    private void printModelStatements(Model m) {
    	StmtIterator iter = m.listStatements();
    	while (iter.hasNext()) {
            Statement stmt      = iter.nextStatement();         // get next statement
            Resource  subject   = stmt.getSubject();   // get the subject
            Property  predicate = stmt.getPredicate(); // get the predicate
            RDFNode   object    = stmt.getObject();    // get the object
            
            System.out.print(subject.toString());
            System.out.print(" " + predicate.toString() + " ");
            if (object instanceof Resource) {
                System.out.print(object.toString());
            } else {
                // object is a literal
                System.out.print(" \"" + object.toString() + "\"");
            }
            System.out.println(" .");
        }
    }

    /**
     * The only method to implement is align.
     * All the resources for reading the ontologies and rendering the alignment are from ObjectAlignment and its superclasses:
     * - ontology1() and ontology2() returns objects LoadedOntology
     * - addAlignCell adds a new mapping in the alignment object  
     */
    public void align( Alignment alignment, Properties param ) throws AlignmentException {
    	initEntities();
    	//printEntities();
    	initScores();
		try {
		    // Match classes
		    for ( Object cl2: ontology2().getClasses() ){
		    	int indexB = getEntityIndex(entitiesB, cl2);
		    	for ( Object cl1: ontology1().getClasses() ){
		    		int indexA = getEntityIndex(entitiesA, cl1);
		    		scores[indexA][indexB] = match(cl1, cl2, 1);
		    	}
		    }
		    
		    // Match dataProperties
		    for ( Object p2: ontology2().getDataProperties() ){
		    	int indexB = getEntityIndex(entitiesB, p2);
		    	for ( Object p1: ontology1().getDataProperties() ){
		    		int indexA = getEntityIndex(entitiesA, p1);
	    			scores[indexA][indexB] = match(p1, p2, 2);
		    	}
		    }
		    // Match objectProperties
		    for ( Object p2: ontology2().getObjectProperties() ){
		    	int indexB = getEntityIndex(entitiesB, p2);
		    	for ( Object p1: ontology1().getObjectProperties() ){
		    		int indexA = getEntityIndex(entitiesA, p1);
	    			scores[indexA][indexB] = match(p1, p2, 3);
		    	}
		    }
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
		HungarianAlgorithm hungarianAlgorithm = new HungarianAlgorithm(scores);
		int[] assignments = hungarianAlgorithm.execute();
		for (int i = 0; i < assignments.length; i++) {
			if (assignments[i] != -1 && scores[i][assignments[i]] < 0.2) {
				addAlignCell(entitiesA[i], entitiesB[assignments[i]], "=", scores[i][assignments[i]]);
			}
		}
	}

    public double match(Object o1, Object o2, int type) throws AlignmentException, OWLException, URISyntaxException {    	
    	String tempFile1 = "to1";
    	String tempFile2 = "to2";
    	String extension = ".owl";
    	
    	File t1 = null;
    	File t2 = null;
    	File t3 = null;
		try {
			t1 = File.createTempFile(tempFile1, extension);
			t2 = File.createTempFile(tempFile2, extension);
			t3 = File.createTempFile(tempFile1 + tempFile2, extension);
		} catch (IOException e) {
			e.printStackTrace();
		}    	    	
    	
//    	IRI to1IRI = IRI.create(o1.toString().substring(1,o1.toString().length()-1));
//    	IRI to2IRI = IRI.create(o2.toString().substring(1,o2.toString().length()-1));
//    	IRI d1IRI = IRI.create("file:" + pathTempFile1);
//    	IRI d2IRI = IRI.create("file:" + pathTempFile2);
    	
    	String concept1Label = o1.toString().substring(o1.toString().indexOf('#') + 1, o1.toString().length()-1);
    	String concept2Label = o2.toString().substring(o2.toString().indexOf('#') + 1, o2.toString().length()-1);
    	String concept1URI = "file:" + t1.getAbsolutePath() + "#" + concept1Label;
    	String concept2URI = "file:" + t2.getAbsolutePath() + "#" + concept2Label;
    	URI to1URI = URI.create(concept1URI);
    	URI to2URI = URI.create(concept2URI);
    	
    	OntModel model1 = ModelFactory.createOntologyModel();
    	OntModel model2 = ModelFactory.createOntologyModel();
    	OntClass co1 = null;
    	OntClass co2 = null;
    	ObjectProperty opo1 = null;
    	ObjectProperty opo2 = null;
    	DatatypeProperty dpo1 = null;
    	DatatypeProperty dpo2 = null;
    	if (type == 1) {
    		co1 = model1.createClass(to1URI.toString());
    		co2 = model2.createClass(to2URI.toString());
    	} else if (type == 2) {
    		opo1 = model1.createObjectProperty(to1URI.toString());
    		opo2 = model2.createObjectProperty(to2URI.toString());
    	} else {
    		dpo1 = model1.createDatatypeProperty(to1URI.toString());
    		dpo2 = model2.createDatatypeProperty(to2URI.toString());
    	}
    	try {
    		model1.write(new FileOutputStream(t1), "RDF/XML-ABBREV");
    		model2.write(new FileOutputStream(t2), "RDF/XML-ABBREV");
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
//    	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

//    	SimpleIRIMapper mapper1 = new SimpleIRIMapper(to1IRI, d1IRI);
//    	SimpleIRIMapper mapper2 = new SimpleIRIMapper(to2IRI, d2IRI);
//        manager.addIRIMapper(mapper1);
//        manager.addIRIMapper(mapper2);
//        try {
//        	//System.out.println(to1IRI);
//        	//System.out.println(to2IRI);
//        	org.semanticweb.owlapi.model.OWLOntology to1 = manager.createOntology(to1IRI);
//        	org.semanticweb.owlapi.model.OWLOntology to2 = manager.createOntology(to2IRI);
//        	org.semanticweb.owlapi.model.OWLDataFactory dataFactory = manager.getOWLDataFactory();
//        	OWLEntity co1 = null;
//        	OWLEntity co2 = null;
//        	if (type == 1) {
//	        	co1 = dataFactory.getOWLClass(to1IRI);
//	        	co2 = dataFactory.getOWLClass(to2IRI);
//
//        	} else if (type == 2) {
//        		co1 = dataFactory.getOWLDataProperty(to1IRI);
//	        	co2 = dataFactory.getOWLDataProperty(to2IRI);
//        	} else {
//        		co1 = dataFactory.getOWLObjectProperty(to1IRI);
//	        	co2 = dataFactory.getOWLObjectProperty(to2IRI);
//        	}
//        	org.semanticweb.owlapi.model.OWLDeclarationAxiom declarationAxiom1 = dataFactory.getOWLDeclarationAxiom(co1);
//        	org.semanticweb.owlapi.model.OWLDeclarationAxiom declarationAxiom2 = dataFactory.getOWLDeclarationAxiom(co2);
//        	manager.addAxiom(to1, declarationAxiom1);
//        	manager.addAxiom(to2, declarationAxiom2);
//        	File to1File = File.createTempFile(tempFile1, extension);
////        	System.out.println(to1.toString());
////        	System.out.println(d1IRI.toString());
////        	manager.saveOntology(to1, IRI.create(to1File));
//        	TurtleOntologyFormat turtle = new TurtleOntologyFormat();
////        	manager.saveOntology(to1, turtle, IRI.create(to1File));
//        	OutputStream oto1 = new ByteArrayOutputStream();
//        	manager.saveOntology(to1, oto1);
////        	manager.saveOntology(to2, d2IRI);
//        } catch (OWLOntologyCreationException e) {
//        	e.printStackTrace();
//        } catch (OWLOntologyStorageException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
        try {
	        OutputStream out = new FileOutputStream(t3);
	        byte[] buf = new byte[1024];
	        Vector<String> files = new Vector<String>();
	        files.add(t1.getAbsolutePath());
	        files.add(t2.getAbsolutePath());
	        for (String file : files) {
	            InputStream in = new FileInputStream(file);
	            int b = 0;
	            while ( (b = in.read(buf)) >= 0) {
	                out.write(buf, 0, b);
	                out.flush();
	            }
	        }
	        out.close();
        	double s1 = zipFile(new FileInputStream(t1.getAbsolutePath()), t1.getAbsolutePath() + ".zip");
        	double s2 = zipFile(new FileInputStream(t2.getAbsolutePath()), t2.getAbsolutePath() + ".zip");
        	double s12 = zipFile(new FileInputStream(t3.getAbsolutePath()), t3.getAbsolutePath() + ".zip");
        	return Math.max((s12 - s1)/s2, (s12 - s2)/s1);
        } catch (FileNotFoundException e) {
        	e.printStackTrace();
        } catch (IOException e) {
			e.printStackTrace();
		}
 
//		}
        
    	return 0.0;
    }
    
    private long zipFile(InputStream fileStream, String name) {
        try {
        	FileOutputStream zippedfile = new FileOutputStream(name);
        	GZIPOutputStream zip = new GZIPOutputStream(zippedfile);

        	// buffer
        	int lenght;
        	byte[] buffer = new byte[1024];
        	while ((lenght = fileStream.read(buffer)) > 0)
        		zip.write(buffer, 0, lenght);
        	
        	zip.finish();
        	zip.close();
        	zippedfile.close();
        	fileStream.close();

        	File tempfile = new File(name);
        	long size = tempfile.length();
        	tempfile.delete();

        	return size;

        } catch (FileNotFoundException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        }
        return 0;
    }
}
