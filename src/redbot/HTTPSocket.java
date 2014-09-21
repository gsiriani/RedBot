package redbot;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
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
    private boolean redirection = false;
    
    
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
           keepAlive = "\nConnection: close"; 

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
               
               Environment.getInstance().imprimirError("Error: " + e.getMessage() +". Reintentando.");
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
               Environment.getInstance().imprimirDebug("Desconectado por no ser una conexión persistente!");
               socket.close();
           } catch (IOException ex) {
               Logger.getLogger(HTTPSocket.class.getName()).log(Level.SEVERE, null, ex);
           }
        }    
        
        
    }
    
    private void connectIfNecessary() {
        
        transferEncoding = "";
        contentLength = -1;
        redirection = false;
        message = "";
        code = -1;

        headers = new HashMap<String,String>();
        
        if (socket.isClosed() || !socket.isConnected()) { // Si hay que conectar

            InetSocketAddress adress = getSocketAdress();
            
            try {
                socket = new Socket();
                socket.connect(adress, CONNECTION_TIMEOUT);
                Environment.getInstance().imprimirDebug("Socket conectado al host '" + currentLink.getHost() + "'");
                
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

            String firstLine = null;
            
            int retry = 10;
            while (retry > 0) {
                if(in.available() > 0)
                {
                    firstLine = in.readLine();
                    break;
                }
                retry--;
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(HTTPSocket.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            if (retry == 0) {
                   throw new LinkFailed("El servidor para '" + currentLink.getHost()+ "' demoró mucho en responder al request");
            }
            
            
            
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
            
            if(!isOK)
            {
                message = "";
                for(int i = 2; i < firstLineItems.length; i++)
                {
                    message = message + " " + firstLineItems[i]; // TODO: Capturar mensaje completo
                }
                Environment.getInstance().imprimirError("Respuesta del servidor: " + code + "" + message);
                
               
            }

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
                        
             if(code >= 300 && code <= 399) {
                    if(headers.containsKey("Location")) {
                        Link l = obtenerUrlAbsoluta(headers.get("Location"));
                        if (l != null) {
                            String newLocation = l.getURL();
                            redirection = true;
                            Environment.getInstance().imprimirDebug("Redirección en '" + currentLink.getURL() + "' a '" + newLocation + "'");
                            Environment.getInstance().addLink(l);
                        } else {
                            Environment.getInstance().imprimirDebug("Redirección en '" + currentLink.getURL() + "' mal formada");    
                        }
                    } else {
                        Environment.getInstance().imprimirDebug("Redirección en '" + currentLink.getURL() + "' sin header 'Location'");
                    }
             }
                
            
            if(protocol == HTTPProtocol.HTTP10){

                StringBuilder sb = new StringBuilder();
                
                int b;

                while((b = in.read()) != -1 && !socket.isClosed()) {
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
           
           Environment.getInstance().NoEsPagina();
           
           throw new NoParseLinkException("El link '" + currentLink.getURL() +  "' no es una página");
           
           
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
        if(!Environment.getInstance().getNombreArchivoMultilang().isEmpty())
        {
            consultarMultilang(body);
        }
        // Corroboro si es pozo
        currentLink.setPozo(!tieneAbsolutas && !tieneReativas && !redirection);
        if(currentLink.isPozo() && !Environment.getInstance().getNombreArchivoPozos().isEmpty())
        {
            Environment.getInstance().pedirPozosAvailable();
            Environment.getInstance().addPozo(this.currentLink.getLowerURL());
            Environment.getInstance().retornarPozosAvailable();     
        }
        
    }
    
    private boolean extractUrlsAbsolutas(String body){     
        boolean encontro = false;
        String urlPattern = "((http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
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
                   int puerto = 80;
                   if(url.getPort() != -1) {
                       puerto = url.getPort();
                   }
                   Link l = new Link(url.getHost(), url.getFile(), puerto, ttl);
                   Environment.getInstance().pedirLinksAvailable();
                   Environment.getInstance().addLink(l);   
                   Environment.getInstance().retornarLinksAvailable();
                }                
            } catch (MalformedURLException e) {
                
            }        
            
        }
        Environment.getInstance().imprimirDebug("Encontré " + i + " links absolutas en la URL '" + currentLink.getURL() + "'");
        
        return encontro;
    }
    
    private boolean extractUrlsRelativas(String body){   
        
        boolean encontro = false;
        String relativeUrlPattern = "href=\\\"(.*?)\\\"";
        Pattern p = Pattern.compile(relativeUrlPattern,Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(body);
        int i = 0;
        
        while (m.find()) {
            String encontrado = (body.substring(m.start(0),m.end(0)));
            if (encontrado.length()> 9){
                String u = "";
                if (encontrado.substring(6, 9).equals("www")){
                    u = "http://" + encontrado.substring(6, encontrado.length()-1);
                } else if (encontrado.substring(6, 8).equals("//")) {
                    u = "http:" + encontrado.substring(6, encontrado.length()-1);
                } else if (encontrado.substring(6, 7).equals("/")) {
                    u = "http://" + currentLink.getHost() + encontrado.substring(6, encontrado.length()-1);    
                } else if (!encontrado.substring(6, 10).equals("http") && !encontrado.contains("@")){
                    String currentLinkURL = currentLink.getURL();
                    String cortarURL = currentLinkURL.substring(0, currentLinkURL.lastIndexOf("/"));
                    u = cortarURL + "/" + encontrado.substring(6, encontrado.length()-1);
                }
                
                if (!u.isEmpty()) {
                    encontro = true;
                    i++;
                    try {
                        URL url = new URL(u);
                        int ttl = currentLink.getTtl();
                        if (ttl != -1) ttl = ttl-1;
                        if (ttl != 0)
                        {
                            int puerto = 80;
                            if(url.getPort() != -1) {
                                puerto = url.getPort();
                            }
                           Link l = new Link(url.getHost(), url.getFile(), puerto, ttl);
                           Environment.getInstance().pedirLinksAvailable();
                           Environment.getInstance().addLink(l);   
                           Environment.getInstance().retornarLinksAvailable();
                        }                
                    } catch (MalformedURLException e) {

                    }
                }
            }
        }
        
        Environment.getInstance().imprimirDebug("Encontré " + i + " links relativas en la URL '" + currentLink.getURL() + "'");
        
        return encontro;
        
        
    }
    
    private Link obtenerUrlAbsoluta(String body){     
        boolean encontro = false;
        Link l;
        
        String urlPattern = "((http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
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
                l = new Link(url.getHost(), url.getFile(), 80, ttl);
                return l;
            } catch (MalformedURLException e) {

            }        
            
        }
        
        if (encontro = false) {
            String relativeUrlPattern = "href=\\\"(.*?)\\\"";
            p = Pattern.compile(relativeUrlPattern,Pattern.CASE_INSENSITIVE);
            m = p.matcher(body);
            
            
            String puerto;
            if(currentLink.getPort() != 80) {
                puerto = ":" + currentLink.getPort();
            } else {
                puerto = "";
            }

            while (m.find()) {
                String encontrado = (body.substring(m.start(0),m.end(0)));
                if (encontrado.length()> 9){
                    String u = "";
                    if (encontrado.substring(6, 9).equals("www")){
                        u = "http://" + encontrado.substring(6, encontrado.length()-1);
                    } else if (encontrado.substring(6, 8).equals("//")) {
                        u = "http:" + encontrado.substring(6, encontrado.length()-1);
                    } else if (encontrado.substring(6, 7).equals("/")) {
                        u = "http://" + currentLink.getHost() + puerto + encontrado.substring(6, encontrado.length()-1);    
                    } else if (!encontrado.substring(6, 10).equals("http") && !encontrado.contains("@")){
                        String currentLinkURL = currentLink.getURL();
                        String cortarURL = currentLinkURL.substring(0, currentLinkURL.lastIndexOf("/"));
                        u = cortarURL + "/" + encontrado.substring(6, encontrado.length()-1);
                    }

                    if (!u.isEmpty()) {
                        encontro = true;
                        i++;
                        try {
                            URL url = new URL(u);
                            int ttl = currentLink.getTtl();
                            l = new Link(url.getHost(), url.getFile(), url.getPort(), ttl);
                            return l;
                        } catch (MalformedURLException e) {

                        }
                    }
                }
            }
        }
        
        return null;
        
    }
    
    private void extractMails(String body){
        String mailPattern = "[_A-Za-z0-9-]+(\\.[\\+_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})";
        Pattern p = Pattern.compile(mailPattern);
        Matcher m = p.matcher(body);        
        while (m.find()) {
            String mail = m.group();
            Environment.getInstance().pedirMailsAvailable();
            Environment.getInstance().addMail(mail);
            Environment.getInstance().retornarMailsAvailable();
        }       
    }
    
    private void consultarMultilang(String body){
        String[] lenguajes = {"en", "es", "ca", "cs", "da", "de", "nl", "el",
            "eu", "fi", "fr", "he", "hr", "hu", "it", "ja", "ko", "no", "pl", 
            "pt", "ru", "sv", "tr", "uk", "zh"};
        boolean encontrado = false;
        // Me fijo en URL
        int pos = 0;
        String currentLinkHost = currentLink.getHost();
        while(pos < lenguajes.length && !encontrado)
        {
            encontrado = currentLink.getURL().contains("/" + lenguajes[pos] + "/");
            encontrado = encontrado || currentLink.getHost().startsWith(lenguajes[pos] + ".");
            if (currentLink.getHost().startsWith(lenguajes[pos] + ".")) 
                currentLinkHost = currentLinkHost.substring(3);
            encontrado = encontrado || currentLink.getPath().endsWith("-" + lenguajes[pos]);
            encontrado = encontrado || currentLink.getPath().contains("-" + lenguajes[pos] + "/");
            encontrado = encontrado || currentLink.getPath().contains("-" + lenguajes[pos] + ".");
            pos++;
        }
        if(!encontrado)
        {
            // Busco en el html
            String linkLengAlt = " hreflang=\"";
            encontrado = body.contains(linkLengAlt);
        }        
        
        if(encontrado){
            Environment.getInstance().pedirMultilangAvailable();
            Environment.getInstance().addMultilang(currentLinkHost);
            Environment.getInstance().retornarMultilangAvailable();
        }    
    }
    
    
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
