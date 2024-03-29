package in.dream_lab.bm.stream_iot.storm.bolts.IoTPredictionBolts.SYS;

import in.dream_lab.bm.stream_iot.tasks.AbstractTask;
import in.dream_lab.bm.stream_iot.tasks.io.MQTTPublishTask;

import org.apache.storm.Config;
import org.apache.storm.metric.api.MeanReducer;
import org.apache.storm.metric.api.ReducedMetric;
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

public class MQTTPublishBolt extends BaseRichBolt {
	
	private transient ReducedMetric reducedMetric;
	private int sampleCount = 0;
	private int sampleRate;
	
	private Properties p;

	public MQTTPublishBolt(Properties p_) {
		p = p_;

	}

	OutputCollector collector;
	private static Logger l;

	public static void initLogger(Logger l_) {
		l = l_;
	}

	MQTTPublishTask mqttPublishTask;

	@Override
	public void prepare(Map config, TopologyContext context, OutputCollector outputCollector) {

		this.collector = outputCollector;
		initLogger(LoggerFactory.getLogger("APP"));

		mqttPublishTask = new MQTTPublishTask();

		mqttPublishTask.setup(l, p);
		
		System.out.println("TOPOLOGY_BUILTIN_METRICS_BUCKET_SIZE_SECS = " + config.get(Config.TOPOLOGY_BUILTIN_METRICS_BUCKET_SIZE_SECS));
		Long builtinPeriod = (Long) config.get(Config.TOPOLOGY_BUILTIN_METRICS_BUCKET_SIZE_SECS);
        reducedMetric= new ReducedMetric(new MeanReducer());
        context.registerMetric("total-latency", reducedMetric, builtinPeriod.intValue());
        
        sampleRate =(int) (1 / (double) config.get(Config.TOPOLOGY_STATS_SAMPLE_RATE));
	}

	@Override
	public void execute(Tuple input) {
		String msgId = (String) input.getValueByField("MSGID");
		String meta = (String) input.getValueByField("META");
		// String obsType = (String)input.getValueByField("OBSTYPE");
		String analyticsType = input.getStringByField("ANALAYTICTYPE");
		String obsVal = input.getStringByField("OBSVAL");

		StringBuffer temp = new StringBuffer();
		String res = "";
		if (analyticsType.equals("DTC")) {
			res = (String) input.getValueByField("RES");
			temp.append(msgId).append(",").append(meta).append(",").append(analyticsType).append(",obsVal:")
					.append(obsVal).append(",RES:").append(res);
		}
		if (analyticsType.equals("MLR")) {
			res = (input.getValueByField("ERROR")).toString();
			temp.append(msgId).append(",").append(meta).append(",").append(analyticsType).append(",obsVal:")
					.append(obsVal).append(",ERROR:").append(res);
		}

/*		if (l.isInfoEnabled())
			l.info("MQTT result:{}", temp);*/

		HashMap<String, String> map = new HashMap();
		map.put(AbstractTask.DEFAULT_KEY, String.valueOf(temp));
		mqttPublishTask.doTask(map);
		
		Values values = new Values(msgId, meta,analyticsType, obsVal);
		//System.out.println(this.getClass().getName() + " - EMITS - " + values.toString());
		
		/*
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
		
		collector.emit(values);
		*/
		if (sampleCount == 0) {
    		Long spoutTimestamp = input.getLongByField("SPOUTTIMESTAMP");
    		if (spoutTimestamp > 0) {
    			reducedMetric.update(System.currentTimeMillis() - spoutTimestamp);
    		}
    	}
    	
    	sampleCount++;
    	if (sampleCount == sampleRate) {
    		sampleCount = 0;
    	}
	}

	@Override
	public void cleanup() {
		mqttPublishTask.tearDown();
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
		// outputFieldsDeclarer.declare(new Fields("MSGID", "META", "ANALYTICTYPE" ,"OBSVAL", "TIMESTAMP", "SPOUTTIMESTAMP"));
	}

}
