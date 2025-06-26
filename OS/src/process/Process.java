package process;

import os.OS;
import resource.Resource;

import java.util.ArrayList;
import java.util.List;

public class Process implements Comparable<Process> {
    @Override
    public int compareTo(Process other) {
        return Integer.compare(other.priority, this.priority);
    }
    private String name;   //pid
    private ProcessState processState;   //st
    private String father_process;   //parent process PPID
    private String ptt;
    private int priority;
    private List<Resource> resources;

    private List<Process> child;
    private Process parent;
    private int step = 1;

    public Process() {
        setName(getClass().getSimpleName());
        resources = new ArrayList<>();
        child = new ArrayList<>();
    }

    public void execute(OS os) {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProcessState getState() {
        return processState;
    }

    public void setState(ProcessState processState) {
        this.processState = processState;
    }

    public String getFather_process() {
        return father_process;
    }
    public void increaseStep() {
        this.step++;
    }

    public void setFather_process(String father_process) {
        this.father_process = father_process;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public List<Process> getChild() {
        return child;
    }

    public void setChild(List<Process> child) {
        this.child = child;
    }

    public Process getParent() {
        return parent;
    }

    public void setParent(Process parent) {
        this.parent = parent;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public void addResource(Resource resource) {
        this.resources.add(resource);
    }
}
