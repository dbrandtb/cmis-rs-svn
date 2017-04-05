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
import org.apache.chemistry.opencmis.commons.impl.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.biosnettcs.dto.RespuestaVO;
import com.biosnettcs.util.Constantes;

@Path("/cmis")
public class CMISRESTService {
    
    Logger logger = LoggerFactory.getLogger(CMISRESTService.class);
    
    public static final String URL_CMIS_SERVICE = "http://192.253.245.126:9080/openfncmis/atom";
    //public static final String URL_CMIS_SERVICE = "http://localhost:8080/chemistry-opencmis-server-inmemory-1.0.0/atom11";
    
    
	@POST
	@Path("/documentos/json")
	@Produces("application/json; charset=utf8")
    public Response createDocumentCMISPOST(
            @FormParam("repositoryId")  String repositoryId,
            @FormParam("folder")        String folder,
            @FormParam("idName")        String idName,
            @FormParam("fullFileName")  String fullFileName,
            @FormParam("documentClass") String documentClass,
            @FormParam("description")   String description) {
	    
	    return crearDocumento(repositoryId, folder, idName, fullFileName, documentClass, description);
	}
	
	
    @GET
    @Path("/documentos/json")
    @Produces("application/json; charset=utf8")
    public Response createDocumentCMISGET(
            @QueryParam("repositoryId")  String repositoryId,
            @QueryParam("folder")        String folder,
            @QueryParam("idName")        String idName,
            @QueryParam("fullFileName")  String fullFileName,
            @QueryParam("documentClass") String documentClass,
            @QueryParam("description")   String description) {
        
        return crearDocumento(repositoryId, folder, idName, fullFileName, documentClass, description);
    }
    
    
    @OPTIONS
    @Path("/documentos/json/")
    @Produces("application/json; charset=utf8")
    public Response optionsDocumentosJSON(){
        int status =404;
        String respuesta = "Allowed: GET, POST"; // "Allowed: GET, POST, PATCH, DELETE, OPTIONS";
        return Response.status(status).entity(respuesta).build();
    }
    
    
    /**
     * Crea un documento en el servidor CMIS
     * 
     * @param repositoryId  Id del repositorio donde se va a crear el documento
     * @param folder        Carpeta donde se va a crear el documento
     * @param idName        (Opcional) Id del documento, si es nulo se tomara el nombre del documento a subir
     * @param fullFileName  Ruta completa y nombre del documento a subir
     * @param documentClass Clase de documento segun el proceso al que pertenece
     * @param description   (Opcional) Descripcion del documento
     * @return              Respuesta de la creacion del documento
     */
    private Response crearDocumento(
            String repositoryId,
            String folder,
            String idName,
            String fullFileName,
            String documentClass,
            String description) {
        
        logger.info("Entrando a createDocumentCMIS...");
        int status  = 404;
        RespuestaVO respuesta = new RespuestaVO();
        String paso = "recuperando par\u00E1metros iniciales";
        try {
            //TODO:agregar en archivo de propiedades:
            //String url = JerseyConfig.properties.getProperty("service.cmis.atom.url");
            logger.info("URL de servidor CMIS= {}", URL_CMIS_SERVICE);
            logger.debug("repositoryId={}, folder={}, idName={}, fullFileName={}, documentClass={}, description={}",
                repositoryId, folder, idName, fullFileName, documentClass, description);
            
            Map<String, String> parameters = initParameters(URL_CMIS_SERVICE, repositoryId, null);
            
            paso = "creando sesi\u00F3n de CMIS";
            // Default factory implementation
            SessionFactory factory = SessionFactoryImpl.newInstance();
            // Create session
            Session session = factory.createSession(parameters);
            
            // Preparing properties:
            paso = new StringBuilder("asignando propiedades del documento ").append(fullFileName).toString();
            // idName:
            Map<String, Object> documentProps = new HashMap<String, Object>();
            if(idName == null || idName.length()==0) {
                idName = new File(fullFileName).getName();
            }
            documentProps.put(PropertyIds.NAME, idName);
            // ObjectTypeId:
            if("Cotizacion".equalsIgnoreCase(documentClass)) {
                documentProps.put(PropertyIds.OBJECT_TYPE_ID, "QuotationDocument");
            } else {
                documentProps.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
            }
            // description:
            documentProps.put(PropertyIds.DESCRIPTION, "cmis:description");
            documentProps.put("cmis:description", description);
            // contentType:
            String contentType = MimeTypes.getMIMEType(new File(fullFileName));
            
            // Create folder structure:
            paso = new StringBuilder("creando ruta ").append(String.valueOf(folder)).toString();
            Folder parent = createPath(session, folder);

            // Create a ContentStream object from file:
            // Init array with file length:
            paso = new StringBuilder("creando archivo ").append(fullFileName).toString();
            File file = new File(fullFileName);
            byte[] bytesArray = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(bytesArray); // read file into bytes[]
            fis.close();
            ByteArrayInputStream stream = new ByteArrayInputStream(bytesArray);
            ContentStream contentStream = session.getObjectFactory().createContentStream(file.getName(),
                    bytesArray.length, contentType, stream);
            // Create the document:
            // TODO: Que el servicio permita enviar el estado del versionamiento:
            Document newDoc = parent.createDocument(documentProps, contentStream, VersioningState.MAJOR);

            logger.info("Documento creado: {} ({}) en {}", newDoc.getName(), newDoc.getId(), parent.getPath());

            status = 200;
            respuesta.setValue(newDoc.getId());

        } catch (Exception e) {
            logger.error("Error {}: ", paso, e);
            respuesta = new RespuestaVO("Error " + paso + ": " + e.toString());
        }
        logger.info("Fin de createDocumentCMIS");
        return Response.status(status).entity(respuesta).build();
    }
    
    
    /**
     * Crea un folder en el servidor CMIS
     * 
     * @param session        Referencia a la sesion CMIS
     * @param folderRoot     Ruta del folder raiz
     * @param folderName     Ruta del folder a crear
     * @param objectTypeId   Object Type de la carpeta usado en el servidor CMIS
     * @return object Folder Objeto CMIS que representa al folder
     * @throws Exception     Excepcion Error en el proceso
     */
    private Folder createFolder(Session session, String folderRoot, String folderName, String objectTypeId)
            throws Exception {
        
        String ruta = folderRoot + folderName;
        logger.info("Se intentara crear el folder:{} en la ruta:{}", folderName, folderRoot);
        Folder parent = null;
        try {
            parent = (Folder)session.getObjectByPath(ruta);
            logger.info("La carpeta {} con ruta {} ya existe", folderName, parent.getPath());
        } catch (CmisObjectNotFoundException onf) {
            logger.debug("La carpeta {} con ruta {} no existe, se intentará crear...", folderName, ruta);
            // Create the folder:
            Map<String, Object> folderProps = new HashMap<String, Object>();
            folderProps.put(PropertyIds.NAME, folderName);
            folderProps.put(PropertyIds.OBJECT_TYPE_ID, objectTypeId);
            logger.info("ruta{}", ruta);
            parent = ((Folder)session.getObjectByPath(folderRoot)).createFolder(folderProps);
            logger.info("Carpeta {} con ruta {} fue creada", folderName, parent.getPath());
        }
        return parent;
    }
    
    
    @GET
    @Path("/carpetas/json")
    @Produces("application/json; charset=utf8")
    public Response createPath(
            @QueryParam("repositoryId")  String repositoryId,
            @QueryParam("rutaCompleta")  String rutaCompleta) {
        
      logger.info("Entrando a createPath...");
        int status  = 404;
        RespuestaVO respuesta = new RespuestaVO();
        String paso = "recuperando par\u00E1metros iniciales";
        try {
            //TODO:agregar en archivo de propiedades:
            //String url = JerseyConfig.properties.getProperty("service.cmis.atom.url");
            logger.info("URL de servidor CMIS= {}", URL_CMIS_SERVICE);
            logger.debug("repositoryId={}, rutaCompleta={}", repositoryId, rutaCompleta);
            
            Map<String, String> parameters = initParameters(URL_CMIS_SERVICE, repositoryId, null);
            
            paso = "creando sesi\u00F3n de CMIS";
            // Default factory implementation
            SessionFactory factory = SessionFactoryImpl.newInstance();
            // Create session
            Session session = factory.createSession(parameters);
            
            paso = "creando carpetas";

            Folder folder = createPath(session, rutaCompleta);
            
            status = 200;
            respuesta.setValue(folder.getPath());

        } catch (Exception e) {
            logger.error("Error {}: ", paso, e);
            respuesta = new RespuestaVO("Error " + paso + ": " + e.toString());
        }
        logger.info("Fin de createDocumentCMIS");
        return Response.status(status).entity(respuesta).build();
    }
    
