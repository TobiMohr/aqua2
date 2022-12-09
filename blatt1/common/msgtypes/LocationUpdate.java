package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class LocationUpdate implements Serializable {

    private final String fishID;

    public LocationUpdate(String fishID){
        this.fishID = fishID;
    }

    public String getFishID(){
        return fishID;
    }
}
