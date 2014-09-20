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
        while(!Environment.getInstance().isEmptyPendingLinks())
        {       
            Environment.getInstance().pedirLinksAvailable();
            Link link = Environment.getInstance().getLink();
            Environment.getInstance().retornarLinksAvailable();
            Environment.getInstance().imprimirDebug("Evaluando link: " + link.getLowerURL());
            socket.queryURL(link);            
        }
        Environment.getInstance().agregarHiloEnEspera(threadID);
        Environment.getInstance().imprimirDebug("Terminado hilo: " + threadID);
    }
        
    public static void main(String[] args){
        (new Thread(new redbotThread(Integer.valueOf(args[0])))).start();    
    }
    
}