    /**
     * Crea la ruta completa en el servidor CMIS
     * @param session      Referencia a la sesion CMIS
     * @param rutaCompleta Ruta completa a crear
     * @return
     * @throws Exception
     */
    private Folder createPath(Session session, String rutaCompleta) throws Exception {
        
        String [] subrutas = rutaCompleta.split(Constantes.PATH_SEPARATOR);
        
        String ruta = Constantes.PATH_SEPARATOR;
        Folder parent = null;
        String nuevaRuta = "";
        for (int i = 0; i < subrutas.length; i++) {
            if(subrutas[i] != null && subrutas[i].trim().length() > 0) {
                try {
                    if(Constantes.PATH_SEPARATOR.equals(ruta)) {
                        nuevaRuta = ruta+subrutas[i];
                    } else {
                        nuevaRuta = ruta+Constantes.PATH_SEPARATOR+subrutas[i];
                    }
                    logger.debug("Se va a intentar recuperar la carpeta {}", nuevaRuta);
                    parent = (Folder)session.getObjectByPath(nuevaRuta);
                    logger.info("La carpeta {} con ruta {} ya existe", subrutas[i], parent.getPath());
                } catch (CmisObjectNotFoundException onf) {
                    logger.debug("La carpeta {} con ruta {} no existe, se intentará crear...", subrutas[i], nuevaRuta);
                    // Create the folder:
                    Map<String, Object> folderProps = new HashMap<String, Object>();
                    folderProps.put(PropertyIds.NAME, subrutas[i]);
                    folderProps.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
                    parent = ((Folder)session.getObjectByPath(ruta)).createFolder(folderProps);
                    logger.info("Carpeta {} con ruta {} fue creada", subrutas[i], parent.getPath());
                }
                if(Constantes.PATH_SEPARATOR.equals(ruta)) {
                    ruta += subrutas[i];
                } else {
                    ruta += Constantes.PATH_SEPARATOR + subrutas[i];
                }
                logger.info("ruta nueva={}", ruta);
            }
        }
        return parent;
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
        parameters.put(SessionParameter.USER, "administrator");
        parameters.put(SessionParameter.PASSWORD, "p@ssw0rd");
        
        return parameters;
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
        
        parameters = initParameters(url, repositoryId, parameters);
        
        // Default factory implementation
        SessionFactory factory = SessionFactoryImpl.newInstance();
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
	
}