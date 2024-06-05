package com.feiyu.fsm;


import com.feiyu.fsm.guard.RoleGuard;
import lombok.extern.java.Log;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachineContextRepository;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.persist.DefaultStateMachinePersister;
import org.springframework.statemachine.persist.RepositoryStateMachinePersist;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.feiyu.fsm.ExpStateEnum.*;

/**
 * 快递状态机配置类
 * @author Zhuff
 */
@Log
@Configuration
@EnableStateMachineFactory
public class ExpStateMachineConfiguration extends StateMachineConfigurerAdapter<ExpStateEnum, ExpEventEnum> {
    private final StateMachineListener<ExpStateEnum, ExpEventEnum> stateMachineListener;
    private final RecordStateTransitionAction stateTransitionAction;

    private final RoleGuard roleGuard;

    public ExpStateMachineConfiguration(StateMachineListener<ExpStateEnum, ExpEventEnum> stateMachineListener, RecordStateTransitionAction stateTransitionAction, RoleGuard roleGuard) {
        this.stateMachineListener = stateMachineListener;
        this.stateTransitionAction = stateTransitionAction;
        this.roleGuard = roleGuard;
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<ExpStateEnum, ExpEventEnum> config) throws Exception {
        config.withConfiguration()
                .listener(this.stateMachineListener)
                .autoStartup(false);
    }

    @Override
    public void configure(StateMachineStateConfigurer<ExpStateEnum, ExpEventEnum> states) throws Exception {
        super.configure(states);
        states.withStates()
                .initial(TO_BE_COLLECTED)
                .states(EnumSet.allOf(ExpStateEnum.class))
                .end(CANCELED)
                .end(COMPLETED)
                .end(REJECTED)
                .end(ABNORMAL);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<ExpStateEnum, ExpEventEnum> transitions) throws Exception {
        transitions
                .withExternal()
                .source(TO_BE_COLLECTED)
                .target(IN_TRANSIT)
                .event(ExpEventEnum.COLLECTED)
                .guard(this.roleGuard)
                // 状态转移成功才会执行。
                .action(this.stateTransitionAction)
                .and()
                .withExternal()
                .source(IN_TRANSIT)
                .target(COMPLETED)
                .event(ExpEventEnum.TRANSFER)
                .guard(this.roleGuard)
                .action(this.stateTransitionAction);
    }


    @Bean
    public StateMachinePersist<ExpStateEnum, ExpEventEnum, String> stateMachinePersist(StateMachineContextRepository<ExpStateEnum, ExpEventEnum, StateMachineContext<ExpStateEnum, ExpEventEnum>> repository) {
        return new RepositoryStateMachinePersist<>(repository);
    }

    @Bean
    public StateMachinePersister<ExpStateEnum, ExpEventEnum, String> defaultStateMachinePersister(StateMachinePersist<ExpStateEnum, ExpEventEnum, String> stateMachinePersist) {
        return new DefaultStateMachinePersister<>(stateMachinePersist);
    }

    /**
     * 用来记录状态转换信息
     * @author Zhuff
     */
    @Component
    protected static class RecordStateTransitionAction implements Action<ExpStateEnum, ExpEventEnum> {
        @SuppressWarnings("unchecked")
        @Override
        public void execute(StateContext<ExpStateEnum, ExpEventEnum> context) {
            Map<Object, Object> map = context.getExtendedState().getVariables();
            State<ExpStateEnum, ExpEventEnum> source = context.getTransition().getSource();
            State<ExpStateEnum, ExpEventEnum> target = context.getTransition().getTarget();
            List<ExpStateInfo> list = (List<ExpStateInfo>) map.computeIfAbsent(Constants.STR_EXP_STATE_INFO, o -> new ArrayList<>());
            ExpStateInfo info = new ExpStateInfo();
            info.setOperator((Operator) map.get(Constants.STR_OP_CUR));
            info.setSourceState(source.getId());
            info.setTargetState(target.getId());
            list.add(info);
        }
    }

    /**
     * 状态机监听器
     * @author Zhuff
     */
    @Component
    protected static class SimpleStateMachineListener extends StateMachineListenerAdapter<ExpStateEnum, ExpEventEnum> {
        @Override
        public void stateChanged(State<ExpStateEnum, ExpEventEnum> from, State<ExpStateEnum, ExpEventEnum> to) {

        }
        @Override
        public void eventNotAccepted(Message<ExpEventEnum> event) {
            log.info("Event not accepted. Event Type : " + event.getPayload());
        }
        @Override
        public void transitionEnded(Transition<ExpStateEnum, ExpEventEnum> transition) {
            State<ExpStateEnum, ExpEventEnum> source = transition.getSource();
            State<ExpStateEnum, ExpEventEnum> target = transition.getTarget();
            log.info("【State change】 from " + (source != null && source.getId() != null ? source.getId() : "null") + " to "+ target.getId()+ " .");
        }
    }

    /**
     * 用于保存 快递状态上下文
     * @author Zhuff
     */
    @Component
    @Log
    public static class InMemoryStateMachineContextRepository implements StateMachineContextRepository<ExpStateEnum, ExpEventEnum, StateMachineContext<ExpStateEnum, ExpEventEnum>> {
        /** 用ConcurrentHashMap来解决并发问题 */
        private final Map<String, StateMachineContext<ExpStateEnum, ExpEventEnum>> map = new ConcurrentHashMap<>(1000);
        @Override
        public void save(StateMachineContext<ExpStateEnum, ExpEventEnum> context, String id) {
            map.put(id, context);
        }

        @Override
        public StateMachineContext<ExpStateEnum, ExpEventEnum> getContext(String id) {
            return map.getOrDefault(id, null);
        }
    }

}