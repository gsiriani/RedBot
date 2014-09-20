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
public class LinkFailed extends RuntimeException{

    public LinkFailed() {
    }

    public LinkFailed(String message) {
        super(message);
    }

    public LinkFailed(String message, Throwable cause) {
        super(message, cause);
    }

    public LinkFailed(Throwable cause) {
        super(cause);
    }

    public LinkFailed(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    
    
    
}
