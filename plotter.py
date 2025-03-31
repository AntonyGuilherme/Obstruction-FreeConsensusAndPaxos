import json
import matplotlib.pyplot as plt
import numpy as np

class ExperimentPlotter:

    def __init__(self, file_name):
        self.file_name = file_name
        self.average_latency_cons = {}
        self.total_average_latency = {}

    def setup(self):
        file_path = self.file_name + ".json" 

        with open(file_path, "r") as file:
            file_result = json.load(file)

            for scen in file_result:
                self.execution_measure(scen)

    def execution_measure(self, scenario):
        n = scenario["numberOfProcess"]
        f = scenario["numberOfFaultyProcesses"]
        fail_prob  = scenario["failureProb"]
        leaderElectionTime = scenario["leaderElectionTime"]
        results = scenario["results"]

        latencies = 0
        cons_latencies = 0

        for res in results:
            latencies += res["averageLatency"]
            cons_latencies += res["consensusLatency"]
        
        avg_latency = latencies/5
        cons_latency = cons_latencies/5

        if n not in self.total_average_latency:
            self.total_average_latency[n] = []

        if n not in self.average_latency_cons:
            self.average_latency_cons[n] = []

        self.average_latency_cons[n].append({
            "number_of_processes": n,
            "fail_probability": fail_prob,
            "leader_election_time": leaderElectionTime,
            "latency": cons_latency
        })
        self.total_average_latency[n].append({
            "number_of_processes": n,
            "fail_probability": fail_prob,
            "leader_election_time": leaderElectionTime,
            "latency": avg_latency
        })


    def plot_bar_graphs(self, analysis, fixed_param, fixed_value):
        
        plt.style.use('bmh') 
        plt.rcParams.update({'font.size': 16})

        data = self.total_average_latency if analysis == "average" else self.average_latency_cons

        fancy_params = {
            "number_of_processes": "number of processes",
            "fail_probability": "failure probability",
            "leader_election_time": "leader election time (ms)"}

        if fixed_param not in ["number_of_processes", "fail_probability", "leader_election_time"]:
            raise ValueError("fixed_param must be 'number_of_processes', 'fail_probability', or 'leader_election_time'")
        
        if fixed_param == "number_of_processes":
            filtered_data = data.get(fixed_value, [])
        else:
            filtered_data = [res for res_list in data.values() for res in res_list if res[fixed_param] == fixed_value]
        
        if not filtered_data:
            print(f"No data available for {fixed_param}={fixed_value}")
            return
        
        varying_params = [p for p in ["fail_probability", "number_of_processes", "leader_election_time"] if p != fixed_param]
        x_param, color_param = varying_params
        
        x_values = sorted(set(res[x_param] for res in filtered_data))
        color_values = sorted(set(res[color_param] for res in filtered_data))
        
        latencies = {xv: {cv: None for cv in color_values} for xv in x_values}
        
        for res in filtered_data:
            x_val = res[x_param]
            color_val = res[color_param]
            latencies[x_val][color_val] = res["latency"]
        
        indices = np.arange(len(x_values)) * 0.5
        width = 0.1
        
        min_value = min([min(latencies[xv].values()) for xv in x_values if None not in latencies[xv].values()])
        max_value = max([max(latencies[xv].values()) for xv in x_values if None not in latencies[xv].values()])

        plt.figure(figsize=(15, 7))
        
        for i, cv in enumerate(color_values):
            values = [latencies[xv][cv] if latencies[xv][cv] is not None else 0 for xv in x_values]
            plt.bar(indices + i * width - (len(color_values) / 2) * width, values, width=width, label=f"{cv}")
        
        plt.ylim([0, max_value+0.05*max_value])
        plt.xlabel(fancy_params[x_param])
        plt.ylabel("Latency (ms)")
        plt.title(f"Latency Analysis for {fancy_params[fixed_param]} = {fixed_value}")
        plt.xticks(indices + width * (len(color_values) / 2 - 2.5), x_values)
        plt.legend(title=f"{fancy_params[color_param]}", ncol=2, loc=[1.01,0.5])

        plt.grid(axis='y', linestyle='--', alpha=0.7, linewidth=0.8, which='both') 
        plt.minorticks_on() 
        plt.grid(which='minor', linestyle=':', alpha=0.4, linewidth=0.5) 

        plt.tight_layout()
        plt.savefig(f"fixed_{fixed_param}_{fixed_value}.png")
        #plt.show()
	    

exp_plotter = ExperimentPlotter("exp_results/results")
exp_plotter.setup()

exp_plotter.plot_bar_graphs("consensus", "leader_election_time", 500)
exp_plotter.plot_bar_graphs("consensus", "leader_election_time", 1000)
exp_plotter.plot_bar_graphs("consensus", "leader_election_time", 1500)
exp_plotter.plot_bar_graphs("consensus", "leader_election_time", 2000)

exp_plotter.plot_bar_graphs("consensus", "number_of_processes", 3)
exp_plotter.plot_bar_graphs("consensus", "number_of_processes", 10)
exp_plotter.plot_bar_graphs("consensus", "number_of_processes", 100)
