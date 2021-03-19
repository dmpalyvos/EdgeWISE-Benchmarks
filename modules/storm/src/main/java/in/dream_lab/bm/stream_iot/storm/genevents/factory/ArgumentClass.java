package in.dream_lab.bm.stream_iot.storm.genevents.factory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tarun on 28/5/15.
 */
public class ArgumentClass{
    String deploymentMode;  //Local ('L') or Distributed-cluster ('C') Mode
    String topoName;
    String inputDatasetPathName; // Full path along with File Name
    String experiRunId;
    double scalingFactor;  //Deceleration factor with respect to seconds.
    String outputDirName;  //Path where the output log file from spout and sink has to be kept
    String tasksPropertiesFilename;
    String tasksName;
    int inputRate;
    long numEvents;
    List<Integer> boltInstances;
    int numWorkers;
    int bucket_time;
    String statisticsFolder;

    public String getTasksName() {
		return tasksName;
	}

	public void setTasksName(String tasksName) {
		this.tasksName = tasksName;
	}

	public String getOutputDirName() {
        return outputDirName;
    }

    public void setOutputDirName(String outputDirName) {
        this.outputDirName = outputDirName;
    }

    public String getDeploymentMode() {
        return deploymentMode;
    }

    public void setDeploymentMode(String deploymentMode) {
        this.deploymentMode = deploymentMode;
    }

    public String getTopoName() {
        return topoName;
    }

    public void setTopoName(String topoName) {
        this.topoName = topoName;
    }

    public String getInputDatasetPathName() {
        return inputDatasetPathName;
    }

    public void setInputDatasetPathName(String inputDatasetPathName) {
        this.inputDatasetPathName = inputDatasetPathName;
    }

    public String getExperiRunId() {
        return experiRunId;
    }

    public void setExperiRunId(String experiRunId) {
        this.experiRunId = experiRunId;
    }

    public double getScalingFactor() {
        return scalingFactor;
    }

    public void setScalingFactor(double scalingFactor) {
        this.scalingFactor = scalingFactor;
    }
    
    public int getInputRate() {
        return inputRate;
    }

    public void setInputRate(int inputRate) {
        this.inputRate = inputRate;
    }
    
    public long getNumEvents() {
        return numEvents;
    }

    public void setNumEvents(long numEvents) {
        this.numEvents = numEvents;
    }

    public long getBucketTime() {
        return bucket_time;
    }

    public void setBucketTime(int _bucket_time) {
        this.bucket_time = _bucket_time;
    }

    public String getTasksPropertiesFilename() {
        return tasksPropertiesFilename;
    }

    public void setTasksPropertiesFilename(String tasksPropertiesFilename) {
        this.tasksPropertiesFilename = tasksPropertiesFilename;
    }
    
	public int getNumWorkers() {
		return numWorkers;
	}

	public void setNumWorkers(int numWorkers) {
		this.numWorkers = numWorkers;
	}


    public void setBoltInstances(String boltInstanceList) {
    	String[] list = boltInstanceList.split(",");
    	boltInstances = new ArrayList<Integer>();
    	for (String bolt : list) {
    		boltInstances.add(Integer.parseInt(bolt));
    	}
    }

    public List<Integer> getBoltInstances() {
    	return boltInstances;
    }

    public String getStatisticsFolder() {
        return statisticsFolder;
    }

    public void setStatisticsFolder(String statisticsFolder) {
        this.statisticsFolder = statisticsFolder;
    }
}
