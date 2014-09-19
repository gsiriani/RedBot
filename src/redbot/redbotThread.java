/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package redbot;

/**
 *
 * @author Guille
 */
public class redbotThread implements Runnable{
    
    @Override
    public void run() {
        HTTPSocket socket = new HTTPSocket();        
        while(!Environment.getInstance().isEmptyPendingLinks())
        {       
            Environment.getInstance().pedirLinksAvailable();
            Link link = Environment.getInstance().getLink();
            Environment.getInstance().retornarLinksAvailable();
            socket.queryURL(link);
            
        }
    }

    public static void main(String[] args){
        (new Thread(new redbotThread())).start();    
    }
    
}
