package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NeighborUpdate implements Serializable {

    private final InetSocketAddress addressRight;
    private final InetSocketAddress addressLeft;

    public NeighborUpdate (InetSocketAddress addressRight, InetSocketAddress addressLeft){
        this.addressLeft = addressLeft;
        this.addressRight = addressRight;
    }

    public InetSocketAddress getAddressRight(){
        return addressRight;
    }

    public InetSocketAddress getAddressLeft(){
        return addressLeft;
    }
}
