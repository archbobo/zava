package com.github.sefler1987.javaworker.worker.mapreduce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.sefler1987.javaworker.worker.ConfigurableWorker;
import com.github.sefler1987.javaworker.worker.WorkerEvent;
import com.github.sefler1987.javaworker.worker.WorkerListener;

public class Map2ReduceConnector implements WorkerListener {
    private List<ConfigurableWorker> reduces = new ArrayList<ConfigurableWorker>();

    private int lastIndex = 0;

    public Map2ReduceConnector(List<ConfigurableWorker> reduces) {
        this.reduces.addAll(reduces);
    }

    @Override
    public List<WorkerEvent> intrests() {
        return Arrays.asList(WorkerEvent.TASK_COMPLETE);
    }

    @Override
    public synchronized void onEvent(WorkerEvent event, Object... args) {
        MapReducePageURLMiningTask task = (MapReducePageURLMiningTask) args[0];

        lastIndex = ++lastIndex % reduces.size();
        reduces.get(lastIndex).addTask(task);
    }
}
