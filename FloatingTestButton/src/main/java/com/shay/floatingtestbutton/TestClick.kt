package com.shay.floatingtestbutton

import java.lang.annotation.ElementType
import java.lang.annotation.RetentionPolicy

/**
 * PACK com.shay.floatingtestbutton
 * CREATE BY Shay
 * DATE BY 2023/2/27 16:51 星期一
 * <p>
 * DESCRIBE
 * <p>
 */
// TODO:2023/2/27 
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class TestClick()
