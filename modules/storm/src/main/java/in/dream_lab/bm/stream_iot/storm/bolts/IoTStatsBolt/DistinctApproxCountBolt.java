package in.dream_lab.bm.stream_iot.storm.bolts.IoTStatsBolt;

import in.dream_lab.bm.stream_iot.tasks.AbstractTask;
import in.dream_lab.bm.stream_iot.tasks.aggregate.DistinctApproxCount;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DistinctApproxCountBolt extends BaseRichBolt {

	private Properties p;

	public DistinctApproxCountBolt(Properties p_) {
		p = p_;
	}

	OutputCollector collector;
	private static Logger l;

	public static void initLogger(Logger l_) {
		l = l_;
	}

	// DistinctApproxCount distinctApproxCount;
	Map<String, DistinctApproxCount> distinctMap;
	String useMsgField;
	// Map<String, DistinctApproxCount> distinctApproxCountMap ;

	@Override
	public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {

		this.collector = outputCollector;
		initLogger(LoggerFactory.getLogger("APP"));
		this.useMsgField = p.getProperty("AGGREGATE.DISTINCT_APPROX_COUNT.USE_MSG_FIELD");
		// distinctApproxCountMap = new HashMap<String, DistinctApproxCount>();
		//System.out.println("use msg  " + useMsgField);
		// distinctApproxCount = new DistinctApproxCount();
		// distinctApproxCount.setup(l, p);
		
		distinctMap = new HashMap<String, DistinctApproxCount>();
	}

	@Override
	public void execute(Tuple input) {

		String msgId = input.getStringByField("MSGID");
		String sensorMeta = input.getStringByField("sensorMeta");
		String sensorID = input.getStringByField("sensorID");
		String obsType = input.getStringByField("obsType");

		HashMap<String, String> map = new HashMap();
		map.put(AbstractTask.DEFAULT_KEY, sensorID);

		if (!distinctMap.containsKey(obsType)) {
			DistinctApproxCount distinctApproxCount = new DistinctApproxCount();
			distinctApproxCount.setup(l, p);
			distinctMap.put(obsType, distinctApproxCount);
		}
		
		DistinctApproxCount distinctApproxCount = distinctMap.get(obsType);
		
		Float res = null;
		//if (obsType.equals(useMsgField)) { // useMsgField=source
		distinctApproxCount.doTask(map);
		res = (Float) distinctApproxCount.getLastResult();
		//}

		if (res != null) {
			sensorMeta = sensorMeta.concat(",").concat(obsType);
			obsType = "DA";
			if (res != Float.MIN_VALUE) {
				Values values = new Values(sensorMeta, sensorID, obsType, res.toString(), msgId);
				//System.out.println(this.getClass().getName() + " - EMITS - " + values.toString());
				
				values.add("DAC");
				
				if (input.getLongByField("TIMESTAMP") > 0) {
    				values.add(System.currentTimeMillis());
    			} else {
    				values.add(-1L);
    			}
				
				Long spoutTimestamp = input.getLongByField("SPOUTTIMESTAMP");
    			if (spoutTimestamp > 0) {
    				values.add(spoutTimestamp);
    			} else {
    				values.add(-1L);
    			}
				
    			values.add(input.getLongByField("CHAINSTAMP"));
    			values.add(input.getValueByField("TIMESTAMP_EXT"));
				collector.emit(values);
			} else {
				//if (l.isWarnEnabled())
				//	l.warn("Error in distinct approx");
				throw new RuntimeException();
			}
		}
	}

	@Override
	public void cleanup() {
		// distinctApproxCount.tearDown();
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
		outputFieldsDeclarer.declare(new Fields("sensorMeta", "sensorID", "obsType", "res", "MSGID", "ANALYTICTYPE", "TIMESTAMP", "SPOUTTIMESTAMP", "CHAINSTAMP", "TIMESTAMP_EXT"));
	}

}