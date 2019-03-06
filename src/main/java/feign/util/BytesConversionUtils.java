package feign.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class BytesConversionUtils {
	 /**  
     * 对象转数组  
     * @param obj  
     * @return  
     */  
    public static byte[] toBytes (Object obj) {      
        byte[] bytes = null;      
        ByteArrayOutputStream bos = new ByteArrayOutputStream();      
        try {        
            ObjectOutputStream oos = new ObjectOutputStream(bos);         
            oos.writeObject(obj);        
            oos.flush();         
            bytes = bos.toByteArray();      
            oos.close();         
            bos.close(); 
        } catch (IOException ex) {        
            ex.printStackTrace();   
        }      
        return bytes;    
    }   
       
    /**  
     * 数组转对象  
     * @param bytes  
     * @return  
     */  
    public static Object toObject (byte[] bytes) {      
        Object obj = null;      
        try {        
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);        
            ObjectInputStream ois = new ObjectInputStream(bis);        
            obj = ois.readObject();      
            ois.close();   
            bis.close();   
        } catch (IOException ex) {        
            ex.printStackTrace();   
        } catch (ClassNotFoundException ex) {        
            ex.printStackTrace();   
        }      
        return obj;    
    } 
    
    /**  
     * inputstream转对象  
     * @param bytes  
     * @return  
     */  
    public static Object toObject (InputStream input) {      
        Object obj = null;      
        try {                
            ObjectInputStream ois = new ObjectInputStream(input);        
            obj = ois.readObject();      
            ois.close();   
            input.close();   
        } catch (IOException ex) {        
            ex.printStackTrace();   
        } catch (ClassNotFoundException ex) {        
            ex.printStackTrace();   
        }      
        return obj;    
    } 
}
