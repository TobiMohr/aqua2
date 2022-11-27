package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NameResolutionRequest implements Serializable {
    private final String tankID;
    private final InetSocketAddress requestID;

    public NameResolutionRequest (String tankID, InetSocketAddress requestID){
        this.tankID = tankID;
        this.requestID = requestID;
    }

    public String getTankID(){
        return tankID;
    }

    public InetSocketAddress getRequestID(){
        return requestID;
    }

}
