package com.feiyu.fsm;

/**
 * 快递状态枚举
 * @author Zhuff
 */
public enum ExpStateEnum {
    /** 待揽收 */
    TO_BE_COLLECTED("待揽收"),
    /** 已取消 */
    CANCELED("已取消"),
    /** 已揽收 */
    COLLECTED("已揽收"),
    /** 运输中 */
    IN_TRANSIT("运输中"),
    /** 派送中 */
    DELIVERING("派送中"),
    /** 异常件 */
    ABNORMAL("异常件"),
    /** 已拒收 */
    REJECTED("已拒收"),
    /** 已签收 */
    SIGNED("已签收"),
    /** 已完成 */
    COMPLETED("已完成");

    private final String name;
    ExpStateEnum(String name) {
        this.name = name;
    }
}
