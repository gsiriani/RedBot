package redbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTTPSocket {
    
    private Socket socket;
    PrintWriter out;
    BufferedReader in;
    
    private final  int CONNECTION_TIMEOUT = 100000;
    
    private boolean persistent = true; // Por defecto asumimos persistencia en
                                       // el host
    private String host = null;
    private int port = 80;
  
    private HashMap<String, String> headers = new HashMap<String, String>();   
    private String body;
    
    private HTTPProtocol protocol;
    private boolean isOK;
    private int code;
    private String message;
    
    private Link currentLink = null;
    
    public HTTPSocket() {
        socket = new Socket();
    }
        
    public boolean isPersistent() {
        return persistent;
    }

    public void queryURL(Link link) {
       
       currentLink = link;
       String strProtocol;
       
       // Compruebo persistencia
       if(Environment.getInstance().isPersistent() && isPersistent()) {
           strProtocol = "HTTP/1.1";
           persistent = true;
       } else {
           strProtocol = "HTTP/1.0";
           persistent = false;
       }
       
       //TODO: Si path tiene espacios falla
       String request = "GET " + link.getPath() + " " + strProtocol + 
               "\nHost: " + link.getHost() + "\n\n";
       
       if(    !link.getHost().equals(getHost())  // Esto si hay que cambiar socket
           || link.getPort() != getPort()) 
       {
      
            host = link.getHost(); // Actualizo los datos
            port = link.getPort();
         
            socket = new Socket();
        
       }
       
       if (socket.isClosed() || !socket.isConnected()) { // Si hay que conectar
            
            InetSocketAddress adress = new InetSocketAddress(getHost(),getPort());
            
            try {
                System.err.println("Reconectado, protocolo:" + strProtocol);
                socket = new Socket();
                socket.connect(adress, CONNECTION_TIMEOUT);
                System.err.println("Conectado!!!!");
                out = new PrintWriter(socket.getOutputStream(),true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (UnknownHostException ex) {
                System.out.println("Host " + getHost() + " desconocido");
            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
            }    
        }
        
        out.print(request); // Mandamos la request
        out.flush();
       
        String response = getResponse(in); 
        
        try {
            parseResponse(response);               
        } catch (Exception e) {
            System.err.println("Error en: " + link.getURL());
        }
       
       
        
        

       
        if(!isPersistent()) {
           try {
               System.err.println("Desconectado por no ser persistente!");
               socket.close();
           } catch (IOException ex) {
               Logger.getLogger(HTTPSocket.class.getName()).log(Level.SEVERE, null, ex);
           }
        }
        
        
    }

    
    private String getResponse(BufferedReader in) {

        StringBuilder builder = new StringBuilder();
        String line;
                
        try {
            while ((line = in.readLine()) != null) {
                //System.out.println(line);
                builder.append(line + "\n");
            }
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
        
        
        String response = builder.toString();
        
       
        return response;
                
        
    }
    
    private void parseResponse(String response) {
        String[] lines = response.split("\\r?\\n");
        
        String firstLine = lines[0];
        
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
        
        code = Integer.valueOf(firstLineItems[1]);
        
        isOK = code == 200;
        
        message = firstLineItems[2]; // TODO: Capturar mensaje completo
        
        int currentLine = 1;
        
        String[] lineContent;
        
        while(currentLine < lines.length && !lines[currentLine].isEmpty()){
            int firstColon = lines[currentLine].indexOf(":");
            String value = lines[currentLine].substring(0, firstColon).trim();
            String key =  lines[currentLine].substring(firstColon + 1).trim();
            headers.put(value, key);
            currentLine++;
        }
        
        currentLine++;
        
        StringBuilder sb = new StringBuilder();
        
        for( ; currentLine < lines.length ; currentLine++)
            sb.append(lines[currentLine]);
        
        body = sb.toString();
        ParseBody(getBody());
        
    }
    
    public void ParseBody(String body) {
        
        extractUrls(body);
        
    }
    private void extractUrls(String body){
         //TODO: Links relativos
                
        boolean esPozo = true;
        
        List<String> result = new ArrayList<>();
        String urlPattern = "((http|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern p = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(body);
        
        while (m.find()) {
            String u = (body.substring(m.start(0),m.end(0)));
            try {
                URL url = new URL(u);
                esPozo = false;
                int ttl = currentLink.getTtl();
                if (ttl != -1) ttl = ttl-1;
                if (ttl != 0)
                {
                   Link l = new Link(url.getHost(), url.getFile(), 80, ttl);
                   Environment.getInstance().addLink(l);   
                }                
            } catch (MalformedURLException e) {
                
            }        
            
        }
        if(esPozo && !Environment.getInstance().getNombreArchivoPozos().isEmpty())
        {
            try {
                Path pathPozos = Environment.getInstance().getPathPozos();
                byte[] urlActual = this.currentLink.getURL().getBytes();
                Files.write(pathPozos, urlActual, StandardOpenOption.APPEND);
            } catch (IOException ex) {
                Logger.getLogger(HTTPSocket.class.getName()).log(Level.SEVERE, null, ex);
            }
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
    


    
    
    
    
    
}
