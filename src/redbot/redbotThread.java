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

    public redbotThread(int threadID) {
        this.threadID = threadID;
    }
    
    private int threadID;
    
    @Override
    public void run() {
            Environment.getInstance().imprimirDebug("Inicia hilo: " + threadID);
            HTTPSocket socket = new HTTPSocket();  
            Environment.getInstance().pedirLinksAvailable();
            while(!Environment.getInstance().isEmptyPendingLinks())
            {                   
                Link link = Environment.getInstance().getLink();
                Environment.getInstance().retornarLinksAvailable();
                String ttl = "";
                if(link.getTtl() != -1) {
                    ttl = "con TTL " + link.getTtl();
                }
                Environment.getInstance().imprimirDebug("Evaluando link '" + link.getLowerURL() + "'" + ttl);
                try {
                    socket.queryURL(link);   
                } catch (NoParseLinkException ex) {
                    Environment.getInstance().imprimirDebug("Página no parseada:" + ex.getMessage());
                }
                Environment.getInstance().pedirLinksAvailable();
            }
            Environment.getInstance().retornarLinksAvailable();
            Environment.getInstance().agregarHiloEnEspera(threadID);
            Environment.getInstance().imprimirDebug("Hilo " + threadID + " en pausa");
        
    }
            
}
