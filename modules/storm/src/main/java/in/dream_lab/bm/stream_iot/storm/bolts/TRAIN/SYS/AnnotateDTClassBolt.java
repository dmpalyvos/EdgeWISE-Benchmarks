package in.dream_lab.bm.stream_iot.storm.bolts.TRAIN.SYS;

import in.dream_lab.bm.stream_iot.tasks.AbstractTask;
import in.dream_lab.bm.stream_iot.tasks.annotate.AnnotateDTClass;
import in.dream_lab.bm.stream_iot.tasks.io.MQTTPublishTask;
import org.apache.storm.shade.com.google.common.base.Stopwatch;
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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class AnnotateDTClassBolt extends BaseRichBolt {

	private Properties p;

	public AnnotateDTClassBolt(Properties p_) {
		p = p_;

	}

	OutputCollector collector;
	private static Logger l;

	public static void initLogger(Logger l_) {
		l = l_;
	}

	AnnotateDTClass annotateDTClass;

	@Override
	public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {

		this.collector = outputCollector;
		initLogger(LoggerFactory.getLogger("APP"));

		annotateDTClass = new AnnotateDTClass();

		annotateDTClass.setup(l, p);
	}

	@Override
	public void execute(Tuple input) {
		String msgId = (String) input.getValueByField("MSGID");
		String data = input.getStringByField("TRAINDATA");
		String rowkeyend = input.getStringByField("ROWKEYEND");

		// System.out.println(this.getClass().getName() + " - GOT - msgId: " +
		// msgId + " rowKeyEnd: " + rowkeyend
		// + "\ndata: \n" + data);

		HashMap<String, String> map = new HashMap<String, String>();
		map.put(AbstractTask.DEFAULT_KEY, data);

		// Stopwatch stopwatch = Stopwatch.createStarted(); //

		annotateDTClass.doTask(map);

		// stopwatch.stop(); // optional
		// System.out.println("Time elapsed for annotateDTClass() is "+
		// stopwatch.elapsed(MILLISECONDS)); //

		String annotData = annotateDTClass.getLastResult();

		Values values = new Values(msgId, annotData, rowkeyend);
		//System.out.println(this.getClass().getName() + " - EMITS - " + values.toString());
		
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
		
		collector.emit(values);
	}

	@Override
	public void cleanup() {
		annotateDTClass.tearDown();
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
		outputFieldsDeclarer.declare(new Fields("MSGID", "ANNOTDATA", "ROWKEYEND", "TIMESTAMP", "SPOUTTIMESTAMP", "CHAINSTAMP"));
	}

}
