package com.biosnettcs.utilities;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;

/**
 * Example showing how to set the description and title for a folder and document in Alfresco via CMIS.
 * 
 * @author jpotts
 */
public class AspectExample {
  private static final String CM_TITLE = "cm:title";
  private static final String CM_DESCRIPTION = "cm:description";
  private static final String TEST_TITLE = "this is my title";
  private static final String TEST_DESCRIPTION = "this is my description";
  private static final String SERVICE_URL = "http://localhost:8080/alfresco/cmisatom";
  private static final String USAGE = "java AspectExample <username> <password> <non-root folder path> <content name>";
  private static final String NON_ROOT = "Specify a target base folder other than root";

  private Session session = null;
  private String user;
  private String password;
  private String folderPath;
  private String contentName;
  
  public static void main(String[] args) throws Exception {     
        if (args.length != 4) doUsage(AspectExample.USAGE);
        if (args[2].equals("/")) doUsage(AspectExample.NON_ROOT);
        AspectExample ae = new AspectExample();
        ae.setUser(args[0]);
        ae.setPassword(args[1]);
        ae.setFolderPath(args[2]);
        ae.setContentName(args[3]);
        ae.doExample();     
  }
    
  public void doExample() {
    createTestDoc(getFolderPath(), getContentName());
    return;
  }
    
  public Session getSession() {
    if (this.session == null) {
        // default factory implementation
        SessionFactory factory = SessionFactoryImpl.newInstance();
        Map<String, String> parameter = new HashMap<String, String>();
    
        // user credentials
        parameter.put(SessionParameter.USER, getUser());
        parameter.put(SessionParameter.PASSWORD, getPassword());
    
        // connection settings
        parameter.put(SessionParameter.ATOMPUB_URL, getServiceUrl());
            
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
            
        // Set the alfresco object factory
        // Used when using the CMIS extension for Alfresco for working with aspects
        parameter.put(SessionParameter.OBJECT_FACTORY_CLASS, "org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl");
            
        List<Repository> repositories = factory.getRepositories(parameter);
    
        this.session = repositories.get(0).createSession();
    }
    return this.session;
  }
    
  public Folder getTestFolder(String folderPath) throws CmisObjectNotFoundException {
    Session session = getSession();

        // Grab a reference to the folder where we want to create content
    Folder folder = null;
    try {
        folder = (Folder) session.getObjectByPath(folderPath);
        System.out.println("Found folder: " + folder.getName() + "(" + folder.getId() + ")");
    } catch (CmisObjectNotFoundException confe) {
        Folder targetBaseFolder = null;
        String baseFolderPath = folderPath.substring(0, folderPath.lastIndexOf('/') + 1);
        String folderName = folderPath.substring(folderPath.lastIndexOf('/') + 1);
            
        //if this one is not found, we'll let the exception bubble up
        targetBaseFolder = (Folder) session.getObjectByPath(baseFolderPath);

        // Create a Map of objects with the props we want to set
        Map <String, Object> properties = new HashMap<String, Object>();
            
        // Following sets the content type and adds the webable and productRelated aspects
        // This works because we are using the OpenCMIS extension for Alfresco
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder, P:cm:titled");
        properties.put(PropertyIds.NAME, folderName);

        properties.put(CM_DESCRIPTION, TEST_DESCRIPTION);
        properties.put(CM_TITLE, TEST_TITLE);       
            
        folder = targetBaseFolder.createFolder(properties);
        System.out.println("Created folder: " + folder.getName() + " (" + folder.getId() + ")");
    }       
            
    return folder;
  }
    
  public Document createTestDoc(String folderPath, String docName) {
        // Grab a reference to the folder where we want to create content
    Folder folder = getTestFolder(folderPath);
        
    // Set up a name for the test document
    String timeStamp = new Long(System.currentTimeMillis()).toString();
    String fileName = docName + " (" + timeStamp + ")";
        
    // Create a Map of objects with the props we want to set
    Map <String, Object> properties = new HashMap<String, Object>();

    // Following sets the content type and adds the webable and productRelated aspects
    // This works because we are using the OpenCMIS extension for Alfresco
    properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document, P:cm:titled");
    properties.put(PropertyIds.NAME, fileName);
    properties.put(CM_DESCRIPTION, TEST_DESCRIPTION);
    properties.put(CM_TITLE, TEST_TITLE);
        
    // Set the content text
    String docText = "This is a sample document called " + docName;
    byte[] content = docText.getBytes();
    InputStream stream = new ByteArrayInputStream(content);
    ContentStream contentStream = new ContentStreamImpl(fileName, BigInteger.valueOf(content.length), "text/plain", stream);

    // Create the document
    Document doc = folder.createDocument(
                   properties,
                   contentStream,
                   VersioningState.MAJOR);
    System.out.println("Created content: " + doc.getName() + "(" + doc.getId() + ")");
    System.out.println("Content Length: " + doc.getContentStreamLength());
        
    return doc;
  }

  public String getServiceUrl() {
    return SERVICE_URL;
  }

  public String getContentName() {
    return this.contentName;
  }

  public void setContentName(String contentName) {
    this.contentName = contentName;
  }
    
  public static void doUsage(String message) {
    System.out.println(message);
    System.exit(0);
  }
    
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getFolderPath() {
    return folderPath;
  }

  public void setFolderPath(String folderPath) {
    this.folderPath = folderPath;
  }
    
}