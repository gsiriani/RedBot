/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package redbot;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bruno.garate
 */
public class Environment {
    
    private static Environment _instance = null;

    private Set<Link> pendingLinks = new HashSet<>();
    private Semaphore linksAvailable;
    private HashMap<String,Link> allLinks = new HashMap<>();
    private Set<String> mails = new HashSet<>();
    private Set<String> pozos = new HashSet<>();
    private Semaphore pozosAvailable;
    private Set<String> multilang = new HashSet<>();
    private Semaphore multilangAvailable;
    private boolean Debug;
    private int maxDepth;
    private String nombreArchivoPozos;
    private Path pathPozos;
    private String nombreArchivoMultilang;
    private Path pathMultilang;
    private int maxCantThreads;
    private String proxyURL;
    private int proxyPort = 3128;
    
    
    private Environment() {
        Debug = false;
        linksAvailable = new Semaphore(1);
        maxDepth = -1;
        nombreArchivoMultilang = "";
        nombreArchivoPozos = "";
        maxCantThreads = 1;
        proxyURL = "";
    }

    public static Environment getInstance() {
        
        if(_instance == null)
            _instance = new Environment();
        
        return _instance;
    }

    public void addLink(Link link) {
        
        
        if(!allLinks.containsKey(link.getLowerURL())) {
            pendingLinks.add(link);    
            allLinks.put(link.getLowerURL(), link);
        }
        
    }
    
    public Link getLinkByHost(String host) {
        for (Link link : pendingLinks) {
            if (link.getHost().toLowerCase().equals(host.toLowerCase())){
               pendingLinks.remove(link);
               return link;
            }
        }
        return null;
    }
    
    public Link getLink () {
        //TODO: modificar para retornar un host no utilizado por socket
        if(!pendingLinks.isEmpty()) {
            Link link = pendingLinks.iterator().next();
            pendingLinks.remove(link);
            return link;
        } else {
            return null;
        }
    }

    public Set<String> getMails() {
        return mails;
    }

    public void setMails(Set<String> mails) {
        this.mails = mails;
    }
        
    public void addMail(String mail) {
        mails.add(mail);
    }
    
    private boolean persistent = false;

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }
    
    public void setDebug(boolean debug){
        this.Debug=debug;
    }
    
    public boolean getDebug(){
        return this.Debug;
    }
    
    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public String getNombreArchivoPozos() {
        return nombreArchivoPozos;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }
    
    
    
    public void setNombreArchivoPozos(String nombreArchivoPozos) {
        this.nombreArchivoPozos = nombreArchivoPozos;
        pathPozos = Paths.get(nombreArchivoPozos);
        // Creo el archivo
        try {
            // Create the empty file with default permissions, etc.
            Files.createFile(pathPozos);
        } catch (FileAlreadyExistsException x) {
            System.err.format("file named %s" +
                " already exists%n", pathPozos);
        } catch (IOException x) {
            // Some other sort of failure, such as permissions.
            System.err.format("createFile error: %s%n", x);
        }
    }

    public String getNombreArchivoMultilang() {
        return nombreArchivoMultilang;
    }

    public void setNombreArchivoMultilang(String nombreArchivoMultilang) {
        this.nombreArchivoMultilang = nombreArchivoMultilang;
        pathMultilang = Paths.get(nombreArchivoMultilang);
        // Creo el archivo
        try {
            // Create the empty file with default permissions, etc.
            Files.createFile(pathMultilang);
        } catch (FileAlreadyExistsException x) {
            System.err.format("file named %s" +
                " already exists%n", pathMultilang);
        } catch (IOException x) {
            // Some other sort of failure, such as permissions.
            System.err.format("createFile error: %s%n", x);
        }
    }

    public int getMaxCantThreads() {
        return maxCantThreads;
    }

    public void setMaxCantThreads(int maxCantThreads) {
        this.maxCantThreads = maxCantThreads;
    }

    public String getProxyURL() {
        return proxyURL;
    }

    public void setProxyURL(String proxyURL) {
        this.proxyURL = proxyURL;
    }

    public Path getPathPozos() {
        return pathPozos;
    }

    public Path getPathMultilang() {
        return pathMultilang;
    }   

    public void pedirLinksAvailable() {
        try {
            linksAvailable.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void retornarLinksAvailable(){
        linksAvailable.release();
    }
    
    public void pedirPozosAvailable(){
        try {
            pozosAvailable.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void retornarPozosAvailable(){
        pozosAvailable.release();
    }
    
    public void addPozo(String urlPozo){
        pozos.add(urlPozo);
    }
    
    public void pedirMultilangAvailable(){
        try {
            multilangAvailable.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void retornarMultilangAvailable(){
        multilangAvailable.release();
    }
    
    public void addMultilang(String urlMultilang){
        multilang.add(urlMultilang);
    }

    public HashMap<String, Link> getAllLinks() {
        return allLinks;
    }
    
    public boolean isEmptyPendingLinks(){
        return pendingLinks.isEmpty();
    }
    
}
