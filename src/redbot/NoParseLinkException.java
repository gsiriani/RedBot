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
public class NoParseLinkException extends RuntimeException{

    public NoParseLinkException() {
    }

    public NoParseLinkException(String message) {
        super(message);
    }

    public NoParseLinkException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoParseLinkException(Throwable cause) {
        super(cause);
    }

    public NoParseLinkException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    
    
    
}
