package in.dream_lab.bm.stream_iot.storm.sinks;

import org.apache.storm.tuple.Tuple;

public class IoTPredictionTopologySinkBolt extends Sink {
	
	public IoTPredictionTopologySinkBolt(String csvFileNameOutSink) {
		super(csvFileNameOutSink);
	}

	@Override
	public void execute(Tuple input) {
		String msgId = input.getStringByField("MSGID");
		String analyticType = input.getStringByField("ANALYTICTYPE");
		String identifier = msgId + "," + analyticType;

		try {
			getBa().batchLogwriter(System.currentTimeMillis(), identifier);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
