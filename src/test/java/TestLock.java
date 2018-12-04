import com.lemont.DistributeLock;
import com.lemont.ZkDistributeLock;
import org.I0Itec.zkclient.ZkClient;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class TestLock {
    public static void main(String[] args) {
        ZkClient zkClient = new ZkClient("localhost");
        int count = 100;
        CyclicBarrier cyclicBarrier = new CyclicBarrier(count);
        for (int i = 0; i < count; i++) {
            Thread t = new Thread(() -> {
                try {
                    cyclicBarrier.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
                DistributeLock distributeLock = new ZkDistributeLock(zkClient, "/lock","test001");
                try {
                    distributeLock.lock();
                    //System.out.println(Thread.currentThread().getName());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                distributeLock.unLock();
            });
            t.setName("Thread-" + i);
            t.start();
        }
    }
}
