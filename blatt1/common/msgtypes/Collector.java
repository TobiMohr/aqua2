package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public class Collector implements Serializable {
    private int localfishes;
    public Collector(int localfishes){
        this.localfishes = localfishes;
    }

    public int getLocalfishes(){
        return localfishes;
    }

    public void setLocalfishes(int localfishes){
        this.localfishes = localfishes;
    }
}
