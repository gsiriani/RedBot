
package redbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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
    public static void main(String[] args) {
     
       
        Link l = new Link("www.fing.edu.uy","/",80,5);
        Environment.getInstance().addLink(l);
        HTTPSocket socket = new HTTPSocket();
        
        while(!Environment.getInstance().pendingLinks.isEmpty())
        { 
            
            Link link = Environment.getInstance().getLink();
            
            
            System.out.println("NUEVO LINK: " + link.getURL() + " TTL: " + link.getTtl());
            socket.queryURL(link);
            
        }
        
        System.out.println(Environment.getInstance().allLinks.toString());
    }
    
}
