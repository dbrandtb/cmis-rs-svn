package com.biosnettcs.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RespuestaVO {
	
    private String value;
    
    public RespuestaVO(){
        
    }
    
    public RespuestaVO(String value){
        this.value = value;
    }
    
	@XmlElement
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
