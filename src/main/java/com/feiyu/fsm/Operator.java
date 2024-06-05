package com.feiyu.fsm;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 操作员类
 * @author Zhuff
 */
@Data
@AllArgsConstructor
public class Operator {
    /** 操作员 ID */
    private String oid;
    /** 操作员角色 */
    private RoleEnum role;

    public enum RoleEnum {
        /** 快递员 */
        DELIVERY,
        CUSTOMER;
        public boolean test(ExpEventEnum eventEnum, Operator createOperator, Operator curOperator) {
            return createOperator == curOperator;
        }
    }
}
