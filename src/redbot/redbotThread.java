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
    
    private boolean enoughMemory(){
        Runtime runtime = Runtime.getRuntime();
        float totalMemory = runtime.totalMemory();
        float freeMemory = runtime.freeMemory();
        float porcentajeLibre = freeMemory*100/totalMemory;
        System.err.print(porcentajeLibre + "\n");
        if(porcentajeLibre < 30)
        {
            System.gc();
        }
        return (porcentajeLibre > 20);
    }
    
    private int threadID;
    
    @Override
    public void run() {
            Environment.getInstance().imprimirDebug("Inicia hilo: " + threadID);
            HTTPSocket socket = new HTTPSocket();  
            boolean memoriaInsuficiente = false;
            Environment.getInstance().pedirLinksAvailable();
            while(!memoriaInsuficiente && !Environment.getInstance().isEmptyPendingLinks())
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
                                
                if(!enoughMemory()){
                    Environment.getInstance().setRecursosAgotados(true);
                }
                memoriaInsuficiente = Environment.getInstance().isRecursosAgotados();
                Environment.getInstance().pedirLinksAvailable();
            }
            Environment.getInstance().retornarLinksAvailable();
            Environment.getInstance().imprimirDebug("Hilo " + threadID + " en pausa");
            Environment.getInstance().agregarHiloEnEspera(threadID);
    }
            
}
