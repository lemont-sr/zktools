package com.lemont;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

public class ZkDistributeLock implements DistributeLock {
    private ZkClient zkClient;
    private String path;
    private String currentLock;
    private String seq;
    private String waitLock;
    private Semaphore semaphore = new Semaphore(0);

    public ZkDistributeLock(ZkClient zkClient, String lockRootPath, String key) {
        this.zkClient = zkClient;
        this.path = lockRootPath + "/" + key;
        zkClient.createPersistent(path, true);
    }

    @Override
    public boolean tryLock() {
        try {

            String sequential = zkClient.createEphemeralSequential(path + "/", "lock");
            currentLock = sequential;
            seq = currentLock.substring(currentLock.lastIndexOf("/") + 1);
            List<String> children = zkClient.getChildren(path);
            if (isCurrentMin(children)) {
                System.out.println(Thread.currentThread().getName() + " 获得锁:" + currentLock);
                return true;
            } else {
                waitLock = children.get(Collections.binarySearch(children, seq) - 1);
                System.out.println(Thread.currentThread().getName() + "等待锁" + waitLock);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void lock() throws InterruptedException {
        if (tryLock())
            return;
        else {
            IZkDataListener listener = new IZkDataListener() {
                @Override
                public void handleDataChange(String dataPath, Object data) throws Exception {

                }

                @Override
                public void handleDataDeleted(String dataPath) throws Exception {
                    System.out.println("delete:" + dataPath);
                    semaphore.release();
                }
            };
            String waitPath = path + "/" + waitLock;
            zkClient.subscribeDataChanges(waitPath, listener);
            if (zkClient.exists(waitPath))
                semaphore.acquire();
            zkClient.unsubscribeDataChanges(waitPath, listener);
        }

    }

    public boolean isCurrentMin(List<String> currentChilds) {
        Collections.sort(currentChilds);
        if (seq.equals(currentChilds.get(0))) {
            return true;
        }
        return false;
    }

    @Override
    public void unLock() {
        zkClient.delete(currentLock);
    }
}
