package redbot;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTTPSocket {
    
    private Socket socket;
    PrintWriter out;
    DataInputStream in;
    
    private final  int CONNECTION_TIMEOUT = 5000;
    
    private boolean persistent = true; // Por defecto asumimos persistencia en
                                       // el host
    private String host = null;
    private int port = 80;
  
    private HashMap<String, String> headers = new HashMap<String,String>();   
    private String body;
    
    private HTTPProtocol protocol;
    private boolean isOK;
    private int code;
    private String message;
    private int contentLength = -1;
    private String transferEncoding = "";
    private Link currentLink = null;
    private int noReconnections = 0;
    
    public HTTPSocket() {
        socket = new Socket();
        
    }
        
    public boolean isPersistent() {
        return persistent;
    }

    public void queryURL(Link link) {
       // TODO: arreglar pozos
       currentLink = link;
       
       String strProtocol;
       String keepAlive;
       
       // Compruebo persistencia
       if(Environment.getInstance().isPersistent() && isPersistent()) {
           strProtocol = "HTTP/1.1";
           persistent = true;
           keepAlive = "\nConnection: Keep-Alive";
       } else {
           strProtocol = "HTTP/1.0";
           persistent = false;
           keepAlive = ""; 

       }
       
       
       //TODO: Si path tiene espacios falla
       String request = "GET " + link.getURL()+ " " + strProtocol + 
               "\nHost: " + link.getHost() +
               keepAlive +
               "\nAccept-Charset: utf-8" +
               "\n\n";
       
       if(!link.getHost().equals(getHost())  // Esto si hay que cambiar socket
           || link.getPort() != getPort()) 
       {
      
            host = link.getHost(); // Actualizo los datos
            port = link.getPort();
            socket = new Socket();
        
       }
       
       int retry_attempts = 1;
       boolean retry = true;
       while(retry) 
       {
           connectIfNecessary();
           retry = false;
           sendRequest(request);
           try {
               
               getResponse(in); 
               
           } catch (LinkFailed e) {
               
               System.err.println("Error: " + e.getMessage() +".\nReintentando.");
               retry = true;
               
               try {
                   socket.close();
                   socket = new Socket();
               } catch (IOException ex) {
                   Logger.getLogger(HTTPSocket.class.getName()).log(Level.SEVERE, null, ex);
               }
               
               retry_attempts--;
               
               if(retry_attempts < 0)
               {
                   throw new NoParseLinkException("Demasiados intentos. Se considera incorrecto el link");
               }
               
           }
            
       }
        //Pregunto por multilang
        /*String pidoMultilang = "";
        if(!Environment.getInstance().getNombreArchivoMultilang().isEmpty())
            consultarMultilang();
       */
        
        if(!isPersistent() || !Environment.getInstance().isPersistent()) {
           try {
               System.err.println("Desconectado por no ser persistente!");
               socket.close();
           } catch (IOException ex) {
               Logger.getLogger(HTTPSocket.class.getName()).log(Level.SEVERE, null, ex);
           }
        }       
        
    }
    
    private void connectIfNecessary() {
        
        transferEncoding = "";
        contentLength = -1;

        headers = new HashMap<String,String>();
        
        if (socket.isClosed() || !socket.isConnected()) { // Si hay que conectar

            InetSocketAddress adress = getSocketAdress();
            
            try {
                socket = new Socket();
                socket.connect(adress, CONNECTION_TIMEOUT);
                System.err.println("Conectado!!!!");
                
                try {
                    out = new PrintWriter(socket.getOutputStream(),true);
                    in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                } catch (IOException ex) {
                    throw new NoParseLinkException(ex.getMessage());
                }
            } catch (UnknownHostException ex) {
                throw new NoParseLinkException("Host " + getHost() + " desconocido");
            } catch (IOException ex) {
                throw new NoParseLinkException(ex.getMessage());
            }    
            
            
        } 
    }
    
    private void sendRequest(String request){
        
       connectIfNecessary();
        out.print(request); // Mandamos la request
        out.flush();
    }

    
    private String getResponse(DataInputStream in) {

        StringBuilder builder = new StringBuilder();
        String line;
                       
        try {
            /* LEO PRIMERA LINEA */

            String firstLine = in.readLine();
            
            if(firstLine == null)
                throw new LinkFailed("Primera Linea Null");
            
            String[] firstLineItems = firstLine.split("\\s");

            // TODO: Resolver si no soporta el protocolo de pedido
            if ("HTTP/1.0".equals(firstLineItems[0])) {
                protocol = HTTPProtocol.HTTP10;
                persistent = false;
            } else if ("HTTP/1.1".equals(firstLineItems[0])) {
                protocol = HTTPProtocol.HTTP11;
                persistent = true;
            } else {
                protocol = HTTPProtocol.UNKNOWN;
                persistent = false;
            }
            
            try {
                code = Integer.valueOf(firstLineItems[1]);
            } catch (Exception ex) {
                throw new LinkFailed("Respuesta inválida. La linea era: " +firstLine);
            }

            isOK = code == 200;

            message = firstLineItems[2]; // TODO: Capturar mensaje completo

            /* LEO CABEZALES */
                       
            while (true) {
                line = in.readLine();
                
                if(line == null)
                    continue;
                else if(line.isEmpty())
                    break;
                        
                int firstColon = line.indexOf(":");
                String value = line.substring(0, firstColon).trim();
                String key =  line.substring(firstColon + 1).trim();
                headers.put(value, key);
            }
            
            analyzeHeaders();    
            
            System.out.println("te = " + transferEncoding);
            System.out.println("cl = " + contentLength);
            
            
            if(protocol == HTTPProtocol.HTTP10){

                StringBuilder sb = new StringBuilder();
                
                int b;

                while(socket.isClosed() || !socket.isConnected()) {
                    b = in.read();
                    sb.append((char)b);
                }
                
                body = sb.toString();
            } else if(contentLength != -1)
            {
                byte[] content = new byte[contentLength];
                int readed = 0;
                while(readed < contentLength)
                {
                    int read = in.read(content,readed,contentLength - readed);
                    readed += read;
                }
                body = new String(content,"UTF-8");
            } else if (transferEncoding.toLowerCase().equals("chunked")){
                StringBuilder sb = new StringBuilder();
                boolean terminado = false;

                while(!terminado) {

                    String l;

                    while("".equals(l= in.readLine())){}

                    int index;

                    if ((index = l.indexOf(';')) != -1)
                    {
                        l = l.substring(0,index);
                    }

                    int size;

                    try {
                         size = Integer.decode("0x"+l);
                    } catch (NumberFormatException ex) {
                        throw new LinkFailed("Linea hex inválida, leí:" + l);
                    }

                    if(size == 0) {
                        terminado = true;
                        l = in.readLine();
                    }   

                    byte[] content = new byte[size];
                    int readed = 0;
                    while(readed < size)
                    {
                        int read = in.read(content,readed,size - readed);
                        readed += read;
                    }

                    sb.append(new String(content,"UTF-8"));  

                }

                body = sb.toString();

            } else {
                throw new NoParseLinkException("No hay suficiente información para procesar");
            }
            //in.close();   
            ParseBody(body);
        } catch (IOException ex) {
            throw new NoParseLinkException(ex.getMessage());
        }
        String response = builder.toString();
        return response;
    }            

    
    private void analyzeHeaders() {
    
        // TODO : Estoy filtrando mas de lo necesario
        if(!(headers.containsKey("Content-Type") && (headers.get("Content-Type").contains("text/html"))))
        {
           
            try {
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger(HTTPSocket.class.getName()).log(Level.SEVERE, null, ex);
            }
           socket = new Socket();
           
           throw new NoParseLinkException("El archivo no es una página");
           
        }

        if(headers.containsKey("Content-Length"))
        {
            contentLength = Integer.parseInt(headers.get("Content-Length"));

        }
        
        if(headers.containsKey("Transfer-Encoding"))
        {
            transferEncoding = headers.get("Transfer-Encoding");
        }
        
    }
        
    public void ParseBody(String body) {
        
        boolean tieneAbsolutas = extractUrlsAbsolutas(body);
        boolean tieneReativas = extractUrlsRelativas(body);
        extractMails(body);
        // Corroboro si es pozo
        currentLink.setPozo(!tieneAbsolutas && !tieneReativas);
        if(currentLink.isPozo() && !Environment.getInstance().getNombreArchivoPozos().isEmpty())
        {
            Environment.getInstance().pedirPozosAvailable();
            Environment.getInstance().addPozo(this.currentLink.getLowerURL());
            Environment.getInstance().retornarPozosAvailable();     
        }
        
    }
    
    private boolean extractUrlsAbsolutas(String body){     
        boolean encontro = false;
        String urlPattern = "((http|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern p = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(body);
        int i = 0;
        while (m.find()) {
            encontro = true;
            String u = (body.substring(m.start(0),m.end(0)));
            try {
                i++;
                URL url = new URL(u);
                int ttl = currentLink.getTtl();
                if (ttl != -1) ttl = ttl-1;
                if (ttl != 0)
                {
                   Link l = new Link(url.getHost(), url.getFile(), 80, ttl);
                   Environment.getInstance().pedirLinksAvailable();
                   Environment.getInstance().addLink(l);   
                   Environment.getInstance().retornarLinksAvailable();
                }                
            } catch (MalformedURLException e) {
                
            }        
            
        }
        System.out.println("Encontré:" + i + " links");
        
        return encontro;
    }
    
    private boolean extractUrlsRelativas(String body){   
        boolean encontro = false;
        String relativeUrlPattern = "(href=\"[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*\")";
        Pattern p = Pattern.compile(relativeUrlPattern,Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(body);
        
        while (m.find()) {
            String encontrado = (body.substring(m.start(0),m.end(0)));
            if (encontrado.length()> 9){
                String u = "";
                if (encontrado.substring(6, 9).equals("www")){
                    u = "http://" + encontrado.substring(6, encontrado.length()-1);
                } else if (encontrado.substring(6, 7).equals("/")) {
                    u = "http://" + currentLink.getHost() + encontrado.substring(6, encontrado.length()-1);
                } else if (!encontrado.substring(6, 10).equals("http") && !encontrado.contains("@")){
                    String currentLinkURL = currentLink.getURL();
                    String cortarURL = currentLinkURL.substring(0, currentLinkURL.lastIndexOf("/"));
                    u = cortarURL + "/" + encontrado.substring(6, encontrado.length()-1);
                }
                
                if (!u.isEmpty()) {
                    System.out.println("Encontre el link " + u); 
                    encontro = true;
                    try {
                        URL url = new URL(u);
                        int ttl = currentLink.getTtl();
                        if (ttl != -1) ttl = ttl-1;
                        if (ttl != 0)
                        {
                           Link l = new Link(url.getHost(), url.getFile(), 80, ttl);
                           Environment.getInstance().pedirLinksAvailable();
                           Environment.getInstance().addLink(l);   
                           Environment.getInstance().retornarLinksAvailable();
                        }                
                    } catch (MalformedURLException e) {
                        System.out.println(e);
                    }
                }
            }
        }
        return encontro;
    }
    
    private void extractMails(String body){
        String mailPattern = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+"
                + "(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:"
                + "[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23"
                + "-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c"
                + "\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*"
                + "[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"
                + "|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)"
                + "\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|"
                + "[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x"
                + "0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x0"
                + "9\\x0b\\x0c\\x0e-\\x7f])+)\\])";
        Pattern p = Pattern.compile(mailPattern,Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(body);        
        while (m.find()) {
            String mail = (body.substring(m.start(0),m.end(0)));
            Environment.getInstance().pedirMailsAvailable();
            Environment.getInstance().addMail(mail);
            Environment.getInstance().retornarMailsAvailable();
        }       
    }
    
//    private void consultarMultilang(){
//        String[] lenguajes = {"en", "es", "ca", "cs", "da", "de", "nl", "el",
//            "eu", "fi", "fr", "he", "hr", "hu", "it", "ja", "ko", "no", "pl", 
//            "pt", "ru", "sv", "tr", "uk", "zh"}; 
//        Link link = currentLink;
//        int contador = 0;
//        int pos = 0;
//        
//        while(pos < lenguajes.length && contador < 2)
//        {
//            String lenguaje = lenguajes[pos];
//            String solicitudLenguaje = "\nAccept-Language:" + lenguaje;
//            //TODO: Si path tiene espacios falla
//            String request = "GET " + link.getPath() + " HTTP/1.0" + 
//               "\nHost: " + link.getHost() + solicitudLenguaje + "\n\n";
//
//            if( !link.getHost().equals(getHost())  // Esto si hay que cambiar socket
//                || link.getPort() != getPort()) 
//            {
//                 host = link.getHost(); // Actualizo los datos
//                 port = link.getPort();
//                 socket = new Socket();
//            }
//
//            if (socket.isClosed() || !socket.isConnected()) { // Si hay que conectar
//                 InetSocketAddress adress = getSocketAdress();
//                 try {
//                     socket = new Socket();
//                     socket.connect(adress, CONNECTION_TIMEOUT);
//                     System.err.println("Conectado!!!!");
//                     out = new PrintWriter(socket.getOutputStream(),true);
//                     in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                 } catch (UnknownHostException ex) {
//                     throw new NoParseLinkException("Host " + getHost() + " desconocido");
//                 } catch (IOException ex) {
//                     throw new NoParseLinkException(ex.getMessage());
//                 }    
//            }
//
//            out.print(request); // Mandamos la request
//            out.flush();
//            out.close();
//            
//            String response = getResponse(in); 
//             
//            String[] lines = response.split("\\r?\\n");
//
//            int currentLine = 1;
//            String key, value;
//            value = "";
//            boolean encontro = false;
//            while(currentLine < lines.length && !lines[currentLine].isEmpty() && !encontro){
//                int firstColon = lines[currentLine].indexOf(":");
//                key = lines[currentLine].substring(0, firstColon).trim();
//                if(key.equals("Content-Language"))
//                {
//                    value =  lines[currentLine].substring(firstColon + 1).trim();
//                    encontro = true;
//                }
//                currentLine++;
//            }
//            if(value.contains(lenguaje))
//            {
//                contador = contador + 1;
//            }
//            pos++;
//        }
//        if(contador == 2)
//        {
//            if(!Environment.getInstance().getNombreArchivoMultilang().isEmpty())
//            {
//                Environment.getInstance().pedirMultilangAvailable();
//                Environment.getInstance().addMultilang(currentLink.getLowerURL());
//                Environment.getInstance().retornarMultilangAvailable();
//            }
//        }
//    }
    
    
    
    public String getHost() {
        return host;
    }
    String getBody() {
        return body;
    }
    
    public int getCode() {
        return code;
    }

    public HTTPProtocol getProtocol() {
        return protocol;
    }

    public String getMessage() {
        return message;
    }

    
     
    public boolean isOK() {
        return isOK;
    }

    public int getPort() {
        return port;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    private InetSocketAddress getSocketAdress() {
            
            if(!Environment.getInstance().getProxyURL().isEmpty())
            {
                return new InetSocketAddress(
                        Environment.getInstance().getProxyURL(),
                        Environment.getInstance().getProxyPort());    
            } else {
                return new InetSocketAddress(getHost(),getPort());    
            }
           
    }
  
}
