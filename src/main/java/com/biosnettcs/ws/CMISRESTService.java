package com.biosnettcs.ws;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.client.util.FileUtils;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.biosnettcs.dto.RespuestaVO;

@Path("/cmis")
public class CMISRESTService {
    
    Logger logger = LoggerFactory.getLogger(CMISRESTService.class);
    
    public static final String URL_CMIS_SERVICE = "http://10.1.21.218:8080/chemistry-opencmis-server-inmemory-1.0.0/atom";

    public static final String DEFAULT_PATH = "/";
    
	@POST
	@Path("/documentos/json")
	@Produces("application/json; charset=utf8")
    public Response createDocumentCMIS(
            @FormParam("repositoryId") String repositoryId,
            @FormParam("folder") String folder,
            @FormParam("idName") String idName,
            @FormParam("description") String description,
            @FormParam("fullFileName") String fullFileName,
            @FormParam("contentType") String contentType) {
	    
	    return creaDocumento(repositoryId, folder, idName, description, fullFileName, contentType);
	}
	
	
    @GET
    @Path("/documentos/json")
    @Produces("application/json; charset=utf8")
    public Response createDocumentCMISGET(@QueryParam("repositoryId") String repositoryId,
            @QueryParam("folder") String folder,
            @QueryParam("idName") String idName,
            @QueryParam("description") String description,
            @QueryParam("fullFileName") String fullFileName,
            @QueryParam("contentType") String contentType) {
        
        return creaDocumento(repositoryId, folder, idName, description, fullFileName, contentType);
    }


