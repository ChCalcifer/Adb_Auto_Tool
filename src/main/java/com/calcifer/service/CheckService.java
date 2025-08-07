package com.calcifer.service;

/**
 * @author CYC
 */
public class CheckService {
    public enum ConditionType {
        /**
         *
         */
        EQUALS,
        CONTAINS,
        MULTI_CONDITION,
        QUERY_ONLY,
        NOT_CONTAINS,
        LOG_FILTER
    }
}