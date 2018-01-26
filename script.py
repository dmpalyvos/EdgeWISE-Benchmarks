#################################################################################
#   Generate metrics after topology's execution
#   Execute as:
#   python <script_name.py> <topology_name> /path/to/spout_log /path/to/sink_log 
#   Outputs: Input Rate, Throughput, Average Latency
#################################################################################

import sys
import numpy
from datetime import datetime
import time
import matplotlib
matplotlib.use('Agg')
import csv
import collections
import matplotlib.mlab as mlab
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import re
from scipy.interpolate import spline
import os

########################################
#   Calculations using dataframes.
########################################

# CONSTANTS
topo_to_paths = {
                'etl' : ['AzureInsert', 'PublishBolt'],
                'wordcount' : ['wordcount'],
                'pred' : ['MLR', 'DTC']
                }

spout_names = ["machine", "thread", "source", "timestamp", "MID", "messageId"]
sink_names = ["machine", "thread", "source", "timestamp", "messageId", "MID"]
names_subset = ["timestamp", "messageId"]
sink_names_subset = ["timestamp", "messageId","path"]
spout_cols = [3,5]
sink_cols = [3,4,5]

def plot_inst_rate(df, path_name, xlabel, ylabel, title, fig_name):
    """
    Plots instantaneous rate (input rate or throughput)
    """
    df["timestamp_sec"] = df["timestamp"]//1000
    plt.figure(figsize = (16,9))
    ax = df.groupby("timestamp_sec")["messageId"].count().plot()
    ax.set_xlabel(xlabel,fontsize=20)
    ax.set_ylabel(ylabel, fontsize=20)
    ax.set_title(title, fontsize=20)
    plt.savefig(fig_name)
    plt.close()
    

def plot_latency_hist(df, path, fig_name):
    plt.figure(figsize = (16,9))
    bins=int((max(df[df["path"]==path]["event_latency"]) - min(df[df["path"]==path]["event_latency"]))/10)+1
    ax1 = df[df["path"]==path]["event_latency"].plot(
                                    kind="hist", bins=bins, 
                                    title="Latency Histogram " + path + " path", 
                                    grid=True, fontsize=20)
    ax1.set_xlabel("Latency", fontsize=20)
    plt.savefig(fig_name)
    plt.close()

    
def get_event_rate(series):
    return len(series) / ((series.index[len(series)-1] - series.index[0])/1000)


def process_sink_file(sink_log, topo_name):
    sink_path_dfs = {}
    for _p in topo_to_paths[topo_name]:
        sink_path_dfs.update({_p : sink_log[sink_log["path"]==_p]})
    return sink_path_dfs


