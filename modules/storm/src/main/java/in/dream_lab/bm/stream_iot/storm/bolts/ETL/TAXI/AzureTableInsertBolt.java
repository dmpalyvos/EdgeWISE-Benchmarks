package in.dream_lab.bm.stream_iot.storm.bolts.ETL.TAXI;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import in.dream_lab.bm.stream_iot.tasks.io.AzureTableBatchInsert;

public class AzureTableInsertBolt  extends BaseRichBolt {

    private Properties p;

    public AzureTableInsertBolt(Properties p_)
    {
         p=p_;

    }
    OutputCollector collector;
    private static Logger l; 
    public static void initLogger(Logger l_) {     l = l_; }
    AzureTableBatchInsert azureTableInsertTask; 
    private HashMap<String, String> tuplesMap ;
    private int insertBatchSize ;
    private String batchFirstMsgId;
    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {

        this.collector=outputCollector;
        initLogger(LoggerFactory.getLogger("APP"));

        azureTableInsertTask= new AzureTableBatchInsert();

        azureTableInsertTask.setup(l,p);
        tuplesMap = new HashMap<String, String>();
        insertBatchSize = Integer.parseInt(p.getProperty("IO.AZURE_TABLE.INSERTBATCHSIZE", "100"));
    }

    @Override
    public void execute(Tuple input) 
    {
    	String msgId = (String)input.getValueByField("MSGID");
    	String meta = (String)input.getValueByField("META");
    	String obsType = (String)input.getValueByField("OBSTYPE");
    	String obsVal = (String)input.getValueByField("OBSVAL");
    	String val = obsVal + "," + msgId;
    	int count = tuplesMap.size();
    	
    	if(count == 0 )
    		batchFirstMsgId = msgId;
    	tuplesMap.put(String.valueOf(count), obsVal);
    	if(tuplesMap.size() >= insertBatchSize )
    	{
    		//System.out.println(this.getClass().getName() + " - " + Thread.currentThread().getId() + "-"+Thread.currentThread().getName());
    		Float res = azureTableInsertTask.doTask(tuplesMap);
    		tuplesMap = new HashMap<>();
    	 	//collector.emit(new Values(batchFirstMsgId, meta, obsType, (String)input.getValueByField("OBSVAL")));
    	}
    	//long time = System.currentTimeMillis();
    	
    	Values values = new Values(msgId, meta, obsType, obsVal);
    	
    	if (input.getLongByField("TIMESTAMP") > 0) {
			values.add(System.currentTimeMillis());
		} else {
			values.add(-1L);
		}
    	
    	collector.emit(values);
    	
    }

    @Override
    public void cleanup() {
    	azureTableInsertTask.tearDown();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
    	outputFieldsDeclarer.declare(new Fields("MSGID", "META", "OBSTYPE", "OBSVAL", "TIMESTAMP"));
    }
}
