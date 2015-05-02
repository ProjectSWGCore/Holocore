package resources.control;

import intents.ServerStatusIntent;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import resources.control.Service;

/**
 * Might be best to keep this protected, from a server security standpoint
 */
final class ServerManager implements IntentReceiver {
	
	private static final ServerManager INSTANCE = new ServerManager();
	
	private final Map <String, ServiceStats> serviceStats;
	private ServerStatus status;
	
	private ServerManager() {
		serviceStats = new HashMap<String, ServiceStats>();
		status = ServerStatus.OFFLINE;
		ServerPublicInterface.initialize(this);
		IntentManager.getInstance().registerForIntent(ServerStatusIntent.TYPE, this);
	}
	
	public static final ServerManager getInstance() {
		return INSTANCE;
	}
	
	public void onIntentReceived(Intent i) {
		if (i instanceof ServerStatusIntent)
			processServerStatusIntent((ServerStatusIntent) i);
	}
	
	public boolean initialize() {
		ServerPublicInterface.initialize(this);
		return true;
	}
	
	public boolean terminate() {
		ServerPublicInterface.terminate();
		return true;
	}
	
	public void addChild(Service parent, Service child) {
		getServiceStats(parent).addChild(getServiceStats(child));
	}
	
	public void removeChild(Service parent, Service child) {
		getServiceStats(parent).removeChild(getServiceStats(child));
	}
	
	public void setServiceInitTime(Service service, double timeMilliseconds, boolean success) {
		getServiceStats(service).addInitTime(timeMilliseconds, success);
	}
	
	public void setServiceStartTime(Service service, double timeMilliseconds, boolean success) {
		getServiceStats(service).addStartTime(timeMilliseconds, success);
	}
	
	public void setServiceTerminateTime(Service service, double timeMilliseconds, boolean success) {
		getServiceStats(service).addTerminateTime(timeMilliseconds, success);
	}
	
	private void processServerStatusIntent(ServerStatusIntent i) {
		status = i.getStatus();
	}
	
	public byte [] serializeControlTimes() {
		synchronized (serviceStats) {
			int size = 0;
			for (ServiceStats stats : serviceStats.values())
				size += stats.getSerializeSize();
			ByteBuffer data = ByteBuffer.allocate(size);
			for (ServiceStats stats : serviceStats.values())
				data.put(stats.serialize());
			return data.array();
		}
	}
	
	public ServerStatus getServerStatus() {
		return status;
	}
	
	private ServiceStats getServiceStats(Service service) {
		synchronized (serviceStats) {
			ServiceStats stats = serviceStats.get(service.getClass().getName());
			if (stats == null) {
				stats = new ServiceStats(service);
				serviceStats.put(service.getClass().getName(), stats);
			}
			return stats;
		}
	}
	
	private static class ServiceStats {
		private final Service service;
		private final List <ServiceControlTime> initTimes;
		private final List <ServiceControlTime> startTimes;
		private final List <ServiceControlTime> terminateTimes;
		private final List <ServiceStats> children;
		
		public ServiceStats(Service service) {
			this.service = service;
			initTimes = new ArrayList<ServiceControlTime>();
			startTimes = new ArrayList<ServiceControlTime>();
			terminateTimes = new ArrayList<ServiceControlTime>();
			children = new ArrayList<ServiceStats>();
		}
		
		public void addChild(ServiceStats service) {
			children.add(service);
		}
		
		public void removeChild(ServiceStats service) {
			children.remove(service);
		}
		
		public void addInitTime(double time, boolean success) {
			initTimes.add(new ServiceControlTime(time, success));
		}
		
		public void addStartTime(double time, boolean success) {
			startTimes.add(new ServiceControlTime(time, success));
		}
		
		public void addTerminateTime(double time, boolean success) {
			terminateTimes.add(new ServiceControlTime(time, success));
		}
		
		public int getSerializeSize() {
			return 2 + service.getClass().getName().length() + 9*3;
		}
		
		public byte [] serialize() {
			String name = service.getClass().getName();
			ByteBuffer data = ByteBuffer.allocate(getSerializeSize());
			data.putShort((short) name.length());
			data.put(name.getBytes(Charset.forName("UTF-8")));
			serializeLastControlTime(data, initTimes);
			serializeLastControlTime(data, startTimes);
			serializeLastControlTime(data, terminateTimes);
			return data.array();
		}
		
		private void serializeLastControlTime(ByteBuffer data, List <ServiceControlTime> times) {
			if (initTimes.size() > times.size() || times.size() == 0)
				serializeControlTime(data, null);
			else
				serializeControlTime(data, times.get(initTimes.size()-1));
		}
		
		private void serializeControlTime(ByteBuffer data, ServiceControlTime time) {
			if (time == null) {
				data.put((byte) 0);
				data.putDouble(Double.NaN);
			} else {
				data.put((byte) (time.isSuccess() ? 1 : 0));
				data.putDouble(time.getTime());
			}
		}
		
		public String toString() {
			return "ServiceStats[" + service.getClass().getName() + "]";
		}
	}
	
	private static class ServiceControlTime {
		private final double time;
		private final boolean success;
		
		public ServiceControlTime(double time, boolean success) {
			this.time = time;
			this.success = success;
		}
		
		public double getTime() {
			return time;
		}
		
		public boolean isSuccess() {
			return success;
		}
		
		public String toString() {
			return "[" + (isSuccess()?"Success":"Failure") + " in " + getTime() + "ms]";
		}
	}
	
}
