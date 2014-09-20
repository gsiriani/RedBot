/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package redbot;

/**
 *
 * @author bruno.garate
 */
public class Link {
    
    private String host;
    private String path;
    private int port;
    private int ttl;  
    private boolean pozo;

    public boolean isPozo() {
        return pozo;
    }

    public void setPozo(boolean pozo) {
        this.pozo = pozo;
    }

    public Link(String host, String path, int port, int ttl) {
        this.host = host;
        this.path = path;
        this.port = port;
        this.ttl = ttl;
        this.pozo = true;
    }

    public Link() {
    }
    
    

    public void setHost(String host) {
        this.host = host;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public String getHost() {
        return host;
    }

    public String getPath() {
        return path;
    }

    public int getPort() {
        return port;
    }

    public int getTtl() {
        return ttl;
    }
    
    public String getLowerURL() {
        return (getHost() + getPath()).toLowerCase();
    }
    
    public String getURL() {
        return ("http://" + getHost() + getPath());
    }

    @Override
    public String toString() {
        return getURL();
    }
 
    
    
}
