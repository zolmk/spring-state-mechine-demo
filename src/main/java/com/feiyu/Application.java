package com.feiyu;

import com.feiyu.fsm.*;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachineContextRepository;
import org.springframework.statemachine.access.StateMachineAccess;
import org.springframework.statemachine.access.StateMachineFunction;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static com.feiyu.fsm.Constants.*;


/**
 * @author Zhuff
 */
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}

@Log
@Component
class SampleRunner implements ApplicationRunner {

    private final ThreadPoolExecutor executor;
    private final StateMachineBuilder stateMachineBuilder;
    @Autowired
    private StateMachineContextRepository<ExpStateEnum, ExpEventEnum, StateMachineContext<ExpStateEnum, ExpEventEnum>> repository;

    @Autowired private StateMachinePersister<ExpStateEnum, ExpEventEnum, String> persister;

    SampleRunner(StateMachineBuilder builder) {
        this.stateMachineBuilder = builder;
        this.executor = new ThreadPoolExecutor(4, 8, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<>(200000), new ThreadFactory() {
            final AtomicInteger cnt = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(String.format("thread-"+cnt));
                thread.setDaemon(false);
                return thread;
            }
        });
        this.executor.allowCoreThreadTimeOut(true);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String m = String.valueOf(21313);

        StateMachine<ExpStateEnum, ExpEventEnum> sm = stateMachineBuilder.build(m);

        //log.info("current state:" + sm.getState());
        // 设置当前操作者
        Operator curOperator = new Operator("hhh", Operator.RoleEnum.DELIVERY);
        Operator createOperator = sm.getExtendedState().get(STR_OP_CREATE, Operator.class);
        if (createOperator == null) {
            sm.getExtendedState().getVariables().put(STR_OP_CREATE, curOperator);
        }
        sm.getExtendedState().getVariables().put(STR_OP_CUR, curOperator);



        sm.sendEvent(ExpEventEnum.COLLECTED);
        sm.sendEvent(ExpEventEnum.TRANSFER);
        sm.sendEvent(ExpEventEnum.CANCELED);
        //log.info("current state:" + sm.getState());


        log.info((sm.getExtendedState().get(STR_EXP_STATE_INFO, List.class)).toString());

        // TODO
        System.exit(0);
        int N = 100000;
        Date start = new Date();
        Long orderId = 1312L;
        for (int i = 0; i < 2*N; i++) {
            final int t = i;
            this.executor.execute(()->{
                try {
                    String mid = String.valueOf((orderId + (t%N)));

                    StateMachine<ExpStateEnum, ExpEventEnum> stateMachine = stateMachineBuilder.build(mid);

                    log.info("current state:" + stateMachine.getState());

                    stateMachine.sendEvent(MessageBuilder.withPayload(ExpEventEnum.COLLECTED).setHeader("role", "user").build());

                    log.info("current state:" + stateMachine.getState());

                    persister.persist(stateMachine, mid);
                }catch (Exception e) {
                    // not
                }
            });
        }
        while (true) {
            try {
                Thread.sleep(1000);
                if (executor.getActiveCount() == 0) {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.info("completion task cnt: " + executor.getCompletedTaskCount());
        executor.shutdown();
        log.info("spend Time: " + (System.currentTimeMillis() - start.getTime()) / 1000);

    }
}
