package in.dream_lab.bm.stream_iot.storm.bolts.ETL.TAXI;

import in.dream_lab.bm.stream_iot.tasks.AbstractTask;
import in.dream_lab.bm.stream_iot.tasks.io.MQTTPublishTask;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Properties;
import org.apache.storm.Config;
import org.apache.storm.metric.api.MeanReducer;
import org.apache.storm.metric.api.ReducedMetric;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;

public class MQTTPublishBolt extends BaseRichBolt {
	private static Logger LOG = LoggerFactory.getLogger("APP");
	private transient ReducedMetric reducedMetric;
	private transient ReducedMetric reducedMetricExt;
	private int sampleCount = 0;
	private int sampleRate;
    private Properties p;

    // ############## added by Gabriele Mencagli ############## //
	long received = 0;
	long start_time;
	long last_time;
	long latencyCount = 0;
	long sumLatencyValues = 0;
	Vector<Long> latencies = new Vector();
	// ######################################################## //

    public MQTTPublishBolt(Properties p_) {
         p = p_;
    }

    OutputCollector collector;
    private static Logger l;
    public static void initLogger(Logger l_) { l = l_; }
    MQTTPublishTask mqttPublishTask;

    @Override
    public void prepare(Map config, TopologyContext context, OutputCollector outputCollector) {
        this.collector = outputCollector;
        initLogger(LoggerFactory.getLogger("APP"));
        mqttPublishTask = new MQTTPublishTask();

        // mqttPublishTask.setup(l,p); // <-- disabled by Gabriele Mencagli

        System.out.println("TOPOLOGY_BUILTIN_METRICS_BUCKET_SIZE_SECS = " + config.get(Config.TOPOLOGY_BUILTIN_METRICS_BUCKET_SIZE_SECS));
		Long builtinPeriod = (Long) config.get(Config.TOPOLOGY_BUILTIN_METRICS_BUCKET_SIZE_SECS);
        reducedMetric = new ReducedMetric(new MeanReducer());
        reducedMetricExt = new ReducedMetric(new MeanReducer());
        context.registerMetric("total-latency", reducedMetric, builtinPeriod.intValue());
        context.registerMetric("total-latency-ext", reducedMetricExt, builtinPeriod.intValue());
        sampleRate = (int) (1 / (double) config.get(Config.TOPOLOGY_STATS_SAMPLE_RATE));
    }

    @Override
    public void execute(Tuple input) {
    	String msgId = (String)input.getValueByField("MSGID");
    	String meta = (String)input.getValueByField("META");
    	String obsType = (String)input.getValueByField("OBSTYPE");
    	String obsVal = (String)input.getValueByField("OBSVAL");
    	HashMap<String, String> map = new HashMap();
        map.put(AbstractTask.DEFAULT_KEY, obsVal);
        //LOG.info("[SINK] sink received");
        // ############## added by Gabriele Mencagli ############## //
        received++;
		// set the starting time
        if (received == 1) {
            start_time = System.nanoTime();
            last_time = start_time;
        }
        else {
        	last_time = System.nanoTime();
        }
        // ######################################################## //

    	// Float res = mqttPublishTask.doTask(map); // <-- disabled by Gabriele Mencagli

    	//long time = System.currentTimeMillis();
    	/*
    	Values values = new Values(msgId, meta, obsType, obsVal);

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
    		long timestamp_ext = (long) input.getValueByField("TIMESTAMP_EXT");
    		if (spoutTimestamp > 0) {
    			reducedMetric.update(System.currentTimeMillis() - spoutTimestamp);
    			reducedMetricExt.update(System.currentTimeMillis() - timestamp_ext);
    			// ############## added by Gabriele Mencagli ############## //
    			long latency = System.currentTimeMillis() - spoutTimestamp.longValue();
					sumLatencyValues += latency;
    			latencyCount++;
    			latencies.addElement(new Long(latency));
    			// ######################################################## //
    		}
    	}
    	sampleCount++;
    	if (sampleCount == sampleRate) {
    		sampleCount = 0;
    	}
    }

    @Override
    public void cleanup() {
    	// mqttPublishTask.tearDown(); // <-- disabled by Gabriele Mencagli

		// ############## added by Gabriele Mencagli ############## //
 		double rate = received / ((last_time - start_time) / 1e9); // per second
		long t_elapsed = (long) ((last_time - start_time) / 1e6);  // elapsed time in milliseconds
		LOG.info("[SINK] Measured sink throughput: " + (int) rate + " tuples/second");
		LOG.info("[SINK] Measured mean latency (ms): " + ((double) sumLatencyValues) / latencyCount);
		try {
			File file = new File("/home/mencagli/latencies.txt");
			file.createNewFile();
			FileWriter writer = new FileWriter(file);
			for (int i=0; i<latencies.size(); i++) {
				writer.write(i + "\t" + latencies.get(i) + "\n");
			}
			writer.flush();
      		writer.close();
      		LOG.info("[SINK] Latency file printed");
		}
		catch(IOException e) {}
		// ######################################################## //
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        // outputFieldsDeclarer.declare(new Fields("MSGID", "META", "OBSTYPE", "OBSVAL", "TIMESTAMP", "SPOUTTIMESTAMP"));
    }
}
