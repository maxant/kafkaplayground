package ch.maxant.kafkaplayground.producer;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
@Lock(LockType.READ) // to avoid timeouts which are not relevant here // TODO needed?
public class LoadProducer implements Runnable {

    private static AtomicLong createdInSession = new AtomicLong();
    private static AtomicLong totalCreated = new AtomicLong();

    @Resource
    ManagedExecutorService executorService;

    @Inject
    ProducerService producerService;

    private long start;
    private boolean running = true;

    @PostConstruct
    public void init() {
        start = System.currentTimeMillis();
        createdInSession.set(0);
        executorService.submit(this);
    }

    @Override
    public void run() {
        try {
            producerService.sync("my-topic", null, "{\"createdAt\": " + System.currentTimeMillis() + "}");
            createdInSession.incrementAndGet();
            totalCreated.incrementAndGet();
            System.out.println("sent " + totalCreated.get() + " messages in total, averaging " + (1000.0*createdInSession.get()/(System.currentTimeMillis()-start+0.0)) + " per second in this session");
            if(running) {
                executorService.submit(this);
            }
        } catch (Exception e) {
            System.out.println("FAILED TO SEND RECORD");
            e.printStackTrace();
        }
    }

    public void toggle() {
        if(!running) {
            init();
        }
        running = !running;
    }
}