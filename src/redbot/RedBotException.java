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
public class RedBotException extends RuntimeException{

    public RedBotException() {
    }

    public RedBotException(String message) {
        super(message);
    }

    public RedBotException(String message, Throwable cause) {
        super(message, cause);
    }

    public RedBotException(Throwable cause) {
        super(cause);
    }

    public RedBotException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    
    
    
}
