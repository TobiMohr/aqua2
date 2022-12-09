package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NameResolutionResponse implements Serializable {
    private final InetSocketAddress homeLocation;
    private final String requestID;

    public NameResolutionResponse (InetSocketAddress homeLocation, String requestID){
        this.requestID = requestID;
        this.homeLocation = homeLocation;
    }

    public String getRequestID(){
        return requestID;
    }

    public InetSocketAddress getHomeLocation(){
        return homeLocation;
    }
}
