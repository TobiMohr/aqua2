package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishLocation;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.RecordsState;
import aqua.blatt1.common.msgtypes.*;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	public static final	int NUMTHREADS = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;
	public InetSocketAddress rightNeighbor;
	public InetSocketAddress leftNeighbor;
	protected boolean booltoken;
	protected RecordsState recordsState = RecordsState.IDLE;
	protected int localfishes;
	protected boolean initiatorReady = false;
	protected boolean waitForIDLE = false;
	protected int showGlobalSnapshot;
	protected boolean showDialog;
	protected int counter = 0;
	Timer timer = new Timer();
	ExecutorService executorService = Executors.newFixedThreadPool(NUMTHREADS);
	Map<String, InetSocketAddress> homeAgent = new HashMap<>();

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
			homeAgent.put(fish.getId(), null);
		}
	}

	synchronized void receiveFish(FishModel fish) {
		if (fish.getDirection() == Direction.LEFT && recordsState == RecordsState.RIGHT
		|| fish.getDirection() == Direction.RIGHT && recordsState == RecordsState.LEFT){
			localfishes++;
		}

		fish.setToStart();
		fishies.add(fish);
		updateFishLocation(fish);
	}

	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge())
				hasToken(fish);

			if (fish.disappears()){
				it.remove();
			}

		}
	}

	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() {
		forwarder.deregister(id);
	}

	public void updateNeighbors(InetSocketAddress addressLeft, InetSocketAddress addressRight){
		this.leftNeighbor = addressLeft;
		this.rightNeighbor = addressRight;
	}

	public synchronized  void receiveToken(Token token){
		this.booltoken = true;
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				booltoken = false;
				forwarder.sendToken(leftNeighbor, token);
			}
		}, 2 * 1000);
	}

	public synchronized void hasToken(FishModel fishModel){
		if(booltoken){
			forwarder.handOff(fishModel, rightNeighbor, leftNeighbor);
		} else {
			fishModel.reverse();
		}
	}

	public void initiateSnapshot() {
		if (recordsState == RecordsState.IDLE){
			localfishes = fishies.size();
			recordsState = RecordsState.BOTH;
			initiatorReady = true;
			forwarder.sendSnapshotMarker(leftNeighbor, new SnapshotMarker());
			forwarder.sendSnapshotMarker(rightNeighbor, new SnapshotMarker());
		}
	}

	public void onReceiveCollector(Collector collector){
		waitForIDLE = true;
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				while (waitForIDLE){
					if (recordsState == RecordsState.IDLE){
						int currentFishState = collector.getLocalfishes();
						int newFishState = currentFishState + localfishes;
						forwarder.sendCollector(leftNeighbor, new Collector(newFishState));
						waitForIDLE = false;
					}
				}
			}
		});

		if (initiatorReady){
			initiatorReady = false;
			showDialog = true;
			System.out.println(collector.getLocalfishes());
			showGlobalSnapshot = collector.getLocalfishes();
		}
	}

	public void receiveSnapshotMarker(InetSocketAddress sender, SnapshotMarker snapshotMarker) {
		if (recordsState == RecordsState.IDLE) {
			localfishes = fishies.size();
			if (!leftNeighbor.equals(rightNeighbor)) {
				if (sender.equals(leftNeighbor)) {
					recordsState = RecordsState.RIGHT;
				} else if (sender.equals(rightNeighbor)) {
					recordsState = RecordsState.LEFT;
				}
			} else {
				recordsState = RecordsState.BOTH;
			}
			if (leftNeighbor.equals(rightNeighbor)) {
				forwarder.sendSnapshotMarker(leftNeighbor, snapshotMarker);
			} else {
				forwarder.sendSnapshotMarker(leftNeighbor, snapshotMarker);
				forwarder.sendSnapshotMarker(rightNeighbor, snapshotMarker);
			}

		} else {
			if (!leftNeighbor.equals(rightNeighbor)) {
				if (sender.equals(leftNeighbor)) {
					if (recordsState == RecordsState.BOTH) {
						recordsState = RecordsState.RIGHT;
					}
					if (recordsState == RecordsState.LEFT) {
						recordsState = RecordsState.IDLE;
					}
				} else {
					if (recordsState == RecordsState.BOTH) {
						recordsState = RecordsState.LEFT;
					}
					if (recordsState == RecordsState.RIGHT) {
						recordsState = RecordsState.IDLE;
					}
				}
			} else {
				recordsState = RecordsState.IDLE;
			}
		}
		if (initiatorReady && recordsState == RecordsState.IDLE) {
			forwarder.sendCollector(leftNeighbor, new Collector(localfishes));
		}
	}

	public void locateFishGlobally(String fishID) {

		if (homeAgent.get(fishID) == null) {
			locateFishLocally(fishID);
		} else {
			InetSocketAddress currentFishLocation = homeAgent.get(fishID);
			forwarder.sendLocationRequest(currentFishLocation, new LocationRequest(fishID));
		}
	}

	public void locateFishLocally(String fishID) {
		for(FishModel fish : this.fishies) {
			if (fish.getId().equals(fishID)) {
				fish.toggle();
			}
		}
	}

	public void updateFishLocation(FishModel fish){
		String fishID = fish.getId();
		if (homeAgent.containsKey(fishID)){
			homeAgent.put(fishID, null);
		} else {
			forwarder.sendResolutionRequest(new NameResolutionRequest(fish.getTankId(), fishID));
		}
	}

	public void handleResponse(InetSocketAddress homeLocation, String fishID){
		forwarder.sendCurrentFishLocation(homeLocation, fishID);
	}

	public void updateCurrentLocation(String fishID, InetSocketAddress currentLocation){
		homeAgent.put(fishID, currentLocation);
	}

}