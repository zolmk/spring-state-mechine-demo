package com.feiyu.fsm;

import lombok.Data;
import lombok.ToString;

/**
 * 快递状态信息类
 * @author Zhuff
 */

@Data
@ToString
public class ExpStateInfo {
    private Operator operator;
    private ExpStateEnum sourceState;
    private ExpStateEnum targetState;
    private long dateTime;
    public ExpStateInfo() {
        this.dateTime = System.currentTimeMillis();
    }
    public ExpStateInfo(Operator operator, ExpStateEnum sourceState, ExpStateEnum targetState) {
        this.dateTime = System.currentTimeMillis();
        this.operator = operator;
        this.sourceState = sourceState;
        this.targetState = targetState;
    }
}
