package app;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.AtomicDoubleArray;

import app.ApplicationMessage.AppMessage;
import app.ApplicationMessage.AppMessage.MessageType;
import util.Neighbor;
import network.Node;

public class PushSumEngine {

	public static final int PUSH_SUM_PORT = 9856;
	
	private static final double ERROR = 0.001;
	private static final double PROBABILITY = 0.99;
	
	private Node localNode;
	private AtomicInteger mixTime;
	
	private int currentRound;
	private int numOfRounds;
	
	private AtomicBoolean protocolRunning;
	
	private Map<String, Double> query;
	
	AtomicDoubleArray values;
	AtomicDoubleArray weights;
	AtomicDoubleArray receivedValues;
	AtomicDoubleArray receivedWeights;
	
	private String currentQuery;
	private int numberOfInLinks;
	private int [] linksInRound;
	
	
	private ExecutorService executor;

	
	public PushSumEngine(Node localNode) {
		this.localNode = localNode;
//		this.numOfRounds = (int)(this.mixTime.get()+Math.ceil(Math.log(networkSize)));
		this.mixTime = new AtomicInteger(0);
		this.numOfRounds = 0;
		this.protocolRunning = new AtomicBoolean(false);
		this.currentRound = 0;
		executor = Executors.newFixedThreadPool(1);
		executor.execute(new MessageReceiver());
		query = new ConcurrentHashMap<String, Double>();
		currentQuery = "";
		numberOfInLinks = 0;
	}
	
	public double estimateSize(boolean initiator, double mixTime) {
		double estimation = 0;
		this.protocolRunning.set(true);
		this.mixTime = new AtomicInteger((int)Math.ceil(mixTime));
		this.numOfRounds = this.mixTime.get()+(int)Math.ceil(Math.log(1/ERROR))+
				(int)Math.ceil(Math.log(1/(1-PROBABILITY)));
		Random r = new Random();
		if (query.get(currentQuery) != null)
			return Double.POSITIVE_INFINITY;
		linksInRound = new int[numOfRounds];
		this.currentRound = 0;
		values = new AtomicDoubleArray(numOfRounds+1);
		weights = new AtomicDoubleArray(numOfRounds+1);
		receivedValues = new AtomicDoubleArray(numOfRounds+1);
		receivedWeights = new AtomicDoubleArray(numOfRounds+1);
		
		int initValue = r.nextInt(1001);
		
		try {
			Socket s = new Socket(InetAddress.getByName("130.43.171.48"),45678);
			PrintWriter connectionInputter = new PrintWriter(new OutputStreamWriter(s.getOutputStream()) );
			connectionInputter.write(new Integer(initValue).toString()+"\n");
			connectionInputter.flush();
			connectionInputter.close();
			s.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
		
		receivedValues.set(currentRound, initValue);
		receivedWeights.set(currentRound, 1);
		
		//Notify the rest of the nodes
		Set<Neighbor> neighbors = localNode.getOutNeighbors();
		
		if(initiator) {
			currentQuery = UUID.randomUUID().toString();
		}
		
		for (Neighbor n : neighbors) {
			AppMessage m = AppMessage.newBuilder()
							.setType(MessageType.NEW)
							.setQueryId(currentQuery)
							.setMixTime(mixTime)
							.build();
			
			MessageContainer mc = new MessageContainer(m, n.getAddress());
			//Put to sending queue
			sendMessage(mc);
		}
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
		}
		
		for (int i = 0; i<numOfRounds; i++) {
			synchronized (linksInRound) {
				linksInRound[i] += numberOfInLinks;
				
			}
		}
		//For i=1 to mixTime
		//Wait to receive values
		//Get the values add them and proceed to the next round
		
		for (int round = 1; round < numOfRounds; round++) {
			weights.set(round, receivedWeights.get(round-1));
			values.set(round, receivedValues.get(round-1));
			estimation = values.get(round)/weights.get(round);			
			
			float percentComplete = ((float)round/numOfRounds)*100;
			System.out.print("\rSampling at "+(int)percentComplete+"%...Current estimation: "+estimation);
			
			double currentRecWeight = receivedWeights.get(round);
			double currentRecVals = receivedValues.get(round);
			receivedWeights.set(round, weights.get(round)*0.5+currentRecWeight);
			receivedValues.set(round, values.get(round)*0.5+currentRecVals);

			AppMessage m = AppMessage.newBuilder()
					.setType(MessageType.VAL)
					.setQueryId(currentQuery)
					.setRound(round)
					.setWeight(weights.get(round)*0.5)
					.setValue(values.get(round)*0.5)
					.build();
			
			neighbors = localNode.getOutNeighbors();
			Neighbor [] n = neighbors.toArray(new Neighbor[0]);
			int size = n.length;
			int item = r.nextInt(size); 
			Neighbor selected = null;
			for (int i = 0; i < size; i++) {
				if (i == item) {
					selected = n[i];
				} else {
					AppMessage voidM = AppMessage.newBuilder()
							.setType(MessageType.VOID)
							.setQueryId(currentQuery)
							.setRound(round)
							.build();
					MessageContainer mc = new MessageContainer(voidM,n[i].getAddress());
					sendMessage(mc);
				}
			}
			
			MessageContainer mc = new MessageContainer(m,selected.getAddress());
			sendMessage(mc);
			synchronized (linksInRound) {
				if(linksInRound[round] != 0 ) {
					try {
						linksInRound.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
					}
				}
			}
		}

		estimation = values.get(numOfRounds-1)/weights.get(numOfRounds-1);
		query.put(currentQuery, estimation);
		currentQuery = "";
		numberOfInLinks = 0;
		protocolRunning.set(false);
		System.out.println("\r");
		return estimation;
	}
	
