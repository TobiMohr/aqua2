package aqua.blatt1.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToggleController implements ActionListener {
    private final String fishID;
    private final TankModel tankModel;

    public ToggleController(String fishID, TankModel tankModel){
        this.fishID = fishID;
        this.tankModel = tankModel;
    }

    @Override
    public void actionPerformed(ActionEvent e){
        tankModel.locateFishGlobally(fishID);
    }
}
