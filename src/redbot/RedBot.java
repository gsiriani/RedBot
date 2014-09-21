
package redbot;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bruno.garate
 */
public class RedBot {

    /**
     * @param args the command line arguments
     */
    public synchronized static void main(String[] args) {
         try {
            int i = 0;
            boolean error = false;
            while ((i < args.length) && !error ) {
                if(args[i].startsWith("-")) {
                    String paramString = args[i].substring(1);
                    SwitchString paramEnum = toSwitchString(paramString);
                    switch (paramEnum) {
                        case d:  
                                Environment.getInstance().setDebug(true);
                                i++;
                                if (i == args.length) {
                                         System.out.println("Error de sintaxis para d");
                                         error = true;
                                }
                                break;
                        case depth:  
                                i++;
                                if (i < args.length) {
                                     try {
                                        int n = Integer.valueOf(args[i]);
                                        Environment.getInstance().setMaxDepth(n);
                                        i++;
                                        if (i == args.length) {
                                            System.out.println("Error de sintaxis para depth");
                                            error = true;
                                        }
                                     } catch (Exception e) {
                                         System.out.println("Error de sintaxis para depth");
                                         error = true;
                                     } 
                                } else {
                                    System.out.println("Error de sintaxis para depth");
                                    error = true;
                                }
                                break;
                        case persistent:  
                                 Environment.getInstance().setPersistent(true);
                                 i++;
                                 if (i == args.length) {
                                         System.out.println("Error de sintaxis para persistencia");
                                         error = true;
                                 }
                                 break;
                        case pozos:  
                                 i++;
                                 if (i < args.length) {
                                     String fileName = args[i];
                                     Environment.getInstance().setNombreArchivoPozos(fileName);
                                     i++;
                                     if (i == args.length) {
                                         System.out.println("Error de sintaxis para pozos");
                                         error = true;
                                     }
                                 } else {
                                    System.out.println("Error de sintaxis para pozos");
                                    error = true;
                                 }
                                 break;
                        case multilang: 
                                 i++;
                                 if (i < args.length) {
                                     String fileName = args[i];
                                     Environment.getInstance().setNombreArchivoMultilang(fileName);
                                     i++;
                                     if (i == args.length) {
                                         System.out.println("Error de sintaxis para multilang");
                                         error = true;
                                     }
                                 } else {
                                    System.out.println("Error de sintaxis para multilang");
                                    error = true;
                                 }
                                 break;
                        case p:  
                                 i++;
                                 if (i < args.length) {
                                     try {
                                        int n = Integer.valueOf(args[i]);
                                        Environment.getInstance().setMaxCantThreads(n);
                                        i++;
                                        if (i == args.length) {
                                            System.out.println("Error de sintaxis para p");
                                            error = true;
                                        }
                                     } catch (Exception e) {
                                         System.out.println("Error de sintaxis para p");
                                         error = true;
                                     } 
                                 } else {
                                    System.out.println("Error de sintaxis para p");
                                    error = true;
                                 }
                                 break;
                        case prx: 
                                 i++;
                                 if (i < args.length) {
                                     String url = args[i];
                                     String[] datos = url.split(":");
                                     try {
                                     Environment.getInstance().setProxyPort(Integer.valueOf(datos[1]));
                                     } catch (Exception e) {
                                         System.out.println("Error de proxy Puerto");
                                     }
                                     Environment.getInstance().setProxyURL(datos[0]);
                                     i++;
                                     if (i == args.length) {
                                         System.out.println("Error de sintaxis para proxy");
                                         error = true;
                                     }

                                 } else {
                                    System.out.println("Error de sintaxis para proxy");
                                    error = true;
                                 }
                                 break;
                        default: 
                                 System.out.println("Error de sintaxis switch incorrecto");
                                 error = true;
                                 break;
                    } // cierra switch           
                } else {
                    if ((i+1)< args.length) {
                        System.out.println("Error de sintaxis extra parametros");
                        error = true;
                    } else {
                        String urlIngresado = args[i];
                        if(!urlIngresado.substring(0,7).equals("http://"))
                            urlIngresado = "http://" + urlIngresado; 
                        try {
                            // Cargo el primer URL
                            URL url = new URL(urlIngresado);
                            //TODO : que pasa si el puerto no es 80?
                            int ttl = Environment.getInstance().getMaxDepth();
                            Link l = new Link(url.getHost(), url.getFile(), 80, ttl);
                            Environment.getInstance().pedirLinksAvailable();
                            Environment.getInstance().addLinkInicial(l);
                            Environment.getInstance().retornarLinksAvailable();
                        } catch (MalformedURLException e) {
                                throw new NoParseLinkException(e);
                        }
                        i++;
                    }     
                }
            } // cierra while      

            Environment.getInstance().iniciarHilos();    
        } catch (RedBotException ex) {
            System.out.println("Error:" + ex.getMessage());
        }
        
    }
    
    public static SwitchString toSwitchString(String param){
        if (param.equals("d")) {
            return SwitchString.d;
        }
        if (param.equals("depth")) {
            return SwitchString.depth;
        }
        if (param.equals("multilang")) {
            return SwitchString.multilang;
        }
        if (param.equals("p")) {
            return SwitchString.p;
        }
        if (param.equals("persistent")) {
            return SwitchString.persistent;
        }
        if (param.equals("pozos")) {
            return SwitchString.pozos;
        }
        if (param.equals("prx")) {
            return SwitchString.prx;
        }
        return SwitchString.error;
    }
    
}
