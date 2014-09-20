
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
        int i = 0;
        boolean error = false;
        while ((i < args.length) && !error ) {
            if(args[i].startsWith("-")) {
                switch (args[i].substring(1)) {
                    case "d":  
                            Environment.getInstance().setDebug(true);
                            i++;
                            if (i == args.length) {
                                     System.out.println("Error de sintaxis para d no URL");
                                     error = true;
                            }
                            break;
                    case "depth":  
                            i++;
                            if (i < args.length) {
                                 try {
                                    int n = Integer.valueOf(args[i]);
                                    Environment.getInstance().setMaxDepth(n);
                                    i++;
                                    if (i == args.length) {
                                        System.out.println("Error de sintaxis para depth no cantidad");
                                        error = true;
                                    }
                                 } catch (Exception e) {
                                     System.out.println("Error de sintaxis para depth no int");
                                     error = true;
                                 } 
                            } else {
                                System.out.println("Error de sintaxis para depth no parametro");
                                error = true;
                            }
                            break;
                    case "persistent":  
                             Environment.getInstance().setPersistent(true);
                             i++;
                             if (i == args.length) {
                                     System.out.println("Error de sintaxis para persistent no URL");
                                     error = true;
                             }
                             break;
                    case "pozos":  
                             i++;
                             if (i < args.length) {
                                 String fileName = args[i];
                                 Environment.getInstance().setNombreArchivoPozos(fileName);
                                 i++;
                                 if (i == args.length) {
                                     System.out.println("Error de sintaxis para pozos no fileName");
                                     error = true;
                                 }
                             } else {
                                System.out.println("Error de sintaxis para pozos no parametro");
                                error = true;
                             }
                             break;
                    case "multilang": 
                             i++;
                             if (i < args.length) {
                                 String fileName = args[i];
                                 Environment.getInstance().setNombreArchivoMultilang(fileName);
                                 i++;
                                 if (i == args.length) {
                                     System.out.println("Error de sintaxis para multilang no fileName");
                                     error = true;
                                 }
                             } else {
                                System.out.println("Error de sintaxis para multilang no parametro");
                                error = true;
                             }
                             break;
                    case "p":  
                             i++;
                             if (i < args.length) {
                                 try {
                                    int n = Integer.valueOf(args[i]);
                                    Environment.getInstance().setMaxCantThreads(n);
                                    i++;
                                    if (i == args.length) {
                                        System.out.println("Error de sintaxis para p no cantidad");
                                        error = true;
                                    }
                                 } catch (Exception e) {
                                     System.out.println("Error de sintaxis para p no int");
                                     error = true;
                                 } 
                             } else {
                                System.out.println("Error de sintaxis para p no parametro");
                                error = true;
                             }
                             break;
                    case "prx": 
                             i++;
                             if (i < args.length) {
                                 String URL = args[i];
                                 Environment.getInstance().setProxyURL(URL);
                                 i++;
                                 if (i == args.length) {
                                     System.out.println("Error de sintaxis para proxy no URL");
                                     error = true;
                                 }

                             } else {
                                System.out.println("Error de sintaxis para proxy no parametro");
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

        
//        HTTPSocket socket = new HTTPSocket();
//        
//        while(!Environment.getInstance().isEmptyPendingLinks())
//        { 
//            Environment.getInstance().pedirLinksAvailable();
//            Link link = Environment.getInstance().getLink();
//            Environment.getInstance().retornarLinksAvailable();
//            System.out.println("***NUEVO LINK: " + link.getLowerURL() + " TTL: " + link.getTtl());
//            try {
//                //if(link.getURL().contains("fing.edu.uy")) {
//                    socket.queryURL(link);
//                    
//                //}    
//            } catch (NoParseLinkException ex) {
//                System.err.println(ex.getMessage());
//            }     
//        }
        
        
        // MOVIDO A ENVIRONMENT.agregarHiloEnEspera
//        //Imprimo mails
//        for(String mail : Environment.getInstance().getMails())
//        {
//            System.out.println(mail);
//        }
//        
//        if(!Environment.getInstance().getNombreArchivoPozos().equals("")){
//            Environment.getInstance().escribirArchivoPozos();
//        }
//        
//        if(!Environment.getInstance().getNombreArchivoMultilang().equals("")){
//            Environment.getInstance().escribirArchivoMultilang();
//        }
//        
//        // TODO llamar imprimirDebug con  estadisticas, lista errores, etc
    }
    
}