def get_Results(topology_name, spout_file, sink_file):
    """
    Processes the spout and sink log files to compute
    input rate, throughputs and latencies for each of 
    the paths in the topology.
    @param topology_name: Name of the topology (format: name_<input_rate>)
    @param spout_file: Spout file generated by the application
    @param sink_file: Sink file generated by the application
    """
    topo_name = topology_name.split('_')[0]
    spout_log = pd.read_csv(spout_file, names=names_subset, usecols=spout_cols, squeeze=True)
    spout_series = pd.Series(spout_log["messageId"].values, index=spout_log.timestamp)

    sink_log = pd.read_csv(sink_file, names=sink_names_subset, usecols=sink_cols)
    sink_series = pd.Series(sink_log["messageId"].values, index=sink_log.timestamp)

    sink_path_dfs = process_sink_file(sink_log, topo_name)
    sink_paths_series = {} # Contains a list of timeseries for each sink path.
    for _path in sink_path_dfs:
        sink_p = sink_path_dfs[_path]
        sink_paths_series.update({_path : pd.Series(sink_p["messageId"].values, index=sink_p.timestamp)})

    # Merge for latency calculation
    # Latency Calculation
    merged = pd.merge(
                    pd.DataFrame(spout_log), 
                    pd.DataFrame(sink_log), 
                    how='inner', 
                    on="messageId", 
                    suffixes=('_spout', '_sink'))
    # per event latency
    merged["event_latency"] = merged["timestamp_sink"] - merged["timestamp_spout"]

    # Input rate
    input_rate = get_event_rate(spout_series) 
    throughputs = [] # Contains throughputs for all paths. Ordering is established by the list in topo_to_paths
    for sink in sink_paths_series:
        series = sink_paths_series[sink]
        throughputs.append(get_event_rate(series))

    # latency for each path
    latencies = []
    for sink in sink_paths_series:
        series = sink_paths_series[sink]
        latencies.append(merged[merged["path"]==sink]["event_latency"].mean())

    #tp_p1 = throughputs[0]
    #tp_p2 = throughputs[1]
    #lat_p1 = latencies[0]
    #lat_p2 = latencies[1]
    # Display Results
    #print ("Input Rate = {:.4f}".format(input_rate))
    #print ("Throughput (AzureInsert)= {:.4f}".format(tp_p1))
    #print ("Throughput (PublishBolt)= {:.4f}".format(tp_p2))
    #print ("Latency (AzureInsert)= {:.4f}".format(lat_p1))
    #print ("Latency (PublishBolt)= {:.4f}".format(lat_p2))

    # Generate Plots
    for _path in topo_to_paths[topo_name]:
        plot_latency_hist(merged, _path, topology_name+"_latency_hist_"+_path)

    pd.options.mode.chained_assignment = None  # default='warn'
    # Input Rate
    plot_inst_rate(spout_log, "", "Time", "Input Rate", "Input Rate", topology_name+"_input_rate")
    # Instantaneous Throughput Plot
    for sink in sink_path_dfs:
        series = sink_path_dfs[sink]
        plot_inst_rate(series, sink, "Time", "Throughput (" + sink + " path)", 
                   "Throughput (" + sink + " path)", topology_name+"_tp_"+sink+"_path")

    return Results(topology_name, input_rate, throughputs, latencies)




def get_rates(_map, interval=500, path=None, topo_name=None):
    """
    @param _map: Ordered dictionary that maps  msg_id to observed timestamp
    @param interval interval for which instantaneous throughput needs to be calculated
    @param path Topo path for which TP is being calculated.
    @param topo_name Name of the topology
    Calculates the input rate/ throughput given the map for spout/sink file.
    """
    items=list(_map.items())
    if len(items)==0:
        return -1
    prev_mid=items[0][0]
    prev_ts=items[0][1]
    inst_tp=[]
    count=1
    for k in _map:
        if count % interval == 0:
            cur_mid=k
            cur_ts=_map[k]
            msgs=long(cur_mid)-long(prev_mid)
            ts=float(long(cur_ts)-long(prev_ts))
            inst_tp.append(float(interval)/(ts/1000))
            prev_mid=cur_mid
            prev_ts=cur_ts
        count+=1

    img_name="inst_rate"
    if topo_name is not None:
        img_name=img_name+"-"+str(topo_name)
    if path is not None:
        img_name=img_name+"-"+str(path)
    
    axes=plt.gca()
    axes.set_xlabel('time/events')
    y_label=""
    if topo_name is not None:
        y_label=y_label+topo_name
    if path is not None:
        y_label=y_label+path

    axes.set_ylabel(y_label+' rate (events/sec)')
    plt.plot(inst_tp)
    plt.savefig(img_name)
    plt.close()
    
    first_mid=items[0][0]
    first_ts=items[0][1]
    last_mid=items[-1][0]
    last_ts=items[-1][1]
    rate = float(long(last_mid)-long(first_mid)) / ((long(last_ts)-long(first_ts))/1000)
    inst_tp=numpy.asarray(inst_tp)
    #print(">10000: " + str(len(inst_tp[inst_tp>10000])))
    #print ("Overal Rate: "+ str(rate))
    return rate



def getInputRate(inFile):
    return getRate(inFile, 3, 5)

def getThroughput(outFile):
    return getRate(outFile, 3, 4)

def get_rate(_file, ts_index, mid_index, path=None, topo_name=None):
    """
    Calculates the input rate or throughput for a topology with
    only one path
    """
    _map= collections.OrderedDict()
    with open(_file, 'rb') as f:
        reader = csv.reader(f, delimiter=',')
        for row in reader:
            _map[row[mid_index]] = row[ts_index]
    return get_rates(_map, 500)


