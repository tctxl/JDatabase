package util.exception;



/**
 * @author : Jeffrey Shey
 * Mail : shijunfan@gmail.com
 */
public class NoInstanceException extends Exception{
public NoInstanceException() {
	super("不能初始化该类！");
}
}
