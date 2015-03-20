package com.github.wangxuehui.rpc.snrpc.conf;

import java.io.Serializable;

import com.esotericsoftware.reflectasm.MethodAccess;

/**
 * @author skyim E-mail:wxh64788665@gmail.com
 * 类说明
 */
public class RpcImplementor implements Serializable{

	private static final long serialVersionUID = 6299038261897480465L;

	private Class<?> processorClass;
	
	private MethodAccess methodAccess;
	
	public RpcImplementor(){
		
	}
	public RpcImplementor(Class<?> processorClass){
		super();
		this.processorClass = processorClass;
		this.methodAccess   = MethodAccess.get(processorClass);
	}

	public Class<?> getProcessorClass() {
		return processorClass;
	}
	public void setProcessorClass(Class<?> processorClass) {
		this.processorClass = processorClass;
	}
	public MethodAccess getMethodAccess() {
		return methodAccess;
	}


	public void setMethodAccess(MethodAccess methodAccess) {
		this.methodAccess = methodAccess;
	}
	
	
}
