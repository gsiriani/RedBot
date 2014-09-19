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
    
    private final  int CONNECTION_TIMEOUT = 100000;
    
    private boolean persistent = true; // Por defecto asumimos persistencia en
                                       // el host
    private String host = null;
    private int port = 80;
  
    private HashMap<String, String> headers = new HashMap<>();   
    private String body;
    
    private HTTPProtocol protocol;
    private boolean isOK;
    private int code;
    private String message;
    private int contentLength = -1;
    private String transferEncoding = "";
    
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
       
       if(    !link.getHost().equals(getHost())  // Esto si hay que cambiar socket
           || link.getPort() != getPort()) 
       {
      
            host = link.getHost(); // Actualizo los datos
            port = link.getPort();
            socket = new Socket();
        
       }
       
       if (socket.isClosed() || !socket.isConnected()) { // Si hay que conectar
            
            InetSocketAddress adress = getSocketAdress();
            
            try {
                System.err.println("Protocolo:" + strProtocol);
                socket = new Socket();
                socket.connect(adress, CONNECTION_TIMEOUT);
                System.err.println("Conectado!!!!");
            } catch (UnknownHostException ex) {
                throw new NoParseLinkException("Host " + getHost() + " desconocido");
            } catch (IOException ex) {
                throw new NoParseLinkException(ex.getMessage());
            }    
        }
       
        try {
            out = new PrintWriter(socket.getOutputStream(),true);
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        } catch (IOException ex) {
            throw new NoParseLinkException(ex.getMessage());
        }
        
        
        out.print(request); // Mandamos la request
        out.flush();
       
        getResponse(in); 
        
        
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

    
    private String getResponse(DataInputStream in) {

        StringBuilder builder = new StringBuilder();
        String line;
                       
        try {
            
            
            /* LEO PRIMERA LINEA */

            String firstLine = in.readLine();
            
            if(firstLine == null)
                throw new NoParseLinkException("Primera Linea Null");
            
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
            } catch (NumberFormatException ex) {
                code = -1;
                isOK = false;
                throw new NoParseLinkException("Respuesta inválida!");
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
            
            try {
                analyzeHeaders();    
            } catch (NoParseLinkException ex) {
                System.err.println(ex.getMessage());
            }
            
            if(protocol == HTTPProtocol.HTTP11) {
                if(contentLength != -1)
                {
                    byte[] content = new byte[contentLength];
                    int readed = 0;
                    while(readed < contentLength)
                    {
                        int read = in.read(content,readed,contentLength - readed);
                        readed += read;
                    }
                    body = new String(content,"UTF-8");
                } else { // CHUNKED
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

                        int size = Integer.decode("0x"+l);

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

                }
            } else {

                StringBuilder sb = new StringBuilder();
                
                int b;

                while((b = in.read()) != -1) {
                    
                    sb.append((char)b);
                }
                
                body = sb.toString();
                
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
           Environment.getInstance().addPozo(currentLink); 
           throw new NoParseLinkException("El archivo no es una página");
        }

        if(!headers.containsKey("Content-Length"))
        {
          //  throw new NoParseLinkException("La respuesta no especifica un tamaño de contenido");
        } else {
            contentLength = Integer.parseInt(headers.get("Content-Length"));
        }
        
        if(!headers.containsKey("Transfer-Encoding"))
        {
          //  throw new NoParseLinkException("La respuesta no especifica un tamaño de contenido");
        } else {
            transferEncoding = headers.get("Transfer-Encoding");
        }
        
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
                byte[] urlActual = this.currentLink.getLowerURL().getBytes();
                Files.write(pathPozos, urlActual, StandardOpenOption.APPEND);
            } catch (IOException ex) {
                Logger.getLogger(HTTPSocket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
 /*   private void consultarMultilang(){
        String[] lenguajes = {"en", "es", "ca", "cs", "da", "de", "nl", "el",
            "eu", "fi", "fr", "he", "hr", "hu", "it", "ja", "ko", "no", "pl", 
            "pt", "ru", "sv", "tr", "uk", "zh"}; 
        Link link = currentLink;
        int contador = 0;
        int pos = 0;
        
        while(pos < lenguajes.length && contador < 2)
        {
            String lenguaje = lenguajes[pos];
            String solicitudLenguaje = "\nAccept-Language:" + lenguaje;
            //TODO: Si path tiene espacios falla
            String request = "GET " + link.getPath() + " HTTP/1.0" + 
               "\nHost: " + link.getHost() + solicitudLenguaje + "\n\n";

            if( !link.getHost().equals(getHost())  // Esto si hay que cambiar socket
                || link.getPort() != getPort()) 
            {
                 host = link.getHost(); // Actualizo los datos
                 port = link.getPort();
                 socket = new Socket();
            }

            if (socket.isClosed() || !socket.isConnected()) { // Si hay que conectar
                 InetSocketAddress adress = getSocketAdress();
                 try {
                     socket = new Socket();
                     socket.connect(adress, CONNECTION_TIMEOUT);
                     System.err.println("Conectado!!!!");
                     out = new PrintWriter(socket.getOutputStream(),true);
                     in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 } catch (UnknownHostException ex) {
                     throw new NoParseLinkException("Host " + getHost() + " desconocido");
                 } catch (IOException ex) {
                     throw new NoParseLinkException(ex.getMessage());
                 }    
            }

            out.print(request); // Mandamos la request
            out.flush();
            out.close();
            
            String response = getResponse(in); 
             
            String[] lines = response.split("\\r?\\n");

            int currentLine = 1;
            String key, value;
            value = "";
            boolean encontro = false;
            while(currentLine < lines.length && !lines[currentLine].isEmpty() && !encontro){
                int firstColon = lines[currentLine].indexOf(":");
                key = lines[currentLine].substring(0, firstColon).trim();
                if(key.equals("Content-Language"))
                {
                    value =  lines[currentLine].substring(firstColon + 1).trim();
                    encontro = true;
                }
                currentLine++;
            }
            if(value.contains(lenguaje))
            {
                contador = contador + 1;
            }
            pos++;
        }
        if(contador == 2)
        {
            if(!Environment.getInstance().getNombreArchivoMultilang().isEmpty())
            {
                try {
                    Path pathMultilang = Environment.getInstance().getPathMultilang();
                    byte[] urlActual = this.currentLink.getLowerURL().getBytes();
                    Files.write(pathMultilang, urlActual, StandardOpenOption.APPEND);
                } catch (IOException ex) {
                    Logger.getLogger(HTTPSocket.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }*/
    
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