    /**
     * Crea un documento en el repositorio CMIS
     * @param repositoryId
     * @param folder
     * @param idName
     * @param description
     * @param fullFileName
     * @param contentType
     * @return
     */
    private Response creaDocumento(String repositoryId,
            String folder,
            String idName,
            String description,
            String fullFileName,
            String contentType) {
        
        logger.info("Entrando a createDocumentCMIS...");
        logger.debug("repositoryId:{}", repositoryId);
        logger.debug("folder:{}", folder);
        logger.debug("idName:{}", idName);
        logger.debug("description:{}", description);
        logger.debug("fullFileName:{}", fullFileName);
        logger.debug("contentType:{}", contentType);
        
        //TODO:agregar properties
        //String url = JerseyConfig.properties.getProperty("service.cmis.atom.url");
        logger.debug("url:{}", URL_CMIS_SERVICE);
        int status  = 404;
        RespuestaVO respuesta = new RespuestaVO();
        
        try {
            Map<String, String> parameters = initParameters(URL_CMIS_SERVICE, repositoryId, null);

            // Default factory implementation
            SessionFactory factory = SessionFactoryImpl.newInstance();
            // Create session
            Session session = factory.createSession(parameters);
            // Create folder:
            Folder parent = session.getRootFolder();
            
            // prepare properties
            Map<String, Object> props = new HashMap<String, Object>();
            //props.put(PropertyIds.PATH, DEFAULT_PATH + folder);
            props.put(PropertyIds.NAME, folder);
            props.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
            
            try {
                CmisObject cmisObject = session.getObjectByPath(DEFAULT_PATH + folder);
                parent = (Folder) cmisObject;
                logger.info("La carpeta {} con ruta {} ya existe", folder, parent.getPath());
            } catch (CmisObjectNotFoundException onf) {
                logger.info("La carpeta {} con ruta {} no existe, se intentará crear...", folder, parent.getPath());
                // create the folder
                Folder folderObj = parent.createFolder(props);
                logger.info("Despues de crear folder {} con ruta {} ...", folder, parent.getPath());
                if(folderObj == null) {
                    throw new Exception("Error al crear folder:"+ folder);
                }
                logger.info("Carpeta {} con ruta {} fue creada", folderObj.getName(), parent.getPath());
            }
            CmisObject cmisObject = session.getObjectByPath(DEFAULT_PATH + folder);
            logger.debug("folder obtenido :{}", cmisObject);
            
            if(description == null || description.length()==0) {
                description = idName;
            }

            // Create a ContentStream object from file:
            // Init array with file length:
            File file = new File(fullFileName);
            byte[] bytesArray = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(bytesArray); // read file into bytes[]
            fis.close();
            ByteArrayInputStream stream = new ByteArrayInputStream(bytesArray);
            ContentStream contentStream = session.getObjectFactory().createContentStream(file.getName(),
                    bytesArray.length, contentType, stream);

            // prepare properties
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(PropertyIds.NAME, idName);
            // TODO: Que el servicio permita modificar el tipo de objeto:
            properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
            properties.put(PropertyIds.DESCRIPTION, "cmis:description");
            properties.put("cmis:description", description);

            // Create the document:
            // TODO: Que el servicio permita enviar el estado del versionamiento:
            Document newDoc = parent.createDocument(properties, contentStream, VersioningState.NONE);

            logger.info("Documento creado:{}", newDoc.getId());

            status = 200;
            respuesta.setValue(newDoc.getId());

        } catch (Exception e) {
            logger.error("Error en createDocumentCMIS: ", e);
            respuesta = new RespuestaVO(e.toString());
        }
        logger.info("Fin de createDocumentCMIS");
        return Response.status(status).entity(respuesta).build();
    }
	
    
    /**
     * 
     * @param url
     * @param repositoryId
     * @param objectId
     * @param parameters
     * @return Nombre del objeto (si existe)
     * @throws Exception
     */
    public String getObjectById(String url, String repositoryId, String objectId, Map<String, String> parameters) throws Exception {
        
        // Default factory implementation
        SessionFactory factory = SessionFactoryImpl.newInstance();
        
        parameters = initParameters(url, repositoryId, parameters);
        // Create session
        Session session = factory.createSession(parameters);
        
        CmisObject cmisObject = session.getObject(objectId);

        if (cmisObject instanceof Document) {
            Document document = (Document) cmisObject;
            FileUtils.download(document, "E:\\downloads\\"+document.getName());
        } else if (cmisObject instanceof Folder) {
            //Folder folder = (Folder) cmisObject;
        } else {
            throw new Exception("The object is not a document neither a folder.");
        }
        return cmisObject.getName();
    }
    
    
    /**
     * Inicia los parametros de conexion
     * @param parameters
     * @return Mapa con los parametros de conexion
     */
    public static Map<String, String> initParameters(String url, String repositoryId, Map<String, String> parameters) {

        // Iniciando parametros:
        if(parameters == null) {
            parameters = new HashMap<String, String>();
        }
        parameters.put(SessionParameter.REPOSITORY_ID, repositoryId);
        // TODO: Que el servicio permita modificar el tipo de binding:
        parameters.put(SessionParameter.ATOMPUB_URL, url);
        parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        //parameters.put(SessionParameter.OBJECT_FACTORY_CLASS, "org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl");

        // TODO: Obtener datos de usuario (user credentials):
        //parameters.put(SessionParameter.USER, "Otto");
        //parameters.put(SessionParameter.PASSWORD, "****");
        
        return parameters;
    }
    
    @GET
    @Path("/hola")
    @Produces("text/plain")
    public String hola(){
        logger.debug("Entrando a hola...");
        return "Hola desde el servicio";
    }
    
    @GET
    @Path("/hola/{mensaje}")
    @Produces("text/plain")
    public String hola(@PathParam("mensaje") String mensaje){
        return "Hola, mensaje: "+ mensaje;
    }
    
	@OPTIONS
	@Path("/documentos/json/")
	@Produces("application/json; charset=utf8")
	public Response optionsEmepleadoJSON(){
		int status =404;
		//String respuesta = "Allowed: GET, POST, PATCH, DELETE, OPTIONS";
		String respuesta = "Allowed: GET, POST";
		return Response.status(status).entity(respuesta).build();
	}
	
}