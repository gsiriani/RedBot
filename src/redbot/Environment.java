/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package redbot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author bruno.garate
 */
public class Environment {
    
    private static Environment _instance = null;

    Set<Link> pendingLinks = new HashSet<Link>();
    HashMap<String,Link> allLinks = new HashMap<String,Link>();
    Set<String> mails = new HashSet<>();
    private boolean Debug;
    private int maxDepth;
    private String nombreArchivoPozos;
    private String nombreArchivoMultilang;
    private int cantThreads;
    private String proxyURL;
 
    
    private Environment() {
        Debug = false;
        maxDepth = -1;
        nombreArchivoMultilang = null;
        nombreArchivoPozos = null;
        cantThreads = 1;
        proxyURL = null;
    }

    public static Environment getInstance() {
        
        if(_instance == null)
            _instance = new Environment();
        
        return _instance;
    }

    public Set<Link> getLinks() {
        return pendingLinks;
    }

    public void setLinks(Set<Link> links) {
        this.pendingLinks = links;
    }
    
    public void addLink(Link link) {
        
        
        if(!allLinks.containsKey(link.getURL())) {
            pendingLinks.add(link);    
            allLinks.put(link.getURL(), link);
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

    public void setNombreArchivoPozos(String nombreArchivoPozos) {
        this.nombreArchivoPozos = nombreArchivoPozos;
    }

    public String getNombreArchivoMultilang() {
        return nombreArchivoMultilang;
    }

    public void setNombreArchivoMultilang(String nombreArchivoMultilang) {
        this.nombreArchivoMultilang = nombreArchivoMultilang;
    }

    public int getCantThreads() {
        return cantThreads;
    }

    public void setCantThreads(int cantThreads) {
        this.cantThreads = cantThreads;
    }

    public String getProxyURL() {
        return proxyURL;
    }

    public void setProxyURL(String proxyURL) {
        this.proxyURL = proxyURL;
    }
       
}
