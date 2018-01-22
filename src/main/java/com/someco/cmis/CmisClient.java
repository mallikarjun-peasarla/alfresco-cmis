package com.someco.cmis;

import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class CmisClient {
    public static void main(String[] args) throws UnsupportedEncodingException {
        // create alfresco session
        SessionFactory factory = SessionFactoryImpl.newInstance();
        Map<String, String> parameter = new HashMap<String, String>();
        parameter.put(SessionParameter.USER, "admin");
        parameter.put(SessionParameter.PASSWORD, "admin");
        parameter.put(SessionParameter.ATOMPUB_URL, "http://127.0.0.1:8080/alfresco/api/-default-/public/cmis/versions/1.1/atom");
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        parameter.put(SessionParameter.REPOSITORY_ID, "-default-");
        Session session = factory.createSession(parameter);

        searchDocuments(session);
        // System.exit(1);

        // locate the document library folder for Marketing Site
        String path = "/Sites/marketing/documentLibrary";
        Folder documentLibrary = (Folder) session.getObjectByPath(path);

        // locate marketing folder in the document library
        Folder marketingFolder = null;
        for (CmisObject child :documentLibrary.getChildren()) {
            if ("Marketing".equals(child.getName())) {
                marketingFolder = (Folder) child;
            }
        }

        // create the marketing folder if needed
        if (marketingFolder == null) {
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(PropertyIds.NAME, "Marketing");
            properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
            marketingFolder = documentLibrary.createFolder(properties);
        }

        // prepare properties for new document
        String filename = "My new whitepaper.txt";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.NAME, filename);
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:sc:marketingDoc");

        // prepare content
        String content = "Hello World!";
        String mimetype = "text/plain; charset=UTF-8";
        byte[] contentBytes = content.getBytes("UTF-8");
        ByteArrayInputStream stream = new ByteArrayInputStream(contentBytes);
        ContentStream contentStream = session.getObjectFactory().createContentStream(filename, contentBytes.length, mimetype, stream);

        // create the document
        Document marketingDocument = marketingFolder.createDocument(properties, contentStream, VersioningState.MAJOR);

        // creating associations for whitepaper
        Folder whitepaperFolder = null;
        for (CmisObject child :documentLibrary.getChildren()) {
            if ("Whitepapers".equals(child.getName())) {
                whitepaperFolder = (Folder) child;
            }
        }

        // look for a whitepaper
        Document whitepaper = null;
        for (CmisObject child :whitepaperFolder.getChildren()) {
            if (child.getType().getId().equals("D:sc:whitepaper"))
                whitepaper = (Document) child;
        }

        properties = new HashMap<String, Object>();
        properties.put(PropertyIds.NAME, "a new relationship");
        properties.put(PropertyIds.OBJECT_TYPE_ID, "R:sc:relatedDocuments");
        properties.put(PropertyIds.SOURCE_ID, marketingDocument.getId());
        properties.put(PropertyIds.TARGET_ID, whitepaper.getId());
        session.createRelationship(properties);

        searchDocuments(session);
    }

    private static void searchDocuments(Session session) {
        // select all documents of someCo (i.e, docs of type sc:doc)
        ItemIterable<QueryResult> results = session.query("SELECT * FROM sc:doc",false);

        // display all properties for each document
        for (QueryResult hit : results) {
            for (PropertyData<?> property :hit.getProperties()) {
                String queryName = property.getQueryName();
                Object value = property.getFirstValue();
                System.out.println(queryName + ": " + value);
            }
            System.out.println("--------------------------------------");
        }
    }

    private static void deleteDocuments(Session session) {
        String path = "/Sites/marketing/documentLibrary";
        Folder documentLibrary = (Folder) session.getObjectByPath(path);

        // locate the marketing folder
        Folder marketingFolder = null;
        for (CmisObject child :documentLibrary.getChildren()) {
            if ("Marketing".equals(child.getName())) {
                marketingFolder = (Folder) child;
            }
        }

        // delete all children in marketing folder
        if (marketingFolder != null) {
            for (CmisObject child :marketingFolder.getChildren()) {
                session.delete(child);
            }
        }
    }
}
