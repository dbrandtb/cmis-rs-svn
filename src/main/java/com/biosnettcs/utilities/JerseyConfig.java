package com.biosnettcs.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.biosnettcs.ws.CMISRESTService;

@ApplicationPath("sample")
public class JerseyConfig extends Application {

    public static final String PROPERTIES_FILE = "config.properties";
    public static Properties properties = new Properties();

    private Properties readProperties() {
        System.out.println("Entrando a readProperties()");
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE);
        if (inputStream != null) {
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                // TODO Add your custom fail-over code here
                e.printStackTrace();
            }
        }
        System.out.println("Saliendo de readProperties()");
        return properties;
    }

    @Override
    public Set<Class<?>> getClasses() {
        System.out.println("Entrando a getClasses()");
        // Read the properties file
        readProperties();

        // Set up your Jersey resources
        Set<Class<?>> rootResources = new HashSet<Class<?>>();
        rootResources.add(CMISRESTService.class);
        System.out.println("Saliendo de getClasses()");
        return rootResources;
    }

}