package net.ssehub.sparkyservice.api.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HttpTimestamp {
    private String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
    
    public String toString() {
        return timeStamp;
    }
}
