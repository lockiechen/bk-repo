package com.tencent.bkrepo.common.query.interceptor

import com.tencent.bkrepo.common.query.builder.MongoQueryBuilder
import com.tencent.bkrepo.common.query.model.Rule

/**
 *
 * @author: carrypan
 * @date: 2019/11/15
 */
interface QueryRuleInterceptor {

    fun match(rule: Rule): Boolean

    fun intercept(rule: Rule, context: MongoQueryBuilder): Rule
}
