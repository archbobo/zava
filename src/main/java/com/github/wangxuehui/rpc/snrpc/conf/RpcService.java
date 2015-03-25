package com.github.wangxuehui.rpc.snrpc.conf;
/**
 * RpcService 
 * <rpcServices>
 * 	 <rpcService name="SnRpcInterface" interface="org.stefan.snrpc.server.SnRpcInterface" overload="true">
 *		 <rpcImplementor class="org.stefan.snrpc.server.SnRpcImpl"/>
 *	 </rpcService>
 * </rpcServices>
 * @author skyim E-mail:wxh64788665@gmail.com
 * 类说明
 */
public class RpcService {

	private Class<?> typeClass; //接口类型
	private String id;
	private String name;
	private boolean overload = false;
	private RpcImplementor rpcImplementor; //具体实现类
	
	public RpcService(String id, String name){
		super();
		this.id = id;
		this.name = name;
	}

	public Class<?> getTypeClass() {
		return typeClass;
	}

	public void setTypeClass(Class<?> typeClass) {
		this.typeClass = typeClass;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isOverload() {
		return overload;
	}

	public void setOverload(boolean overload) {
		this.overload = overload;
	}

	public RpcImplementor getRpcImplementor() {
		return rpcImplementor;
	}

	public void setRpcImplementor(RpcImplementor rpcImplementor) {
		this.rpcImplementor = rpcImplementor;
	}
}
