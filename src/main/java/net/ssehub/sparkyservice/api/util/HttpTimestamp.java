package net.ssehub.sparkyservice.api.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Format of used HTTP timestamps in sparkyservice.
 * 
 * @author marcel
 */
public class HttpTimestamp {
    private String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
    
    @Override
    public String toString() {
        return timeStamp;
    }
}
