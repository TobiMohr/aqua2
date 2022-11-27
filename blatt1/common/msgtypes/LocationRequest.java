package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class LocationRequest implements Serializable {
    private final String fishID;

    public LocationRequest (String fishID){
        this.fishID = fishID;
    }

    public String getFishID(){
        return fishID;
    }
}
