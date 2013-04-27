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
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLOntology;

// Alignment API implementation classes
import fr.inrialpes.exmo.align.impl.ObjectAlignment;

import fr.inrialpes.exmo.ontowrap.Ontology;
import fr.inrialpes.exmo.ontowrap.OntologyFactory;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

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
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

/**
 * The Skeleton of code for extending the alignment API
 */

public class LeanOMMatcher extends ObjectAlignment implements AlignmentProcess{


    public LeanOMMatcher() {
    }

    /**
     * The only method to implement is align.
     * All the resources for reading the ontologies and rendering the alignment are from ObjectAlignment and its superclasses:
     * - ontology1() and ontology2() returns objects LoadedOntology
     * - addAlignCell adds a new mapping in the alignment object  
     */
    public void align( Alignment alignment, Properties param ) throws AlignmentException {
	try {
	    // Match classes
	    for ( Object cl2: ontology2().getClasses() ){
		for ( Object cl1: ontology1().getClasses() ){
		    // add mapping into alignment object 
		    addAlignCell(cl1,cl2,"=",match(cl1,cl2));    
		}
	    }
	    // Match dataProperties
//	    for ( Object p2: ontology2().getDataProperties() ){
//		for ( Object p1: ontology1().getDataProperties() ){
//		    // add mapping into alignment object 
//		    addAlignCell(p1,p2,"=",match(p1,p2));    
//		}
//	    }
//	    // Match objectProperties
//	    for ( Object p2: ontology2().getObjectProperties() ){
//		for ( Object p1: ontology1().getObjectProperties() ){
//		    // add mapping into alignment object 
//		    addAlignCell(p1,p2,"=",match(p1,p2));    
//		}
//	    }
	} catch (Exception e) { e.printStackTrace(); }
    }

    public double match(Object o1, Object o2) throws AlignmentException, OWLException, URISyntaxException {
    	String workingDir = "/tmp/";
    	String tempFile1 = "to1.owl";
    	String tempFile2 = "to2.owl";
    	String pathTempFile1 = workingDir + tempFile1;
    	String pathTempFile2 = workingDir + tempFile2;
    	String pathTempFile3 = workingDir + tempFile1 + tempFile2;
    	
    	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    	IRI to1IRI = IRI.create(o1.toString().substring(1,o1.toString().length()-1));
    	IRI to2IRI = IRI.create(o2.toString().substring(1,o2.toString().length()-1));
    	IRI d1IRI = IRI.create("file:" + pathTempFile1);
    	IRI d2IRI = IRI.create("file:" + pathTempFile2);
    	SimpleIRIMapper mapper1 = new SimpleIRIMapper(to1IRI, d1IRI);
    	SimpleIRIMapper mapper2 = new SimpleIRIMapper(to2IRI, d2IRI);
        manager.addIRIMapper(mapper1);
        manager.addIRIMapper(mapper2);
        try {
        	org.semanticweb.owlapi.model.OWLOntology to1 = manager.createOntology(to1IRI);
        	org.semanticweb.owlapi.model.OWLOntology to2 = manager.createOntology(to2IRI);
        	org.semanticweb.owlapi.model.OWLDataFactory dataFactory = manager.getOWLDataFactory();
        	org.semanticweb.owlapi.model.OWLClass co1 = dataFactory.getOWLClass(to1IRI);
        	org.semanticweb.owlapi.model.OWLClass co2 = dataFactory.getOWLClass(to2IRI);
        	org.semanticweb.owlapi.model.OWLDeclarationAxiom declarationAxiom1 = dataFactory.getOWLDeclarationAxiom(co1);
        	org.semanticweb.owlapi.model.OWLDeclarationAxiom declarationAxiom2 = dataFactory.getOWLDeclarationAxiom(co2);
        	manager.addAxiom(to1, declarationAxiom1);
        	manager.addAxiom(to2, declarationAxiom2);
        	manager.saveOntology(to1, d1IRI);
        	manager.saveOntology(to2, d2IRI);
        } catch (OWLOntologyCreationException e) {
        	e.printStackTrace();
        } catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
        try {
	        OutputStream out = new FileOutputStream(workingDir + tempFile1 + tempFile2);
	        byte[] buf = new byte[1024];
	        Vector<String> files = new Vector<String>();
	        files.add(pathTempFile1);
	        files.add(pathTempFile2);
	        for (String file : files) {
	            InputStream in = new FileInputStream(file);
	            int b = 0;
	            while ( (b = in.read(buf)) >= 0) {
	                out.write(buf, 0, b);
	                out.flush();
	            }
	        }
	        out.close();
        	double s1 = zipFile(new FileInputStream(pathTempFile1), pathTempFile1 + ".zip");
        	double s2 = zipFile(new FileInputStream(pathTempFile2), pathTempFile2 + ".zip");
        	double s12 = zipFile(new FileInputStream(pathTempFile3), pathTempFile3 + ".zip");
        	return 1 - Math.max((s12 - s1)/s2, (s12 - s2)/s1);
        } catch (FileNotFoundException e) {
        	e.printStackTrace();
        } catch (IOException e) {
			e.printStackTrace();
		}        
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