# Don't use this...
def getRate(_file, timestampIndex, msgIdIndex):
    with open(_file, "r") as f:
        first = f.readline().split(",")
        msgs = 0
        for line in f:
            msgs+=1

        last = line.split(",")
        startTime = long(first[timestampIndex])
        endTime = long(last[timestampIndex])
        #startMsg = long(first[msgIdIndex])
        #endMsg = long(last[msgIdIndex])
        #print msgs
        rate = float((msgs)) / ((endTime-startTime)/1000)
    return rate


def get_topo_tp(_file, ts_index, m_index, p1, p2, topo_name=None):
    """
    Calculate throughput for topology with multiple (2 for now) paths
    """
    p1_map=collections.OrderedDict()
    p2_map=collections.OrderedDict()
    with open(_file, 'rb') as f:
        reader = csv.reader(f, delimiter=',')
        for row in reader:
            if p1 in row:
                p1_map[row[m_index]] = row[ts_index]
            else:
                p2_map[row[m_index]] = row[ts_index]
    p1_tp = get_rates(p1_map, 500, p1, topo_name)
    p2_tp = get_rates(p2_map, 500, p2, topo_name)
    return (p1_tp, p2_tp)
    

# Don't use this...
def get_etl_topo_throughput(_file, timestampIndex, msgIdIndex, path1, path2):
    with open(_file, "r") as f:
        firstAz = ""
        firstMq = ""
        lastAz = ""
        lastMq = ""
        azMsgs = 0
        mqMsgs = 0
        for line in f:
            if path1 in line and firstAz == "":
                firstAz = line
            if path2 in line and firstMq == "":
                firstMq = line
            if firstAz != "" and firstMq != "":
                break

        for line in f:
            if path1 in line:
                azMsgs+=1
                lastAz = line
            elif path2 in line:
                mqMsgs+=1
                lastMq = line
    
    firstAz = firstAz.split(",")
    lastAz = lastAz.split(",")
    firstMq = firstMq.split(",")
    lastMq = lastMq.split(",")
    azTime = long(lastAz[timestampIndex]) - long(firstAz[timestampIndex])
    mqTime = long(lastMq[timestampIndex]) - long(firstMq[timestampIndex])
    #azMsgs = long(lastAz[msgIdIndex]) - long(firstAz[msgIdIndex])
    #mqMsgs = long(lastMq[msgIdIndex]) - long(firstMq[msgIdIndex])
    #print lastMq[msgIdIndex]
    #print firstMq[msgIdIndex]
    #print azTime
    #print mqTime
    #print azMsgs
    #print mqMsgs
    azThroughput = float(azMsgs) / (azTime/1000)
    mqThroughput = float(mqMsgs) / (mqTime/1000)
    
    return (azThroughput, mqThroughput)

def get_etl_topo_latency(topology_name, in_file, out_file, in_ts_index, in_msg_index, out_ts_index, out_msg_index, path1, path2):
    in_map = {}
    az_out_map = {}
    mq_out_map = {}
    az_latency_map = {}
    mq_latency_map = {}
    with open(in_file, "r") as f:
        for line in f:
            line_arr = line.split(",")
            ts = line_arr[in_ts_index]
            msg_id = line_arr[in_msg_index].rstrip()
            in_map[msg_id] = ts
    
    with open(out_file, "r") as f:
        for line in f:
            line_arr = line.split(",")
            if path1 in line:
                ts = line_arr[out_ts_index]
                msg_id = line_arr[out_msg_index]
                az_out_map[msg_id] = ts

            elif path2 in line:
                ts = line_arr[out_ts_index]
                msg_id = line_arr[out_msg_index]
                mq_out_map[msg_id] = ts
    az_total_latency = 0
    mq_total_latency = 0
    az_count = 0
    mq_count = 0
    az_latencies = []
    mq_latencies = []
    for key in in_map:
        start_time = long(in_map[key])
        try:   
            az_end_time = long(az_out_map[key])
            if (az_end_time):
                latency = (az_end_time - start_time)
                az_total_latency += latency
                az_latencies.append(latency)
                az_count+=1
        except:
            pass
        try:
            mq_end_time = long(mq_out_map[key])
            if (mq_end_time):
                latency = (mq_end_time - start_time)
                mq_total_latency += latency
                mq_latencies.append(latency)
                mq_count+=1
        except:
            pass
    #print len(az_latencies)
    #print az_count
    mq_av_latency = mq_total_latency / mq_count
    az_av_latency = az_total_latency / az_count
    #print len(mq_out_map)
    #print len(az_out_map)
    
    #print(len(az_latencies))

    az_latencies = numpy.asarray(az_latencies)
    mq_latencies = numpy.asarray(mq_latencies)
    
    #print(len(az_latencies[az_latencies > 30000]))
    
    #num_bins = numpy.arange(az_latencies.min(),az_latencies.max(), len(az_latencies)/1000)
    num_bins = 50
    fig, ax = plt.subplots()
    #print ("num_bins: " + str(num_bins)) 
    n, bins, patches = ax.hist(az_latencies, num_bins, facecolor='green')
    ax.set_xlabel('Average Latency')
    ax.set_ylabel('Occurrence')
    ax.set_title(path1+"-"+'Latency histogram')
    plt.savefig(topology_name + "-latency-" + path1)
    
    #num_bins = numpy.arange(mq_latencies.min(),mq_latencies.max(), len(mq_latencies)/1000)

    n, bins, patches = ax.hist(mq_latencies, num_bins, facecolor='green')    
    ax.set_xlabel('Average Latency')
    ax.set_ylabel('Occurrence')
    ax.set_title(path2+"-"+'Latency histogram')

    plt.savefig(topology_name + "-latency-" + path2)

    return (az_av_latency, mq_av_latency)
    

