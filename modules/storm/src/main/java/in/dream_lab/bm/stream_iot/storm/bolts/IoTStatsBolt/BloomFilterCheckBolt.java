package in.dream_lab.bm.stream_iot.storm.bolts.IoTStatsBolt;

import in.dream_lab.bm.stream_iot.tasks.AbstractTask;
import in.dream_lab.bm.stream_iot.tasks.filter.BloomFilterCheck;
import in.dream_lab.bm.stream_iot.tasks.filter.MultipleBloomFilterCheck;

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

public class BloomFilterCheckBolt extends BaseRichBolt {
	// static fields common to all threads
    private static Logger l;
    static {
    	l = LoggerFactory.getLogger("APP");
    }

	// local fields assigned to each thread
    private OutputCollector collector; 
	private Properties p;

    // BloomFilterCheck bloomFilterCheck;
    MultipleBloomFilterCheck bloomFilterCheck;


    public BloomFilterCheckBolt(Properties p_){
        p = p_;
    }
    
    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {

        this.collector=outputCollector;
        
        // bloomFilterCheck=new BloomFilterCheck();
        bloomFilterCheck = new MultipleBloomFilterCheck();
        bloomFilterCheck.setup(l,p);
    }


    @Override
    public void execute(Tuple input) {

        String msgId = input.getStringByField("MSGID");
        String sensorMeta=input.getStringByField("sensorMeta");
        String sensorID=input.getStringByField("sensorID");
        String obsVal=input.getStringByField("obsVal");
        String obsType=input.getStringByField("obsType");
        
        HashMap<String, String> map = new HashMap();
        // map.put(AbstractTask.DEFAULT_KEY, obsVal);
        map.put(obsType, obsVal);
        Float res = bloomFilterCheck.doTask(map); // obsval change



        if(res!=null ) {
            if(res!=Float.MIN_VALUE) {

                // if(l.isInfoEnabled())
                // l.info("res from bloom-"+res);

                if(res==1) {
                	Values values = new Values(sensorMeta,sensorID,obsType,obsVal,msgId);
                	
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
                }
                    
            }
            else {
                // if (l.isWarnEnabled()) l.warn("Error in BloomFilterCheckBolt");
                throw new RuntimeException();
            }
        }
    }

    @Override
    public void cleanup() {
        bloomFilterCheck.tearDown();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("sensorMeta","sensorID","obsType","obsVal","MSGID", "TIMESTAMP", "SPOUTTIMESTAMP", "CHAINSTAMP", "TIMESTAMP_EXT")); // obsType = {temp, humid, airq, light, dust}

    }

}