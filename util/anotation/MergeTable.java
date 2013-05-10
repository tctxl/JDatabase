package util.anotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author : Jeffrey Shey
 * Mail : shijunfan@gmail.com
 * 合并两个class为一张表时，注意不要有相同字段出现
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MergeTable{
boolean value() default true;
}
