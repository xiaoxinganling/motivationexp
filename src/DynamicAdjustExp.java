import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.util.*;

import static java.lang.System.exit;

public class DynamicAdjustExp {
    private static double MEMORY_SIZE = 0.7;
    private static int TOTAl = 0;
    private static int trunc = 6;
    public static void main(String[] args) throws Exception{
        if(args.length != 6){
            System.err.println("Usage: tracePath multiPath maxOut maxStep maxIteration trunc");
            exit(-1);
        }
        String tracePath = args[0];//batch_task.csv "C:\\Users\\xiaoxinganling\\Desktop\\batch_task.csv"
        String multiPath = args[1];//dynamic_res_multiple_job
        String[] paths = multiPath.split("/");
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < paths.length - 1; i++){
            sb.append(paths[i] + "/");
        }
        File workDir = new File(sb.toString());
        if(!sb.toString().equals("") && !workDir.exists()){
            System.err.printf("Path %s is not exist!\n", workDir.getPath());
            System.exit(-1);
        }
        SketchExp.MAXOUT = Integer.valueOf(args[2]);//4
        SketchExp.MAXSTEP = Integer.valueOf(args[3]);//4
        SketchExp.MAXITERATION = Integer.valueOf(args[4]);//14295731
        trunc = Integer.valueOf(args[5]);
        // single job
        Map<String, Job> res = SketchExp.generateJobsWithIteration(tracePath, SketchExp.MAXITERATION);
        // validate correct
        Map<String, Job> test = new HashMap<>();
        int testOD = 0;
        int testS = 0;
        // end vc
//        List<String> keys = new ArrayList<>(res.keySet());
//        Collections.sort(keys, (o1, o2) -> res.get(o1).startTime.subtract(res.get(o2).startTime).intValue());
//        for(String k : keys){
////            if(res.get(k).tasks.size() == 33){
////                test.put(k, res.get(k));
////                multiJobExp(test, multiPath);
////                //singleJobExp(res.get(k));
////                break;
////            }
//            Job cur = res.get(k);
//            Map<String, GNode> graph = SketchExp.getGraph(cur);
//            String endTask = SketchExp.getEndTask(graph);
//            int maxOD = SketchExp.getMaxOutDegree(graph);
//            int maxS = SketchExp.getMaxStep(cur,endTask);
//            if(maxOD == 4 || maxS == 8){
//                testOD = maxOD == 4 ? testOD + 1 : testOD;
//                testS = maxS == 8 ? testS + 1 : testS;
//                test.put(k, cur);
//            }
//            if(testOD >= 2 && testS >= 2){
//                System.out.println("do dynamic " + test.size());
//                multiJobExp(test, multiPath);
//                break;
//            }
//        }
        multiJobExp(res, multiPath);
    }

    // ???job dynamic adjust exp
    private static void multiJobExp(Map<String, Job> jobs, String multiPath) throws Exception{
        // fixed size
        Map<Integer, List<Double>> fixedSizeOutDegreeTimeAvg = new HashMap<>();
        Map<Integer, List<Double>> fixedSizeOutDegreeMemAvg = new HashMap<>();
        Map<Integer, List<Double>> fixedSizeStepTimeAvg = new HashMap<>();
        Map<Integer, List<Double>> fixedSizeStepMemAvg = new HashMap<>();
        // sequence
        Map<Integer, List<Double>> sequenceStepTimeAvg = new HashMap<>();
        Map<Integer, List<Double>> sequenceStepMemAvg = new HashMap<>();
        // total size
        Map<Integer, Integer> outDegreeMap = new HashMap<>();
        Map<Integer, Integer> stepMap = new HashMap<>();
        // filtered jobs
        List<Job> filteredJobs = SketchExp.getFilteredJobs(jobs, outDegreeMap, stepMap);
        int curNum = 0;
        for(Job j : filteredJobs){
            // 0 => fixed size out degree's decreased time
            // 1 => fixed size out degree's memory consumption
            // 2 => fixed size step's decreased time
            // 3 => fixed size step's memory consumption
            // 4 => sequence step's decreased time
            // 5 => sequence step's memory consumption
            List<List<Double>> list = singleJobExp(j);
            System.out.println("Job " + j.jobName + " " + list);
            curNum++;
            if(curNum % 1000 == 0){
                System.out.println(curNum + "/" + SketchExp.MAXITERATION);
            }
            SketchExp.updateAvg(fixedSizeOutDegreeTimeAvg, outDegreeMap, list.get(0));
            SketchExp.updateAvg(fixedSizeOutDegreeMemAvg, outDegreeMap, list.get(1));
            SketchExp.updateAvg(fixedSizeStepTimeAvg, stepMap, list.get(2));
            SketchExp.updateAvg(fixedSizeStepMemAvg, stepMap, list.get(3));
            SketchExp.updateAvg(sequenceStepTimeAvg, stepMap, list.get(4));
            SketchExp.updateAvg(sequenceStepMemAvg, stepMap, list.get(5));
        }
        System.out.println("filtered Job size: " + filteredJobs.size());
        System.out.println("out degree " + outDegreeMap);
        System.out.println("step " + stepMap);

        // write file
        BufferedWriter bw = new BufferedWriter(new FileWriter(multiPath + "_fixed_size_out_degree"));
        BufferedWriter bw2 = new BufferedWriter(new FileWriter(multiPath + "_fixed_size__step"));
        BufferedWriter bw3 = new BufferedWriter(new FileWriter(multiPath + "_sequence_step"));
        SketchExp.writeTimeAndMem(bw, fixedSizeOutDegreeTimeAvg, fixedSizeOutDegreeMemAvg);
        SketchExp.writeTimeAndMem(bw2, fixedSizeStepTimeAvg, fixedSizeStepMemAvg);
        SketchExp.writeTimeAndMem(bw3, sequenceStepTimeAvg, sequenceStepMemAvg);
        bw.close();
        bw2.close();
        bw3.close();
    }
    // ???job dynamic adjust exp
    // ??????6???[]
    private static List<List<Double>> singleJobExp(Job j) throws Exception{
        List<List<Double>> res = new ArrayList<>();
        Map<String, Task> tasks = new HashMap<>();
        for(Task t : j.tasks){
            tasks.put(t.taskId, t);
        }
        System.out.println(j.jobName);
        // get graph
        Map<String, GNode> graph = SketchExp.getGraph(j);
        // get endTask
        String endTask = SketchExp.getEndTask(graph);
        // ??????max out degree???max step
        int maxOutDegree = SketchExp.getMaxOutDegree(graph);
        int maxStep = SketchExp.getMaxStep(j, endTask);
        System.out.println(maxOutDegree + " degree-step " + maxStep);
        if(maxOutDegree < SketchExp.MAXOUT || maxStep < SketchExp.MAXSTEP){
            return null;
        }
        //BufferedWriter bw = new BufferedWriter(new FileWriter("dynamic_res_one_job_fixed_memory"));
        // ??????JCT
        // ???????????????????????????cache???????????????
//        List<Integer> decreaseTime = new ArrayList<>();
//        for(int i = 1; i < 12; i++){
//            Set<String> cache = SketchExp.getTasksWithOutdegree(j, i);
//            decreaseTime.add(getJCT(j, endTask).subtract(getJCTWithCache(j, cache, endTask)).intValue());
//        }
//        bw.write(decreaseTime + "\n");
//        System.out.println(decreaseTime);
//        // ????????????action??????????????????cache???????????????
//        decreaseTime = new ArrayList<>();
//        for(int i = 1; i < 12; i++){
//            Set<String> cache = SketchExp.getTasksWithStep(j, i, endTask);
//            decreaseTime.add(getJCT(j, endTask).subtract(getJCTWithCache(j, cache, endTask)).intValue());
//        }
//        System.out.println(decreaseTime);
//        bw.write(decreaseTime + "\n");
        // ??????JCT??????
        // print tasks' waiting time
        // getTaskWaitTime(j, tasks);
        // ??????memorysize??????????????????????????????task
//                System.out.println(getTasksWithOutdegreeAndMemory(j,1,MEMORY_SIZE));
        // System.out.println(getTasksWithStepAndMemory(j, 2, endTask, MEMORY_SIZE));
        // ??????memory size?????????
        // ????????????????????????cache???????????????
        List<Double> fixedSizeOutDegreeDecreaseTime = new ArrayList<>();
        List<Double> fixedSizeOutDegreeMemorySize = new ArrayList<>();
        for(int i = 1; i <= maxOutDegree; i++){
            Set<String> cache = getTasksWithOutdegreeAndMemory(j, i, MEMORY_SIZE);
            //decreaseTime.add(SketchExp.getTimeWithCache(j, cache, endTask).intValue());
            fixedSizeOutDegreeDecreaseTime.add(getJCT(j, endTask).subtract(getJCTWithCache(j, cache, endTask)).doubleValue());
            fixedSizeOutDegreeMemorySize.add(SketchExp.getSizeWithCache(j, cache));
        }
        res.add(fixedSizeOutDegreeDecreaseTime);
        res.add(fixedSizeOutDegreeMemorySize);
        // ????????????endTask??????????????????cache???????????????
        List<Double> fixedSizeStepDecreaseTime = new ArrayList<>();
        List<Double> fixedSizeStepMemorySize = new ArrayList<>();
        for(int i = 1; i <= maxStep; i++){
            Set<String> cache = getTasksWithStepAndMemory(j, i, endTask, MEMORY_SIZE);
            //decreaseTime.add(SketchExp.getTimeWithCache(j, cache, endTask).intValue());
            fixedSizeStepDecreaseTime.add(getJCT(j, endTask).subtract(getJCTWithCache(j, cache, endTask)).doubleValue());
            fixedSizeStepMemorySize.add(SketchExp.getSizeWithCache(j, cache));
        }
        res.add(fixedSizeStepDecreaseTime);
        res.add(fixedSizeStepMemorySize);
        // ???????????????
//                System.out.println(getTaskListWithStepAndMemory(j, 3, endTask, MEMORY_SIZE).keySet().size());
//                System.out.println(getTaskListWithStepAndMemory(j, 13, endTask, MEMORY_SIZE).keySet().size());

        List<Double> sequenceStepDecreaseTime = new ArrayList<>();
        List<Double> sequenceStepMemorySize = new ArrayList<>();
        chooseCondition(1, maxStep + 1, sequenceStepDecreaseTime, sequenceStepMemorySize, j, endTask);
        res.add(sequenceStepDecreaseTime);
        res.add(sequenceStepMemorySize);
        // test all conditions
//                StringBuilder sb = new StringBuilder();
//                int mul = 1;
//                for(int i = 1; i < 12; i++){
//                    int size = getTaskListWithStepAndMemory(j, i, endTask, MEMORY_SIZE).keySet().size();
//                    System.out.println("step: " + i + ", conditions: " + size);
//                    sb.append(size).append("x");
//                    mul *= size;
//                }
//                sb.deleteCharAt(sb.length() - 1);
//                System.out.println(sb.toString() + " multiply: "+ mul);
        //chooseCondition(1, 12, decreaseTime, memorySize, j, endTask);
//                Set<String> cache1 = new HashSet<>();
//                cache1.add("20");
//                Set<String> cache2 = new HashSet<>();
//                cache2.add("31");
//                Set<String> cache3 = new HashSet<>();
//                cache3.add("3");
//                Set<String> cache4 = new HashSet<>();
//                cache4.add("14");
//                System.out.println(cache1);
//                System.out.println(getJCT(j, endTask) + " ============== " + getJCT(j, endTask).subtract(getJCTWithCache(j, cache1, endTask)).intValue());
//                System.out.println(cache2);
//                System.out.println(getJCT(j, endTask).subtract(getJCTWithCache(j, cache2, endTask)).intValue());
//                System.out.println(cache3);
//                System.out.println(getJCT(j, endTask).subtract(getJCTWithCache(j, cache3, endTask)).intValue());
//                System.out.println(cache4);
//                System.out.println(getJCT(j, endTask).subtract(getJCTWithCache(j, cache4, endTask)).intValue());
        return res;
    }
    // 1. ??????job?????????????????????
    public static BigInteger getJCT(Job j, String endTask){
//        // ??????1. ????????????endtime-starttime,????????????????????????
//        System.out.println("total time " + j.endTime.subtract(j.startTime));
//        return j.endTime.subtract(j.startTime);
        Map<String, Task> tasks = new HashMap<>();
        for(Task t : j.tasks){
            tasks.put(t.taskId, t);
        }
        return dfs(tasks, tasks.get(endTask));
    }
    public static BigInteger dfs(Map<String, Task> tasks, Task t){
        // ?????????t??????null???trace?????????j_738534 + ?????????task?????????task_abcalsjfoiiajefvflmf32r02
        if(t == null){
            return BigInteger.valueOf(0);
        }
        BigInteger res = t.endTime.subtract(t.startTime);
        BigInteger max = BigInteger.valueOf(0);
        for(String next : t.parents){
//            if(tasks.get(next) == null){
//                System.out.println(next + " " + tasks);
//            }
            max = max.max(dfs(tasks, tasks.get(next)));
        }
        return res.add(max);
    }
    // 2. ??????job???cache??????????????????
    public static BigInteger getJCTWithCache(Job j, Set<String> cache, String endTask){
        Map<String, Task> tasks = new HashMap<>();
        for(Task tt : j.tasks){
            tasks.put(tt.taskId, tt);
        }
        Set<String> needToCalculate = new HashSet<>();
        needToCalculate.add(endTask);
        Queue<Task> queue = new LinkedList<>();
        queue.offer(tasks.get(endTask));
        getTasksNeedToCalculate(cache, tasks, needToCalculate, queue);
        //System.out.println(needToCalculate + " " + needToCalculate.size());
//        System.out.println("cache" + cache);
//        System.out.println("need to caculate " + needToCalculate);
        BigInteger totalTime = dfsWithCache(tasks, tasks.get(endTask), needToCalculate);
        //System.out.println("total Time With Cache: " + totalTime);
        return totalTime;
    }

    static void getTasksNeedToCalculate(Set<String> cache, Map<String, Task> tasks, Set<String> needToCalculate, Queue<Task> queue) {
        while(!queue.isEmpty()){
            Task cur = queue.poll();
            if(cur == null){
                continue;
            }
            for(String parent : cur.parents) {
                if (cache.contains(parent)) {
                    continue;
                }
                queue.offer(tasks.get(parent));
                needToCalculate.add(parent);
            }
        }
    }

    public static BigInteger dfsWithCache(Map<String, Task> tasks, Task t, Set<String> needToCalculate) {
        if(t == null){
            return BigInteger.valueOf(0);
        }
        BigInteger res = t.endTime.subtract(t.startTime);
        BigInteger max = BigInteger.valueOf(0);
        for (String next : t.parents) {
            if (needToCalculate.contains(next)) {
                Task cur = tasks.get(next);
                BigInteger tmp = dfsWithCache(tasks, cur, needToCalculate);
//                if(t.taskId.equals("32")){
//                    System.out.println("chosed " + next);
//                    System.out.println("tmp.value? " + tmp + " tmp is bigger ? " + tmp.compareTo(max));
//                }//??????????????????????????????
                max = max.max(tmp);//???????????????
            }

        }
        return res.add(max);
    }
    // 3. ??????job?????????task???????????????
    public static void getTaskWaitTime(Job j, Map<String, Task> tasks){
//        for(Task t : j.tasks){
//            if(t.parents.size() == 0){
//                System.out.println(t + " 0");
//            }else{
//                BigInteger latest = BigInteger.valueOf(0);
//                for(String parent : t.parents){
//                    latest = latest.max(tasks.get(parent).endTime);
//                }
//                System.out.println(t + " \n" + t.startTime.subtract(latest));//maybe minus
//            }
//        }
    }
    // 4. ??????memory size??????Outdegree???????????????task
    public static Set<String> getTasksWithOutdegreeAndMemory(Job j, int outDegree, double memorySize){
        Set<String> res = new HashSet<>();
        Map<String, GNode> graph = SketchExp.getGraph(j);
        Map<String, Task> tasks = new HashMap<>();
        for(Task t : j.tasks){
            tasks.put(t.taskId, t);
        }
        //????????????????????????????????????
        List<String> sortedKeyset = new ArrayList<>(graph.keySet());
//        Collections.sort(sortedKeyset, (o1, o2) -> tasks.get(o2).startTime.subtract(tasks.get(o1).startTime).intValue());
//        for(String key : sortedKeyset){
//            System.out.println(tasks.get(key).startTime + " " + key);
//        }// startTime??????????????????
        for(String key : sortedKeyset){
            if(!tasks.containsKey(key)){
                continue;
            }
            if(graph.get(key).outDegree == outDegree){
//                if(tasks.get(key) == null){
//                    System.out.println(graph.get(key) + " nnnnnnnnnnnnnnnnnnnnnn" + tasks + " " + key);
//                }
                if(memorySize - tasks.get(key).memorySize < 0){
                    break;
                }
                res.add(key);
                //System.out.println(key + " " + tasks.get(key).memorySize);
                memorySize -= tasks.get(key).memorySize;
            }
        }
        return res;
    }

    // 3. get tasks with step and Memory size
    public static Set<String> getTasksWithStepAndMemory(Job j, int step, String endTask, double memory){
        Map<String, Task> tasks = new HashMap<>();
        for(Task t : j.tasks){
            tasks.put(t.taskId, t);
        }
        Queue<Task> queue = new LinkedList<>();
        queue.offer(tasks.get(endTask));
        getTasksWithStepByQueue(step, tasks, queue);
        Set<String> res = new HashSet<>();
        while(!queue.isEmpty()){
            Task toBeAdd = queue.poll();
//            if(toBeAdd == null){
//                System.out.println(j + "jjjjjjjjjjjjjjjjjjjjj");
//            }
            if(memory - toBeAdd.memorySize < 0){
                break;
            }
            res.add(toBeAdd.taskId);
            memory -= toBeAdd.memorySize;
            //System.out.println("add " + toBeAdd.taskId + " " +memory);
        }
        return res;
    }

    // 4. get tasks with step and memory size different sequence
    public static Map<String, Set<String>> getTaskListWithStepAndMemory(Job j, int step, String endTask, double memory){
        Map<String, Task> tasks = new HashMap<>();
        for(Task t : j.tasks){
            tasks.put(t.taskId, t);
        }
        Queue<Task> queue = new LinkedList<>();
        queue.offer(tasks.get(endTask));
        getTasksWithStepByQueue(step, tasks, queue);
        // ???????????????item??????queue??????
        List<Task> items = new ArrayList<>();
        while(!queue.isEmpty()){
            if(items.size() == trunc){
                break;
            }
            Task cur = queue.poll();
            if(!items.contains(cur)){
                items.add(cur);//?????????,??????cur??????queue.poll()
            }
        }
        // ?????????
        //List<List<Task>> allCondition = new ArrayList<>();
        //System.out.println("for generating all condition: " + items.size() + " " + items);
        // ??????????????????????????????????????????????????????????????????(????????????set)
        Map<String, Set<String>> res = new HashMap<>();
        backtrack(0, items, res);//??????????????????????????????oom?????????????????????????????????????????????????????????
        //System.out.println(items.size() + " items generates " + allCondition.size() + " conditions with memory size: " + MEMORY_SIZE);
        // ????????????
//        for(List<Task> condition : allCondition){
//            // ???????????????????????????????????????????????????
//            Set<String> oneCache = new HashSet<>();
//            if(condition.size() == 0){
//                res.put("null", oneCache);
//            }
//            //??????memory
//            memory = MEMORY_SIZE;
//            List<String> chosed = new ArrayList<>();
//            for(Task toBeAdd : condition){
//                if(memory - toBeAdd.memorySize < 0){//????????????bug,??????memory_size???0.7???????????????task???0.5?????????????????????????????????condition??????
//                    //??????break
//                    break;
//                }
//                oneCache.add(toBeAdd.taskId);
//                memory -= toBeAdd.memorySize;
//                //key.append(toBeAdd.taskId).append("_");
//                chosed.add(toBeAdd.taskId);
//            }
//            // put into map
//            Collections.sort(chosed);
//            StringBuilder key = new StringBuilder();
//            for(String s : chosed){
//                key.append(s).append("_");
//            }
//            //System.out.println(key.toString() + res.containsKey(key.toString()));
//            if(!res.containsKey(key.toString())){//??????????????????????????????2_22???22_2????????????
////                        double sum = 0;
////                        for(String s : chosed){
////                            sum += tasks.get(s).memorySize;
////                            System.out.print(tasks.get(s).memorySize + " ");
////                        }
////                        System.out.println(sum);
//                res.put(key.toString(), oneCache);
//            }
//        }
        //System.out.printf("===========> after filter: %d conditions\n", res.keySet().size());
        return res;
    }

    // 5. ?????????step?????????cache??????????????????
    private static void chooseCondition(int curStep, int totalStep, List<Double> decreaseTime,
                                        List<Double> memorySize, Job j, String endTask) throws Exception {
        if(curStep == totalStep){
//            BufferedWriter bw = new BufferedWriter(new FileWriter("dynamic_res_one_job_sequence_average"));
//            bw.write(decreaseTime + "\n");
//            bw.write(memorySize + "\n");
//            bw.close();
            return;
        }
        Map<String, Set<String>> cacheCondition = getTaskListWithStepAndMemory(j, curStep, endTask, MEMORY_SIZE);
        //System.out.println("step: " + curStep + " generates " + cacheCondition.size() + " conditions");
        //System.out.println(cacheCondition.values());
        // get average
        double timeSum = 0;
        double sizeSum = 0;
        for(Set<String> cache : cacheCondition.values()){
            timeSum += getJCT(j, endTask).subtract(getJCTWithCache(j, cache, endTask)).intValue();
            sizeSum += SketchExp.getSizeWithCache(j, cache);
        }
        decreaseTime.add(timeSum / cacheCondition.size());
        memorySize.add(sizeSum / cacheCondition.size());
        chooseCondition(curStep + 1, totalStep, decreaseTime, memorySize, j, endTask);

//        for(Set<String> cache : cacheCondition.values()){
//            decreaseTime.add(getJCT(j, endTask).subtract(getJCTWithCache(j, cache, endTask)).intValue());
//            memorySize.add(SketchExp.getSizeWithCache(j, cache));
//            System.out.println(cache + " " + decreaseTime + " " + memorySize);
//            chooseCondition(curStep + 1, totalStep, decreaseTime, memorySize, j, endTask);
//            decreaseTime.remove(decreaseTime.size() - 1);
//            memorySize.remove(memorySize.size() - 1);
//        }
    }
    public static void getTasksWithStepByQueue(int step, Map<String, Task> tasks, Queue<Task> queue) {
        while(step > 0 && !queue.isEmpty()){
            int size = queue.size();
            oneStep(tasks, queue, size);
            step--;
        }
    }

    static void oneStep(Map<String, Task> tasks, Queue<Task> queue, int size) {
        for(int i = 0; i < size; i++){
            Task cur = queue.poll();
            if(cur == null){
                continue;
            }
            for(String parent : cur.parents){
//                if(tasks.get(parent) == null){
//                    System.out.println(cur + "printtttttttttttt");
//                    // ????????????????????????task_MTM0ODUxMTY0NjQzMTI1NTc1MQ==
//                }
                if(tasks.containsKey(parent)){
                    queue.offer(tasks.get(parent));
                }
            }
        }
    }

    // ???item???????????????
    private static void backtrack(int depth, List<Task> items, Map<String, Set<String>> res){
        if(depth == items.size()){
            //allCondition.add(new ArrayList<>(items));
//            for(Task t : items){
//                System.out.print(t.taskId + " ");
//            }
//            System.out.println();
            // ????????????????????????????????????????????????
            // ???????????????????????????????????????????????????
            Set<String> oneCache = new HashSet<>();
            if(items.size() == 0){
                res.put("null", oneCache);
            }
            //??????memory
            double memory = MEMORY_SIZE;
            List<String> chosed = new ArrayList<>();
            for(Task toBeAdd : items){
                if(memory - toBeAdd.memorySize < 0){//??????????????????bug,??????memory_size???0.7???????????????task???0.5?????????????????????????????????condition??????
                    //??????break
                    break;
                }
                oneCache.add(toBeAdd.taskId);
                memory -= toBeAdd.memorySize;
                //key.append(toBeAdd.taskId).append("_");
                chosed.add(toBeAdd.taskId);
            }
            // put into map
            Collections.sort(chosed);
            StringBuilder key = new StringBuilder();
            for(String s : chosed){
                key.append(s).append("_");
            }
            //System.out.println(key.toString() + res.containsKey(key.toString()));
            if(!res.containsKey(key.toString())){//??????????????????????????????2_22???22_2??????????????????????????????
//                        double sum = 0;
//                        for(String s : chosed){
//                            sum += tasks.get(s).memorySize;
//                            System.out.print(tasks.get(s).memorySize + " ");
//                        }
//                        System.out.println(sum);
                res.put(key.toString(), oneCache);
            }
            return;//??????return
        }
        for(int i = depth; i <  items.size(); i++){
            swap(items, i, depth);
            backtrack(depth + 1, items, res);
            swap(items, i, depth);
        }
    }
    private static void swap(List<Task> items, int i, int j){
        if(i == j){
            return;
        }
        Task tmp = items.get(i);
        items.set(i, items.get(j));
        items.set(j, tmp);
    }
    // ????????????: ???????????????????????????????????????????????????????????????????????????cache set??????cache set????????????????????????????????????
    // ????????????: ???????????????????????????????????????*????????????????????????*???????????????????????????????????????cache set??????~???????????????
    // ?????????????????????????????????????????????step????????????????????????????????????????????????cache set??????????????????cache set???list?????????map???
}