def getLatency(in_file, out_file, in_ts_index, in_msg_index, out_ts_index, out_msg_index, topo_name=None):
    in_map = {}
    out_map = {}
    latency_map = {}
    with open(in_file, "r") as f:
        for line in f:
            line_arr = line.split(",")
            ts = line_arr[in_ts_index]
            msg_id = line_arr[in_msg_index]
            in_map[msg_id] = ts
    
    with open(out_file, "r") as f:
        for line in f:
            line_arr = line.split(",")
            ts = line_arr[out_ts_index]
            msg_id = line_arr[out_msg_index]
            out_map[msg_id] = ts
    

    latencies=[]
    total_latency=0
    count=0
    for key in in_map:
        try: 
            start_time = long(in_map[key])
            end_time = long(out_map[key])
            if (end_time):
                latency = (end_time - start_time)
                total_latency += latency
                latencies.append(latency)
                count+=1
        except:
            pass
   
    latencies = numpy.asarray(latencies)

    num_bins = 50
    fig, ax = plt.subplots()
    #print ("num_bins: " + str(num_bins)) 
    n, bins, patches = ax.hist(latencies, num_bins, facecolor='green')
    ax.set_xlabel('Average Latency')
    ax.set_ylabel('Occurrence')
    ax.set_title('Latency histogram')
    plt.savefig(topo_name + "-latency") 
    return total_latency / count


def get_stats_topo_throughput(sink_file, timestampIndex):
    with open(sink_file, "r") as f:
        first = f.readline().split(",")
        num_msgs=1
        for line in f:
            num_msgs+=1
#        print num_msgs
        last = line.split(",")
        startTime = long(first[timestampIndex])
        endTime = long(last[timestampIndex])
        rate = float(num_msgs) / ((endTime-startTime)/1000)
    return rate

class Results(object):
    def __init__(self, topo_name, input_rate, throughput, latencies):
        self.topo_naem = topo_name
        self.input_rate = input_rate
        self.throughput = throughput
        self.latencies = latencies
    
    def get_paths(self, topo_name):
        if topo_name.startswith("etl"):
            return ["azure_insert_path", "publish_path"]
        elif topo_name.startswith("wordcount"):
            return ["wordcount"]
        elif topo_name.startswith(("pred", "train")):
                return ["DTC_path", "MLR_path"]
        elif topo_name.startswith("stat"):
                return ["stat"]
        else:
            return None
    
    def get_top_row(self, topo_name):
        row = "input_rate,"
        paths = self.get_paths(topo_name)
        for path in paths:
            row = row + "throughput_" + path + ","
        for path in paths:
            row = row + "latency_" + path + ","
        return row[0:-1]

    def get_csv_data(self, topo_name):
        data = str(self.input_rate) + ","
        for tp in self.throughput:
                data = data + str(tp) + ","
        for lat in self.latencies:
            data = data + str(lat) + ","
        return data[0:-1]

    def get_data_dict(self, topo_name):
        header=self.get_top_row(topo_name).split(',')
        data=self.get_csv_data(topo_name).split(',')
        _dict={}
        for _index, _head in enumerate(header):
            _dict[_head]=data[_index]
        return _dict


    def get_csv_rep(self, topo_name):
        return [self.get_top_row(topo_name), self.get_csv_data(topo_name)]



