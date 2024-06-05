package com.feiyu.fsm.guard;

import com.feiyu.fsm.ExpEventEnum;
import com.feiyu.fsm.ExpStateEnum;
import com.feiyu.fsm.Operator;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;
import static com.feiyu.fsm.Constants.*;

/**
 *
 * @author Zhuff
 */
@Component
public class RoleGuard implements Guard<ExpStateEnum, ExpEventEnum> {
    @Override
    public boolean evaluate(StateContext context) {
        ExpEventEnum event = (ExpEventEnum) context.getEvent();
        Operator operator = (Operator) context.getExtendedState().getVariables().get(STR_OP_CUR);
        Operator createOp = (Operator) context.getExtendedState().getVariables().get(STR_OP_CREATE);
        if (operator.getRole().test(event, createOp, operator)) {
            return true;
        }
        return false;
    }
}
