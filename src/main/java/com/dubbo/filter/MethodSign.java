package com.dubbo.filter;

import com.stupidbird.api.dubbo.bean.DubboMessage;
import org.apache.dubbo.rpc.Invocation;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author: wudong
 * @create: 2022-02-15 17:43
 **/
public class MethodSign {
    private String serviceName;
    private String methodName;
    private Class<?>[] parameterTypes;
    private String arguments;

    public MethodSign() {
    }

    public MethodSign(Invocation invocation) {
        this.serviceName = invocation.getServiceName();
        this.methodName = invocation.getMethodName();
        this.parameterTypes = invocation.getParameterTypes();
        this.arguments = Arrays.toString(invocation.getArguments());
        Object argument = invocation.getArguments()[0];
        if(argument instanceof DubboMessage){
            DubboMessage dubboMessage =  (DubboMessage) argument;
            this.arguments = dubboMessage.getType() + "";
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodSign)) return false;
        MethodSign that = (MethodSign) o;
        return Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(methodName, that.methodName) &&
                Arrays.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(serviceName, methodName);
        result = 31 * result + Arrays.hashCode(parameterTypes);
        return result;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        return "MethodSign{" +
                "serviceName='" + serviceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", parameterTypes=" + Arrays.toString(parameterTypes) +

                '}';
    }
}