def get_results(topology_name, spoutFile, sinkFile):
    input_rate = 0
    throughputs = []
    latencies = []

    if topology_name.startswith("etl"):
        input_rate = get_rate(spoutFile, 3, 5)
        #print "Input Rate (msgs/sec): " + str(input_rate)
        (tp0, tp1) = get_topo_tp(sinkFile, 3, 4, "AzureInsert", "PublishBolt", topology_name)
        (l0, l1) = get_etl_topo_latency(topology_name, spoutFile, sinkFile, 3, 5, 3 ,4, "AzureInsert", "PublishBolt")
        throughputs.append(tp0)
        throughputs.append(tp1)
        latencies.append(l0)
        latencies.append(l1)
        print topology_name + ": AzureInsert path Throughput (msgs/sec): " + str(throughputs[0])
        print topology_name + ": PublishBolt path Throughput (msgs/sec): " + str(throughputs[1])
        print topology_name + ": AzureInsert path Latency (msec): " + str(latencies[0])
        print topology_name + ": PublishBolt path Latency (msec): " + str(latencies[1])

    elif  topology_name.startswith("wordcount"):
        input_rate = get_rate(spoutFile, 3, 5)
        tp0 = get_rate(sinkFile, 3, 4, topo_name="wordcount")
        l0 = getLatency(spoutFile, sinkFile, 3, 5, 3 ,4, topo_name="wordcount")
        throughputs.append(tp0)
        latencies.append(l0)
        #print "Input Rate (msgs/sec): " + str(input_rate)
        #print "Throughput (msgs/sec): " + str(throughputs[0])
        #print "Average Latency (ms): " + str(latencies[0])

    elif topology_name.startswith("pred"):
        input_rate = get_rate(spoutFile, 3, 5)
        #print "Input Rate (msgs/sec): " + str(input_rate)
        (tp0, tp1) = get_topo_tp(sinkFile, 3, 4, "DTC", "MLR", topology_name)
        (l0, l1) = get_etl_topo_latency(topology_name, spoutFile, sinkFile, 3, 5, 3 ,4, "DTC", "MLR")
        throughputs.append(tp0)
        throughputs.append(tp1)
        latencies.append(l0)
        latencies.append(l1)

    elif topology_name.startswith("stat"):
        input_rate = get_rate(spoutFile, 3, 5)
        tp0 = get_rate(sinkFile, 3, 4, topo_name=topology_name)
        l0 = getLatency(spoutFile, sinkFile, 3, 5, 3 ,4, topology_name)
        throughputs.append(tp0)
        latencies.append(l0)
        #print "Input Rate (msgs/sec): " + str(input_rate)
        #print "Throughput (msgs/sec): " + str(throughputs[0])
        #print "Average Latency (ms): " + str(latencies[0])

    elif topology_name.startswith("train"):
        input_rate = getInputRate(spoutFile)
        #print "Input Rate (msgs/sec): " + str(input_rate)
        (tp0, tp1) = get_etl_topo_throughput(sinkFile, 3, 4, "DTC", "MLR")
        (l0, l1) = get_etl_topo_latency(topology_name, spoutFile, sinkFile, 3, 5, 3 ,4, "DTC", "MLR")
        throughputs.append(tp0)
        throughputs.append(tp1)
        latencies.append(l0)
        latencies.append(l1)

    return Results(topology_name, input_rate, throughputs, latencies)
    #return (input_rate, throughputs, latencies)

def main():
    usage = "python <script_name.py> <topology_name> </path/to/spout_log_file> </path/to/sink_log_file>"

    if len(sys.argv) != 4:
        print "Invalid number of arguments. See usage below."
        print usage
        exit(-1)

    topology_name = sys.argv[1]
    spoutFile = sys.argv[2]
    sinkFile = sys.argv[3]
    
    get_results(topology_name, spoutFile, sinkFile)

if __name__ == "__main__":
    main()