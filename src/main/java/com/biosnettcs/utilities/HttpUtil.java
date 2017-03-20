package com.biosnettcs.utilities;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtil {

	private static Logger logger = LoggerFactory.getLogger(HttpUtil.class);

	public static final String GET = "GET";
	public static final String POST = "POST";
	public static final int CODIGO_RESPUESTA_OK = 200;
	
	
	/**
	 * Obtiene un archivo a partir de su URL y lo genera en una ruta destino
	 * @param urlOrigen   URL de donde se obtendr� el flujo de datos del archivo a guardar 
	 * @param rutaDestino Ruta y nombre del archivo donde se almacenar� el archivo con el flujo de datos
	 * @return true si gener&oacute; el archivo, false si fall&oacute;
	 */
	public static boolean generaArchivo(String urlOrigen, String rutaDestino) {
		
		logger.debug("urlOrigen={}", urlOrigen);
		logger.debug("rutaDestino={}", rutaDestino);
		boolean isArchivoGenerado = false;
		
		InputStream inputStream = null;
		OutputStream outputStream = null;
		
		try {
			
			//Creacion de carpeta
			File carpeta=new File(new File(rutaDestino).getParent());
			if(!carpeta.exists())
			{
				logger.debug("no existe la carpeta {}", carpeta);
				if(!carpeta.mkdir())
				{
					throw new Exception("Error al crear la carpeta "+carpeta);
				}
				logger.debug("Carpeta creada {}", carpeta);
			}
			//Creacion de carpeta
			
			URL obj = new URL(urlOrigen);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod(GET);
	
			int responseCode = con.getResponseCode();
			logger.debug("Codigo de Respuesta : {}", responseCode);
			if (responseCode != CODIGO_RESPUESTA_OK) {
				throw new Exception("Codigo de respuesta erroneo: " + responseCode);
			}
			
			inputStream = con.getInputStream();
			outputStream = null;
			// write the inputStream to a FileOutputStream
			outputStream = new FileOutputStream(new File(rutaDestino));
			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = inputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}
			
			isArchivoGenerado = true;
			logger.debug("Archivo creado: {}", rutaDestino);

		} catch (Exception e) {
			logger.error("Error al generar el archivo en {}", rutaDestino, e);
			isArchivoGenerado = false;
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					logger.warn("No se pudo cerrar el flujo de entrada", e);
				}
			}
			if (outputStream != null) {
				try {
					// outputStream.flush();
					outputStream.close();
				} catch (IOException e) {
					logger.warn("No se pudo cerrar el flujo de salida", e);
				}

			}
		}
		return isArchivoGenerado;
	}
	
	
	/**
	 * Obtiene un archivo a partir de su URL y lo genera en una ruta destino
	 * @param urlOrigen   URL de donde se obtendr� el flujo de datos del archivo a guardar 
	 * @param rutaDestino Ruta y nombre del archivo donde se almacenar� el archivo con el flujo de datos
	 * @return objeto File que hace referencia al archivo creado, null si no se cre&oacute;
	 */
	public static File generaYObtenArchivo(String urlOrigen, String rutaDestino) {
		File file = null;
		if (generaArchivo(urlOrigen, rutaDestino)) {
			file = new File(rutaDestino);
			if(logger.isDebugEnabled()) {
				logger.debug("Se obtiene referencia al archivo {}", rutaDestino);
			}
		}
		return file;
	}
	
	
	/**
	 * Obtiene un flujo archivo a partir de una URL
	 * @param urlOrigen   URL de donde se obtendr� el flujo de datos 
	 * @return InputStream o null en caso de que no existan datos
	 */
	public static InputStream obtenInputStream(String urlOrigen) throws Exception{
		
		logger.debug("Entrando a obtenInputStream()");
		logger.debug("urlOrigen={}", urlOrigen);
		
		InputStream inputStream = null;
		
		try {
			
			URL obj = new URL(urlOrigen);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod(GET);
	
			int responseCode = con.getResponseCode();
			logger.debug("Codigo de Respuesta : {}", responseCode);
			if (responseCode != CODIGO_RESPUESTA_OK) {
				throw new Exception("Codigo de respuesta erroneo: " + responseCode);
			}
			
			inputStream = con.getInputStream();

		}catch(ConnectException cex){
			logger.error("Error al conectarce para obtener los datos " , cex);
			throw cex;
		} catch (Exception e) {
			logger.error("Error al obtener los datos " , e);
			if(e.getMessage().indexOf("Codigo de respuesta erroneo: ")>-1){
				throw e;
			}
		}
		return inputStream;
	}

	
	/*
	public static void creaPeticionGet(String url) throws Exception {
		String inputLine;
		URL obj = new URL(url);

		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod(GET);

		int responseCode = con.getResponseCode();
		logger.debug("Codigo de Respuesta : " + responseCode);
		if (responseCode != CODIGO_RESPUESTA_OK) {
			throw new Exception("Codigo de respuesta erroneo: " + responseCode);
		}
		// Se lee la informaci�n de la respuesta:
		BufferedReader in = new BufferedReader(new InputStreamReader(
				con.getInputStream()));
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		logger.debug("Respuesta GET --> " + response.toString());
	}
	*/

	public static void main(String[] args) throws Exception {
		HttpUtil.sendPost(
		    "http://localhost:8088/cmis-rs/cmis/documentos/json",
		    "repositoryId=A1&folder=&idName=arquetipos_20170317_2.pdf&fullFileName=E:\\arquetipos.pdf&contentType=application/pdf");
	}
	
	
	public static String sendPost(String url, String params) throws Exception {
		
		logger.debug("sendPost url={},params={}", url, params);
		
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		//add reuqest header
		con.setRequestMethod("POST");
		//con.setRequestProperty("User-Agent", USER_AGENT);
		//con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		String urlParameters = params;
		
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		logger.debug("sendPost Sending 'POST' request to URL : {}", url);
		logger.debug("sendPost Post parameters : {}", urlParameters);
		logger.debug("sendPost Response Code : {}", responseCode);

		if(responseCode < 200 || responseCode>=300) {
			throw new Exception("Codigo de respuesta erroneo: " + responseCode);
		}
		BufferedReader in = new BufferedReader(
		       new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		//print result
		logger.debug("sendPost response={}", response.toString());
		return response.toString();
		
	}

}