	private void sendMessage(MessageContainer mc) {
		boolean notSent = true;
		while(notSent) {
			try {
				Socket s = new Socket(mc.getAddress(), PUSH_SUM_PORT);
				mc.getMessage().writeTo(s.getOutputStream());
				s.close();
				notSent = false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
			}
		}
		
	}
	
	class MessageReceiver implements Runnable {

		private ServerSocket ss;
		Socket incomingSocket;
		private ExecutorService protocolExec = Executors.newFixedThreadPool(1);

		
		@Override
		public void run() {
			try {
				ss = new ServerSocket(PUSH_SUM_PORT);
				while (true) {
					Socket incomingSocket = ss.accept();
					AppMessage pm = AppMessage.parseFrom(
							incomingSocket.getInputStream());
					if (pm.getType() == MessageType.NEW) {
						numberOfInLinks++;
						if (!protocolRunning.get()) {
							protocolRunning.set(true);
							currentQuery = pm.getQueryId();
							final double mixTime = pm.getMixTime();
							Thread t = new Thread() {
								public void run()
								{
									estimateSize(false, mixTime);
								}
							};
							protocolExec.execute(t);
						}
					} else if (pm.getType() == MessageType.VAL){
						double weight = pm.getWeight();
						double value = pm.getValue();
						int round = pm.getRound();
						double newWeight = receivedWeights.get(round);
						double newValue = receivedValues.get(round);
						synchronized (linksInRound) {
							linksInRound[round]-=1;
							if(linksInRound[round] == 0)
								linksInRound.notify();
						}
						receivedWeights.set(round, weight+newWeight);
						receivedValues.set(round, value+newValue);
					} else {
						int round = pm.getRound();
						synchronized (linksInRound) {
							linksInRound[round]-=1;
							if(linksInRound[round] == 0)
								linksInRound.notify();
						}
					}
					incomingSocket.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// TODO Must fix this to locate the exact case of the exception
			}

			
		}
		
	}
	
	class MessageContainer {
		private InetAddress address;
		private AppMessage message;
		
		public MessageContainer(AppMessage m, InetAddress a) {
			this.setMessage(m);
			this.setAddress(a);
		}

		public InetAddress getAddress() {
			return address;
		}

		public void setAddress(InetAddress address) {
			this.address = address;
		}

		public AppMessage getMessage() {
			return message;
		}

		public void setMessage(AppMessage message) {
			this.message = message;
		}
	}

}
