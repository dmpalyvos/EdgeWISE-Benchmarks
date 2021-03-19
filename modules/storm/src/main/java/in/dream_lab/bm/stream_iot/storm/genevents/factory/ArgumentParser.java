package in.dream_lab.bm.stream_iot.storm.genevents.factory;

import java.net.InetAddress;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * Created by tarun on 28/5/15.
 */
public class ArgumentParser {

    /*
    Convention is:
    Command Meaning: topology-fully-qualified-name <local-or-cluster> <Topo-name> <input-dataset-path-name> <Experi-Run-id> <scaling-factor>
    Example command: SampleTopology L NA /var/tmp/bangalore.csv E01-01 0.001
     */
    public static ArgumentClass parserCLI(String [] args)
    {

    	if(args == null || !(args.length == 15 || args.length == 16)){
            System.out.println("invalid number of arguments");
            return null;
        }
    	// L
      // {topology_name} {dataset_filename} 1 1 {topology_output_dir}
      // {query_properties} {topology_name} {stream_length} {num_workers} 1 {variant_args} {extra_args}"
        else {
            ArgumentClass argumentClass = new ArgumentClass();
            argumentClass.setDeploymentMode(args[0]);
            argumentClass.setTopoName(args[1]);
            argumentClass.setInputDatasetPathName(args[2]);
            argumentClass.setExperiRunId(args[3]);
            argumentClass.setScalingFactor(Double.parseDouble(args[4]));
            argumentClass.setOutputDirName(args[5]);
            argumentClass.setTasksPropertiesFilename(args[6]);
            argumentClass.setTasksName(args[7]);
            argumentClass.setNumEvents(Long.parseLong(args[8]));
            argumentClass.setNumWorkers(Integer.parseInt(args[9]));
            argumentClass.setBucketTime(Integer.parseInt(args[10]));
            if (args.length == 14) {
                argumentClass.setBoltInstances(args[11]);
            }
            // Command must always end with --rate <rate_value>
            if (!"--rate".equals(args[args.length-4])) {
                System.out.println("Invalid CLI arguments: Before-last argument should be --rate <rate_value>");
                return null;
            }
          argumentClass.setInputRate(Integer.valueOf(args[args.length-3]));
          System.out.println("Rate = " + argumentClass.getInputRate());
          if (!"--statisticsFolder".equals(args[args.length-2])) {
              System.out.println("Invalid CLI arguments: Before-last argument should be --statisticsFolder <statistics_folder>");
              return null;
          }
          argumentClass.setStatisticsFolder(args[args.length-1]);
            System.out.println("Statistics Folder = " + argumentClass.getStatisticsFolder());
            Configurator.setRootLevel(Level.ERROR);
            return argumentClass;
        }
    }

    public static void main(String [] args){
        try {
        }catch(Exception e){
            e.printStackTrace();
        }
        ArgumentClass argumentClass = parserCLI(args);
        if(argumentClass == null){
            System.out.println("Improper Arguments");
        }
        else{
            System.out.println(argumentClass.getDeploymentMode() +" : " + argumentClass.getExperiRunId() + ":" + argumentClass.getScalingFactor());
        }
    }
}