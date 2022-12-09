package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import java.net.InetSocketAddress;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Broker {

    int NUMTHREADS = 5;
    int count;
    volatile boolean stopRequested = false;
    Endpoint endpoint = new Endpoint(4711);
    ClientCollection clientCollection = new ClientCollection();
    ExecutorService executorService = Executors.newFixedThreadPool(NUMTHREADS);
    ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    int leaseDauer = 10;
    Timer timer = new Timer();


    private class BrokerTask {
        public void brokerTask(Message message) {

            if (message.getPayload() instanceof RegisterRequest) {
                synchronized (clientCollection) {
                    register(message);
                }
                ;
            }

            if (message.getPayload() instanceof DeregisterRequest) {
                synchronized (clientCollection) {
                    deregister(message);
                }
            }

            if (message.getPayload() instanceof HandoffRequest) {
                readWriteLock.writeLock().lock();
                HandoffRequest handoffRequest = (HandoffRequest) message.getPayload();
                InetSocketAddress inetSocketAddress = message.getSender();
                handoffFish(handoffRequest, inetSocketAddress);
                readWriteLock.writeLock().unlock();
            }
            if (message.getPayload() instanceof PoisonPill) {
                System.exit(0);
            }

            if (message.getPayload() instanceof NameResolutionRequest){
                String TankID = ((NameResolutionRequest) message.getPayload())
                        .getTankID();
                String RequestID = ((NameResolutionRequest) message.getPayload()).getRequestID();
                InetSocketAddress sender = message.getSender();
                sendResponse(TankID, RequestID, sender);
            }
        }
    }

    public static void main(String[] args){
        Broker broker = new Broker();
        broker.broker();
    }

    public void broker() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("test");
                Date timestamp = new Date();
                System.out.println(clientCollection.size());
                if (clientCollection.size() > 0){
                    for(int i = 0; i < clientCollection.size(); i++){
                        Date date = clientCollection.getTimestamp(i);
                        System.out.println(date);
                        long leaseTime = timestamp.getTime() - date.getTime();
                        System.out.println(leaseTime);
                        if (leaseTime > 10 * 1000){
                            endpoint.send((InetSocketAddress) clientCollection.getClient(i), new LeasingRunOut());
                        }
                    }
                }
            }
        }, 0, 3 *  1000);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, "Press OK to stop the server");
                stopRequested = true;
            }
        });

        while (!stopRequested) {
            Message message = endpoint.blockingReceive();
            BrokerTask brokerTask = new BrokerTask();
            executorService.execute(() -> brokerTask.brokerTask(message));
        }
        executorService.shutdown();
    }

    private void register(Message msg) {
        Date timestamp = new Date();
        if (clientCollection.indexOf(msg.getSender()) == -1) {
            String id = "tank" + (count++);
            clientCollection.add(id, msg.getSender(), timestamp);
            Neighbor neighbor = new Neighbor(id);

            InetSocketAddress newClientAddress = (InetSocketAddress) clientCollection.getClient(clientCollection.indexOf(id));

            if (clientCollection.size() == 1) {
                endpoint.send(msg.getSender(), new NeighborUpdate(newClientAddress, newClientAddress));
                endpoint.send(msg.getSender(), new Token());
            } else {

                endpoint.send(neighbor.getRightNeighborSocker(), new NeighborUpdate(neighbor.getInitialRightNeighborSocket(), newClientAddress));
                endpoint.send(neighbor.getLeftNeighborSocket(), new NeighborUpdate(newClientAddress, neighbor.getInitialLeftNeighborSocket()));
                endpoint.send(newClientAddress, new NeighborUpdate(neighbor.getRightNeighborSocker(), neighbor.getLeftNeighborSocket()));
            }

            endpoint.send(msg.getSender(), new RegisterResponse(id, leaseDauer));
        } else {
            System.out.println("prüfe ob bereits registriert");
            int index = clientCollection.indexOf(msg.getSender());
            clientCollection.setTimestamp(index, timestamp);
            endpoint.send(msg.getSender(), new RegisterResponse(clientCollection.getId(index), leaseDauer));
        }
    }

    private void deregister(Message msg) {
        String removeID = ((DeregisterRequest) msg.getPayload()).getId();
        Neighbor neighbor = new Neighbor(removeID);

        if (clientCollection.size() == 2) {
            endpoint.send(neighbor.getRightNeighborSocker(), new NeighborUpdate(neighbor.getLeftNeighborSocket(),
                    neighbor.getLeftNeighborSocket()));
        } else {
            endpoint.send(neighbor.getRightNeighborSocker(), new NeighborUpdate(neighbor.getInitialRightNeighborSocket(), neighbor.getLeftNeighborSocket()));
            endpoint.send(neighbor.getLeftNeighborSocket(), new NeighborUpdate(neighbor.getRightNeighborSocker(), neighbor.getInitialLeftNeighborSocket()));
        }
        clientCollection.remove(clientCollection.indexOf(removeID));
    }

    private void handoffFish(HandoffRequest handoffRequest, InetSocketAddress inetSocketAddress) {
        int index = clientCollection.indexOf(inetSocketAddress);
        FishModel fishModel = handoffRequest.getFish();
        Direction direction = fishModel.getDirection();

        InetSocketAddress neighborReceiver;
        if (direction == Direction.LEFT) {
            neighborReceiver = (InetSocketAddress) clientCollection.getLeftNeighorOf(index);
        } else {
            neighborReceiver = (InetSocketAddress) clientCollection.getRightNeighorOf(index);
        }
        endpoint.send(neighborReceiver, handoffRequest);
    }

    final class Neighbor {
        private String id;

        public Neighbor(String id){
            this.id = id;
        }

        public InetSocketAddress getRightNeighborSocker(){
            InetSocketAddress rightNeighborSocket;
            rightNeighborSocket = (InetSocketAddress) clientCollection.getRightNeighorOf(clientCollection.indexOf(id));
            return  rightNeighborSocket;
        }

        public InetSocketAddress getInitialRightNeighborSocket(){
            InetSocketAddress initialRightNeighborSocket;
            int indexInitialRightNeighborSocket = clientCollection.indexOf(clientCollection.getRightNeighorOf(clientCollection.indexOf(id)));
            initialRightNeighborSocket = (InetSocketAddress) clientCollection.getRightNeighorOf(indexInitialRightNeighborSocket);
            return  initialRightNeighborSocket;
        }

        public InetSocketAddress getLeftNeighborSocket(){
            InetSocketAddress leftNeighborSocket;
            leftNeighborSocket = (InetSocketAddress) clientCollection.getLeftNeighorOf(clientCollection.indexOf(id));
            return leftNeighborSocket;
        }

        public InetSocketAddress getInitialLeftNeighborSocket(){
            InetSocketAddress initialLeftNeighborSocket;
            int indexInitialLeftNeighborSocket = clientCollection.indexOf(clientCollection.getLeftNeighorOf(clientCollection.indexOf(id)));
            initialLeftNeighborSocket = (InetSocketAddress) clientCollection.getLeftNeighorOf(indexInitialLeftNeighborSocket);
            return initialLeftNeighborSocket;
        }
    }

    private void sendResponse(String TankID, String RequestID, InetSocketAddress sender){
        InetSocketAddress homeClient = (InetSocketAddress) clientCollection.getClient(clientCollection.indexOf(TankID));
        endpoint.send(sender, new NameResolutionResponse(homeClient, RequestID));
    }
}
