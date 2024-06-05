package com.feiyu.fsm;

import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.access.StateMachineAccess;
import org.springframework.statemachine.access.StateMachineFunction;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.stereotype.Component;

import java.lang.ref.SoftReference;

/**
 * 用来快速构建和恢复（如果有）状态机
 * @author Zhuff
 */
@Component
public class StateMachineBuilder {
    private final StateMachineFactory<ExpStateEnum, ExpEventEnum> factory;
    private final StateMachinePersister<ExpStateEnum, ExpEventEnum, String> persister;
    private final ThreadLocal<SoftReference<StateMachine<ExpStateEnum, ExpEventEnum>>> threadLocal;


    public StateMachineBuilder(StateMachineFactory<ExpStateEnum, ExpEventEnum> factory, StateMachinePersister<ExpStateEnum, ExpEventEnum, String> persister) {
        this.factory = factory;
        this.persister = persister;
        this.threadLocal = new ThreadLocal<>();
    }

    public StateMachine<ExpStateEnum, ExpEventEnum> build(String mid) throws Exception {
        SoftReference<StateMachine<ExpStateEnum, ExpEventEnum>> stateMachineSoftReference = threadLocal.get();
        StateMachine<ExpStateEnum, ExpEventEnum> stateMachine;
        if (stateMachineSoftReference == null ||
                (stateMachine = stateMachineSoftReference.get()) == null) {
            stateMachine = factory.getStateMachine(mid);
            stateMachineSoftReference = new SoftReference<>(stateMachine);
            threadLocal.set(stateMachineSoftReference);
        }
        // 重新加载指定状态机
        stateMachine.getStateMachineAccessor().doWithAllRegions(
                new StateMachineFunction<StateMachineAccess<ExpStateEnum, ExpEventEnum>>() {
                    @Override
                    public void apply(StateMachineAccess<ExpStateEnum, ExpEventEnum> function) {
                        function.addStateMachineInterceptor(new StateMachineInterceptorAdapter<ExpStateEnum, ExpEventEnum>() {

                        });
                    }
                }
        );
        persister.restore(stateMachine, mid);
        return stateMachine;
    }
}